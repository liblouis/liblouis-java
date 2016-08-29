package org.liblouis;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TableResolverTest {
	
	@Test
	public void testMagicTokenTable() throws Exception {
		Translator translator = new Translator("<FOOBAR>");
		assertEquals(
			"foobar",
			translator.translate("foobar", null, null, null).getBraille());
	}
	
	@Test
	public void testIncludeMagicTokenTable() throws Exception {
		Translator translator = new Translator("tables/include_magic_token");
		assertEquals(
			"foobar",
			translator.translate("foobar", null, null, null).getBraille());
	}
	
	@Test
	public void testDeepIncludes() throws Exception {
		new Translator("tables/1");
		new Translator("tables/2");
		new Translator("tables/3");
		new Translator("tables/4");
		new Translator("tables/5");
		new Translator("tables/6");
	}
	
	final TableResolver resolver;
	
	@SuppressWarnings("unchecked")
	public TableResolverTest() {
		Helper.setLibraryPath();
		final File testRootDir = new File(this.getClass().getResource("/").getPath());
		resolver = new TableResolver() {
			public File[] invoke(String table, File base) {
				if (table == null)
					return null;
				File tableFile = new File(base != null ? base.getParentFile() : testRootDir, table);
				if (tableFile.exists())
					return new File[]{tableFile};
				if (table.equals("<FOOBAR>"))
					return invoke("tables/foobar.cti", null);
				return null;
			}
		};
		Louis.getLibrary().lou_registerTableResolver(resolver);
	}
}
