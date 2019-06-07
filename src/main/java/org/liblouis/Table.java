package org.liblouis;

/**
 * Proxy for a Translator, with metadata.
 */
public class Table {
	
	private final String table;
	private final TableInfo info;
	private Translator translator = null;
	
	Table(String table) {
		this.table = table;
		this.info = new TableInfo(table);
	}
	
	public TableInfo getInfo() {
		return info;
	}
	
	public Translator getTranslator() throws CompilationException {
		if (translator == null)
			translator = new Translator(table);
		return translator;
	}
}
