/*******************************************************************************
 * Copyright (c) 2013-2016 Charles Hache <chache@cygnustech.ca>.  
 * All rights reserved. 
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
 * along with softlogger.  If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 * 
 * Contributors:
 *     Charles Hache <chache@cygnustech.ca> - initial API and implementation
 ******************************************************************************/

package ca.brood.softlogger.modbus.register;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.logging.log4j.LogManager;
import net.wimpi.modbus.msg.*;
import ca.brood.softlogger.util.*;

public class ConfigRegister extends RealRegister {
	private RegisterData value = null;
	public ConfigRegister(int device) {
		super(device);
		log = LogManager.getLogger(ConfigRegister.class+": D: "+device);
	}
	public ConfigRegister(ConfigRegister c) {
		super(c);
		this.value = new RegisterData(c.value);
	}
	@Override
	public ConfigRegister clone() {
		return new ConfigRegister(this);
	}
	private void setupLog(int device, int address) {
		log = LogManager.getLogger(ConfigRegister.class+": D: "+device+" A: "+address);
	}
	
	public RegisterData getValue() {
		return value;
	}
	
	public boolean dataIsGood() {
		if (this.value.equals(this.registerData))
			return true;
		return false;
	}
	
	public ModbusRequest getWriteRequest() {
		//TODO:
		ModbusRequest req = null;
		if (this.getRegisterType() == RegisterType.OUTPUT_COIL) {
			req = new WriteCoilRequest(this.address, this.value.getBool());
		}
		if (this.getRegisterType() == RegisterType.OUTPUT_REGISTER) {
			if (this.size == 2) {
				req = new WriteMultipleRegistersRequest(this.address, this.value.getBothRegisters());
			} else {
				req = new WriteSingleRegisterRequest(this.address, this.value);
			}
		}
		return req;
	}
	
	public boolean configure(Node registerNode) {
		if (!super.configure(registerNode)) {
			return false;
		}
		this.setupLog(device, address);
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0) || 
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("value".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.value = Util.parseRegisterData(configNode.getFirstChild().getNodeValue()); 
				} catch (NumberFormatException e) {
					log.error("Couldn't parse value to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (this.regType == RegisterType.INPUT_COIL || this.regType == RegisterType.INPUT_REGISTER) {
			log.error("Config register has a read-only type: "+this.regType);
			return false;
		}
		
		this.sampling = Sampling.LATEST;
		
		//log.debug(this.toString());
		return true;
	}
	@Override
	public String toString() {
		return "ConfigRegister: fieldname="+this.fieldName+"; address="+this.address+"; type="+this.regType+"; size="+this.size+"; scanRate="+this.scanRate+"; properValue="+this.value+"; actualData: "+registerData;
	}
	 
}
