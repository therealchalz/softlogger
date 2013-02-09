package ca.brood.softlogger.modbus.register;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class DataRegister extends RealRegister {
	private String guid = "";
	
	public DataRegister(int device) {
		super(device);
		log = Logger.getLogger(DataRegister.class);
	}
	public DataRegister(DataRegister d) {
		super(d);
		guid = d.guid;
	}
	private void setupLog() {
		log = Logger.getLogger(DataRegister.class+" GUID: "+this.guid);
	}
	public boolean configure(Node registerNode) {
		if (!super.configure(registerNode)) {
			return false;
		}
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0) ||
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if ("guid".compareToIgnoreCase(configNode.getNodeName())==0){
				this.guid = configNode.getFirstChild().getNodeValue();
			}else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		
		if (this.guid.equals("")) {
			log.error("No guid configured.");
			return false;
		}
		
		this.setupLog();
		
		//log.debug(this.toString());
		return true;
	}
	@Override
	public String toString() {
		return "DataRegister: guid="+this.guid+"; fieldname="+this.fieldName+"; address="+this.address+"; type="+this.regType+"; size="+this.size+"; scanRate="+this.scanRate+"; data: "+registerData.toString();
	}
}
