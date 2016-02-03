/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chache@brood.ca>. All rights reserved. 
 * 
 * This file is part of the softlogger project.
 * softlogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * softlogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with softlogger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache <chache@brood.ca> - initial API and implementation
 ******************************************************************************/
package ca.brood.softlogger.modbus.channel;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.*;

import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ModbusTcpChannel extends ModbusChannel {
	
	private TCPMasterConnection connection = null;
	private InetAddress host = null;
	private int port = 0;
	private boolean connected = false;
	
	public ModbusTcpChannel(int chanId) {
		super(chanId);
		log = LogManager.getLogger(ModbusTcpChannel.class+": "+id+" on Channel: "+chanId);
	}
	@Override
	public synchronized boolean close() {
		if (connected) {
			connection.close();
			connected = false;
		}
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
		//trans.setReconnecting(true);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusIOException e) {
			//channel closed?
			this.close();
			throw e;
		}
		return trans.getResponse();
	}

	@Override
	public synchronized boolean isOpen() {
		return connected;
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
			//log.error("Couldn't connect to the host: ",e);
			connection = null;
			connected = false;
			return false;
		}
		log.trace("Connected");
		connected = true;
		return true;
	}
	
	@Override
	public void printAll() {
		log.info("Host: " + this.host);
		log.info("Port: " + this.port);
	}
}
