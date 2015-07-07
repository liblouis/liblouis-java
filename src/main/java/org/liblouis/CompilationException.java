package org.liblouis;

@SuppressWarnings("serial")
public class CompilationException extends Exception {
	
	public CompilationException(String message) {
		super(message);
	}
	
	public CompilationException(Throwable throwable) {
		super(throwable);
	}
	
	public CompilationException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
