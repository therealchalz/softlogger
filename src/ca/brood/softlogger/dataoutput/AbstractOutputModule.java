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
package ca.brood.softlogger.dataoutput;

import ca.brood.softlogger.modbus.register.RegisterCollection;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public abstract class AbstractOutputModule 
		extends PeriodicSchedulable 
		implements OutputModule {
	
	protected OutputableDevice m_outputDevice;
	protected RegisterCollection m_Registers;
	
	public AbstractOutputModule() {
		super();
		this.setAction(this);
		m_outputDevice = null;
		m_Registers = new RegisterCollection();
	}
	
	public AbstractOutputModule(AbstractOutputModule other) {
		super(other);
		m_Registers = new RegisterCollection(other.m_Registers);
		m_outputDevice = other.m_outputDevice;
		this.setAction(this);
	}
	
	@Override
	abstract public AbstractOutputModule clone();

	@Override
	public RegisterCollection getRegisterCollection() {
		return m_Registers;
	}
	
	@Override
	public void setRegisterCollection(RegisterCollection reg) {
		m_Registers = reg;
	}
	
	public void resetRegisterSamplings() {
		m_Registers.resetRegisterSamplings();
	}
/*	
	public void setDeviceToOutputA(OutputableDevice dev) {
		m_outputDevice = dev;
		this.setRegisters(m_outputDevice.getAllRegisters());
	}*/
}
