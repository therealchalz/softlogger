package ca.brood.softlogger;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import ca.brood.softlogger.dataoutput.DataOutputManager;
import ca.brood.softlogger.dataoutput.DataServer;
import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.util.*;


import java.io.File;


public class Softlogger {
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
	 * -dataFunction element (and type attribute)
	 * 
	 * Longer term TODO:
	 * -logging to local database so other things can use the data (web realtime frontend etc)
	 * -sending of file to remote server
	 * -email alerts
	 * -reverse tunnel
	 * -uptime tracking(cpu, internet, rtunnel)
	 *
	 */
	
	
	public Softlogger() {
		log = Logger.getLogger(Softlogger.class);
		PropertyConfigurator.configure("logger.config");
		softloggerChannels = new ArrayList<SoftloggerChannel>();
		dataOutputManager = new DataOutputManager();
	}
	public void configure(String configFile) {
		log.info("");
		log.info("");
		log.info("******************************");
		log.info("SOFTLOGGER IS STARTING");
		log.info("******************************");
		log.info("Softlogger using config file: "+configFile);
		configFilePath = configFile;
		if (!loadConfig(configFilePath)) {
			log.fatal("Error loading config file.");
		}
	}
	public static void main(String[] args) {
		Softlogger s = new Softlogger();
		s.configure("config.xml");
		s.run();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
		s.stop();
		ThreadPerformanceMonitor.printPerformanceData();
		s.log.info("All done");
	}
	public void kill() {
		log.info("Softlogger killing softloggerChannels");
	}
	public void stop() {
		log.info("Softlogger stopping softloggerChannels");
		dataOutputManager.stop();
		for (int i=0; i<softloggerChannels.size(); i++) {
			softloggerChannels.get(i).stop();
		}
	}
	
	public void run() {
		//Start all the softloggerChannels, which will in turn start all the devices
		for (int i=0; i<softloggerChannels.size(); i++) {
			softloggerChannels.get(i).start();
		}
		
		ArrayList<Device> devices = new ArrayList<Device>();
		for (SoftloggerChannel channel : softloggerChannels) {
			devices.addAll(channel.getDevices());
		}
		dataOutputManager.refresh(devices);
		dataOutputManager.start();
	}

	private boolean loadConfig(String filename) {
		File xmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setValidating(true);
		dbFactory.setNamespaceAware(true);
		XmlErrorCallback error = new XmlErrorCallback();
		Document doc;
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.setErrorHandler(new SimpleXmlErrorHandler(this.log, error));
			doc = dBuilder.parse(xmlFile);
			
			doc.getDocumentElement().normalize();
			if (!error.isConfigValid()) {
				throw new Exception("Config doesn't conform to schema.");
			}
		} catch (Exception e) {
			log.fatal("Exception while trying to load config file: "+filename + " " + e.getMessage());
			return false;
		}
		Node currentConfigNode = doc.getDocumentElement();
		NodeList loggerConfigNodes = currentConfigNode.getChildNodes();
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
		loggerConfigNodes = doc.getElementsByTagName("server");
		if (loggerConfigNodes.getLength() == 0) {
			log.fatal("Could not find a server defined in the config file.");
			return false;
		}
		if (loggerConfigNodes.getLength() > 1) {
			log.fatal("Too many servers are defined in the config file");
			return false;
		}
		currentConfigNode = loggerConfigNodes.item(0);
		server = new DataServer();
		if (!server.configure(currentConfigNode)) {
			return false;
		}
		
		//Load the softloggerChannels
		loggerConfigNodes = doc.getElementsByTagName("channel");
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
		
		//Load the global output modules
		
		for (int i=0; i<softloggerChannels.size(); i++) {
			softloggerChannels.get(i).setDefaultScanRate(this.defaultScanRate);
		}
		
		this.printAll();
		
		
		return true;
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
}
