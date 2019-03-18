package org.liblouis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import static java.nio.file.Files.walkFileTree;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jna.Callback;
import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.StringArray;
import com.sun.jna.FromNativeContext;
import com.sun.jna.ToNativeContext;
import com.sun.jna.TypeConverter;
import com.sun.jna.TypeMapper;

import org.slf4j.LoggerFactory;

public class Louis {
	
	private static File libraryPath = null;
	
	/**
	 * To have any effect, this method needs to be called before any other method in Louis or
	 * Translator is called. If no library path is provided, the binaries embedded inside this JAR
	 * will be used.
	 */
	public static void setLibraryPath(File path) {
		libraryPath = path;
	}
	
	private static Lou_TableResolver lou_tableResolver = null;
	private static TableResolver tableResolver = null;
	private static boolean tableResolverIsRegistered = false;
	// table names are generated for provided URLs without a name
	private final static Map<String,URL> generatedTableNames = new HashMap<String,URL>();
	// non-file URLs returned by resolver are read and stored to temporary files
	private final static Map<URL,File> tablesStoredToFile = new HashMap<URL,File>();
	private final static Map<File,URL> tablesStoredToFileInv = new HashMap<File,URL>();
	
	static synchronized String getTableNameForURL(URL table) {
		int i = 1;
		for (;;) {
			String name = "" + i;
			if (!generatedTableNames.containsKey(name)) {
				generatedTableNames.put(name, table);
				return name;
			}
			i++;
		}
	}
	
	public static synchronized void setTableResolver(final TableResolver tableResolver) {
		Louis.tableResolver = tableResolver;
		lou_tableResolver = new Lou_TableResolver() {
				public File[] invoke(String table, File base) {
					File[] ret = _invoke(table, base);
					if (ret == null)
						Louis.getLibrary()._lou_logMessage(Logger.Level.ERROR.value(), "Cannot resolve table '%s'", table);
					return ret;
				}
				private File[] _invoke(String table, File base) {
					URL baseURL;
					if (base == null)
						baseURL = null;
					else {
						try {
							base = base.getCanonicalFile(); }
						catch (IOException e) {
							throw new RuntimeException(e); }
						baseURL = tablesStoredToFileInv.get(base);
						if (baseURL == null)
							baseURL = asURL(base);
					}
					URL tableURL;
					if (base == null && generatedTableNames.containsKey(table)) {
						tableURL = generatedTableNames.get(table);
					} else {
						tableURL = tableResolver.resolve(table, baseURL);
						if (tableURL == null)
							return null; // table cannot be resolved
					}
					File tableFile;
					if (tablesStoredToFile.containsKey(tableURL)) {
						tableFile = tablesStoredToFile.get(tableURL);
					} else {
						if (tableURL.toString().startsWith("file:")) {
							tableFile = asFile(tableURL);
							if (!tableFile.exists())
								return null; // table cannot be resolved
						} else {
							// save to temporary file
							InputStream in = null;
							try {
								in = tableURL.openStream();
								tableFile = File.createTempFile("liblouis-java-", ".tbl");
								tableFile.delete();
								Files.copy(in, tableFile.toPath());
								tableFile.deleteOnExit();
							} catch (IOException e) {
								return null; // table cannot be resolved
							} finally {
								if (in != null) try { in.close(); } catch (IOException e) {}
							}
							try {
								tableFile = tableFile.getCanonicalFile(); }
							catch (IOException e) {
								throw new RuntimeException(e); }
							tablesStoredToFile.put(tableURL, tableFile);
							tablesStoredToFileInv.put(tableFile, tableURL);
						}
					}
					return new File[]{tableFile};
				}
			};
		tableResolverIsRegistered = false;
	}
	
	private static Lou_LogCallback lou_logCallback = null;
	private static boolean loggerIsRegistered = false;
	
	public static synchronized void setLogger(final Logger logger) {
		lou_logCallback = new Lou_LogCallback() {
			public void invoke(int level, String message) {
				logger.log(Logger.Level.from(level), message);
			}
		};
		loggerIsRegistered = false;
	}
	
	public static void setLogLevel(Logger.Level level) {
		getLibrary().lou_setLogLevel(level.value());
	}
	
	/**
	 * Get the version number of Liblouis.
	 */
	public static String getVersion() {
		return getLibrary().lou_version();
	}
	
