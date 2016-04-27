package org.liblouis;

import com.sun.jna.ptr.IntByReference;

public class TranslationResult {
	
	private String braille = null;
	private int[] characterAttributes = null;
	private int[] interCharacterAttributes = null;
	
	protected TranslationResult(WideString outbuf, IntByReference outlen, int[] inputPos,
	                            int[] characterAttributes, int[] interCharacterAttributes) {
		int len = outlen.getValue();
		this.braille = outbuf.read(len);
		if (characterAttributes != null) {
			this.characterAttributes = new int[len];
			for (int outpos = 0; outpos < len; outpos++)
				this.characterAttributes[outpos] = characterAttributes[inputPos[outpos]]; }
		
		// This is more or less copied from lou_translatePrehyphenated. The difference is that here
		// we work with int arrays instead of byte arrays, and also the nil value is 0 here instead
		// of 48 (character '0').
		if (interCharacterAttributes != null) {
			this.interCharacterAttributes = new int[len - 1];
			int inpos = 0;
			for (int outpos = 1; outpos < len; outpos++) {
				int new_inpos = inputPos[outpos];
				if (new_inpos < inpos)
					throw new RuntimeException();
				if (new_inpos > inpos)
					this.interCharacterAttributes[outpos - 1] = interCharacterAttributes[new_inpos - 1];
				else
					this.interCharacterAttributes[outpos - 1] = 0;
				inpos = new_inpos; }}
	}
	
	public String getBraille() {
		return braille;
	}

	public int[] getCharacterAttributes() {
		return characterAttributes;
	}
	
	public int[] getInterCharacterAttributes() {
		return interCharacterAttributes;
	}
}
