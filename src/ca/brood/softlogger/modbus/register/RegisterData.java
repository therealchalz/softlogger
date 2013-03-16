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

import java.text.DateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.procimg.SimpleRegister;

public class RegisterData implements net.wimpi.modbus.procimg.Register{
	private Integer dataInt = null;
	private Float dataFloat = null;
	private Boolean dataBool = null;
	private Date timestamp = null;
	private Logger log = Logger.getLogger(RegisterData.class);
	public RegisterData() {
		
	}
	public RegisterData(RegisterData r) {
		dataInt = r.dataInt;
		dataFloat = r.dataFloat;
		dataBool = r.dataBool;
		timestamp = r.timestamp;
	}
	public net.wimpi.modbus.procimg.Register getHighRegisterInt() {
		 net.wimpi.modbus.procimg.Register ret = new SimpleRegister(dataInt>>16);
		 return ret;
	}
	public net.wimpi.modbus.procimg.Register getLowRegisterInt() {
		 net.wimpi.modbus.procimg.Register ret = new SimpleRegister(dataInt&0xFFFF);
		 return ret;
	}
	public net.wimpi.modbus.procimg.Register[] getBothRegisters() {
		net.wimpi.modbus.procimg.Register[] ret = new net.wimpi.modbus.procimg.Register[2];
		ret[0] = getHighRegisterInt();
		ret[1] = getLowRegisterInt();
		return ret;
	}
	public RegisterData(ModbusResponse resp) {
		this.setData(resp);
	}
	
	public void nullData() {
		dataInt = null;
		dataFloat = null;
		dataBool = null;
		timestamp = null;
	}
	
	public boolean isNull() {
		return (dataInt == null && dataFloat == null && dataBool == null);
	}
	
