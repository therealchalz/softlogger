package ca.brood.softlogger.modbus.channel;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.wimpi.modbus.msg.*;


public abstract class ModbusChannel {
	protected Logger log;
	protected int poll = 0;
	protected int defaultPoll = 0;
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
	
	public int getPollRate() {
		if (poll != 0)
			return poll;
		return defaultPoll;
	}
	
	public void setDefaultPoll(int poll) {
		defaultPoll = poll;
	}
	
	public boolean configure(Node channelNode) {
		NodeList configNodes = channelNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("poll".compareToIgnoreCase(configNode.getNodeName())==0)) {
				try {
					this.poll = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid device poll: "+configNode.getFirstChild().getNodeValue());
					this.poll = 0;
				}
				channelNode.removeChild(configNode);
			}
		}
		return true;
	}
	
	//These will likely require synchronization in most implementations
	public abstract boolean close();
	public abstract boolean isOpen();
	public abstract boolean open();
	public abstract ModbusResponse executeRequest(ModbusRequest req) throws Exception;
}
