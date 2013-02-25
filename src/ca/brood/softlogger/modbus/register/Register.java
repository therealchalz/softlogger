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
package ca.brood.softlogger.modbus.register;

import net.wimpi.modbus.msg.ModbusResponse;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.util.XmlConfigurable;

public abstract class Register implements XmlConfigurable {
	protected Logger log = Logger.getLogger(Register.class);;
	protected String fieldName;
	protected RegisterData registerData;
	
	protected Register() {
		fieldName = "";
		registerData = new RegisterData();
	}
	protected Register(Register r) {
		fieldName = r.fieldName;
		registerData = new RegisterData(r.registerData);
	}
	public boolean isNull() {
		return registerData.isNull();
	}
	public String getFieldName() {
		return fieldName;
	}
	public int getInteger() {
		return registerData.getInt();
	}
	public boolean getBoolean() {
		return registerData.getBool();
	}
	public int getShort() {
		return registerData.getShort();
	}
	public float getFloat() {
		return registerData.getFloat();
	}
	public RegisterData getData() {
		return new RegisterData(this.registerData);
	}
	public void setData(RegisterData data) {
		this.registerData.setData(data);
	}
	public void setData(ModbusResponse r) {
		registerData.setData(r);
	}
	public void setData(Integer i) {
		registerData.setData(i);
	}
	public void setData(Float f) {
		registerData.setDataFloat(f);
	}
	public void setData(Boolean b) {
		registerData.setData(b);
	}
	@Override
	public boolean configure(Node registerNode) {
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||	
				("#comment".compareToIgnoreCase(configNode.getNodeName())==0)) {
				continue;
			} else if (("fieldName".compareToIgnoreCase(configNode.getNodeName())==0)) {
				fieldName = configNode.getFirstChild().getNodeValue();
				registerNode.removeChild(configNode);
			}
		}
		if (fieldName.equals("")) {
			log.error("Invalid Register - No fieldname");
			return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return "Register: fieldname="+this.fieldName+", data: "+registerData.toString();
	}
}
