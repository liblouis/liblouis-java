package org.liblouis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultTablesTest {
	
	@Test
	public void testTranslateDutch() throws Exception {
		assertEquals(
			"⠨foobar",
			Translator.find("locale:nl").translate("Foobar", null, null, null).getBraille());
	}
}
