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
package ca.brood.softlogger.dataoutput;

import org.apache.log4j.Logger;

import ca.brood.softlogger.scheduler.PrettyPeriodicSchedulable;
import ca.brood.softlogger.util.Util;

public class DBOutputModule extends AbstractOutputModule {
	private Logger log;
	private PrettyPeriodicSchedulable logSchedulable;
	private boolean firstLineOutputted;
	
	public DBOutputModule() {
		super();
		log = Logger.getLogger(DBOutputModule.class);
		logSchedulable = new PrettyPeriodicSchedulable();
		logSchedulable.setAction(this);
		firstLineOutputted = false;
	}
	
	public DBOutputModule(DBOutputModule o) {
		super(o);
		log = Logger.getLogger(DBOutputModule.class);
		logSchedulable = new PrettyPeriodicSchedulable(o.logSchedulable);
		logSchedulable.setAction(this);
		firstLineOutputted = o.firstLineOutputted;
	}
	
	@Override
	public DBOutputModule clone() {
		return new DBOutputModule(this);
	}

	@Override
	public String getDescription() {
		return "DBOutputModule";
	}
	
	@Override
	protected void setConfigValue(String name, String value) {
		if ("logIntervalSeconds".equalsIgnoreCase(name)) {
			logSchedulable.setPeriod(Util.parseInt(value) * 1000);
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}

	@Override
	public long getNextRun() {
		return logSchedulable.getNextRun();
	}

	@Override
	public void execute() {
		logSchedulable.execute();
	}

	@Override
	public void run() {
		log.info("Running"); 
		
		//Skip first line of data
		if (!firstLineOutputted) {
			firstLineOutputted = true;
			this.resetRegisterSamplings();
			return;
		}
		
		//TODO: Write values to DB
	}

}
