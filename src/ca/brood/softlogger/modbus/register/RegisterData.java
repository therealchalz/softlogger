package ca.brood.softlogger.modbus.register;

import org.apache.log4j.Logger;

import net.wimpi.modbus.msg.*;

public class RegisterData {
	private Integer dataInt = null;
	private Float dataFloat = null;
	private Boolean dataBool = null;
	private Logger log = Logger.getLogger(RegisterData.class);
	public RegisterData() {
		
	}
	public RegisterData(ModbusResponse resp) {
		this.setData(resp);
	}
	
	private void nullData() {
		dataInt = null;
		dataFloat = null;
		dataBool = null;
	}
	//Should always check for null first when getting these values
	public Integer getInt() {
		return dataInt;
	}
	public Float getFloat() {
		return dataFloat;
	}
	public Boolean getBool() {
		return dataBool;
	}
	public void setData(Boolean b) {
		if (b) {
			dataInt = null;
			dataFloat = null;
			dataBool = true;
		} else {
			dataInt = null;
			dataFloat = null;
			dataBool = false;
		}
	}
	public void setData(Integer i) {
		dataInt = i;
		dataFloat = new Float(i);
		dataBool = null;
	}
	public void setDataFloat(Integer i) {
		dataFloat = Float.intBitsToFloat(i);
		dataInt = dataFloat.intValue();
		dataBool = null;
	}
	public void setDataFloat(Float f) {
		dataFloat = new Float(f);
		dataInt = dataFloat.intValue();
		dataBool = null;
	}
	public void setData(ModbusResponse resp) {
		if (resp.getClass() == ReadCoilsResponse.class) {
			if (((ReadCoilsResponse)resp).getBitCount() != 1) {
				log.error("Cannot parse ReadCoilsResponse because received unexpected number of coils: "+((ReadCoilsResponse)resp).getBitCount());
				this.nullData();
				return;
			}
			setData(((ReadCoilsResponse)resp).getCoilStatus(0));
		}
		if (resp.getClass() == ReadInputDiscretesResponse.class) {
			if (((ReadInputDiscretesResponse)resp).getBitCount() != 1) {
				log.error("Cannot parse ReadInputDiscretesResponse because received unexpected number of coils: "+((ReadInputDiscretesResponse)resp).getBitCount());
				this.nullData();
				return;
			}
			setData(((ReadInputDiscretesResponse)resp).getDiscreteStatus(0));
		}
		if (resp.getClass() == ReadInputRegistersResponse.class) {
			if (((ReadInputRegistersResponse)resp).getWordCount() == 1) {
				//16 bit register reading.  read it as int, set the float value equal to the int value, and set the bool following C-style evaluation rules
				setData(new Integer(((ReadInputRegistersResponse)resp).getRegister(0).getValue()));
			} else if (((ReadInputRegistersResponse)resp).getWordCount() == 2) {
				//32-bit register reading
				//TODO: figure out how to decide if we should treat the number as IEEE 754 or not.  Right now assume it is.
				setDataFloat(((ReadInputRegistersResponse)resp).getRegister(0).getValue() << 16 + ((ReadInputRegistersResponse)resp).getRegister(1).getValue());
			} else { //not 1 or 2 words
				log.error("Cannot parse ReadInputDiscretesResponse because received unexpected number of coils: "+((ReadInputDiscretesResponse)resp).getBitCount());
				this.nullData();
				return;
			}
		}
		if (resp.getClass() == ReadMultipleRegistersResponse.class) {
			if (((ReadMultipleRegistersResponse)resp).getWordCount() == 1) {
				//16 bit register reading.  read it as int, set the float value equal to the int value, and set the bool following C-style evaluation rules
				setData(((ReadMultipleRegistersResponse)resp).getRegister(0).getValue());
			} else if (((ReadMultipleRegistersResponse)resp).getWordCount() == 2) {
				//32-bit register reading
				//TODO: figure out how to decide if we should treat the number as IEEE 754 or not.  Right now assume it is.
				setDataFloat(((ReadMultipleRegistersResponse)resp).getRegister(0).getValue() << 16 + ((ReadInputRegistersResponse)resp).getRegister(1).getValue());
			} else { //not 1 or 2 words
				log.error("Cannot parse ReadInputDiscretesResponse because received unexpected number of coils: "+((ReadInputDiscretesResponse)resp).getBitCount());
				this.nullData();
				return;
			}
		}
	}
	@Override
	public String toString() {
		String ret = "";
		if (dataInt != null) {
			ret+="Int: "+dataInt+", ";
		}
		if (dataFloat != null) {
			ret+="Float: "+dataFloat+", ";
		}
		if (dataBool != null) {
			ret+="Bool: "+dataBool+", ";
		}
		return ret;
	}
}
