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

package ca.brood.softlogger.util;

//import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RegisterData;

public class Util {
	public static final String SQL_DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";
	//private static Logger log = Logger.getLogger(Util.class);
	public static int parseInt(String number) throws NumberFormatException {
		int ret;
		if (number.matches("0[xX][0-9a-fA-F]+")) { //Hex number
			ret = Integer.parseInt(number.substring(3), 16);
		} else {
			ret = Integer.parseInt(number);
		}
		return ret;
	}
	public static int parseInt(String number, int defaultValue) {
		try {
			return parseInt(number);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	public static boolean parseBool(String text) throws Exception {
		if (text.equalsIgnoreCase("true")
				|| text.equalsIgnoreCase("t")
				|| text.equalsIgnoreCase("yes")
				|| text.equalsIgnoreCase("y"))
			return true;
		else if (text.equalsIgnoreCase("false")
				|| text.equalsIgnoreCase("f")
				|| text.equalsIgnoreCase("no")
				|| text.equalsIgnoreCase("n"))
			return false;
		throw new Exception("Cannot parse a boolean from text: "+text);
	}
	public static RegisterData parseRegisterData(String val) throws NumberFormatException {
		RegisterData ret = new RegisterData();
		if (val.matches("[0-9]+\\.[0-9]+[fF]?")) { //floating point
			ret.setDataFloat(Float.parseFloat(val));
		} else if (val.equalsIgnoreCase("true")){
			ret.setData(true);
		} else if (val.equalsIgnoreCase("false")){
			ret.setData(false);
		} else {
			ret.setData(Integer.parseInt(val));
		}
		return ret;
	}
}
