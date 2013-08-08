package org.liblouis;

import com.sun.jna.ptr.IntByReference;

public class TranslationResult {
	
	private String braille = null;
	private byte[] hyphenPositions = null;
	
	public TranslationResult(
			WideString outbuf,
			IntByReference outlen,
			byte[] outputHyphens) {
		
		this.braille = outbuf.read(outlen.getValue());
		if (outputHyphens != null) {
			this.hyphenPositions = new byte[outlen.getValue()-1];
			for (int i=0; i<hyphenPositions.length; i++)
				hyphenPositions[i] = (byte)(outputHyphens[i+1] - 48); }
	}
	
	public String getBraille() {
		return braille;
		}

	public byte[] getHyphenPositions() {
		return hyphenPositions;
	}
}
