package ca.brood.softlogger.register;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class Register {
	protected Logger log;
	protected String fieldName;
	
	public Register() {
		log = Logger.getLogger(Register.class);
	}
	
	public boolean configure(Node registerNode) {
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("fieldName".compareToIgnoreCase(configNode.getNodeName())==0)) {
				fieldName = configNode.getFirstChild().getNodeValue();
				registerNode.removeChild(configNode);
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (fieldName.equals("")) {
			log.error("Invalid Register - No fieldname");
			return false;
		}
		return true;
	}
}
