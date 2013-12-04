package org.liblouis;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TableResolverTest {
	
	@Test
	public void testMagicTokenTable() {
		Translator translator = new Translator("<FOOBAR>");
		assertEquals(
			"foobar",
			translator.translate("foobar", null, null).getBraille());
	}
	
	@Test
	public void testIncludeMagicTokenTable() {
		Translator translator = new Translator("tables/include_magic_token");
		assertEquals(
			"foobar",
			translator.translate("foobar", null, null).getBraille());
	}
	
	private final File testRootDir = new File(this.getClass().getResource("/").getPath());
	
	@Before
	public void registerMagicTokenResolver() {
		Louis.getLibrary().lou_registerTableResolver(
			new TableResolver() {
				public String invoke(String table, String base) {
					if (table == null)
						return null;
					File tableFile = new File(testRootDir, table);
					if (tableFile.exists())
						return tableFile.getAbsolutePath();
					if (table.equals("<FOOBAR>"))
						return invoke("tables/foobar.cti", null);
					return null;
				}
			}
		);
	}
}
