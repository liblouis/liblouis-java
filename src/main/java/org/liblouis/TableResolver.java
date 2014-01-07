package org.liblouis;

import java.io.File;

import com.sun.jna.Callback;
import com.sun.jna.TypeMapper;

public interface TableResolver extends Callback {
	
	public File[] invoke(String tableList, String base);
	
	public TypeMapper TYPE_MAPPER = Louis.TypeMapper.INSTANCE;
	
}
