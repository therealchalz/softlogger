package ca.brood.softlogger.modbus.register;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;
import net.wimpi.modbus.msg.*;

public class ConfigRegister extends RealRegister {
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
			} else if (("value".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.value = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Couldn't parse value to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		log.trace(this.toString());
		return true;
	}
	@Override
	public String toString() {
		return "ConfigRegister: fieldname="+this.fieldName+"; address="+this.address+"; size="+this.size+"; value="+this.value;
	}
	
	public ReadMultipleRegistersRequest getRequest() {
		//TODO: What happens if this.size % this.address != 0?
		ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(this.address, this.size/this.sizePerAddress);
		return request;
	}
	 
}
