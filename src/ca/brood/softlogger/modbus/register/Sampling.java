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

package ca.brood.softlogger.modbus.register;

public enum Sampling {
	SUM,
	MEAN,
	LATEST,
	LATCHON,
	LATCHOFF;
	
	public static Sampling fromString(String text) {
		if (text.equalsIgnoreCase("average"))
			return MEAN;
		if (text.equalsIgnoreCase("sum"))
			return SUM;
		if (text.equalsIgnoreCase("mean"))
			return MEAN;
		if (text.equalsIgnoreCase("accumulate"))
			return SUM;
		if (text.equalsIgnoreCase("latest"))
			return LATEST;
		if (text.equalsIgnoreCase("latchon"))
			return LATCHON;
		if (text.equalsIgnoreCase("latchoff"))
			return LATCHOFF;
		return MEAN;
	}
}
