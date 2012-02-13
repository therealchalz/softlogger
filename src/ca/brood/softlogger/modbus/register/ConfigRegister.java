package ca.brood.softlogger.modbus.register;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;
import net.wimpi.modbus.msg.*;
import ca.brood.softlogger.util.*;

public class ConfigRegister extends RealRegister {
	private RegisterData value = null;
	public ConfigRegister(int device) {
		super(device);
		log = Logger.getLogger("ConfigRegister: D: "+device);
	}
	private void setupLog(int device, int address) {
		log = Logger.getLogger("ConfigRegister: D: "+device+" A: "+address);
	}
	
	public RegisterData getValue() {
		return value;
	}
	
	public boolean dataIsGood() {
		if (this.value.equals(this.registerData))
			return true;
		return false;
	}
	
	public ModbusRequest getWriteRequest() {
		WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(this.address, this.value);
		return req;
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
			} else if (("value".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.value = Util.parseRegisterData(configNode.getFirstChild().getNodeValue()); 
				} catch (NumberFormatException e) {
					log.error("Couldn't parse value to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (this.regType == RegisterType.INPUT_COIL || this.regType == RegisterType.INPUT_REGISTER) {
			log.error("Config register has a read-only type: "+this.regType);
			return false;
		}
		log.debug(this.toString());
		return true;
	}
	@Override
	public String toString() {
		return "ConfigRegister: fieldname="+this.fieldName+"; address="+this.address+"; size="+this.size+"; properValue="+this.value+"; actualData: "+registerData.toString();
	}
	 
}
