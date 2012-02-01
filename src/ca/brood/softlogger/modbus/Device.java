package ca.brood.softlogger.modbus;
import ca.brood.softlogger.modbus.register.*;

import java.util.Vector;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Device implements Runnable {
	private Logger log;
	
	private int address;
	private String description;
	private Vector<ConfigRegister> configRegs;
	private Vector<DataRegister> dataRegs;
	private Vector<VirtualRegister> virtualRegs;
	
	public Device() {
		log = Logger.getLogger(Device.class);
		configRegs = new Vector<ConfigRegister>();
		dataRegs = new Vector<DataRegister>();
		virtualRegs = new Vector<VirtualRegister>();
	}
	
	public boolean configure(Node deviceNode) {
		NodeList configNodes = deviceNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("address".compareToIgnoreCase(configNode.getNodeName())==0)) {
				this.address = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				log.debug("Device address: "+this.address);
			} else if (("description".compareToIgnoreCase(configNode.getNodeName())==0)) {
				this.description = configNode.getFirstChild().getNodeValue();
				log.debug("Device description: "+this.description);
			} else if (("configregister".compareToIgnoreCase(configNode.getNodeName())==0)) {
				ConfigRegister c = new ConfigRegister();
				c.configure(configNode);
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		return true;
	}
	public int getPollRate() {
		return 0;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
