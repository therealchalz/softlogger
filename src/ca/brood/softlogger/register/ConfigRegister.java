package ca.brood.softlogger.register;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;




public class ConfigRegister extends Register {
	public ConfigRegister() {
		super();
		log = Logger.getLogger(ConfigRegister.class);
	}
	public boolean configure(Node registerNode) {
		if (!super.configure(registerNode)) {
			return false;
		}
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		return true;
	}
}
