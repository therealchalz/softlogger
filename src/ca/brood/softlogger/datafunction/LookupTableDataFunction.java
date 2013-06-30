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
package ca.brood.softlogger.datafunction;

import org.apache.log4j.Logger;

import ca.brood.softlogger.lookuptable.LookupTable;
import ca.brood.softlogger.lookuptable.LookupTableManager;
import ca.brood.softlogger.modbus.register.RegisterData;

public class LookupTableDataFunction implements DataFunction {
	private Logger log;
	
	public LookupTableDataFunction() {
		log = Logger.getLogger(LookupTableDataFunction.class);
	}
	
	@Override
	public void process(RegisterData data, String funcArg) {
		//log.trace("Processing: "+funcArg);
		try {
			LookupTable lut = LookupTableManager.getLookupTable(funcArg);
			Integer reading = data.getInt();
			if (reading == null) {
				throw new Exception("Got a null reading from device");
			}
			Double realValue = lut.lookup(reading);
			if (realValue == Double.NaN || realValue == Float.NaN) {
				log.warn("Got a NaN when trying to process lookup index: "+reading+" from table: "+funcArg);
				data.setNull();	//return null
			} else {
				data.setDataFloat(realValue.floatValue());
			}
		} catch (Exception e) {
			log.error("Error in table: "+funcArg, e);
		}
	}
	
}
