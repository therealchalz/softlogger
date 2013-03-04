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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PrettyPeriodicSchedulable;
import ca.brood.softlogger.util.CsvFileWriter;
import ca.brood.softlogger.util.Util;

public class CsvOutputModule extends AbstractOutputModule {
	private Logger log;
	private CsvFileWriter writer;
	private PrettyPeriodicSchedulable logSchedulable;
	private PrettyPeriodicSchedulable fileCreateSchedulable;
	private boolean firstLineOutputted;
	
	public CsvOutputModule() {
		super();
		log = Logger.getLogger(CsvOutputModule.class);
		writer = new CsvFileWriter("testOut.csv");
		logSchedulable = new PrettyPeriodicSchedulable();
		logSchedulable.setAction(this);
		fileCreateSchedulable = new PrettyPeriodicSchedulable();
		firstLineOutputted = false;
	}
	
	public CsvOutputModule(CsvOutputModule o) {
		super(o);
		log = Logger.getLogger(CsvOutputModule.class);
		writer = new CsvFileWriter(o.writer);
		logSchedulable = new PrettyPeriodicSchedulable(o.logSchedulable);
		logSchedulable.setAction(this);
		fileCreateSchedulable = new PrettyPeriodicSchedulable();
		firstLineOutputted = o.firstLineOutputted;
	}


	@Override
	public String getDescription() {
		return "CsvOutputModule";
	}
	
	private void setConfigValue(String name, String value) {
		if ("logIntervalSeconds".equalsIgnoreCase(name)) { //seconds
			logSchedulable.setPeriod(Util.parseInt(value) * 1000);
		} else if ("newFilePeriodMinutes".equalsIgnoreCase(name)) { //minutes
			fileCreateSchedulable.setPeriod(Util.parseInt(value) * 60 * 1000);
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}

	@Override
	public boolean configure(Node rootNode) {
		NodeList configNodes = rootNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0) || 
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("configValue".compareToIgnoreCase(configNode.getNodeName())==0)) {
				String name = configNode.getAttributes().getNamedItem("name").getNodeValue();
				String value = configNode.getFirstChild().getNodeValue();
				setConfigValue(name, value);
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		return true;
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
		return new CsvOutputModule(this);
	}

	@Override
	public long getNextRun() {
		return logSchedulable.getNextRun();
	}

	@Override
	public void execute() {
		logSchedulable.execute();
	}

}
