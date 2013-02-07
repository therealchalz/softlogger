package ca.brood.softlogger.modbus.channel;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ModbusTcpChannel extends ModbusChannel {
	
	private TCPMasterConnection connection = null;
	private InetAddress host = null;
	private int port = 0;
	
	public ModbusTcpChannel(int chanId) {
		super(chanId);
		log = Logger.getLogger(ModbusTcpChannel.class+": "+id+" on Channel: "+chanId);
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
		this.host = null;
		super.configure(channelNode);
		NodeList configNodes = channelNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("host".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					//TODO: Speed this up by checking if the config is a file (in which case use getByAddress instead, and avoid the NS lookup)
					this.host = InetAddress.getByName(configNode.getFirstChild().getNodeValue());
				} catch (UnknownHostException e) {
					log.fatal("Could not get proper address for host: "+configNode.getFirstChild().getNodeValue());
					return false;
				}
				//log.debug("Host set to: "+this.host.getHostAddress()+" ("+this.host.getHostName()+")");
			} else if (("port".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.port = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					//log.debug("Port set to: "+this.port);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse port to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		
		if (this.port > 65535 || this.port < 1) {
			log.fatal("Bad port configured: "+this.port);
			return false;
		}
		if (this.port == 0) {
			log.warn("No port configured.  Using default of 502");
			this.port = 502;
		}
		if (this.host == null) {
			log.fatal("No host configured.  Can't continue.");
			return false;
		}
		
		return true;
	}

	@Override
	public synchronized ModbusResponse executeRequest(ModbusRequest req) throws Exception,ModbusException {
		if (!isOpen()) {
			log.warn("Trying to execute request on closed TCP connection");
			if (!this.open()) {
				log.error("Couldn't open the connection... Aborting.");
				throw new Exception("Connection Closed");
			}
		}
		
		ModbusTCPTransaction trans = new ModbusTCPTransaction(this.connection);
		
		trans.setRequest(req);
		trans.execute();
		return trans.getResponse();
	}

	@Override
	public synchronized boolean isOpen() {
		if (connection == null)
			return false;
		return connection.isConnected();
	}

	@Override
	public synchronized boolean open() {
		if (this.isOpen()) {
			log.warn("Trying to open an already open connection... aborting");
			return true;
		}
		connection = new TCPMasterConnection(this.host);
		connection.setPort(this.port);
		try {
			connection.connect();
		} catch (Exception e) {
			log.error("Couldn't connect to the host: ");
			log.error(e);
			return false;
		}
		return true;
	}
	
	@Override
	public void printAll() {
		log.info("Host: " + this.host);
		log.info("Port: " + this.port);
	}
}
