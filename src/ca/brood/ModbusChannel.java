package ca.brood;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModbusChannel {
	private Logger log;
	
	public ModbusChannel() {
		log = Logger.getLogger(ModbusChannel.class);
	}
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		return true;
	}
}
