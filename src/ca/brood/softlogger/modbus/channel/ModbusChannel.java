package ca.brood.softlogger.modbus.channel;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.wimpi.modbus.msg.*;


public abstract class ModbusChannel {
	protected Logger log;
	protected final int id;
	protected final int channelId;
	private static int nextId = 1;
	
	protected ModbusChannel(int chanId) {
		id = getNextId();
		channelId = chanId;
		log = Logger.getLogger(ModbusChannel.class.toString()+" SoftloggerChannel: "+chanId+" ID: "+id);
	}
	public static synchronized int getNextId() {
		return nextId++;
	}
	
	public boolean configure(Node channelNode) {
		NodeList configNodes = channelNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			}
		}
		return true;
	}
	
	public abstract void printAll();
	
	//These will likely require synchronization in most implementations
	public abstract boolean close();
	public abstract boolean isOpen();
	public abstract boolean open();
	public abstract ModbusResponse executeRequest(ModbusRequest req) throws Exception;
}
