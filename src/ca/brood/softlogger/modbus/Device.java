package ca.brood.softlogger.modbus;
import ca.brood.softlogger.modbus.channel.ModbusChannel;
import ca.brood.softlogger.modbus.register.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Device implements Runnable {
	private Logger log;
	private final int id;
	private int unitId = Integer.MAX_VALUE;
	private String description = "";
	private ArrayList<ConfigRegister> configRegs;
	private ArrayList<DataRegister> dataRegs;
	private ModbusChannel channel = null;
	private PriorityQueue<ScanGroup> scanGroups;
	
	private int scanRate = 0;
	private int logInterval = 0;
	private static volatile int nextId = 1;
	
	public Device(int channelId) {
		this.id = getNextDeviceId();
		log = Logger.getLogger(Device.class+": "+id+" on Channel: "+channelId);
		configRegs = new ArrayList<ConfigRegister>();
		dataRegs = new ArrayList<DataRegister>();
		scanGroups = new PriorityQueue<ScanGroup>();
	}
	
	private static synchronized int getNextDeviceId() {
		return nextId++;
	}
	public String getDescription() {
		return this.description;
	}
	
	public void setChannel(ModbusChannel chan) {
		this.channel = chan;
	}
	
	private PriorityQueue<RealRegister> getAllRegistersByScanRate() {
		PriorityQueue<RealRegister> ret = new PriorityQueue<RealRegister>(configRegs.size()+dataRegs.size(), new RealRegister.ScanRateComparator());
		ret.addAll(configRegs);
		ret.addAll(dataRegs);
		return ret;
	}
	
	public void elapsed(long elapsedMillis) {
		for (ScanGroup s : scanGroups) {
			s.elapsed(elapsedMillis);
		}
	}
	
	//return -1 if no scan groups
	//otherwise return number of milliseconds until next scan
	public int getTtl() {
		ScanGroup top = scanGroups.peek();
		if (top == null) {
			return -1;
		}
		int ttl = top.getTtl();
		if (ttl < 0)
			return 0;
		return ttl;
	}
	
	private void buildScanGroupQueue() {
		scanGroups = new PriorityQueue<ScanGroup>();
		//Get all registers ordered by scan rate, then type, then address
		PriorityQueue<RealRegister> regs = getAllRegistersByScanRate();
		
		//Combine all the registers with the same scan rate into scan groups
		ScanGroup scanGroup = null;
		int scanRate = Integer.MAX_VALUE;
		while(!regs.isEmpty()) {
			if (scanGroup == null) {
				scanRate = regs.peek().getScanRate();
				if (scanRate == 0) {
					log.warn("Got 0 scanrate: " +regs);
				}
				scanGroup = new ScanGroup(scanRate);
				scanGroup.addRegister(regs.poll());
			} else {
				if (regs.peek().getScanRate() == scanRate) {
					scanGroup.addRegister(regs.poll());
				} else {
					log.debug("Adding Scangroup: "+scanGroup);
					scanGroups.add(scanGroup);
					scanGroup = null;
				}
			}
		}
		if (scanGroup != null) {
			log.debug("Adding Scangroup: "+scanGroup);
			scanGroups.add(scanGroup);
		}
	}
	
	public boolean configure(Node deviceNode) {
		NodeList configNodes = deviceNode.getChildNodes();
		//TODO: check for duplicate GUIDs on the modbus variables
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("unitId".compareToIgnoreCase(configNode.getNodeName())==0)) {
				this.unitId = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				//log.debug("Device unitId: "+this.unitId);
			} else if (("description".compareToIgnoreCase(configNode.getNodeName())==0)) {
				this.description = configNode.getFirstChild().getNodeValue();
				//log.debug("Device description: "+this.description);
			} else if (("configregister".compareToIgnoreCase(configNode.getNodeName())==0)) {
				ConfigRegister c = new ConfigRegister(this.id);
				if (c.configure(configNode)) {
					log.debug("Adding config register: "+c);
					configRegs.add(c);
				} else 
					log.warn("Error configuring a config register: "+configNode);
			} else if (("dataregister".compareToIgnoreCase(configNode.getNodeName())==0)) {
				DataRegister d = new DataRegister(this.id);
				if (d.configure(configNode)) {
					log.debug("Adding data register: "+d);
					dataRegs.add(d);
				} else
					log.warn("Error configuring a data register: "+configNode);
					
			} else if (("defaultScanRate".compareToIgnoreCase(configNode.getNodeName())==0)) {
				try {
					this.scanRate = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid device scan rate: "+configNode.getFirstChild().getNodeValue());
					this.scanRate = 0;
				}
			} else if (("logInterval".compareToIgnoreCase(configNode.getNodeName())==0)) {
				try {
					this.logInterval = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid device log interval: "+configNode.getFirstChild().getNodeValue());
					this.logInterval = 0;
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (this.unitId == Integer.MAX_VALUE) {
			log.error("Error: Device does not have a unitId configured.");
			return false;
		}
		
		Collections.sort(configRegs);
		Collections.sort(dataRegs);
		
		buildScanGroupQueue();
		
		return true;
	}
	public int getScanRate() {
		return scanRate;
	}
	public void setDefaultScanRate(int rate) {
		if (scanRate == 0)
			scanRate = rate;
		for (RealRegister r : this.configRegs) {
			r.setDefaultScanRate(scanRate);
		}
		for (RealRegister r : this.dataRegs) {
			r.setDefaultScanRate(scanRate);
		}

		buildScanGroupQueue();
	}
	
	public void setDefaultLogInterval(int lo) {
		if (logInterval == 0)
			logInterval = lo;
	}
	
	private ModbusRequest getModbusRequest(RealRegister first, RealRegister last) {
		//log.info("Creating modbus request from: "+first+" to: "+last);
		int size = last.getAddress() - first.getAddress() + last.getSize();
		ModbusRequest ret = first.getRequest(size);
		ret.setUnitID(this.unitId);
		log.info("Created Request: "+ret);
		return ret;
	}
	public RegisterType getTypeOfResponse(ModbusResponse response) {
		if (response instanceof ReadCoilsResponse) {
			return RegisterType.OUTPUT_COIL;
		}
		if (response instanceof ReadInputDiscretesResponse) {
			return RegisterType.INPUT_COIL;
		}
		if (response instanceof ReadInputRegistersResponse) {
			return RegisterType.INPUT_REGISTER;
		}
		else  {
			return RegisterType.OUTPUT_REGISTER;
		}
	}
	private ArrayList<RealRegister> getRegisters(int baseAddress, ModbusResponse response) {
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		int numWords = getDataLength(response);
		RegisterType type = getTypeOfResponse(response);
		ArrayList<RealRegister> registerList = new ArrayList<RealRegister>();
		registerList.addAll(dataRegs);
		registerList.addAll(configRegs);
		Collections.sort(registerList);
		addressLoop:
		for (int address = baseAddress; address < baseAddress+numWords; address++) {
			//naive implementation
			//TODO optimize
			Iterator<RealRegister> registerIterator = registerList.iterator();
			RealRegister reg = null;
			while (registerIterator.hasNext()) {
				reg = registerIterator.next();
				if (type == reg.getRegisterType())
					break;
			}
			do {
				if (reg == null) {
					break addressLoop;
				}
				if (address == reg.getAddress() && type == reg.getRegisterType()) {
					log.info("Adding register (add : "+address+", type: "+type+"): "+reg);
					ret.add(reg);
				}
				if (registerIterator.hasNext()) {
					reg = registerIterator.next();
				}
			} while (registerIterator.hasNext());
			
			if (reg == null) {
				break addressLoop;
			}
			if (address == reg.getAddress() && type == reg.getRegisterType()) {
				log.info("Adding register (add : "+address+", type: "+type+"): "+reg);
				ret.add(reg);
			}
			
		}
		return ret;
	}
	private int getDataLength(ModbusResponse response) {
		if (response instanceof ReadCoilsResponse) {
			return ((ReadCoilsResponse)response).getBitCount();
		}
		if (response instanceof ReadInputDiscretesResponse) {
			return ((ReadInputDiscretesResponse)response).getBitCount();
		}
		if (response instanceof ReadInputRegistersResponse) {
			return ((ReadInputRegistersResponse)response).getWordCount();
		}
		if (response instanceof ReadMultipleRegistersResponse) {
			return ((ReadMultipleRegistersResponse)response).getWordCount();
		}
		return 0;
	}
	private void setRegisterData(RealRegister reg, ModbusResponse response, int offset) {
		if (response instanceof ReadCoilsResponse) {
			if (offset > getDataLength(response)) {
				log.error("Invalid offset for ReadCoilsResponse: "+offset+" : "+getDataLength(response));
				reg.setNull();
			} else {
				RegisterData temp = new RegisterData();
				temp.setData(((ReadCoilsResponse)response).getCoilStatus(offset));
				reg.setDataWithSampling(temp);
			}
		}
		if (response instanceof ReadInputDiscretesResponse) {
			if (offset > getDataLength(response)) {
				log.error("Invalid offset for ReadInputDiscretesResponse: "+offset+" : "+getDataLength(response));
				reg.setNull();
			} else {
				RegisterData temp = new RegisterData();
				temp.setData(((ReadInputDiscretesResponse)response).getDiscreteStatus(offset));
				reg.setDataWithSampling(temp);
			}
		}
		if (response instanceof ReadInputRegistersResponse) {
			if (getDataLength(response) >= offset+reg.getSize() && reg.getSize() == 1) {
				RegisterSizeType sizeType = reg.getSizeType();
				int val;
				if (sizeType == RegisterSizeType.SIGNED) {
					short shortVal = (short) ((ReadInputRegistersResponse)response).getRegister(offset).getValue();
					val = shortVal;
				} else {
					val = ((ReadInputRegistersResponse)response).getRegister(offset).getValue();
				}
				RegisterData temp = new RegisterData();
				temp.setData(new Integer(val));
				reg.setDataWithSampling(temp);
			} else if (getDataLength(response) >= offset+reg.getSize() && reg.getSize() == 2) {
				//32-bit register reading
				RegisterSizeType sizeType = reg.getSizeType();
				float val = 0;
				switch (sizeType) {
				case SIGNED:
					val = (int)(((ReadInputRegistersResponse)response).getRegister(offset).getValue() << 16) + ((ReadInputRegistersResponse)response).getRegister(offset+1).getValue();
					break;
				case UNSIGNED:
					val = (long)(((ReadInputRegistersResponse)response).getRegister(offset).getValue() << 16) + ((ReadInputRegistersResponse)response).getRegister(offset+1).getValue();
					break;
				case FLOAT:
					val = Float.intBitsToFloat((((ReadInputRegistersResponse)response).getRegister(offset).getValue() << 16) + ((ReadInputRegistersResponse)response).getRegister(offset+1).getValue());
					break;
				}
				log.info("Registers 1: "+((ReadInputRegistersResponse)response).getRegister(offset).getValue()+" Register 2: "+((ReadInputRegistersResponse)response).getRegister(offset+1).getValue()+" new Value: "+val);
				RegisterData temp = new RegisterData();
				temp.setDataFloat(val);
				reg.setDataWithSampling(temp);
			} else { //not 1 or 2 words
				log.error("Invalid offset for ReadInputRegisterResponse: "+offset+" : "+getDataLength(response)+" : "+reg.getSize());
				reg.setNull();
				return;
			}
		}
		if (response instanceof ReadMultipleRegistersResponse) {
			if (getDataLength(response) >= offset+reg.getSize() && reg.getSize() == 1) {
				RegisterSizeType sizeType = reg.getSizeType();
				int val;
				if (sizeType == RegisterSizeType.SIGNED) {
					short shortVal = (short) ((ReadMultipleRegistersResponse)response).getRegister(offset).getValue();
					val = shortVal;
				} else {
					val = ((ReadMultipleRegistersResponse)response).getRegister(offset).getValue();
				}
				RegisterData temp = new RegisterData();
				temp.setData(new Integer(val));
				reg.setDataWithSampling(temp);
			} else if (getDataLength(response) >= offset+reg.getSize() && reg.getSize() == 2) {
				//32-bit register reading
				RegisterSizeType sizeType = reg.getSizeType();
				float val = 0;
				switch (sizeType) {
				case SIGNED:
					val = (int)(((ReadMultipleRegistersResponse)response).getRegister(offset).getValue() << 16) + ((ReadMultipleRegistersResponse)response).getRegister(offset+1).getValue();
					break;
				case UNSIGNED:
					val = (long)(((ReadMultipleRegistersResponse)response).getRegister(offset).getValue() << 16) + ((ReadMultipleRegistersResponse)response).getRegister(offset+1).getValue();
					break;
				case FLOAT:
					val = Float.intBitsToFloat((((ReadMultipleRegistersResponse)response).getRegister(offset).getValue() << 16) + ((ReadMultipleRegistersResponse)response).getRegister(offset+1).getValue());
					break;
				}
				log.info("Registers 1: "+((ReadMultipleRegistersResponse)response).getRegister(offset).getValue()+" Register 2: "+((ReadMultipleRegistersResponse)response).getRegister(offset+1).getValue()+" new Value: "+val);
				RegisterData temp = new RegisterData();
				temp.setDataFloat(val);
				reg.setDataWithSampling(temp);
			} else { //not 1 or 2 words
				log.error("Invalid offset for ReadMultipleRegistersResponse: "+offset+" : "+getDataLength(response)+" : "+reg.getSize());
				reg.setNull();
				return;
			}
		}
	}

	private void updateRegisters(SortedSet<RealRegister> regs) {
		
		//First build a list of modbus requests
		Iterator<RealRegister> registerIterator = regs.iterator();
		RealRegister firstOfRequest = null;
		RealRegister lastOfRequest = null;
		ArrayList<ModbusRequest> requests = new ArrayList<ModbusRequest>();
		while (registerIterator.hasNext()) {
			if (firstOfRequest == null) {
				firstOfRequest = registerIterator.next();
				lastOfRequest = firstOfRequest;
			} else {
				RealRegister nextRegister = registerIterator.next();
				int nextAddress = lastOfRequest.getAddress() + lastOfRequest.getSize();
				if (firstOfRequest.getRegisterType().equals(nextRegister.getRegisterType()) && nextRegister.getAddress()==nextAddress) {
					lastOfRequest = nextRegister;
				} else {
					ModbusRequest request = getModbusRequest(firstOfRequest, lastOfRequest);
					requests.add(request);
					firstOfRequest = nextRegister;
					lastOfRequest = firstOfRequest;
				}
			}
		}
		if (firstOfRequest != null & lastOfRequest != null) {
			ModbusRequest request = getModbusRequest(firstOfRequest, lastOfRequest);
			requests.add(request);
		}
		
		//Execute each request.  Update our registers that have addresses that
		// match addresses in the response
		for (ModbusRequest r : requests) {
			try {
				ModbusResponse resp = channel.executeRequest(r);
				int baseAddress = r.getReference();
				log.info("Got response (baseAddress: "+baseAddress+"): "+resp);
				ArrayList<RealRegister> regsToUpdate = getRegisters(baseAddress, resp);
				for (RealRegister regToUpdate : regsToUpdate) {
					int addy = regToUpdate.getAddress() - baseAddress;
					log.info("Got Register To Update (offset: "+addy+"): "+regToUpdate);
					this.setRegisterData(regToUpdate, resp, addy);
				}
				//c.setData(resp);
			} catch (ModbusException e) {
				log.trace("Got modbus exception: ",e);
			} catch (Exception e) {
				log.trace("Got no response....", e);
				return; //Couldn't do a modbus request
			}
		}
	}
	
	public ArrayList<DataRegister> getDataRegisters() {
		ArrayList<DataRegister> ret = new ArrayList<DataRegister>();
		for (DataRegister d : this.dataRegs) {
			DataRegister n = new DataRegister(d);
			ret.add(n);
		}
		return ret;
	}

	@Override
	public void run() {
		//TODO device run function
		if (this.channel == null) {
			return;
		}
		
		SortedSet<RealRegister> registersToProcess = new TreeSet<RealRegister>();
		while (getTtl() < 10 && getTtl() >= 0) {
			ScanGroup next = scanGroups.poll();
			next.reset();
			scanGroups.add(next);
			
			registersToProcess.addAll(next.getRegisters());
		}
		
		//log.info("Registers to process: "+registersToProcess);
		updateRegisters(registersToProcess);
		
	}
	
	public void printAll() {
		log.info("Unit ID: "+this.unitId);
		log.info("Description: "+this.description);
		log.info("Scan rate: "+this.scanRate);
		log.info("Log interval: "+this.logInterval);
		for (ConfigRegister c : this.configRegs) {
			log.info(c);
		}
		for (DataRegister d : this.dataRegs) {
			log.info(d);
		}
	}
}
