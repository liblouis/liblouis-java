package org.liblouis;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.asFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import org.apache.commons.io.FileUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import static org.liblouis.Logger.Level.DEBUG;
import static org.liblouis.Logger.Level.INFO;
import static org.liblouis.Logger.Level.WARN;
import static org.liblouis.Logger.Level.ERROR;
import static org.liblouis.Logger.Level.FATAL;

public class LoggerTest {
	
	@Test
	public void testLogger() {
		logger.reset();
		try { new Translator("unexisting_file"); }
		catch (CompilationException e) {}
		assertEquals("[ERROR] Cannot resolve table 'unexisting_file'" + "\n" +
		             "[ERROR] 1 errors found."                        + "\n" +
		             "[ERROR] unexisting_file could not be found"     + "\n",
		             logger.toString());
		logger.reset();
		Louis.getLibrary().lou_setLogLevel(FATAL);
		try { new Translator("unexisting_file"); }
		catch (CompilationException e) {}
		assertEquals("", logger.toString());
	}
	
	private final ByteArrayLogger logger;
	
	@SuppressWarnings("unchecked")
	public LoggerTest() {
		final File testRootDir = new File(this.getClass().getResource("/").getPath());
		Louis.setLibraryPath(((Collection<File>)FileUtils.listFiles(
				new File(testRootDir, "../dependency"),
				asFileFilter(new FilenameFilter() {
					public boolean accept(File dir, String fileName) {
						return dir.getName().equals("shared") && fileName.startsWith("liblouis"); }}),
				trueFileFilter())).iterator().next());
		logger = new ByteArrayLogger() {
			public String format(int level, String message) {
				switch (level) {
				case DEBUG: return "[DEBUG] " + message;
				case INFO: return "[INFO] " + message;
				case WARN: return "[WARN] " + message;
				case ERROR: return "[ERROR] " + message;
				case FATAL: return "[FATAL] " + message; }
				return null;
			}
		};
		Louis.getLibrary().lou_registerLogCallback(logger);
	}
	
	private abstract class ByteArrayLogger implements Logger {
		private ByteArrayOutputStream stream = new ByteArrayOutputStream();
		private PrintStream printStream = new PrintStream(stream);
		public abstract String format(int level, String message);
		public void invoke(int level, String message) {
			String formattedMessage = format(level, message);
			if (formattedMessage != null)
				printStream.println(formattedMessage);
		}
		public void reset() {
			stream.reset();
		}
		public String toString() {
			return stream.toString();
		}
	}
}
