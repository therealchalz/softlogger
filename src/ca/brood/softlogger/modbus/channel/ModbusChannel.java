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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.xml.XMLConfigurable;

import net.wimpi.modbus.msg.*;


public abstract class ModbusChannel implements XMLConfigurable {
	protected Logger log;
	protected final int id;
	protected final int channelId;
	private static int nextId = 1;
	
	protected ModbusChannel(int chanId) {
		id = getNextId();
		channelId = chanId;
		log = LogManager.getLogger(ModbusChannel.class.toString()+" SoftloggerChannel: "+chanId+" ID: "+id);
	}
	public static synchronized int getNextId() {
		return nextId++;
	}
	@Override
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
	public boolean isReady() {
		if (isOpen()) {
			return true;
		} else {
			return open();
		}
	}
	public abstract ModbusResponse executeRequest(ModbusRequest req) throws Exception;
}
