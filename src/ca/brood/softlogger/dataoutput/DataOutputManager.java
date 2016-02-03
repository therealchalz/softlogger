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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.scheduler.Scheduler;

public class DataOutputManager {
	private Map<Class<? extends OutputModule>, Scheduler> schedulers;
	private Logger log;
	
	public DataOutputManager() {
		schedulers = new HashMap<Class<? extends OutputModule>, Scheduler>();
		
		log = LogManager.getLogger(DataOutputManager.class);
	}
	
	public void initializeSchedulers(ArrayList<Device> allDevices) {
		//Get all the output modules from all the devices
		ArrayList<OutputModule> modules = new ArrayList<OutputModule>();
		for (Device d : allDevices) {
			ArrayList<OutputModule> add = d.getOutputModules();
			log.trace("Device "+d.getDescription()+" has "+add.size()+" output modules");
			modules.addAll(d.getOutputModules());
		}
		log.trace("Initializing "+modules.size()+" output module instances");
		//For now, we organize all the output modules of the same class together
		//in the same scheduler.  This way we have an additional thread for each
		//type of output module that is added.  If an output module is buggy then
		//it won't affect the running of other output modules.
		for (OutputModule toAdd : modules) {
			Class<? extends OutputModule> moduleClass = toAdd.getClass();	
			if (!schedulers.containsKey(moduleClass)) {
				Scheduler sched = new Scheduler();
				sched.setThreadName(moduleClass.getName());
				schedulers.put(moduleClass, sched);
			}
			schedulers.get(moduleClass).addSchedulee(toAdd);
		}
	}

	public void start() {
		log.info("Starting");
		for (Scheduler scheduler : schedulers.values()) {
			scheduler.start();
		}
	}
	
	public void stop() {
		log.info("Received Stop Request");
		for (Scheduler scheduler : schedulers.values()) {
			scheduler.stop();
		}
	}
}
