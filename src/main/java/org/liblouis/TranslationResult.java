package org.liblouis;

import com.sun.jna.ptr.IntByReference;

public class TranslationResult {
	
	private String braille = null;
	private boolean[] hyphenPositions = null;
	
	public TranslationResult(
			WideString outbuf,
			IntByReference outlen,
			byte[] outputHyphens) {
		
		this.braille = outbuf.read(outlen.getValue());
		if (outputHyphens != null) {
			this.hyphenPositions = new boolean[outlen.getValue()-1];
			for (int i=0; i<hyphenPositions.length; i++)
				hyphenPositions[i] = (outputHyphens[i+1] == '1'); }
	}
	
	public String getBraille() {
		return braille;
		}

	public boolean[] getHyphenPositions() {
		return hyphenPositions;
	}
}