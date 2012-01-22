package ca.brood;
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
	
	
	public Softlogger() {
		log = Logger.getLogger(Softlogger.class);
		PropertyConfigurator.configure("logger.config");
		
	}
	public void run() {
		log.info("Softlogger starting...");
		loadConfig("config.xml");
	}
	public static void main(String[] args) {
		Softlogger s = new Softlogger();
		s.run();
	}

	private int loadConfig(String filename) {
		File fXmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
