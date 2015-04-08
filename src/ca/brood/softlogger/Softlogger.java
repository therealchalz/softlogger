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
package ca.brood.softlogger;

import java.util.ArrayList;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import ca.brood.brootils.thread.ThreadPerformanceMonitor;
import ca.brood.brootils.xml.XMLConfigurable;
import ca.brood.brootils.xml.XMLFileLoader;
import ca.brood.softlogger.dataoutput.DataOutputManager;
import ca.brood.softlogger.dataoutput.DataServer;
import ca.brood.softlogger.dataoutput.OutputModule;
import ca.brood.softlogger.lookuptable.LookupTableGenerator;
import ca.brood.softlogger.lookuptable.LookupTableManager;
import ca.brood.softlogger.lookuptable.TestGenerator;
import ca.brood.softlogger.modbus.Device;

import java.io.File;


public class Softlogger implements Daemon, XMLConfigurable {
	public static final String DEFAULT_CONFIG_FILE = "softlogger.xml";
	private static Softlogger softlogger;
	private Logger log;
	
	private String loggerName = "Unnamed Logger";
	private int defaultScanRate = 0;
	private String configFilePath = "";
	
	private DataServer server;
	private ArrayList<SoftloggerChannel> softloggerChannels;
	private DataOutputManager dataOutputManager;
	
	
	/*
	 * BUGS:
	 * -If the same register address is defined multiple times in the config
	 * and they're not all the same size, the wrong size may be requested.
	 * 
	 * TODO:
	 * -Improve log levels and use logger.config for output control
	 * -dataFunction element (JEP, LUT is done)
	 * 
	 * Longer term TODO:
	 * -sending of file to remote server
	 * -email alerts
	 * -reverse tunnel
	 * -uptime tracking(cpu, internet, rtunnel)
	 */
	
	static {
		softlogger = new Softlogger();
		PropertyConfigurator.configure("log4j.config");
	}
	
	public Softlogger() {
		log = Logger.getLogger(Softlogger.class);
		softloggerChannels = new ArrayList<SoftloggerChannel>();
		dataOutputManager = new DataOutputManager();
	}
	public boolean configure(String configFile) {
		log.info("");
		log.info("");
		log.info("******************************");
		log.info("SOFTLOGGER IS STARTING");
		log.info("******************************");
		log.info("Softlogger using config file: "+configFile);
		log.info("Java version: "+System.getProperty("java.runtime.version"));
		
		configFilePath = configFile;
		
		XMLFileLoader xmlLoader = new XMLFileLoader(configFilePath, this);
		
		boolean success = false;
		try {
			success = xmlLoader.load();
		} catch (Exception e) {
			log.fatal("Error loading config file.", e);
		}
		
		return success;
		
	}
	public static void main(String[] args) {
		if (softlogger.configure(Softlogger.DEFAULT_CONFIG_FILE)) {
			softlogger.softloggerStart();
			try {
				Thread.sleep(600000);
			} catch (InterruptedException e) {
			}
			softlogger.softloggerStop();
			//Wait a second for the threads to quit before printing performance data
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			ThreadPerformanceMonitor.printPerformanceData();
		} else {
			softlogger.log.fatal("Error loading config file: "+Softlogger.DEFAULT_CONFIG_FILE);
		}
		softlogger.log.info("All done");
	}
	public void kill() {
		log.info("Softlogger killing softloggerChannels");
	}
	public void softloggerStop() {
		log.info("Softlogger stopping softloggerChannels");
		
		//Stop all outputting first
		dataOutputManager.stop();
		
		//Stop data collection
		for (int i=0; i<softloggerChannels.size(); i++) {
			softloggerChannels.get(i).stop();
		}
		
		//Close the global lookup tables
		try {
			LookupTableManager.closeAll();
		} catch (Exception e) {
			log.error("LUT closing error: ", e);
		}
	}
	