	/**
	 * Get a list of all the top-level tables.
	 */
	public static Set<Table> listTables() {
		Set<Table> tables = new HashSet<Table>();
		for (String t : getLibrary().lou_listTables())
			tables.add(new Table(t));
		return Collections.unmodifiableSet(tables);
	}
	
	private static LouisLibrary INSTANCE;
	
	static synchronized LouisLibrary getLibrary() {
		if (INSTANCE == null) {
			// look for binaries inside this JAR first (by default this is done only as a last resort in JNA)
			if (libraryPath == null) {
				try {
					libraryPath = Native.extractFromResourcePath(
						Platform.isWindows() ? "liblouis" : "louis", // otherwise we have to rename the DLL files which
						                                             // is not so easy to do in Maven
						Louis.class.getClassLoader()); }
				catch (IOException e) {}
			}
			try {
				String name = (libraryPath != null) ? libraryPath.getCanonicalPath() : "louis";
				LouisLibrary unsynced = (LouisLibrary)Native.loadLibrary(name, LouisLibrary.class);
				INSTANCE = (LouisLibrary)Native.synchronizedLibrary(unsynced);
				logger.debug("Loaded " + name + ": Liblouis v" + INSTANCE.lou_version());
			} catch (IOException e) {
				throw new RuntimeException("Could not load liblouis", e);
			} finally {
				// delete the binary if it was extracted by JNA
				if (libraryPath != null && libraryPath.getName().startsWith("jna"))
					if (!libraryPath.delete())
						// mark for later removal by JNA
						try {
							new File(libraryPath.getParentFile(), libraryPath.getName() + ".x").createNewFile(); }
						catch (IOException e) { /* ignore */ }
			}
		}
		if (lou_tableResolver == null) {
			// default table resolver implementation that looks for tables inside this JAR and falls back to the file system
			setTableResolver(new TableResolver() {
					private final Map<String,URL> tables;
					private final Set<String> tablePaths; {
						tables = new HashMap<String,URL>();
						for (String table : listResources("org/liblouis/resource-files/tables"))
							tables.put(table, Louis.class.getResource("resource-files/tables/" + table));
						tablePaths = Collections.unmodifiableSet(tables.keySet());
						logger.debug("Using default tables: " + tablePaths);
					}
					private final Map<String,URL> aggregatorTables = new HashMap<String,URL>();
					public URL resolve(String table, URL base) {
						// if we are resolving an include rule from a generated aggregator table, resolve without base
						if (aggregatorTables.containsValue(base))
							base = null;
						if (base == null || tables.containsValue(base)) {
							if (tables.containsKey(table))
								return tables.get(table);
						}
						// if it is a comma separated table list, create a single file that includes all the sub-tables
						if (base == null && table.contains(",")) {
							if (aggregatorTables.containsKey(table))
								return aggregatorTables.get(table);
							StringBuilder b = new StringBuilder();
							for (String s : table.split(","))
								b.append("include ").append(s.replaceAll("\\\\", "\\\\\\\\")).append('\n');
							InputStream in = new ByteArrayInputStream(b.toString().getBytes(StandardCharsets.UTF_8));
							try {
								File f = File.createTempFile("liblouis-java-", ".tbl");
								f.delete();
								Files.copy(in, f.toPath());
								f.deleteOnExit();
								URL u = asURL(f);
								aggregatorTables.put(table, u);
								return u;
							} catch (IOException e) {
								throw new RuntimeException(e); // should not happen
							}
						}
						// try file system
						if (base != null && base.toString().startsWith("file:")) {
							File f = new File(asFile(base), table);
							if (f.exists())
								return asURL(f);
						} else {
							File f = new File(table);
							if (f.exists())
								return asURL(f);
						}
						return null; // table cannot be resolved
					}
					public Set<String> list() {
						return tablePaths;
					}
				}
			);
		}
		if (!tableResolverIsRegistered) {
			INSTANCE.lou_registerTableResolver(lou_tableResolver);
			tableResolverIsRegistered = true;
			Set<String> allFiles = tableResolver.list();
			// only needed for Translator.find() but we do it anyway
			INSTANCE.lou_indexTables(allFiles.toArray(new String[allFiles.size()]));
		}
		if (!loggerIsRegistered && lou_logCallback != null) {
			INSTANCE.lou_registerLogCallback(lou_logCallback);
			loggerIsRegistered = true;
		}
		return INSTANCE;
	}
	
	interface LouisLibrary extends Library {
		
