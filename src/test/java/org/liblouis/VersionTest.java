package org.liblouis;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VersionTest {
	
	@Test
	public void testVersion() {
		assertEquals(
			"3.0.0",
			Louis.getLibrary().lou_version());
	}
	
	@SuppressWarnings("unchecked")
	public VersionTest() {
		Helper.setLibraryPath();
	}
}
