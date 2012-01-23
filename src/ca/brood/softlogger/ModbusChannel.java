package ca.brood.softlogger;

import java.util.Vector;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ModbusChannel {
	private Logger log;
	
	private Vector<Device> devices;
	
	public ModbusChannel() {
		log = Logger.getLogger(ModbusChannel.class);
		devices = new Vector<Device>();
	}
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("device".compareToIgnoreCase(configNode.getNodeName())==0))	{
				Device d = new Device();
				d.configure(configNode);
				devices.add(d);
			}else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		return true;
	}
}
