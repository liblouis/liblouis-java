package org.liblouis;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.io.filefilter.FileFilterUtils.asFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.junit.Assert.assertEquals;
import static org.liblouis.Utilities.Hyphenation.insertHyphens;

public class TranslatorTest {
	
	@Test
	public void testVersion() {
		assertEquals(
			"2.5.4",
			Louis.getLibrary().lou_version());
	}
	
	@Test(expected=CompilationException.class)
	public void testCompileTable() throws Exception {
		new Translator("unexisting_file");
	}
	
	@Test
	public void testTranslate() throws Exception {
		Translator translator = newTranslator("foobar.cti");
		assertEquals(
			"foobar",
			translator.translate("foobar", null, null).getBraille());
	}
	
	@Test
	public void testHyphenate() throws Exception {
		Translator hyphenator = newTranslator("foobar.cti,foobar.dic");
		assertEquals(
			"foo-bar",
			insertHyphens("foobar", hyphenator.hyphenate("foobar"), '-', null));
	}
	
	@Test
	public void testTranslateAndHyphenate() throws Exception {
		Translator translator = newTranslator("foobar.cti,foobar.dic");
		String text = "foobar";
		TranslationResult result = translator.translate(text, translator.hyphenate(text), null);
		assertEquals(
				"foo-bar",
				insertHyphens(result.getBraille(), result.getHyphenPositions(), '-', null));
	}
	
	private Translator newTranslator(String tables) throws IOException, CompilationException {
		return new Translator(new File(tablesDir, tables).getCanonicalPath());
	}
	
	private File tablesDir;
	
	@Before
	@SuppressWarnings("unchecked")
	public void initialize() {
		File testRootDir = new File(this.getClass().getResource("/").getPath());
		tablesDir = new File(testRootDir, "tables");
		Louis.setLibraryPath(((Collection<File>)FileUtils.listFiles(
				new File(testRootDir, "../dependency"),
				asFileFilter(new FilenameFilter() {
					public boolean accept(File dir, String fileName) {
						return dir.getName().equals("shared") && fileName.startsWith("liblouis"); }}),
				trueFileFilter())).iterator().next());
	}
}