		public int lou_translate(String tableList, WideString inbuf, IntByReference inlen,
				WideString outbuf, IntByReference outlen, short[] typeform, byte[] spacing,
				int[] outputPos, int[] inputPos, IntByReference cursorPos, int mode);
		
		public int lou_backTranslate(String tableList, WideString inbuf, IntByReference inlen,
				WideString outbuf, IntByReference outlen, short[] typeform, byte[] spacing,
				int[] outputPos, int[] inputPos, IntByReference cursorPos, int mode);
		
		public int lou_hyphenate(String tableList, WideString inbuf, int inlen, byte[] hyphens, int mode);
		
		public int lou_dotsToChar(String tableList, WideString inbuf, WideString outbuf, int length, int mode);
		
		public int lou_charSize();
		
		public String lou_version();
		
		public Pointer lou_getTable(String tableList);
		
		/**
		 * Note that keeping resolver from being garbage collection is the
		 * responsibility of the caller.
		 */
		public void lou_registerTableResolver(Lou_TableResolver resolver);
		
		/**
		 * Note that keeping logger from being garbage collection is the
		 * responsibility of the caller.
		 */
		public void lou_registerLogCallback(Lou_LogCallback logger);
		
		public void lou_setLogLevel(int level);
		
		public int lou_indexTables(String[] tables);
		
		public String lou_findTable(String query);
		
		public String lou_getTableInfo(String table, String key);
		
		public String[] lou_listTables();
		
		public void _lou_logMessage(int level, String... format);
		
	}
	
	interface Lou_TableResolver extends Callback {
		
		public File[] invoke(String tableList, File base);
		
		public TypeMapper TYPE_MAPPER = FileTypeMapper.INSTANCE;
		
	}
	
	interface Lou_LogCallback extends Callback {
		
		public void invoke(int level, String message);
		
	}
	
	static class FileTypeMapper extends DefaultTypeMapper {
		
		static final FileTypeMapper INSTANCE = new FileTypeMapper();
		
		private FileTypeMapper() {
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
	
	private static File asFile(URL url) throws IllegalArgumentException {
		try {
			if (!"file".equals(url.getProtocol()))
				throw new RuntimeException("expected file URL");
			return new File(new URI("file", url.getPath(), null));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e); // should not happen
		}
	}
	
	@SuppressWarnings("deprecation")
	static URL asURL(File file) {
		try {
			file = file.getCanonicalFile();
			return new URL(URLDecoder.decode(file.toURI().toString().replace("+", "%2B")));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e); // should not happen
		} catch (IOException e) {
			throw new RuntimeException(e); // should not happen
		}
	}
	
	private static Iterable<String> listResources(final String directory) {
		File jarFile = asFile(Louis.class.getProtectionDomain().getCodeSource().getLocation());
		if (!jarFile.exists())
			throw new RuntimeException();
		else if (jarFile.isDirectory()) {
			File d = new File(jarFile, directory);
			if (!d.exists())
				throw new RuntimeException("directory does not exist");
			else if (!d.isDirectory())
				throw new RuntimeException("is not a directory");
			else {
				List<String> resources = new ArrayList<String>();
				for (File f : d.listFiles())
					resources.add(f.getName() + (f.isDirectory() ? "/" : ""));
				return resources; }}
		else {
			FileSystem fs; {
				try {
					fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toURI()),
					                               Collections.<String,Object>emptyMap()); }
				catch (IOException e) {
					throw new RuntimeException(e); }}
			try {
				Path d = fs.getPath("/" + directory);
				BasicFileAttributes a; {
					try {
						a = Files.getFileAttributeView(d, BasicFileAttributeView.class).readAttributes(); }
					catch (NoSuchFileException e) {
						throw new RuntimeException("directory does not exist"); }
					catch (FileSystemNotFoundException e) {
						throw new RuntimeException(e); }
					catch (IOException e) {
						throw new RuntimeException(e); }}
				if (!a.isDirectory())
					throw new RuntimeException("is not a directory");
				final List<String> resources = new ArrayList<String>();
				try {
					walkFileTree(d, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
							public FileVisitResult visitFile(Path f, BasicFileAttributes _) throws IOException {
								resources.add(""+f.getFileName());
								return FileVisitResult.CONTINUE; }}); }
				catch (NoSuchFileException e) {
					throw new RuntimeException(e); }
				catch (IOException e) {
					throw new RuntimeException(e); }
				return resources; }
			finally {
				try {
					fs.close(); }
				catch (IOException e) {
					throw new RuntimeException(e); }
			}
		}
	}
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Louis.class);
	
}
