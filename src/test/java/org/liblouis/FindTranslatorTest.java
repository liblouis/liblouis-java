package org.liblouis;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.asFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import org.apache.commons.io.FileUtils;

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
		File testRootDir = new File(this.getClass().getResource("/").getPath());
		Louis.setLibraryPath(((Collection<File>)FileUtils.listFiles(
				new File(testRootDir, "../dependency"),
				asFileFilter(new FilenameFilter() {
					public boolean accept(File dir, String fileName) {
						return dir.getName().equals("shared") && fileName.startsWith("liblouis"); }}),
				trueFileFilter())).iterator().next());
		File[] tables = new File(testRootDir, "tables").listFiles();
		String[] tableNames = new String[tables.length];
		for (int i = 0; i < tableNames.length; i++)
			tableNames[i] = tables[i].getAbsolutePath();
		Louis.getLibrary().lou_indexTables(tableNames);
	}
}
