package org.liblouis;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class Utilities {
	
	public static class Environment {
		
		public static void setLouisTablePath(String value) {
			if (libc instanceof UnixCLibrary) {
				((UnixCLibrary)libc).setenv("LOUIS_TABLEPATH", value, 1); return;
			} else {
				((WindowsCLibrary)libc)._putenv("LOUIS_TABLEPATH=");
				((WindowsCLibrary)libc)._putenv("LOUIS_TABLEPATH=" + value);
			}
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
