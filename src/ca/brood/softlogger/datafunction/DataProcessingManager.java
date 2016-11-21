/*******************************************************************************
 * Copyright (c) 2013-2016 Charles Hache <chache@cygnustech.ca>.  
 * All rights reserved. 
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
 * along with softlogger.  If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 * 
 * Contributors:
 *     Charles Hache <chache@cygnustech.ca> - initial API and implementation
 ******************************************************************************/

package ca.brood.softlogger.datafunction;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataProcessingManager {
	private static Logger log;
	private static Map<Class<? extends DataFunction>, DataFunction> functions;
	private static DataFunction nullFunction;
	
	static {
		functions = new HashMap<Class<? extends DataFunction>, DataFunction>();
		log = LogManager.getLogger(DataProcessingManager.class);
		nullFunction = new NullDataFunction();
	}
	
	public static DataFunction getDataFunction(Class<? extends DataFunction> functionClass) {
		if (functions.containsKey(functionClass)) {
			return functions.get(functionClass);
		} else {
			try {
				DataFunction newFunc = functionClass.newInstance();
				functions.put(functionClass, newFunc);
				log.info("Loaded a new data function: "+functionClass);
				return newFunc;
			} catch (Exception e) {
				log.error("Error when trying to instantiate: "+functionClass);
			}
		}
		return nullFunction;
	}
}
