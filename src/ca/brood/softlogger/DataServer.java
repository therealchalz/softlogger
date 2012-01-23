package ca.brood.softlogger;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataServer {
	private Logger log;
	
	private String username;
	private String host;
	private String password;
	private String keyfile;
	private String serverPath;
	private int configPoll;
	public DataServer() {
		log = Logger.getLogger(DataServer.class);
		log.info("DataServer Loading...");
	}
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("user".compareToIgnoreCase(configNode.getNodeName())==0))	{
				log.debug("Server username: "+configNode.getFirstChild().getNodeValue());
				username = configNode.getFirstChild().getNodeValue();
			} else if (("host".compareToIgnoreCase(configNode.getNodeName())==0))	{
				log.debug("Server host: "+configNode.getFirstChild().getNodeValue());
				host = configNode.getFirstChild().getNodeValue();
			} else if (("key".compareToIgnoreCase(configNode.getNodeName())==0))	{
				log.debug("Server keyfile: "+configNode.getFirstChild().getNodeValue());
				keyfile = configNode.getFirstChild().getNodeValue();
			} else if (("password".compareToIgnoreCase(configNode.getNodeName())==0))	{
				log.debug("Server password: "+configNode.getFirstChild().getNodeValue().replaceAll(".", "*"));
				password = configNode.getFirstChild().getNodeValue();
			} else if (("configpoll".compareToIgnoreCase(configNode.getNodeName())==0))	{
				log.debug("Server poll config period: "+configNode.getFirstChild().getNodeValue());
				try {
					configPoll = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid server poll config period.  Using default of 3600.");
					configPoll = 3600;
				}
			} else if (("path".compareToIgnoreCase(configNode.getNodeName())==0))	{
				log.debug("Server data file path: "+configNode.getFirstChild().getNodeValue());
				serverPath = configNode.getFirstChild().getNodeValue();
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (username.equals("")) { log.fatal("Missing user in server definition.");
			return false;
		}
		if (host.equals("")) { log.fatal("Missing host in server definition.");
			return false;
		}
		if (serverPath.equals("")) { log.fatal("Missing path in server definition.");
			return false;
		}
		if (configPoll < 0 || configPoll > 2592000) {//30 days
			log.error("Server poll config is out of range (must be in [1,2592000]).  Using default of 3600.");
			configPoll = 3600;
		}
		if (password.equals("") && keyfile.equals("")) {
			log.fatal("Must specify a keyfile or a password in the server definition");
			return false;
		}
		return true;
	}
}
