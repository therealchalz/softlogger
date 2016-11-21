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

package ca.brood.softlogger.dataoutput;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;
import ca.brood.softlogger.util.Util;

public class DebugOutputModule extends AbstractOutputModule  implements Runnable {
	private Logger log;
	private String description;
	private PeriodicSchedulable schedulable;
	
	public DebugOutputModule() {
		super();
		log = LogManager.getLogger(DebugOutputModule.class);
		description = "DebugOutputModule";
		schedulable = new PeriodicSchedulable();
		schedulable.setAction(this);
	}
	
	public DebugOutputModule(DebugOutputModule o) {
		super(o);
		log = LogManager.getLogger(DebugOutputModule.class);
		description = o.description;
		schedulable = new PeriodicSchedulable(o.schedulable);
		schedulable.setAction(this);
	}
	
	@Override
	public DebugOutputModule clone() {
		return new DebugOutputModule(this);
	}

	@Override
	public void run() {
		printDeviceData();
	}
	
	private void printDeviceData() {
		ArrayList<RealRegister> registers = this.m_Registers.readRegisters();
		log.info("Printing "+description);
		DateFormat sqlDateFormat = new SimpleDateFormat(Util.SQL_DATE_FORMAT_STRING);
		for (RealRegister register : registers) {
			try {
				if (!register.isNull())
					log.info(register.getFieldName()+"("+String.format("%06d", register.getLongAddress())+"-"+sqlDateFormat.format(new Date(register.getData().getTimestamp()))+"): "+register.getFloat());
				else
					log.info(register.getFieldName()+"("+String.format("%06d", register.getLongAddress())+"): <null>");
			} catch (Exception e) {
				log.info("Exception on print: ", e);
			}
		}
		this.resetRegisterSamplings();
	}
	
	@Override
	protected void setConfigValue(String name, String value) {
		if ("logIntervalSeconds".equalsIgnoreCase(name)) { //seconds
			schedulable.setPeriod(Util.parseInt(value) * 1000);
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	public String toString() {
		return "DebugOutputModule - description: "+this.description +" period: "+ schedulable.getPeriod();
	}

	@Override
	public long getNextRun() {
		return schedulable.getNextRun();
	}

	@Override
	public void execute() {
		schedulable.execute();
	}

	@Override
	public void close() {
		//Nothing to close
	}

}
