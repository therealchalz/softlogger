package ca.brood.softlogger.modbus.register;

import net.wimpi.modbus.msg.*;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class DataRegister extends RealRegister {
	public DataRegister() {
		super();
		log = Logger.getLogger(DataRegister.class);
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
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		log.trace(this.toString());
		return true;
	}
	@Override
	public String toString() {
		return "DataRegister: fieldname="+this.fieldName+"; address="+this.address+"; size="+this.size+"; data: "+registerData.toString();
	}
}
