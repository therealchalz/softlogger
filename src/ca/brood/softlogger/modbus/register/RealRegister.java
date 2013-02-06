package ca.brood.softlogger.modbus.register;

import ca.brood.softlogger.util.*;
import net.wimpi.modbus.msg.ModbusRequest;
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
	protected int sizePerAddress = 2; //size of each address.  for 16-bit addresses, this is 2.  For coils, this is 1.  We know that the next contiguous register should be at this.address+(this.size/this.sizePerAddress)
	protected RegisterType regType;
	
	protected RealRegister(int device) {
		super();
		this.device = device;
		log = Logger.getLogger(RealRegister.class + " device: "+device);
	}
	public void setDefaultScanRate(int rate) {
		if (scanRate == 0) {
			scanRate = rate;
		}
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
					registerNode.removeChild(configNode);
				} catch (NumberFormatException e) {
					log.error("Couldn't parse size to integer from: "+configNode.getFirstChild().getNodeValue());
				}
			} else if ("scanRate".compareToIgnoreCase(configNode.getNodeName())==0){
				try {
					this.scanRate = Integer.parseInt(configNode.getFirstChild().getNodeValue());
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
			this.sizePerAddress = 1;
			if (this.size != 1) {
				if (this.size != 0)
					log.warn("Got invalid size for an input or output coil.  Changing size to 1 from: "+this.size);
				this.size = 1;
			}
			break;
		case INPUT_REGISTER:
		case OUTPUT_REGISTER:
			this.sizePerAddress = 2;
			if (this.size != 1 && this.size != 2) {
				if (this.size != 0)
					log.warn("Got invalid size for an input or output register.  Changing size to 2 from: "+this.size);
				this.size = 1;
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
		ModbusRequest request = null;
		switch (this.regType) {
		case INPUT_COIL:
			request = new ReadInputDiscretesRequest(this.address, this.size*this.sizePerAddress);
			break;
		case OUTPUT_COIL:
			request = new ReadCoilsRequest(this.address, this.size*this.sizePerAddress);
			break;
		case INPUT_REGISTER:
			request = new ReadInputRegistersRequest(this.address, this.size*this.sizePerAddress);
			break;
		case OUTPUT_REGISTER:
			request = new ReadMultipleRegistersRequest(this.address, this.size*this.sizePerAddress);
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
