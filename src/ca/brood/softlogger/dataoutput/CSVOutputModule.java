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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private String completedFileDirectory;
	private String csvSubdirectory;
	private long nanoTimeOffset;
	private SimpleDateFormat folderDateFormat;
	private SimpleDateFormat filenameDateFormat;
	
	public CSVOutputModule() {
		super();
		log = LogManager.getLogger(CSVOutputModule.class);
		writer = null;
		logSchedulable = new PrettyPeriodicSchedulable();
		fileCreateSchedulable = new PrettyPeriodicSchedulable();
		firstLineOutputted = false;
		firstFileCreated = false;
		writeGuids = true;
		completedFileDirectory = "";
		csvSubdirectory = ".";
		nanoTimeOffset = System.nanoTime();
		folderDateFormat = new SimpleDateFormat("yyyy-MM");
		filenameDateFormat = new SimpleDateFormat("yyyyMMdd-HH.mm.ss");
	}
	
	public CSVOutputModule(CSVOutputModule o) {
		super(o);
		log = LogManager.getLogger(CSVOutputModule.class);
		if (o.writer == null)
			writer = null;
		else
			writer = new CSVFileWriter(o.writer);
		logSchedulable = new PrettyPeriodicSchedulable(o.logSchedulable);
		fileCreateSchedulable = new PrettyPeriodicSchedulable(o.fileCreateSchedulable);
		firstLineOutputted = o.firstLineOutputted;
		firstFileCreated = o.firstFileCreated;
		writeGuids = o.writeGuids;
		completedFileDirectory = o.completedFileDirectory;
		csvSubdirectory = o.csvSubdirectory;
		nanoTimeOffset = o.nanoTimeOffset;
		folderDateFormat = (SimpleDateFormat) o.folderDateFormat.clone();
		filenameDateFormat = (SimpleDateFormat) o.filenameDateFormat.clone();
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
		} else if ("completedFileDirectory".equalsIgnoreCase(name)) {
			completedFileDirectory = value;
		} else if ("csvSubdirectory".equalsIgnoreCase(name)) {
			csvSubdirectory = value;
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}
	
	private void updateFilename() throws Exception {
		String theFileName;
		String oldFileName;
		Calendar cal = Calendar.getInstance();
		long currentWallTime = System.currentTimeMillis();
		cal.setTimeInMillis(currentWallTime+500L);	//Add 500ms to time to round to nearest second when truncated
		theFileName = csvSubdirectory+File.separator+filenameDateFormat.format( cal.getTime())+"-"+m_OutputDevice.getDescription()+".csv";
		
		if (!csvSubdirectory.equals(".")) {
			File csvDir = new File(csvSubdirectory);
			if (!csvDir.isDirectory()) {
				if (!csvDir.mkdirs()) {
					log.error("Error - couldn't create the CSV destination directory: "+csvSubdirectory);
					throw new Exception("Error - couldn't create the CSV destination directory: "+csvSubdirectory);
				}
			}
		}
		
		if (writer == null) {
			writer = new CSVFileWriter(theFileName);
			
			//Move all CSV files from csvSubdirectory to the completedFileDirectory
			if (completedFileDirectory.length() > 0) {
				String[] extensions = new String[1];
				extensions[0] = "csv";
				File csvDir = new File(csvSubdirectory);
				
				try {
					Iterator<File> fileIter = FileUtils.iterateFiles(csvDir, extensions, false);
					
					while (fileIter.hasNext()) {
						File f = fileIter.next();
						Date fileDate = filenameDateFormat.parse(f.getName());
						File completedDir = new File(completedFileDirectory+File.separator+folderDateFormat.format(fileDate));
						FileUtils.moveFileToDirectory(f, completedDir, true);
						log.debug("Moved "+f.getPath()+" into "+completedDir.getCanonicalPath());
					}
				} catch (Exception e) {
					log.error("Couldn't move existing CSV files on startup.", e);
				}
			}
			
		} else {
			oldFileName = writer.getFilename();
			writer.setFilename(theFileName);
			
			if (!oldFileName.equalsIgnoreCase(theFileName)) {
				//File name has changed, move the old file if required
				try {
					if (completedFileDirectory.length() > 0) {
						File oldFile = new File(oldFileName);
						Date fileDate = filenameDateFormat.parse(oldFile.getName());
						File completedDir = new File(completedFileDirectory+"/"+folderDateFormat.format(fileDate));
						
						if (oldFile.exists()) {
							FileUtils.moveFileToDirectory(oldFile, completedDir, true);
						}
					}
				} catch (Exception e) {
					log.error("Couldn't move the completed CSV file", e);
				}
			}
		}
	}
	
	@Override
	public void setOutputableDevice(OutputableDevice d) {
		super.setOutputableDevice(d);
	}

	@Override
	public void run() {
		log.info("Running");
		long currentTime = System.nanoTime();
		//Should the file be recreated? Add in a 25ms (25*1000000 ns) fudge factor
		if (fileCreateSchedulable.getNextRun() - currentTime <= 25l*1000000l || !firstFileCreated) {
			if (fileCreateSchedulable.getNextRun() - currentTime <= 25l*1000000l) {
				fileCreateSchedulable.execute();	//Updates the time (only if the period has elapsed)
				logSchedulable.updateEpoch();
				fileCreateSchedulable.updateEpoch();
			}
			log.trace("Creating new CSV file");
			try {
				updateFilename();
				firstFileCreated = true;
			} catch (Exception e) {
				log.error("Couldn't prepare new CSV file: ", e);
				return;
			}
		}
		
		//Add in a 25ms (25*1000000 ns) fudge factor
		if (logSchedulable.getNextRun() - currentTime <= 25l*1000000l) {
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
				boolean guidAdded = false;
				if ((Object)r instanceof DataRegister) {
					String guid = ((DataRegister)r).getGUID();
					if (guid != null && guid.length() > 0) {
						headerText += "~"+guid;
						guidAdded = true;
					}
				}
				if (!guidAdded) {
					headerText += "~Unknown";
				}
			}
			heads.add(headerText);
		}
		writer.setHeaders(heads);
		
		ArrayList<String> values = new ArrayList<String>();
		Calendar cal = Calendar.getInstance();
		long currentWallTime = System.currentTimeMillis();
		cal.setTimeInMillis(currentWallTime+500L);	//Add 500ms to time to round to nearest second
		values.add(String.format("%1$tY%1$tm%1$td-%1$tT", cal));
		values.add(""+currentWallTime);
		
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
		if (logSchedulable.getNextRun() - fileCreateSchedulable.getNextRun()< 0)
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
