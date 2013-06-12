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
import ca.brood.softlogger.modbus.register.DataRegister;
import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PrettyPeriodicSchedulable;
import ca.brood.softlogger.util.Util;

public class CSVOutputModule extends AbstractOutputModule implements Runnable {
	private Logger log;
	private CSVFileWriter writer;
	private PrettyPeriodicSchedulable logSchedulable;
	private PrettyPeriodicSchedulable fileCreateSchedulable;
	private boolean firstLineOutputted;
	private boolean firstFileCreated;
	private boolean writeGuids;
	
	public CSVOutputModule() {
		super();
		log = Logger.getLogger(CSVOutputModule.class);
		writer = null;
		logSchedulable = new PrettyPeriodicSchedulable();
		fileCreateSchedulable = new PrettyPeriodicSchedulable();
		firstLineOutputted = false;
		firstFileCreated = false;
		writeGuids = true;
	}
	
	public CSVOutputModule(CSVOutputModule o) {
		super(o);
		log = Logger.getLogger(CSVOutputModule.class);
		if (o.writer == null)
			writer = null;
		else
			writer = new CSVFileWriter(o.writer);
		logSchedulable = new PrettyPeriodicSchedulable(o.logSchedulable);
		fileCreateSchedulable = new PrettyPeriodicSchedulable(o.fileCreateSchedulable);
		firstLineOutputted = o.firstLineOutputted;
		firstFileCreated = o.firstFileCreated;
		writeGuids = o.writeGuids;
	}


	@Override
	public String getDescription() {
		return "CSVOutputModule";
	}

	protected void setConfigValue(String name, String value) throws Exception {
		if ("logIntervalSeconds".equalsIgnoreCase(name)) { //seconds
			logSchedulable.setPeriod(Util.parseInt(value) * 1000);
		} else if ("newFilePeriodMinutes".equalsIgnoreCase(name)) { //minutes
			fileCreateSchedulable.setPeriod(Util.parseInt(value) * 60 * 1000);
		} else if ("printGUID".equalsIgnoreCase(name)) {
			writeGuids = Util.parseBool(value);
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}
	
	private void updateFilename() {
		String theFileName;
		Calendar cal = Calendar.getInstance();
		theFileName = String.format("%1$tY%1$tm%1$td-%1$tT", cal)+"-"+m_OutputDevice.getDescription()+".csv";
		if (writer == null)
			writer = new CSVFileWriter(theFileName);
		else
			writer.setFilename(theFileName);
	}
	
	@Override
	public void setOutputableDevice(OutputableDevice d) {
		super.setOutputableDevice(d);
	}

	@Override
	public void run() {
		log.info("Running");
		long currentTime = System.currentTimeMillis();
		//Should the file be recreated?
		if (fileCreateSchedulable.getNextRun() <= currentTime || !firstFileCreated) {
			if (fileCreateSchedulable.getNextRun() <= currentTime)
				fileCreateSchedulable.execute();	//Updates the time (only if the period has elapsed)
			firstFileCreated = true;
			log.trace("Creating new CSV file");
			updateFilename();
		}
		
		if (logSchedulable.getNextRun() <= currentTime) {
			log.trace("Logging data this run");
			logSchedulable.execute();
		} else {
			return;
		}
		
		// If the device is offline don't log.  Make
		// sure this is after the schedulables have .execute
		// called on them because we return here.
		if (!m_OutputDevice.isOnline()) {
			firstLineOutputted = false;
			log.trace("Device is offline... Skipping");
			return;
		}
		
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
			log.trace("Skipping the first line of data");
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
			String headerText = r.getFieldName();
			if (writeGuids) {
				if ((Object)r instanceof DataRegister) {
					headerText += "~"+((DataRegister)r).getGUID();
				}
			}
			heads.add(headerText);
		}
		writer.setHeaders(heads);
		
		ArrayList<String> values = new ArrayList<String>();
		Calendar cal = Calendar.getInstance();
		currentTime = System.currentTimeMillis();
		cal.setTimeInMillis(currentTime);
		values.add(String.format("%1$tY%1$tm%1$td-%1$tT", cal));
		values.add(""+currentTime);
		
		boolean atLeastOneGoodValue = false;
		
		for (RealRegister r : re) {
			if (r.isNull())
				values.add("NULL");
			else {
				values.add(""+r.getFloat());
				atLeastOneGoodValue = true;
			}
		}
		if (atLeastOneGoodValue) {
			writer.writeData(values);
		} else {
			log.trace("No good values to write. Not writing to file.");
		}
	}

	@Override
	public AbstractOutputModule clone() {
		return new CSVOutputModule(this);
	}

	@Override
	public long getNextRun() {
		if (logSchedulable.getNextRun() < fileCreateSchedulable.getNextRun())
			return logSchedulable.getNextRun();
		else
			return fileCreateSchedulable.getNextRun();
	}

	@Override
	public void execute() {
		this.run();
	}

	@Override
	public void close() {
		//No need to close anything since the writer automatically
		//close the stream after every write.
	}

}
