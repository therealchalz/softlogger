package ca.brood;

import java.util.Vector;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;


public class Softlogger {
	private Logger log;
	private boolean configValid = false;
	
	private String loggerName = "Unnamed Logger";
	private int defaultDevicePoll = 60;
	private String logFilePath = "log/";
	private String tableFilePath = "lut/";
	private String dataFilePath = "data/";
	private String configFilePath = "";
	
	private DataServer server;
	private Vector<ModbusChannel> channels;
	
	public Softlogger() {
		log = Logger.getLogger(Softlogger.class);
		PropertyConfigurator.configure("logger.config");
		configValid = false;
	}
	public void configure(String configFile) {
		log.info("******************************");
		log.info("Softlogger starting...  Using "+configFile);
		configFilePath = configFile;
		if (!loadConfig(configFilePath)) {
			log.fatal("Error loading config file.");
		}
	}
	public static void main(String[] args) {
		Softlogger s = new Softlogger();
		s.configure("config.xml");
	}

	private boolean loadConfig(String filename) {
		File xmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc;
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			log.fatal("Exception while trying to load config file: "+filename);
			return false;
		}
		Node currentConfigNode = doc.getDocumentElement();
		NodeList loggerConfigNodes = currentConfigNode.getChildNodes();
		log.debug("Configuring Logger...");
		for (int i=0; i<loggerConfigNodes.getLength(); i++) {
			Node configNode = loggerConfigNodes.item(i);
			if ("name".compareToIgnoreCase(configNode.getNodeName())==0){
				log.debug("Logger Name: "+configNode.getFirstChild().getNodeValue());
				this.loggerName = configNode.getFirstChild().getNodeValue();
			} else if ("poll".compareToIgnoreCase(configNode.getNodeName())==0){
				log.debug("Default poll period: "+configNode.getFirstChild().getNodeValue());
				this.defaultDevicePoll = Integer.parseInt(configNode.getFirstChild().getNodeValue());
			} else if ("tableFilePath".compareToIgnoreCase(configNode.getNodeName())==0){
				log.debug("Lookup table path: "+configNode.getFirstChild().getNodeValue());
				this.tableFilePath = configNode.getFirstChild().getNodeValue();
			} else if ("dataFilePath".compareToIgnoreCase(configNode.getNodeName())==0){
				log.debug("Data file path: "+configNode.getFirstChild().getNodeValue());
				this.dataFilePath = configNode.getFirstChild().getNodeValue();
			} else if (("server".compareToIgnoreCase(configNode.getNodeName())==0) || 
			("channel".compareToIgnoreCase(configNode.getNodeName())==0)|| 
			("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (loggerName.equals("")) {
			log.warn("Softlogger name is blank");
		}
		if (defaultDevicePoll < 0) {
			log.warn("Softlogger default device poll rate is invalid.  Using default of 60.");
			defaultDevicePoll = 60;
		}
		if (tableFilePath.equals("")) {
			log.warn("Softlogger lookup table file path is invalid.  Using default of lut/");
			tableFilePath = "lut/";
		}
		if (dataFilePath.equals("")) {
			log.warn("Softlogger data file path is invalid.  Using default of data/");
			dataFilePath = "data/";
		}
		
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
		
		loggerConfigNodes = doc.getElementsByTagName("channel");
		channels = new Vector<ModbusChannel>();
		boolean workingChannel = false;
		for (int i=0; i<loggerConfigNodes.getLength(); i++) {
			currentConfigNode = loggerConfigNodes.item(i);
			ModbusChannel mc = new ModbusChannel();
			if (mc.configure(currentConfigNode)) {
				workingChannel = true;
				channels.add(mc);
			}
		}
		
		if (!workingChannel) {
			log.fatal("No usable channel configured");
			return false;
		}
		
		return true;
	}
}
