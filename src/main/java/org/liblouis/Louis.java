package org.liblouis;

import java.io.File;
import java.io.IOException;

import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.StringArray;
import com.sun.jna.FromNativeContext;
import com.sun.jna.ToNativeContext;
import com.sun.jna.TypeConverter;

public class Louis {
	
	private static File libraryPath = null;
	
	public static void setLibraryPath(File path) {
		libraryPath = path;
	}
	
	private static LouisLibrary INSTANCE;
	
	public static LouisLibrary getLibrary() {
		if (INSTANCE == null) {
			try {
				String name = (libraryPath != null) ? libraryPath.getCanonicalPath() : "louis";
				LouisLibrary unsynced = (LouisLibrary)Native.loadLibrary(name, LouisLibrary.class);
				INSTANCE = (LouisLibrary)Native.synchronizedLibrary(unsynced); }
			catch (IOException e) {
				throw new RuntimeException("Could not load liblouis", e); }}
		return INSTANCE;
	}
	
	public interface LouisLibrary extends Library {
		
		public int lou_translate(String tableList, WideString inbuf, IntByReference inlen,
				WideString outbuf, IntByReference outlen, short[] typeform, byte[] spacing,
				int[] outputPos, int[] inputPos, IntByReference cursorPos, int mode);
		
		public int lou_backTranslate(String tableList, WideString inbuf, IntByReference inlen,
				WideString outbuf, IntByReference outlen, short[] typeform, byte[] spacing,
				int[] outputPos, int[] inputPos, IntByReference cursorPos, int mode);
		
		public int lou_hyphenate(String tableList, WideString inbuf, int inlen, byte[] hyphens, int mode);
		
		public int lou_dotsToChar(String tableList, WideString inbuf, WideString outbuf, int length, int mode);
		
		public int lou_charSize();
		
		public int lou_free();
		
		public String lou_version();
		
		public Pointer lou_getTable(String tableList);
		
		public void lou_registerTableResolver(TableResolver resolver);
		
		public void lou_registerLogCallback(Logger logger);
		
		public void lou_setLogLevel(int level);
		
		public int lou_indexTables(String[] tables);
		
		public String lou_findTable(String query);
		
	}
	
	public static class TypeMapper extends DefaultTypeMapper {
		
		public static final TypeMapper INSTANCE = new TypeMapper();
		
		protected TypeMapper() {
			TypeConverter converter = new TypeConverter() {
				public Class<?> nativeType() {
					return String.class;
				}
				public Object toNative(Object file, ToNativeContext context) {
					if (file == null)
						return null;
					if (file instanceof File[]) {
						File[] files = ((File[])file);
						String[] paths = new String[files.length];
						for (int i = 0; i < files.length; i++)
							paths[i] = (String)toNative(files[i], context);
						return new StringArray(paths); }
					try { return ((File)file).getCanonicalPath(); }
					catch (IOException e) { return null; }
				}
				public Object fromNative(Object file, FromNativeContext context) {
					if (file == null)
						return null;
					return new File(((Pointer)file).getString(0));
				}
			};
			addToNativeConverter(File.class, converter);
			addToNativeConverter(File[].class, converter);
			addFromNativeConverter(File.class, converter);
		}
	}
}
