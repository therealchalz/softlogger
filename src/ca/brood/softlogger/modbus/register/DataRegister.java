package ca.brood.softlogger.modbus.register;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class DataRegister extends RealRegister {
	public DataRegister(int device) {
		super(device);
		log = Logger.getLogger(DataRegister.class);
	}
	private void setupLog(int device, int address) {
		log = Logger.getLogger("DataRegister: D: "+device+" A: "+address);
	}
	public boolean configure(Node registerNode) {
		if (!super.configure(registerNode)) {
			return false;
		}
		this.setupLog(device, address);
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		log.debug(this.toString());
		return true;
	}
	@Override
	public String toString() {
		return "DataRegister: fieldname="+this.fieldName+"; address="+this.address+"; size="+this.size+"; data: "+registerData.toString();
	}
}
