package ca.brood.softlogger.modbus;
import ca.brood.softlogger.dataoutput.OutputModule;
import ca.brood.softlogger.modbus.channel.ModbusChannel;
import ca.brood.softlogger.modbus.register.*;
import ca.brood.softlogger.scheduler.Schedulable;
import ca.brood.softlogger.scheduler.SchedulerQueue;
import ca.brood.softlogger.util.XmlConfigurable;

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

public class Device implements Schedulable, XmlConfigurable {
	private Logger log;
	private final int id;
	private int unitId = Integer.MAX_VALUE;
	private String description = "";
	private ArrayList<ConfigRegister> configRegs;
	private ArrayList<DataRegister> dataRegs;
	private ModbusChannel channel = null;
	private SchedulerQueue scanGroups;
	private Object registerLock;
	private ArrayList<OutputModule> outputModules;
	
	private int scanRate = 0;
	private static volatile int nextId = 1;
	
	public Device(int channelId) {
		this.id = getNextDeviceId();
		log = Logger.getLogger(Device.class+": "+id+" on Channel: "+channelId);
		configRegs = new ArrayList<ConfigRegister>();
		dataRegs = new ArrayList<DataRegister>();
		scanGroups = new SchedulerQueue();
		registerLock = new Object();
		outputModules = new ArrayList<OutputModule>();
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
	
	public void addOutputModule(OutputModule m) {
		outputModules.add(m);
	}
	
	private SortedSet<RealRegister> getAllRegistersByScanRate() {
		SortedSet<RealRegister> ret = new TreeSet<RealRegister>(new RealRegister.ScanRateComparator());
		ret.addAll(configRegs);
		ret.addAll(dataRegs);
		return ret;
	}
	
	private void buildScanGroupQueue() {
		scanGroups = new SchedulerQueue();
		//Get all registers ordered by scan rate, then type, then address
		SortedSet<RealRegister> regs = getAllRegistersByScanRate();
		log.debug("All registers by scan rate: "+regs);
		
		//Combine all the registers with the same scan rate into scan groups
		ScanGroup scanGroup = null;
		for (RealRegister reg : regs) {
			if (scanGroup != null) {
				if (reg.getScanRate() == scanGroup.getScanRate()) {
					scanGroup.addRegister(reg);
				} else {
					log.debug("Adding ScanGroup: "+scanGroup);
					log.debug(scanGroup.getRegisters());
					scanGroups.add(scanGroup);
					scanGroup = null;
				}
			}
			
			if (scanGroup == null) {
				int sRate = reg.getScanRate();
				if (sRate == 0) {
					log.warn("Got 0 scanrate: " +reg);
				}
				scanGroup = new ScanGroup(sRate);
				scanGroup.addRegister(reg);
			} 
		}
		if (scanGroup != null) {
			log.debug("Adding Scangroup: "+scanGroup);
			log.debug(scanGroup.getRegisters());
			scanGroups.add(scanGroup);
		}
	}
	@Override
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
	public int getLogInterval() {
		return 1;
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
	
	private ModbusRequest getModbusRequest(RealRegister from, RealRegister to) {
		log.info("Creating modbus request from: "+from+" to: "+to);
		int size = to.getAddress() - from.getAddress() + to.getSize();
		ModbusRequest ret = from.getRequest(size);
		ret.setUnitID(this.unitId);
		//log.info("Created Request: "+ret);
		return ret;
	}
	private ArrayList<RealRegister> getRegistersToUpdate(ModbusResponse response) {
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		int numWords = getDataLength(response);
		RegisterType type = RegisterType.fromResponse(response);
		ArrayList<RealRegister> registerList = new ArrayList<RealRegister>();
		registerList.addAll(dataRegs);
		registerList.addAll(configRegs);
		Collections.sort(registerList);
		addressLoop:
		for (int address = response.getReference(); address < response.getReference()+numWords; address++) {
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
					//log.info("Adding register (add : "+address+", type: "+type+"): "+reg);
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
				//log.info("Adding register (add : "+address+", type: "+type+"): "+reg);
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
	private void setRegisterData(RealRegister reg, ModbusResponse response) {
		int offset = reg.getAddress() - response.getReference();
		log.trace("Setting register: "+reg);
		if (response instanceof ReadCoilsResponse) {
			if (offset > getDataLength(response)) {
				log.error("Invalid offset for ReadCoilsResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" "+reg);
				reg.setNull();
			} else {
				RegisterData temp = new RegisterData();
				temp.setData(((ReadCoilsResponse)response).getCoilStatus(offset));
				reg.setDataWithSampling(temp);
			}
		}
		if (response instanceof ReadInputDiscretesResponse) {
			if (offset > getDataLength(response)) {
				log.error("Invalid offset for ReadInputDiscretesResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" "+reg);
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
				//log.info("Registers 1: "+((ReadInputRegistersResponse)response).getRegister(offset).getValue()+" Register 2: "+((ReadInputRegistersResponse)response).getRegister(offset+1).getValue()+" new Value: "+val);
				RegisterData temp = new RegisterData();
				temp.setDataFloat(val);
				reg.setDataWithSampling(temp);
			} else { //not 1 or 2 words
				log.error("Invalid offset for ReadInputRegisterResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" Size: "+reg.getSize()+" "+reg);
				reg.setNull();
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
				//log.info("Registers 1: "+((ReadMultipleRegistersResponse)response).getRegister(offset).getValue()+" Register 2: "+((ReadMultipleRegistersResponse)response).getRegister(offset+1).getValue()+" new Value: "+val);
				RegisterData temp = new RegisterData();
				temp.setDataFloat(val);
				reg.setDataWithSampling(temp);
			} else { //not 1 or 2 words
				log.error("Invalid offset for ReadMultipleRegistersResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" Size: "+reg.getSize()+" "+reg);
				reg.setNull();
			}
		}
		log.trace("Setting register (NEW): "+reg);
	}
	
	private ArrayList<ModbusRequest> getRequests(SortedSet<RealRegister> regs) {
		ArrayList<ModbusRequest> requests = new ArrayList<ModbusRequest>();
		
		RealRegister firstOfRequest = null;
		RealRegister lastOfRequest = null;
		for (RealRegister r : regs) {
			if (firstOfRequest == null) {
				firstOfRequest = r;
				lastOfRequest = firstOfRequest;
			} else {
				int nextAddress = lastOfRequest.getAddress() + lastOfRequest.getSize();
				if (firstOfRequest.getRegisterType().equals(r.getRegisterType()) && r.getAddress()==nextAddress) {
					lastOfRequest = r;
				} else {
					ModbusRequest request = getModbusRequest(firstOfRequest, lastOfRequest);
					requests.add(request);
					firstOfRequest = r;
					lastOfRequest = firstOfRequest;
				}
			}
		}
		if (firstOfRequest != null & lastOfRequest != null) {
			ModbusRequest request = getModbusRequest(firstOfRequest, lastOfRequest);
			requests.add(request);
		}
		
		return requests;
	}

	private void updateRegisters(SortedSet<RealRegister> regs) {
		//First build a list of modbus requests
		ArrayList<ModbusRequest> requests = getRequests(regs);
		
		//Execute each request.  Update our registers that have addresses that
		// match addresses in the response
		ArrayList<ModbusResponse> responses = new ArrayList<ModbusResponse>();
		for (ModbusRequest request : requests) {
			try {
				log.trace("Executing request: "+request);
				ModbusResponse resp = channel.executeRequest(request);
				responses.add(resp);
			} catch (ModbusException e) {
				log.trace("Got modbus exception: ",e);
			} catch (Exception e) {
				log.trace("Got no response....", e);
				return; //Couldn't do a modbus request
			}
		}
		
		for (ModbusResponse response : responses) {
			ArrayList<RealRegister> regsToUpdate = getRegistersToUpdate(response);
			for (RealRegister regToUpdate : regsToUpdate) {
				//log.trace("Got Register To Update: "+regToUpdate);
				this.setRegisterData(regToUpdate, response);
				if (regToUpdate instanceof ConfigRegister) {
					log.info("Set config register: "+regToUpdate);
				}
			}
		}
	}
	
	public ArrayList<DataRegister> getDataRegisters() {
		synchronized (registerLock) {
			ArrayList<DataRegister> ret = new ArrayList<DataRegister>();
			for (DataRegister d : this.dataRegs) {
				DataRegister n = new DataRegister(d);
				ret.add(n);
			}
			return ret;
		}
	}
	
	public ArrayList<DataRegister> getDataRegistersAndResetSamplings() {
		synchronized (registerLock) {
			ArrayList<DataRegister> ret = getDataRegisters();
			resetRegisterSamplings();
			return ret;
		}
		
	}
	
	public void resetRegisterSamplings() {
		synchronized (registerLock) {
			for (DataRegister d : this.dataRegs) {
				d.resetSampling();
			}
		}
	}

	private void run() {
		//TODO device run function
		if (this.channel == null) {
			return;
		}
		
		SortedSet<RealRegister> registersToProcess = new TreeSet<RealRegister>();
		while (getNextRun() < System.currentTimeMillis()) {
			ScanGroup next = (ScanGroup) scanGroups.poll();
			next.execute();
			scanGroups.add(next);
			
			registersToProcess.addAll(next.getRegisters());
		}
		
		log.info("Registers to process: "+registersToProcess);
		synchronized (registerLock) {
			updateRegisters(registersToProcess);
		}

		//If a config register's value is invalid, write the correct one
		log.trace("Writing config registers");
		for (ConfigRegister c : configRegs) {
			if (!c.dataIsGood()) {
				log.debug("Config register has wrong value: "+c.toString());
				ModbusRequest req = c.getWriteRequest();
				req.setUnitID(this.unitId);
				try {
					log.trace("Executing request: "+req);
					ModbusResponse resp = channel.executeRequest(req);
					log.trace("Got response: "+resp);
					c.setData(resp);
				} catch (ModbusException e) {
					log.trace("Got modbus exception: ", e);
				} catch (Exception e) {
					log.trace("Got exception: ", e);
				}
			}
		}
		
	}
	
	public void printAll() {
		log.info("Unit ID: "+this.unitId);
		log.info("Description: "+this.description);
		log.info("Scan rate: "+this.scanRate);
		for (ConfigRegister c : this.configRegs) {
			log.info(c);
		}
		for (DataRegister d : this.dataRegs) {
			log.info(d);
		}
	}

	@Override
	public long getNextRun() {
		return scanGroups.peek().getNextRun();
	}

	@Override
	public void execute() {
		this.run();
	}
}
