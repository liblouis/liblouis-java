package org.liblouis;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import static org.liblouis.Louis.asURL;

public class FindTranslatorTest {
	
	@Test(expected=CompilationException.class)
	public void testInvalidQuery() throws Exception {
		Translator.find("locale: foo");
	}
	
	@Test(expected=CompilationException.class)
	public void testNoMatchFound() throws Exception {
		Translator.find("locale:fu");
	}
	
	@Test
	public void testMatchFound() throws Exception {
		assertEquals(
			"foobar",
			Translator.find("locale:foo").translate("foobar", null, null, null).getBraille());
	}
	
	public FindTranslatorTest() {
		File testRootDir = new File(this.getClass().getResource("/").getPath());
		final Set<String> tables = new HashSet<String>();
		for (File f : new File(testRootDir, "tables").listFiles())
			tables.add(f.getAbsolutePath());
		Louis.setTableResolver(new TableResolver() {
				public URL resolve(String table, URL base) {
					if (table == null)
						return null;
					File tableFile = new File(table);
					if (tableFile.exists())
						return asURL(tableFile);
					return null;
				}
				public Set<String> list() {
					return tables;
				}
			}
		);
	}
}
