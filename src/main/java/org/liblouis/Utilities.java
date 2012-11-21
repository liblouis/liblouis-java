package org.liblouis;

import java.util.Arrays;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class Utilities {
	
	public static class Pair<T1,T2> {
		public final T1 _1;
		public final T2 _2;
		public Pair(T1 _1, T2 _2) {
			this._1 = _1;
			this._2 = _2;
		}
	}
	
	public static class Hyphenation {
		
		public static Pair<String,boolean[]> extractHyphens(String string, char hyphen) {
			StringBuffer unhyphenatedString = new StringBuffer();
			boolean[] hyphens = new boolean[string.length()/2];
			int j = 0;
			boolean seenHyphen = false;
			for (int i = 0; i < string.length(); i++) {
				char c = string.charAt(i);
				if (c == hyphen)
					seenHyphen = true;
				else
					unhyphenatedString.append(c);
					hyphens[j++] = seenHyphen;
					seenHyphen = false; }
			return new Pair<String,boolean[]>(unhyphenatedString.toString(), Arrays.copyOf(hyphens, j-1));
		}
		
		public static String insertHyphens(String string, boolean hyphens[], char hyphen) {
			if (string.equals("")) return "";
			if (hyphens.length != string.length()-1)
				throw new RuntimeException("hyphens.length must be equal to string.length() - 1");
			StringBuffer hyphenatedString = new StringBuffer();
			int i; for (i = 0; i < hyphens.length; i++) {
				hyphenatedString.append(string.charAt(i));
				if (hyphens[i])
					hyphenatedString.append(hyphen); }
			hyphenatedString.append(string.charAt(i));
			return hyphenatedString.toString();
		}
	}
	
	public static class Environment {
		
		public static void setLouisTablePath(String value) {
			if (libc instanceof UnixCLibrary)
				((UnixCLibrary)libc).setenv("LOUIS_TABLEPATH", value, 1);
			else {
				((WindowsCLibrary)libc)._putenv("LOUIS_TABLEPATH=");
				((WindowsCLibrary)libc)._putenv("LOUIS_TABLEPATH=" + value); }
		}
	}
	
	private static Object libc;
	
	static {
		switch (Platform.getOSType()) {
			case Platform.MAC:
			case Platform.LINUX:
				libc = Native.loadLibrary("c", UnixCLibrary.class);
				break;
			case Platform.WINDOWS:
				libc = Native.loadLibrary("msvcrt", WindowsCLibrary.class);
				break;
		}
	}
	
	public interface UnixCLibrary extends Library {
		public int setenv(String name, String value, int overwrite);
	}
	
	public interface WindowsCLibrary extends Library {
		public int _putenv(String string);
	}
}
