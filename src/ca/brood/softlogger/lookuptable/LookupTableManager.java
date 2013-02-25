/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chache@brood.ca>. All rights reserved. 
 * 
 * This file is part of the softlogger project.
 * softlogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * softlogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with softlogger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache <chache@brood.ca> - initial API and implementation
 ******************************************************************************/
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
