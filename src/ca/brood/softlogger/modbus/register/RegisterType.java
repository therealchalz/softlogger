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
package ca.brood.softlogger.modbus.register;

import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;

import org.apache.log4j.Logger;

public enum RegisterType {
	OUTPUT_COIL (0),		//Leading 0 in addresses, aka Coils, rw
	INPUT_COIL (1),			//Leading 1, aka Discretes, ro
	INPUT_REGISTER (3),		//Leading 3, aka Input Registers, ro
	OUTPUT_REGISTER (4);	//Leading 4, aka Holding Registers, rw
	
	private final int leadingDigit;
	private RegisterType(int leadingDig) {
		this.leadingDigit = leadingDig;
	}
	public static int getAddress(int address) {
		String addr = String.valueOf(address);
		Logger log = null;
		int pos = addr.indexOf('0');
		int revPos = addr.length() - pos;
		if (revPos != 5 && revPos != 6 && pos != -1) {
			log = Logger.getLogger(RegisterType.class);
			log.warn("Trying to parse modbus address with funny range: "+address);
		}
		int offset = Integer.parseInt(addr.substring(pos+1));
		return offset;
	}
	
	public static RegisterType fromAddress(int address) {
		String addr = String.valueOf(address);
		Logger log = null;
		int pos = addr.indexOf('0');
		int revPos = addr.length() - pos;
		if (revPos != 5 && revPos != 6 && pos != -1) {
			log = Logger.getLogger(RegisterType.class);
			log.warn("Trying to parse modbus address with funny range: "+address);
		}
		if (pos == 0 || pos == -1) //leading 0, coil
			return RegisterType.OUTPUT_COIL;
		int range = Integer.parseInt(addr.substring(0, pos));
		if (range == RegisterType.INPUT_COIL.leadingDigit)
			return RegisterType.INPUT_COIL;
		if (range == RegisterType.INPUT_REGISTER.leadingDigit)
			return RegisterType.INPUT_REGISTER;
		if (range == RegisterType.OUTPUT_REGISTER.leadingDigit)
			return RegisterType.OUTPUT_REGISTER;
		if (log == null) {
			log = Logger.getLogger(RegisterType.class);
		}
		log.error("Error parsing modbus address - unknown range (falling back to OUTPUT_REGISTER). Leading digit: "+range+", address: "+address);
		return RegisterType.OUTPUT_REGISTER;
	}
	
	public static RegisterType fromResponse(ModbusResponse response) {
		if (response instanceof ReadCoilsResponse) {
			return RegisterType.OUTPUT_COIL;
		}
		if (response instanceof ReadInputDiscretesResponse) {
			return RegisterType.INPUT_COIL;
		}
		if (response instanceof ReadInputRegistersResponse) {
			return RegisterType.INPUT_REGISTER;
		}
		else  {
			return RegisterType.OUTPUT_REGISTER;
		}
	}
}
