package org.liblouis;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
	
	@SuppressWarnings("unchecked")
	public FindTranslatorTest() {
		Helper.setLibraryPath();
		File testRootDir = new File(Helper.class.getResource("/").getPath());
		File[] tables = new File(testRootDir, "tables").listFiles();
		String[] tableNames = new String[tables.length];
		for (int i = 0; i < tableNames.length; i++)
			tableNames[i] = tables[i].getAbsolutePath();
		Louis.getLibrary().lou_indexTables(tableNames);
	}
}
