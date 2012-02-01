package ca.brood.softlogger.modbus.register;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;


public class ConfigRegister extends Register {
	private int address = Integer.MAX_VALUE;
	private int size = 0;
	private Integer value = null;
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
			} else if (("#regAddr".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.address = Integer.parseInt(configNode.getFirstChild().getNodeValue(),16);
					log.debug("Address set to: "+this.address);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse regAddr to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if (("#size".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.size = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					log.debug("Size set to: "+this.size);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse size to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if (("#value".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.value = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					log.debug("Value set to: "+this.value);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse value to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (this.address < 1 || this.address > 65535) {
			log.fatal("Parsed invalid address: "+this.address);
			return false;
		}
		if (this.size < 1 || this.size > 4) {
			log.fatal("Invalid size: "+this.size);
			return false;
		}
		return true;
	}
}
