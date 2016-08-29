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
	
	@SuppressWarnings("unchecked")
	public TableResolverTest() {
		Helper.setLibraryPath();
		final File testRootDir = new File(this.getClass().getResource("/").getPath());
		Louis.getLibrary().lou_registerTableResolver(
			new TableResolver() {
				public File[] invoke(String table, File base) {
					if (table == null)
						return null;
					File tableFile = new File(testRootDir, table);
					if (tableFile.exists())
						return new File[]{tableFile};
					if (table.equals("<FOOBAR>"))
						return invoke("tables/foobar.cti", null);
					return null;
				}
			}
		);
	}
}
