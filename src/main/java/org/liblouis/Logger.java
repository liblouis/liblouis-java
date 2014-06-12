package org.liblouis;

import com.sun.jna.Callback;

public interface Logger extends Callback {
	
	public static abstract class Level {
		public static final int ALL = -2147483648;
		public static final int DEBUG = 10000;
		public static final int INFO = 20000;
		public static final int WARN = 30000;
		public static final int ERROR = 40000;
		public static final int FATAL = 50000;
		public static final int OFF = 2147483647;
	}
	
	public void invoke(int level, String message);
	
}
