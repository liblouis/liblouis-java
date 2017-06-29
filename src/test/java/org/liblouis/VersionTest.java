package org.liblouis;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VersionTest {
	
	@Test
	public void testVersion() {
		assertEquals(
			"3.2.0-1-SNAPSHOT",
			Louis.getLibrary().lou_version());
	}
	
	@SuppressWarnings("unchecked")
	public VersionTest() {
		Helper.setLibraryPath();
	}
}