	/** Returns the value of this register interpreted as signed 16 bits.
	 * Always check that the register is not null first.
	 * @return The 16-bit signed integer value of this register.
	 */
	public Short getShort() {
		return dataInt.shortValue();
	}
	/** Returns the value of this register interpreted as signed 32 bits/unsigned 16 bits.
	 * Always check that the register is not null first.
	 * @return The 16-bit unsigned/32-bit signed integer value of this register.
	 */
	public Integer getInt() {
		return dataInt;
	}
	public Float getFloat() {
		return dataFloat;
	}
	public Boolean getBool() {
		return dataBool;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setData(RegisterData r) {
		dataInt = r.dataInt;
		dataFloat = r.dataFloat;
		dataBool = r.dataBool;
		timestamp = r.timestamp;
	}
	public void setData(Boolean b, Date timestamp) {
		this.timestamp = timestamp;
		setData(b);
	}
	public void setData(Boolean b) {
		if (b) {
			dataInt = 1;
			dataFloat = 1f;
			dataBool = true;
		} else {
			dataInt = 0;
			dataFloat = 0f;
			dataBool = false;
		}
	}
	public void setData(Integer i, Date timestamp) {
		this.timestamp = timestamp;
		setData(i);
	}
	public void setData(Integer i) {
		dataInt = i;
		dataFloat = new Float(i);
		dataBool = (i != 0 ? true : false);
	}
	public void setDataFloat(Integer i, Date timestamp) {
		this.timestamp = timestamp;
		setDataFloat(i);
	}
	public void setDataFloat(Integer i) {
		dataFloat = Float.intBitsToFloat(i);
		dataInt = dataFloat.intValue();
		dataBool = (i != 0 ? true : false);
	}
	public void setDataFloat(Float f, Date timestamp) {
		this.timestamp = timestamp;
		setDataFloat(f);
	}
	public void setDataFloat(Float f) {
		dataFloat = new Float(f);
		dataInt = dataFloat.intValue();
		dataBool = (dataInt != 0 ? true : false);
	}
	public void setData(ModbusResponse resp, Date timestamp) {
		this.timestamp = timestamp;
		setData(resp);
	}
	public void setData(ModbusResponse resp) {
		if (resp.getClass() == ReadCoilsResponse.class) {
			if (((ReadCoilsResponse)resp).getBitCount() != 1) {
				log.error("Cannot parse ReadCoilsResponse because received unexpected number of coils: "+((ReadCoilsResponse)resp).getBitCount());
				this.nullData();
				return;
			}
			setData(((ReadCoilsResponse)resp).getCoilStatus(0));
		} else if (resp.getClass() == WriteCoilResponse.class) {
			setData(((WriteCoilResponse)resp).getCoil());
		} else if (resp.getClass() == ReadInputDiscretesResponse.class) {
			if (((ReadInputDiscretesResponse)resp).getBitCount() != 1) {
				log.error("Cannot parse ReadInputDiscretesResponse because received unexpected number of coils: "+((ReadInputDiscretesResponse)resp).getBitCount());
				this.nullData();
				return;
			}
			setData(((ReadInputDiscretesResponse)resp).getDiscreteStatus(0));
		} else if (resp.getClass() == ReadInputRegistersResponse.class) {
			if (((ReadInputRegistersResponse)resp).getWordCount() == 1) {
				//16 bit register reading.  read it as int, set the float value equal to the int value, and set the bool following C-style evaluation rules
				setData(new Integer(((ReadInputRegistersResponse)resp).getRegister(0).getValue()));
			} else if (((ReadInputRegistersResponse)resp).getWordCount() == 2) {
				//32-bit register reading
				//TODO: figure out how to decide if we should treat the number as IEEE 754 or not.  Right now assume it is.
				setDataFloat(((ReadInputRegistersResponse)resp).getRegister(0).getValue() << 16 + ((ReadInputRegistersResponse)resp).getRegister(1).getValue());
			} else { //not 1 or 2 words
				log.error("Cannot parse ReadInputRegistersResponse because received unexpected number of words: "+((ReadInputRegistersResponse)resp).getWordCount());
				this.nullData();
				return;
			}
		} else if (resp.getClass() == ReadMultipleRegistersResponse.class) {
			if (((ReadMultipleRegistersResponse)resp).getWordCount() == 1) {
				//16 bit register reading.  read it as int, set the float value equal to the int value, and set the bool following C-style evaluation rules
				setData(((ReadMultipleRegistersResponse)resp).getRegister(0).getValue());
			} else if (((ReadMultipleRegistersResponse)resp).getWordCount() == 2) {
				//32-bit register reading
				//TODO: figure out how to decide if we should treat the number as IEEE 754 or not.  Right now assume it is.
				setDataFloat(((ReadMultipleRegistersResponse)resp).getRegister(0).getValue() << 16 + ((ReadMultipleRegistersResponse)resp).getRegister(1).getValue());
			} else { //not 1 or 2 words
				log.error("Cannot parse ReadMultipleRegistersResponse because received unexpected number of words: "+((ReadMultipleRegistersResponse)resp).getWordCount());
				this.nullData();
				return;
			}
		} else if (resp.getClass() == WriteSingleRegisterResponse.class) {
			setData(((WriteSingleRegisterResponse)resp).getRegisterValue());
		} else {
			log.error("Got UNKNOWN RESPONSE");
		}
	}
	@Override
	public String toString() {
		String ret = "";
		if (timestamp != null) {
			ret+="Timestamp: "+DateFormat.getDateTimeInstance().format(timestamp);
		}
		if (dataInt != null) {
			ret+="Int: "+dataInt+", ";
		} else {
			ret+="Int: null, ";
		}
		if (dataFloat != null) {
			ret+="Float: "+dataFloat+", ";
		} else {
			ret+="Float: null, ";
		}
		if (dataBool != null) {
			ret+="Bool: "+dataBool;
		} else {
			ret+="Bool: null";
		}
		return ret;
	}
	@Override
	public boolean equals(Object other) {
		if (RegisterData.class != other.getClass()) {
			return false;
		}
		RegisterData d = (RegisterData)other;
		if (this.dataBool == null && d.dataBool != null)
			return false;
		if (this.dataInt == null && d.dataInt != null)
			return false;
		if (this.dataFloat == null && d.dataFloat != null)
			return false;
		if (!(this.dataBool == null) && !(this.dataBool.equals(d.dataBool))) {
			return false;
		}
		if (!(this.dataInt == null) && !(this.dataInt.equals(d.dataInt))) {
			return false;
		}
		if (!(this.dataFloat == null) && !(this.dataFloat.equals(d.dataFloat))) {
			return false;
		}
		return true;
	}
	
	/*
	 * These functions are from the JAMOD Register interface
	 */
	@Override
	public void setValue(int v) {
		this.setData(v);
	}
	@Override
	public void setValue(short s) {
		int i = s;
		this.setData(i);
	}
	@Override
	public void setValue(byte[] bytes) {
		if (bytes.length >= 2) {
			int i = bytes[0] << 8 + bytes[1];
			if (bytes.length == 4) {
				i = i << 16;
				i += bytes[2] << 8 + bytes[3];
			}
			this.setData(i);
		} else {
			this.nullData();
		}
	}
	@Override
	public int getValue() {
		Integer i = this.getInt();
		if (i != null)
			return i;
		else {
			log.warn("Called getValue on unset register");
			return 0;
		}
	}
	@Override
	public byte[] toBytes() {
		Integer oi = this.getInt();
		if (oi != null) {
			int i = oi;
			byte[] ret = new byte[4];
			ret[1] = (byte)(i&0xff);
			ret[0] = (byte)((i&0xff00)>>8);
			ret[3] = (byte)((i&0xff0000)>>16);
			ret[2] = (byte)((i&0xff000000)>>24);
			return ret;
		} else {
			log.warn("Called getValue on unset register");
			return null;
		}
	}
	@Override
	public short toShort() {
		Integer i = this.getInt();
		if (i != null) {
			short s = (short)((int)i);
			return s;
		} else {
			log.warn("Called getValue on unset register");
			return 0;
		}
	}
	@Override
	public int toUnsignedShort() {
		return this.getValue();
	}
}
