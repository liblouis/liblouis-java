package org.liblouis;

import java.io.File;
import java.io.IOException;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class Louis {
	
	private static File libraryPath = null;
	
	public static void setLibraryPath(File path) {
		libraryPath = path;
	}
	
	private static LouisLibrary instance;
	
	public static LouisLibrary getLibrary() {
		if (instance == null) {
			try {
				String name = (libraryPath != null) ? libraryPath.getCanonicalPath() : "louis";
				instance = (LouisLibrary)Native.loadLibrary(name, LouisLibrary.class); }
			catch (IOException e) {
				throw new RuntimeException("Could not load liblouis", e); }}
		return instance;
	}
	
	public interface LouisLibrary extends Library {
		
		public int lou_translatePrehyphenated(String tableList, WideString inbuf, IntByReference inlen,
				WideString outbuf, IntByReference outlen, byte typeform[], byte spacing[],
				int[] outputPos, int[] inputPos, IntByReference cursorPos,
				byte[] inputHyphens, byte[] outputHyphens, int mode);
		
		public int lou_hyphenate(String tableList, WideString inbuf, int inlen, byte[] hyphens, int mode);
		
		public int lou_charSize();
		
		public int lou_free();
		
		public String lou_version();
		
		public Pointer lou_getTable(String tableList);
		
	}
}
