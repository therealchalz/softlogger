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
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.util.*;
import net.wimpi.modbus.net.*;

import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.util.Util;
import jssc.SerialPort;

public class ModbusSerialChannel extends ModbusChannel {

	private String comport = "";
	private SerialParameters params = null;
	private int baud = 0;
	private SerialConnection connection = null;
	private int interRequestDelay = 50;	//ms
	private long lastRequest = 0;
	private boolean rs485Echo = false;;
	
	public ModbusSerialChannel(int chanId) {
		super(chanId);
		log = LogManager.getLogger(ModbusSerialChannel.class.toString()+" SoftloggerChannel: "+chanId+" ID: "+id);
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
		super.configure(channelNode);
		NodeList configNodes = channelNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("comport".compareToIgnoreCase(configNode.getNodeName())==0))	{
				this.comport = configNode.getFirstChild().getNodeValue();
				log.debug("Comport set to: "+this.comport);
			} else if (("echo".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.rs485Echo = Util.parseBool(configNode.getFirstChild().getNodeValue());
					log.debug("RS485 Echo set to: "+this.rs485Echo);
				} catch (Exception e) {
					log.error("Couldn't parse echo to boolean from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if (("baud".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.baud = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					log.debug("Baud set to: "+this.baud);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse baud to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if (("requestDelay".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.interRequestDelay = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					log.debug("Inter-request delay set to: "+this.interRequestDelay);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse requestDelay to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
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
		params.setEcho(rs485Echo);
		params.setFlowControlIn(SerialPort.FLOWCONTROL_NONE);
		params.setFlowControlOut(SerialPort.FLOWCONTROL_NONE);
		params.setReceiveTimeout(500);
		
		return true;
	}

	@Override
	public synchronized ModbusResponse executeRequest(ModbusRequest req) throws Exception,ModbusException  {
		if (!isOpen()) {
			log.warn("Trying to execute request on closed serial connection");
			if (!this.open()) {
				log.error("Couldn't open the connection... Aborting.");
				throw new Exception("Connection Closed");
			}
		}
		long currentTime = System.nanoTime();
		if (currentTime - (this.lastRequest + this.interRequestDelay * 1000000l) < 0) {
			long sleeptime = (lastRequest + (interRequestDelay * 1000000l) - currentTime)/1000000l;
			log.trace("Sleeping for "+sleeptime+" milliseconds.");
			Thread.sleep(sleeptime);
			currentTime = System.nanoTime();
		}
		lastRequest = currentTime;
		ModbusSerialTransaction trans = new ModbusSerialTransaction(this.connection);
		trans.setRequest(req);
		trans.execute();
		return trans.getResponse();
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
			connection.setReceiveTimeout(500);
		} catch (Exception e) {
			log.error("Serial channel could not open connection", e);
			return false;
		}
		return true;
	}
	
	@Override
	public void printAll() {
		log.info("COM Port: "+this.comport);
		log.info("Baud rate: "+this.baud);
	}
}
