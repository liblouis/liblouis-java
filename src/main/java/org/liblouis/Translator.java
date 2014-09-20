package org.liblouis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.liblouis.Louis.LouisLibrary;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class Translator {
	
	public static final byte SHY = 1;
	public static final byte ZWSP = 2;
	
	private final String tables;
	
	/**
	 * @param tables The translation table or table list to compile.
	 * @throws CompilationException
	 */
	public Translator(String tables) throws CompilationException {
		if (Louis.getLibrary().lou_getTable(tables) == Pointer.NULL)
			throw new CompilationException("Unable to compile table '" + tables + "'");
		this.tables = tables;
	}
	
	/**
	 * @param text The text to translate.
	 * @param hyphenPositions The hyphenation points of the input. Possible values are `0` (no hyphen),
	 *        `1` (SHY, soft hyphen) or `2` (ZWSP, zero-width space). Length must be equal to
	 *        the <code>text</code> length minus 1.
	 * @param typeform The typeform array. Must have the same length as <code>text</code>.
	 * @return A TranslationResult containing the braille translation and, if
	 *         <code>hyphenPositions</code> was not <code>null</code>, the output hyphen points.
	 * @throws TranslationException
	 */
	public TranslationResult translate(String text, byte[] hyphenPositions, byte[] typeform)
			throws TranslationException {
		
		WideString inbuf = getBuffer("in", text.length()).write(text);
		WideString outbuf = getBuffer("out", text.length() * OUTLEN_MULTIPLIER);
		IntByReference inlen = new IntByReference(text.length());
		IntByReference outlen = new IntByReference(outbuf.length());
		
		byte[] inputHyphens = null;
		byte[] outputHyphens = null;
		
		if (hyphenPositions != null) {
			if (hyphenPositions.length != text.length() - 1)
				throw new RuntimeException("length of hyphenPositions must be equal to text length minus 1.");
			inputHyphens = writeHyphens(hyphenPositions, getHyphenBuffer("in", text.length()));
			outputHyphens = getHyphenBuffer("out", text.length() * OUTLEN_MULTIPLIER); }
		
		if (typeform != null) {
			if (typeform.length != text.length())
				throw new RuntimeException("typeform length must be equal to text length.");
			typeform = Arrays.copyOf(typeform, outbuf.length()); }
		
		if (Louis.getLibrary().lou_translatePrehyphenated(tables, inbuf, inlen, outbuf, outlen, typeform,
				null, null, null, null, inputHyphens, outputHyphens, 0) == 0)
			throw new TranslationException("Unable to complete translation");
		
		return new TranslationResult(outbuf, outlen, outputHyphens);
	}
	
	/**
	 * @param text The text to hyphenate. Can be multiple words.
	 * @return The hyphenation points. Possible values are `0` for no hyphenation point, `1` for a
	 *         hyphenation point (soft hyphen), or `2` for a zero-width space (which are inserted
	 *         after hard hyphens). Length is equal to the <code>text</code> length minus 1.
	 */
	public byte[] hyphenate(String text) throws TranslationException {
		WideString inbuf = getBuffer("in", text.length()).write(text);
		int inlen = text.length();
		byte[] hyphens = getHyphenBuffer("out", inlen);
		for (int i = 0; i < inlen; i++) hyphens[i] = '0';
		
		// lou_translate handles single words only
		Matcher matcher = Pattern.compile("\\p{L}+").matcher(text);
		byte[] wordHyphens = getHyphenBuffer("word", inlen);
		LouisLibrary louis = Louis.getLibrary();
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			if (louis.lou_hyphenate(tables, inbuf.substring(start), end - start, wordHyphens, 0) == 0)
				throw new TranslationException("Unable to complete hyphenation");
			for (int i = 0; i < end - start; i++) hyphens[start + i] = wordHyphens[i]; }
		
		byte[] hyphenPositions = readHyphens(new byte[text.length() - 1], hyphens);
		
		// add a zero-width space after hard hyphens
		matcher = Pattern.compile("[\\p{L}\\p{N}]-(?=[\\p{L}\\p{N}])").matcher(text);
		while (matcher.find())
			hyphenPositions[matcher.start() + 1] = ZWSP;
		return hyphenPositions;
	}
	
	public String display(String braille) throws TranslationException {
		WideString inbuf = getBuffer("in", braille.length()).write(braille);
		int length = braille.length();
		WideString outbuf = getBuffer("out", braille.length() * OUTLEN_MULTIPLIER);
		if (Louis.getLibrary().lou_dotsToChar(tables, inbuf, outbuf, length, 0) == 0)
			throw new TranslationException("Unable to complete translation");
		return outbuf.read(length);
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
	
	/*
	 * Convert a hyphen array from the form [0,1,0] to the form ['0','0','1','0']
	 */
	private static byte[] writeHyphens(byte[] hyphenPositions, byte[] buffer) {
		buffer[0] = '0';
		for (int i = 0; i < hyphenPositions.length; i++)
			buffer[i+1] = (byte)(hyphenPositions[i] + 48);
		return buffer;
	}
	
	/*
	 * Convert a hyphen array from the form ['0','0','1','0'] to the form [0,1,0]
	 */
	private static byte[] readHyphens(byte[] hyphenPositions, byte[] buffer) {
		for (int i = 0; i < hyphenPositions.length; i++)
			hyphenPositions[i] = (byte)(buffer[i+1] - 48);
		return hyphenPositions;
	}
}