	public void softloggerStart() {
		
		try {
			File f = new File("lut/LUT1.dat");
			if (!f.exists())
				LookupTableGenerator.generate("lut/LUT1.dat", new TestGenerator(), 4, "Test lookup table... ", 65536);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Start all the softloggerChannels, which will in turn start all the devices
		for (int i=0; i<softloggerChannels.size(); i++) {
			softloggerChannels.get(i).start();
		}
		
		ArrayList<Device> devices = new ArrayList<Device>();
		for (SoftloggerChannel channel : softloggerChannels) {
			devices.addAll(channel.getDevices());
		}
		dataOutputManager.initializeSchedulers(devices);
		dataOutputManager.start();
	}
	
    @Override
    public void init(DaemonContext arg) throws DaemonInitException, Exception {
    	//TODO: get an xml config file from the command line
    	log.info("Linux daemon received init");
    	for (String s : arg.getArguments()) {
    		log.debug("Got argument: "+s);
    	}
    	if (!this.configure(DEFAULT_CONFIG_FILE)) {
    		throw new DaemonInitException("Error configuring logger with file: "+DEFAULT_CONFIG_FILE);
    	}
    }
    
    @Override
    public void start() throws Exception {
    	log.info("Linux daemon received start command");
    	softloggerStart();
    }
    
    @Override
    public void stop() throws Exception {
    	log.info("Linux daemon received stop command");
    	softloggerStop();
    }
    
	@Override
    public void destroy() {
        log.info("Linux daemon received destroy command");
    }
    
    /**
     * Static methods called by prunsrv to start/stop
     * the Windows service.  Pass the argument "start"
     * to start the service, and pass "stop" to
     * stop the service.
     * Stolen from FAQ at commons daemon.
     */
    public static void windowsService(String[] args) {
        String cmd = "start";
        if (args.length > 0) {
            cmd = args[0];
        }

        if ("start".equals(cmd)) {
        	softlogger.log.info("Windows service received Start command");
        	if (!softlogger.configure(DEFAULT_CONFIG_FILE)) {
        		softlogger.log.fatal("Error configuring softlogger with file: "+DEFAULT_CONFIG_FILE);
        	} else {
        		softlogger.softloggerStart();
        	}
        } else if ("stop".equals(cmd)) {
        	softlogger.log.info("Windows service received Stop command");
        	softlogger.softloggerStop();
        } else {
        	softlogger.log.error("Unrecognized service option: "+cmd);
        }
    }

	public void printAll() {
		log.info("Name: "+this.loggerName);
		if (defaultScanRate > 0)
			log.info("Default scan rate: "+this.defaultScanRate);
		
		this.server.printAll();
		for (SoftloggerChannel c : this.softloggerChannels) {
			c.printAll();
		}
	}
	@Override
	public boolean configure(Node rootNode) {
		NodeList loggerConfigNodes = rootNode.getChildNodes();
		log.debug("Configuring Logger...");
		for (int i=0; i<loggerConfigNodes.getLength(); i++) {
			Node configNode = loggerConfigNodes.item(i);
			if ("name".compareToIgnoreCase(configNode.getNodeName())==0){
				//log.debug("Logger Name: "+configNode.getFirstChild().getNodeValue());
				this.loggerName = configNode.getFirstChild().getNodeValue();
			} else if ("defaultScanRate".compareToIgnoreCase(configNode.getNodeName())==0){
				//log.debug("Default scan rate: "+configNode.getFirstChild().getNodeValue());
				try {
					this.defaultScanRate = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid scan rate: "+configNode.getFirstChild().getNodeValue());
					this.defaultScanRate = 0;
				}
			} else if (("server".compareToIgnoreCase(configNode.getNodeName())==0) || 
			("channel".compareToIgnoreCase(configNode.getNodeName())==0)||
			("outputModule".compareToIgnoreCase(configNode.getNodeName())==0)||
			("#comment".compareToIgnoreCase(configNode.getNodeName())==0)||
			("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (loggerName.equals("")) {
			log.warn("Softlogger name is blank");
		}
		if (defaultScanRate <= 0) {
			log.warn("Softlogger default scan rate is invalid.  Using default of 5000.");
			defaultScanRate = 5000;
		}
		
		//Load the data server
		loggerConfigNodes = rootNode.getOwnerDocument().getElementsByTagName("server");
		if (loggerConfigNodes.getLength() == 0) {
			log.fatal("Could not find a server defined in the config file.");
			return false;
		}
		if (loggerConfigNodes.getLength() > 1) {
			log.fatal("Too many servers are defined in the config file");
			return false;
		}
		Node currentConfigNode = loggerConfigNodes.item(0);
		server = new DataServer();
		if (!server.configure(currentConfigNode)) {
			return false;
		}
		
		//Load the softloggerChannels
		loggerConfigNodes = rootNode.getOwnerDocument().getElementsByTagName("channel");
		boolean workingChannel = false;
		for (int i=0; i<loggerConfigNodes.getLength(); i++) {
			currentConfigNode = loggerConfigNodes.item(i);
			SoftloggerChannel mc = new SoftloggerChannel();
			if (mc.configure(currentConfigNode)) {
				workingChannel = true;
				softloggerChannels.add(mc);
			}
		}
		
		if (!workingChannel) {
			log.fatal("No usable softloggerChannels configured");
			return false;
		}
		
		for (int i=0; i<softloggerChannels.size(); i++) {
			softloggerChannels.get(i).setDefaultScanRate(this.defaultScanRate);
		}
		
		ArrayList<Device> devices = new ArrayList<Device>();
		for (SoftloggerChannel channel : softloggerChannels) {
			devices.addAll(channel.getDevices());
		}
		
		//Load the global output modules
		currentConfigNode = rootNode.getOwnerDocument().getDocumentElement();
		loggerConfigNodes = currentConfigNode.getChildNodes();
		for (int i=0; i<loggerConfigNodes.getLength(); i++) {
			currentConfigNode = loggerConfigNodes.item(i);
			if ("outputModule".compareToIgnoreCase(currentConfigNode.getNodeName())!=0) {
				continue;
			}
			try {
				@SuppressWarnings("unchecked")
				Class<? extends OutputModule> outputClass = (Class<? extends OutputModule>) Class.forName(currentConfigNode.getAttributes().getNamedItem("class").getNodeValue());
				OutputModule outputModule = outputClass.newInstance();
				if (outputModule.configure(currentConfigNode)) {
					for (Device d : devices) {
						d.addOutputModule(outputModule.clone());
					}
				}
			} catch (Exception e) {
				log.error("Got exception while loading output module: ",e);
			}
		}
		
		this.printAll();
		
		
		return true;
	}
}
