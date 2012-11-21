package org.liblouis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.liblouis.Louis.LouisLibrary;

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
		
		WideString inbuf = getBuffer("in", text.length()).write(text);
		WideString outbuf = getBuffer("out", text.length() * OUTLEN_MULTIPLIER);
		IntByReference inlen = new IntByReference(text.length());
		IntByReference outlen = new IntByReference(outbuf.length());
		
		byte[] inputHyphens = null;
		byte[] outputHyphens = null;
		
		if (hyphenate || hyphens != null) {
			if (hyphens == null)
				inputHyphens = hyphenate(inbuf, text.length());
			else {
				if (hyphens.length != text.length() - 1)
					throw new RuntimeException("hyphens must be equal to text length minus 1.");
				inputHyphens = writeHyphens(hyphens, getHyphenBuffer("in", text.length())); }
			outputHyphens = getHyphenBuffer("out", text.length() * OUTLEN_MULTIPLIER); }
		
		if (typeform != null) {
			if (typeform.length != text.length())
				throw new RuntimeException("typeform length must be equal to text length.");
			typeform = Arrays.copyOf(typeform, outbuf.length()); }
		
		if (Louis.getLibrary().lou_translatePrehyphenated(tables, inbuf, inlen, outbuf, outlen, typeform,
				null, null, null, null, inputHyphens, outputHyphens, 0) == 0)
			throw new RuntimeException("Unable to complete translation");
		
		return new TranslationResult(outbuf, outlen, outputHyphens);
	}
	
	/**
	 * @param text The text to hyphenate. Can be multiple words.
	 */
	public boolean[] hyphenate(String text) {
		WideString inbuf = getBuffer("in", text.length()).write(text);
		return readHyphens(new boolean[text.length() - 1], hyphenate(inbuf, text.length()));
	}
	
	private byte[] hyphenate(WideString inbuf, int inlen) {
		
		byte[] hyphens = getHyphenBuffer("in", inlen);
		for (int i = 0; i < inlen; i++) hyphens[i] = '0';
		String text = inbuf.read(inlen);
		
		// lou_translate handles single words only
		Matcher matcher = Pattern.compile("\\p{L}+").matcher(text);
		byte[] wordHyphens = getHyphenBuffer("word", inlen);
		LouisLibrary louis = Louis.getLibrary();
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			if (louis.lou_hyphenate(tables, inbuf.substring(start), end - start, wordHyphens, 0) == 0)
				throw new RuntimeException("Unable to complete hyphenation");
			for (int i = 0; i < end - start; i++) hyphens[start + i] = wordHyphens[i]; }
		
		// Add hyphen points after hard hyphens
		matcher = Pattern.compile("[\\p{L}\\p{N}]-(?=[\\p{L}\\p{N}])").matcher(text);
		while (matcher.find())
			hyphens[matcher.start() + 2] = '1';
		return hyphens;
	}
	
	/*
	 * Number by which the input length should be multiplied to calculate
	 * the maximum output length. This default will handle the case where
	 * every input character is undefined in the translation table.
	 */
	private static final int OUTLEN_MULTIPLIER = WideChar.Constants.CHARSIZE * 2 + 4;
	
	private static Map<String,WideString> BUFFERS = new HashMap<String,WideString>();
	private static Map<String,byte[]> HYPHEN_BUFFERS = new HashMap<String,byte[]>();
	
	private static WideString getBuffer(String id, int minCapacity) {
		WideString buffer = BUFFERS.get(id);
		if (buffer == null || buffer.length() < minCapacity) {
			buffer = new WideString(minCapacity * 2);
			BUFFERS.put(id, buffer); }
		return buffer;
	}
		
	private static byte[] getHyphenBuffer(String id, int minCapacity) {
		byte[] buffer = HYPHEN_BUFFERS.get(id);
		if (buffer == null || buffer.length < minCapacity) {
			buffer = new byte[minCapacity * 2]; }
		return buffer;
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
}
