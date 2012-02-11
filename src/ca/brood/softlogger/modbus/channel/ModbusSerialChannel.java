package ca.brood.softlogger.modbus.channel;

import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.util.*;
import net.wimpi.modbus.net.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModbusSerialChannel extends ModbusChannel {

	private String comport = "";
	private SerialParameters params = null;
	private int baud = 0;
	private int poll = 0;
	private SerialConnection connection = null;
	
	public ModbusSerialChannel(int chanId) {
		super(chanId);
		log = Logger.getLogger(ModbusSerialChannel.class.toString()+" SoftloggerChannel: "+chanId+" ID: "+id);
	}

	@Override
	public synchronized boolean close() {
		if (connection == null)
			return true;
		connection.close();
		return true;
	}

	@Override
	public synchronized boolean configure(Node channelNode) {
		this.close();
		this.params = null;
		NodeList configNodes = channelNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("comport".compareToIgnoreCase(configNode.getNodeName())==0))	{
				this.comport = configNode.getFirstChild().getNodeValue();
				log.debug("Comport set to: "+this.comport);
			} else if (("baud".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.baud = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					log.debug("Baud set to: "+this.baud);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse baud to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if (("poll".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.poll = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					log.debug("Polling rate set to: "+this.poll);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse polling rate to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		
		if (this.poll < 0 || this.poll > 2592000) {//30 days
			log.warn("Invalid poll, must be in [0,2592000]: "+this.poll+". Ignoring.");
			this.poll = 0;
		}
		if (this.comport.equals("")) {
			log.fatal("No com port selected.");
			return false;
		}
		if (this.baud == 0) {
			log.warn("No baud rate chosen.  Using default of 9600.");
			this.baud = 9600;
		}
		
		this.params = new SerialParameters();
		params.setBaudRate(this.baud);
		params.setPortName(this.comport);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding("rtu");
		params.setEcho(false);
		
		return true;
	}

	@Override
	public synchronized ModbusResponse executeRequest(ModbusRequest req) {
		log.error("Serial channel executeRequest not implemented");
		return null;
	}

	@Override
	public synchronized boolean isOpen() {
		if (connection == null)
			return false;
		return connection.isOpen();
	}

	@Override
	public synchronized boolean open() {
		if (params == null) {
			log.error("Trying to open an unconfigured Serial connection.");
			return false;
		}
		if (this.isOpen()) {
			log.error("Can't open serial connection; it's already open. Aborting");
			return false;
		}
		connection = new SerialConnection(params);
		try {
			connection.open();
		} catch (Exception e) {
			log.error("Serial channel could open connection");
			return false;
		}
		return true;
	}
}
