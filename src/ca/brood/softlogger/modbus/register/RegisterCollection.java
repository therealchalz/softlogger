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

import java.util.ArrayList;


public class RegisterCollection {
	private ArrayList<RealRegister> registers;
	
	public RegisterCollection() {
		this(new ArrayList<RealRegister>());
	}
	
	public RegisterCollection(ArrayList<? extends RealRegister> regs) {
		registers = new ArrayList<RealRegister>();
		for (RealRegister r : regs) {
			registers.add(r.clone());
		}
	}
	
	public RegisterCollection(RegisterCollection o) {
		this(o.readRegisters());
	}
	
	public ArrayList<RealRegister> readRegisters() {
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		synchronized(registers) {
			for (RealRegister r : registers) {
				ret.add(r.clone());
			}
		}
		return ret;
	}
	
	public ArrayList<RealRegister> beginUpdating() {
		return readRegisters();
	}
	
	public void finishUpdating(ArrayList<RealRegister> newRegisters) {
		synchronized(registers) {
			registers.clear();
			for (RealRegister r : newRegisters) {
				registers.add(r.clone());
			}
		}
	}
	
	public void resetRegisterSamplings() {
		ArrayList<RealRegister> regs = beginUpdating();
		for (RealRegister r : regs) {
			r.resetSampling();
		}
		finishUpdating(regs);
	}
}
