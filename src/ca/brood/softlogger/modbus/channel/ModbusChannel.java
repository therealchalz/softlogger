package ca.brood.softlogger.modbus.channel;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import net.wimpi.modbus.msg.*;


public abstract class ModbusChannel {
	protected Logger log;
	
	public ModbusChannel() {
		log = Logger.getLogger(ModbusChannel.class);
	}
	
	public abstract boolean configure(Node channelNode);
	
	//These will likely require synchronization in most implementation
	public abstract boolean close();
	public abstract boolean isOpen();
	public abstract boolean open();
	public abstract ModbusResponse executeRequest(ModbusRequest req);
}
