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
import java.util.concurrent.locks.ReentrantLock;


public class RegisterCollection {
	private ArrayList<RealRegister> readRegisters;
	private ArrayList<RealRegister> updateRegisters;
	private ArrayList<RealRegister> registerSetA;
	private ArrayList<RealRegister> registerSetB;
	private int whichRegisterSet;	//0-> read=A, !0-> read=B;
	private ReentrantLock readRegisterLock;
	private ReentrantLock updateRegisterLock;
	
	public RegisterCollection() {
		this(new ArrayList<RealRegister>());
	}
	
	public RegisterCollection(ArrayList<? extends RealRegister> regs) {
		registerSetA = new ArrayList<RealRegister>();
		registerSetB = new ArrayList<RealRegister>();
		for (RealRegister r : regs) {
			registerSetA.add(r.clone());
			registerSetB.add(r.clone());
		}
		readRegisterLock = new ReentrantLock();
		updateRegisterLock = new ReentrantLock();
		
		whichRegisterSet = 0;
		readRegisters = registerSetA;
		updateRegisters = registerSetB;
	}
	
	public RegisterCollection(RegisterCollection o) {
		this(o.readRegisters());
	}
	
	public ArrayList<RealRegister> readRegisters() {
		readRegisterLock.lock();
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		for (RealRegister r : readRegisters) {
			ret.add(r.clone());
		}
		readRegisterLock.unlock();
		return ret;
	}
	
	public ArrayList<RealRegister> beginUpdating() {
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		updateRegisterLock.lock();
		ret.addAll(updateRegisters);
		return ret;
	}
	
	public void finishUpdating() {
		toggleRegisterSet();
		updateRegisterLock.unlock();
	}
	
	public void resetRegisterSamplings() {
		ArrayList<RealRegister> regs = beginUpdating();
		for (RealRegister r : regs) {
			r.resetSampling();
		}
		finishUpdating();
	}
	
	private void toggleRegisterSet(){
		//Lock all the registers, then synchronize the read registers
		//with the updated ones.
		readRegisterLock.lock();
		updateRegisterLock.lock();
		
		readRegisters.clear();
		for (int i = 0; i < updateRegisters.size(); i++) {
			readRegisters.add(updateRegisters.get(i).clone());
		}
		
		if (whichRegisterSet == 0) {
			whichRegisterSet = 1;		
			readRegisters = registerSetB;
			updateRegisters = registerSetA;
		} else {
			whichRegisterSet = 0;
			readRegisters = registerSetA;
			updateRegisters = registerSetB;
		}
		
		readRegisterLock.unlock();
		updateRegisterLock.unlock();
	}
}
