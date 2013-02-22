/*******************************************************************************
 * Copyright (c) 2013 Charles Hache. All rights reserved. 
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
 *     Charles Hache - initial API and implementation
 ******************************************************************************/
package ca.brood.softlogger.modbus.register;

public enum RegisterSizeType {
	SIGNED,
	UNSIGNED,
	FLOAT;
	
	public static RegisterSizeType fromString(String attr) {
		if (attr.equalsIgnoreCase("s"))
			return SIGNED;
		if (attr.equalsIgnoreCase("int"))
			return SIGNED;
		if (attr.equalsIgnoreCase("signed"))
			return SIGNED;
		if (attr.equalsIgnoreCase("u"))
			return UNSIGNED;
		if (attr.equalsIgnoreCase("uint"))
			return UNSIGNED;
		if (attr.equalsIgnoreCase("unsigned"))
			return UNSIGNED;
		if (attr.equalsIgnoreCase("f"))
			return FLOAT;
		if (attr.equalsIgnoreCase("float"))
			return FLOAT;
		
		return UNSIGNED;
	}
}
