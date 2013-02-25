package ca.brood.softlogger.lookuptable;

import java.util.HashMap;
import java.util.Map;

public class LookupTableManager {
	private static Map<String, LookupTable> tables;
	
	static  {
		tables = new HashMap<String, LookupTable>();
	}
	
	public static synchronized LookupTable getLookupTable(String filename) throws Exception {
		if (tables.containsKey(filename)) {
			return tables.get(filename);
		} else {
			LookupTable n = new LookupTable(filename);
			n.open();
			tables.put(filename, n);
			return n;
		}
	}
	
	public static synchronized void closeAll() throws Exception {
		for (LookupTable lut : tables.values()) {
			lut.close();
		}
		tables = new HashMap<String, LookupTable>();
	}
}
