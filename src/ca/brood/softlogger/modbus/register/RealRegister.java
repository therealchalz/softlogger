package ca.brood.softlogger.modbus.register;

import ca.brood.softlogger.util.*;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.*;

public class RealRegister extends Register implements Comparable<RealRegister>{
	protected int address = Integer.MAX_VALUE;
	protected int size = 0;
	protected int device = 0;
	protected int scanRate = 0;
	protected RegisterType regType;
	protected ScanRateSampling sampling;
	protected double samplingValue = 0;
	protected int samplingCount = 0;
	protected RegisterSizeType sizeType;
	
	protected RealRegister(int device) {
		super();
		this.device = device;
		log = Logger.getLogger(RealRegister.class + " device: "+device);
		sampling = ScanRateSampling.MEAN;
		sizeType = RegisterSizeType.UNSIGNED;
	}
	protected RealRegister(RealRegister r) {
		super(r);
		address = r.address;
		size = r.size;
		device = r.device;
		scanRate = r.scanRate;
		regType = r.regType;
		sampling = r.sampling;
		samplingValue = r.samplingValue;
		samplingCount = r.samplingCount;
		sizeType = r.sizeType;
	}
	public RegisterSizeType getSizeType() {
		return this.sizeType;
	}
	public void setNull() {
		this.registerData.nullData();
	}
	public void resetSampling() {
		samplingValue = 0;
		samplingCount = 0;
		//Reset all latch-on and latch-off coils to null
		if (this.getRegisterType() == RegisterType.INPUT_COIL || this.getRegisterType() == RegisterType.OUTPUT_COIL) {
			if (this.sampling == ScanRateSampling.LATCHOFF || this.sampling == ScanRateSampling.LATCHON) {
				this.registerData.nullData();
			}
		}
	}
	public void setDataWithSampling(RegisterData temp) {
		switch (sampling) {
		case LATEST:
			this.setData(temp);
			break;
		case SUM:
			//Only for registers
			samplingValue += temp.getFloat();
			samplingCount ++;
			this.setData((int)samplingValue);
			break;
		case MEAN:
			samplingCount ++;
			if (this.getRegisterType() == RegisterType.INPUT_COIL || this.getRegisterType() == RegisterType.OUTPUT_COIL) {
				if (temp.getBool())
					samplingValue ++;
				if ((samplingValue/samplingCount) >0.5f) {
					this.setData(true);
				} else {
					this.setData(false);
				}
			} else {
				samplingValue += temp.getFloat();
				this.setData((float)(samplingValue/samplingCount));
			}
			break;
		case LATCHON:
			//only for coils
			if (temp.getBool() || this.isNull())
				this.setData(temp.getBool());
			break;
		case LATCHOFF:
			//only for coils
			if (!temp.getBool() || this.isNull())
				this.setData(temp.getBool());
			break;
		}
	}
	public void setDefaultScanRate(int rate) {
		if (scanRate == 0) {
			scanRate = rate;
			sampling = ScanRateSampling.MEAN;
		}
	}
	public RegisterType getRegisterType() {
		return regType;
	}
	public int getAddress() {
		return address;
	}
	public int getSize() {
		return size;
	}
	public int getScanRate() {
		return this.scanRate;
	}
	public boolean configure(Node registerNode) {
		if (!super.configure(registerNode)) {
			return false;
		}
		NodeList configNodes = registerNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0) || 
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("registerAddress".compareToIgnoreCase(configNode.getNodeName())==0))	{
				int addy = 0;
				try {
					addy = Util.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Couldn't parse register Address to integer from: "+configNode.getFirstChild().getNodeValue());
					return false;
				}
				try {
					this.regType = RegisterType.fromAddress(addy);
					this.address = RegisterType.getAddress(addy);
					registerNode.removeChild(configNode);
				} catch (NumberFormatException e) {
					log.error("Error converting modbuss address to register type: "+addy);
					return false;
				}
			} else if (("size".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					this.size = Util.parseInt(configNode.getFirstChild().getNodeValue());
					this.sizeType = RegisterSizeType.fromString(configNode.getAttributes().item(0).getTextContent());
					registerNode.removeChild(configNode);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse size to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if ("scanRate".compareToIgnoreCase(configNode.getNodeName())==0){
				try {
					this.scanRate = Integer.parseInt(configNode.getFirstChild().getNodeValue());
					this.sampling = ScanRateSampling.fromString(configNode.getAttributes().item(0).getTextContent());
					registerNode.removeChild(configNode);
				} catch (NumberFormatException e) {
					log.error("Invalid scan rate: "+configNode.getFirstChild().getNodeValue());
					this.scanRate = 0;
				}
			}
		}
		if (this.address < 1 || this.address > 65535) {
			log.error("Parsed invalid address: "+this.address);
			return false;
		}
		switch (this.regType) {
		case INPUT_COIL:
		case OUTPUT_COIL:
			if (this.size != 1) {
				if (this.size != 0)
					log.warn("Got invalid size for an input or output coil.  Changing size to 1 from: "+this.size);
				this.size = 1;
			}
			if (sampling == ScanRateSampling.SUM) {
				log.warn("SUM sampling not allowed for coils.  Using default of MEAN.");
				sampling = ScanRateSampling.MEAN;
			}
			break;
		case INPUT_REGISTER:
		case OUTPUT_REGISTER:
			if (this.size != 1 && this.size != 2) {
				if (this.size != 0)
					log.warn("Got invalid size for an input or output register.  Changing size to 2 from: "+this.size);
				this.size = 1;
			}
			if (sampling == ScanRateSampling.LATCHOFF || sampling == ScanRateSampling.LATCHON) {
				log.warn("LATCHON / LATCHOFF samplings not allowed for non-coil registers. Using default of MEAN.");
				sampling = ScanRateSampling.MEAN;
			}
			break;
		}
		
		switch (this.sizeType) {
		case SIGNED:
			if (this.regType == RegisterType.INPUT_COIL || this.regType == RegisterType.OUTPUT_COIL) {
				log.warn("SIGNED size type is ignored for input and output coils.");
			}
			break;
		case UNSIGNED:
			break;
		case FLOAT:
			if (this.regType == RegisterType.INPUT_COIL || this.regType == RegisterType.OUTPUT_COIL) {
				log.error("FLOAT size type is ignored for input and output coils.");
			} else {
				if (this.size != 2) {
					log.error("FLOAT size type can only be used with double word (size=2) registers.  Changing to unsigned integer type.");
					this.sizeType = RegisterSizeType.UNSIGNED;
				}
			}
			break;
		}
		return true;
	}
	
	@Override
	public int compareTo(RealRegister other) {
		int ret = this.regType.compareTo(other.regType);
		if (ret != 0) {
			return ret;
		}
		ret = this.address - other.address;
		return ret;
	}
	
	@Override
	public String toString() {
		return "RealRegister: fieldname="+this.fieldName+"; address="+this.address+"; size="+this.size+"; data: "+registerData.toString();
	}
	public ModbusRequest getRequest() {
		return getRequest(this.size);
	}
	public ModbusRequest getRequest(int size) {
		ModbusRequest request = null;
		switch (this.regType) {
		case INPUT_COIL:
			request = new ReadInputDiscretesRequest(this.address, size);
			break;
		case OUTPUT_COIL:
			request = new ReadCoilsRequest(this.address, size);
			break;
		case INPUT_REGISTER:
			request = new ReadInputRegistersRequest(this.address, size);
			break;
		case OUTPUT_REGISTER:
			request = new ReadMultipleRegistersRequest(this.address, size);
		}
		
		return request;
	}
	public static class ScanRateComparator implements Comparator<RealRegister> {

		@Override
		public int compare(RealRegister arg0, RealRegister arg1) {
			int ret = arg0.scanRate - arg1.scanRate;
			if (ret != 0)
				return ret;
			ret = arg0.regType.compareTo(arg1.regType);
			if (ret != 0)
				return ret;
			ret = arg0.address - arg1.address;
			if (ret != 0);
				return ret;
		}
		
	}
}
