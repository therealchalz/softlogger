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
import java.util.Calendar;

import org.apache.log4j.Logger;

import ca.brood.brootils.csv.CSVFileWriter;
import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PrettyPeriodicSchedulable;
import ca.brood.softlogger.util.Util;

public class CSVOutputModule extends AbstractOutputModule implements Runnable {
	private Logger log;
	private CSVFileWriter writer;
	private PrettyPeriodicSchedulable logSchedulable;
	private PrettyPeriodicSchedulable fileCreateSchedulable;
	private boolean firstLineOutputted;
	
	public CSVOutputModule() {
		super();
		log = Logger.getLogger(CSVOutputModule.class);
		writer = new CSVFileWriter("testOut.csv");
		logSchedulable = new PrettyPeriodicSchedulable();
		logSchedulable.setAction(this);
		fileCreateSchedulable = new PrettyPeriodicSchedulable();
		firstLineOutputted = false;
	}
	
	public CSVOutputModule(CSVOutputModule o) {
		super(o);
		log = Logger.getLogger(CSVOutputModule.class);
		writer = new CSVFileWriter(o.writer);
		logSchedulable = new PrettyPeriodicSchedulable(o.logSchedulable);
		logSchedulable.setAction(this);
		fileCreateSchedulable = new PrettyPeriodicSchedulable();
		firstLineOutputted = o.firstLineOutputted;
	}


	@Override
	public String getDescription() {
		return "CSVOutputModule";
	}
	
	@Override
	public void setDeviceDescription(String desc) {
		super.setDeviceDescription(desc);
		writer = new CSVFileWriter(desc);
	}
	
	protected void setConfigValue(String name, String value) {
		if ("logIntervalSeconds".equalsIgnoreCase(name)) { //seconds
			logSchedulable.setPeriod(Util.parseInt(value) * 1000);
		} else if ("newFilePeriodMinutes".equalsIgnoreCase(name)) { //minutes
			fileCreateSchedulable.setPeriod(Util.parseInt(value) * 60 * 1000);
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}

	@Override
	public void run() {
		log.info("Running");
		
		/* Always skip the first line of data, aka the first call
		 * to run().  We always want to make sure that the first
		 * line of data in the output is from one full, complete
		 * polling cycle, otherwise registers configured as
		 * accumulate, for example, would have smaller values than 
		 * expected.
		 */
		if (!firstLineOutputted) {
			firstLineOutputted = true;
			this.resetRegisterSamplings();
			return;
		}
		
		ArrayList<String> heads = new ArrayList<String>();
		heads.add("datetime");
		heads.add("timestamp");
		ArrayList<RealRegister> re = this.m_Registers.readRegisters();
		//Reset the samplings as soon as we get our copies of the registers to minimize
		//the chance that we miss an update
		this.resetRegisterSamplings();
		for (RealRegister r : re) {
			heads.add(r.getFieldName());
		}
		writer.setHeaders(heads);
		
		ArrayList<String> values = new ArrayList<String>();
		Calendar cal = Calendar.getInstance();
		long currentTime = System.currentTimeMillis();
		cal.setTimeInMillis(currentTime);
		values.add(String.format("%1$tY%1$tm%1$td-%1$tT", cal));
		values.add(""+currentTime);
		
		//Should the file be recreated?
		if (fileCreateSchedulable.getNextRun() <= currentTime) {
			fileCreateSchedulable.execute();	//Updates the time
			log.trace("Creating new CSV file");
			writer.newFile();
		}
		
		boolean atLeastOneGoodValue = false;
		
		for (RealRegister r : re) {
			if (r.isNull())
				values.add("NULL");
			else {
				values.add(""+r.getFloat());
				atLeastOneGoodValue = true;
			}
		}
		if (atLeastOneGoodValue)
			writer.writeData(values);
	}

	@Override
	public AbstractOutputModule clone() {
		return new CSVOutputModule(this);
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
	public void close() {
		//No need to close anything since the writer automatically
		//close the stream after every write.
	}

}