package org.liblouis;

import com.sun.jna.Callback;

public interface TableResolver extends Callback {
	
	public String invoke(String table, String base);
	
}
