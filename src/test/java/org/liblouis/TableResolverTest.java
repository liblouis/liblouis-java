package org.liblouis;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.asFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import org.apache.commons.io.FileUtils;

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
		final File testRootDir = new File(this.getClass().getResource("/").getPath());
		Louis.setLibraryPath(((Collection<File>)FileUtils.listFiles(
				new File(testRootDir, "../dependency"),
				asFileFilter(new FilenameFilter() {
					public boolean accept(File dir, String fileName) {
						return dir.getName().equals("shared") && fileName.startsWith("liblouis"); }}),
				trueFileFilter())).iterator().next());
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
