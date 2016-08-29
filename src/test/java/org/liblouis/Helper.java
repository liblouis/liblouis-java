package org.liblouis;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.asFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import org.apache.commons.io.FileUtils;

class Helper {
	
	static void setLibraryPath() {
		File testRootDir = new File(Helper.class.getResource("/").getPath());
		Louis.setLibraryPath(((Collection<File>)FileUtils.listFiles(
			new File(testRootDir, "../dependency"),
			asFileFilter(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return dir.getName().equals("shared") && fileName.startsWith("liblouis"); }}),
			trueFileFilter())).iterator().next());
	}
}
