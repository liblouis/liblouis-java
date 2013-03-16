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
			"2.5.2",
			Louis.getLibrary().lou_version());
	}
	
	@Test
	public void testTranslate() {
		Translator translator = newTranslator("foobar.cti,foobar.dic");
		assertEquals(
			"foobar",
			translator.translate("foobar", null, null, false).getBraille());
	}
	
	@Test
	public void testHyphenate() {
		Translator hyphenator = newTranslator("foobar.cti,foobar.dic");
		assertEquals(
			"foo-bar",
			insertHyphens("foobar", hyphenator.hyphenate("foobar"), '-'));
	}
	
	@Test
	public void testTranslateAndHyphenate() {
		Translator translator = newTranslator("foobar.cti,foobar.dic");
		TranslationResult result = translator.translate("foobar", null, null, true);
		assertEquals(
				"foo-bar",
				insertHyphens(result.getBraille(), result.getHyphenPositions(), '-'));
	}
	
	private Translator newTranslator(String tables) {
		try {
			return new Translator(new File(tablesDir, tables).getCanonicalPath()); }
		catch (IOException e) {
			throw new RuntimeException(e); }
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
