package org.liblouis;

import java.util.Arrays;

import com.sun.jna.ptr.IntByReference;

public class Translator {
	
	private final String tables;
	
	public Translator(String tables) {
		this.tables = tables;
	}
	
	/**
	 * @param text The text to translate.
	 * @param typeform The typeform array. Must have the same length as <code>text</code>.
	 * @param hyphens The hyphenation points of the input. Length must be equal
	 *        to the <code>text</code> length minus 1.
	 * @param hyphenate Whether or not to perform hyphenation before translation.
	 *        Will be ignored if <code>hyphens</code> is not <code>null</code>.
	 */
	public TranslationResult translate(String text, byte[] typeform, boolean[] hyphens, boolean hyphenate) {
		
		Pair<WideString> buffers = getBuffers(text.length());
		WideString inbuf = buffers._1;
		WideString outbuf = buffers._2;
		IntByReference inlen = new IntByReference(text.length());
		IntByReference outlen = new IntByReference(outbuf.length());
		inbuf.write(text);
		
		byte[] inputHyphens = null;
		byte[] outputHyphens = null;
		
		if (hyphenate && hyphens == null)
			hyphens = hyphenate(inbuf, text.length());
		
		if (hyphens != null) {
			if (hyphens.length != text.length() - 1)
				throw new RuntimeException("hyphens must be equal to text length minus 1.");
			Pair<byte[]> hyphenBuffers = getHyphenBuffers(text.length());
			inputHyphens = hyphenBuffers._1;
			outputHyphens = hyphenBuffers._2;
			writeHyphens(hyphens, inputHyphens); }
		
		if (typeform != null) {
			if (typeform.length != text.length())
				throw new RuntimeException("typeform length must be equal to text length.");
			typeform = Arrays.copyOf(typeform, outbuf.length()); }
		
		if (Louis.getLibrary().lou_translatePrehyphenated(tables, inbuf, inlen, outbuf, outlen, typeform,
				null, null, null, null, inputHyphens, outputHyphens, 0) == 0)
			throw new RuntimeException("Unable to complete translation");
		
		return new TranslationResult(outbuf, outlen, outputHyphens);
	}
	
	public boolean[] hyphenate(String text) {
		WideString inbuf = getBuffers(text.length())._1;
		inbuf.write(text);
		return hyphenate(inbuf, text.length());
	}
	
	private boolean[] hyphenate(WideString inbuf, int inlen) {
		byte[] hyphens = getHyphenBuffers(inlen)._1;
		if (Louis.getLibrary().lou_hyphenate(tables, inbuf, inlen, hyphens, 0) == 0)
			throw new RuntimeException("Unable to complete hyphenation");
		return readHyphens(new boolean[inlen - 1], hyphens);
	}
	
	/*
	 * Number by which the input length should be multiplied to calculate
	 * the maximum output length. This default will handle the case where
	 * every input character is undefined in the translation table.
	 */
	private static final int OUTLEN_MULTIPLIER = WideChar.Constants.CHARSIZE * 2 + 4;
	
	private static Pair<WideString> BUFFERS = getBuffers(512);
	private static Pair<byte[]> HYPHEN_BUFFERS = getHyphenBuffers(512);
	
	private static Pair<WideString> getBuffers(int capacity) {
		if (BUFFERS == null || capacity > BUFFERS._1.length()) {
			BUFFERS = new Pair<WideString>(
					new WideString(capacity * 2 ),
					new WideString(capacity * 2 * OUTLEN_MULTIPLIER)); }
		return BUFFERS;
	}

	private static Pair<byte[]> getHyphenBuffers(int capacity) {
		if (HYPHEN_BUFFERS == null || capacity > HYPHEN_BUFFERS._1.length) {
			HYPHEN_BUFFERS = new Pair<byte[]>(
					new byte[capacity * 2],
					new byte[capacity * 2 * OUTLEN_MULTIPLIER]); }
		return HYPHEN_BUFFERS;
	}
	
	private static byte[] writeHyphens(boolean[] hyphens, byte[] buffer) {
		buffer[0] = '0';
		for (int i = 0; i < hyphens.length; i++)
			buffer[i+1] = (byte)(hyphens[i] ? '1' : '0');
		return buffer;
	}
	
	private static boolean[] readHyphens(boolean[] hyphens, byte[] buffer) {
		for (int i = 0; i < hyphens.length; i++)
			hyphens[i] = (buffer[i+1] == '1');
		return hyphens;
	}
	
	private static class Pair<T> {
		public final T _1;
		public final T _2;
		public Pair(T _1, T _2) {
			this._1 = _1;
			this._2 = _2;
		}
	}
}
