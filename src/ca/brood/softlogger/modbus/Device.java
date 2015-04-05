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
package ca.brood.softlogger.modbus;
import ca.brood.brootils.xml.XMLConfigurable;
import ca.brood.softlogger.datafunction.DataFunction;
import ca.brood.softlogger.datafunction.DataProcessingManager;
import ca.brood.softlogger.dataoutput.OutputModule;
import ca.brood.softlogger.dataoutput.OutputableDevice;
import ca.brood.softlogger.modbus.channel.ModbusChannel;
import ca.brood.softlogger.modbus.register.*;
import ca.brood.softlogger.scheduler.Schedulable;
import ca.brood.softlogger.scheduler.SchedulerQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Device implements Schedulable, XMLConfigurable, OutputableDevice {
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
	private final AtomicBoolean isOnline;
	private long offlineTime = 0;
	
	private int currentRetryInterval = 0;
	private int retryIntervalPeriod = 5000;
	private int maxRetryInterval = 0;
	
	private int scanRate = 0;
	private static volatile int nextId = 1;
	
	private static synchronized int getNextDeviceId() {
		return nextId++;
	}
	
	public Device(int channelId) {
		this.id = getNextDeviceId();
		log = Logger.getLogger(Device.class+": "+id+" on Channel: "+channelId);
		configRegs = new ArrayList<ConfigRegister>();
		dataRegs = new ArrayList<DataRegister>();
		scanGroups = new SchedulerQueue();
		registerLock = new Object();
		outputModules = new ArrayList<OutputModule>();
		isOnline = new AtomicBoolean(false);
	}
	public void addOutputModule(OutputModule m) {
		m.setRegisterCollection(new RegisterCollection(this.getAllRegisters()));
		m.setOutputableDevice(this);
		outputModules.add(m);
	}
	public void deleteAllOutputModules() {
		outputModules = new ArrayList<OutputModule>();
	}
	
	@Override
	public boolean configure(Node deviceNode) {
		NodeList configNodes = deviceNode.getChildNodes();
		//TODO: check for duplicate GUIDs on the modbus variables
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||
					("outputModule".compareToIgnoreCase(configNode.getNodeName())==0)||
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
					
			} else if ("retryInterval".compareToIgnoreCase(configNode.getNodeName())==0){
				try {
					this.retryIntervalPeriod = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid retryInterval: "+configNode.getFirstChild().getNodeValue());
				}
			} else if ("maxRetryInterval".compareToIgnoreCase(configNode.getNodeName())==0){
				try {
					this.maxRetryInterval = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid maxRetryInterval: "+configNode.getFirstChild().getNodeValue());
				}
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
		
		//Load the device output modules
		NodeList loggerConfigNodes = deviceNode.getChildNodes();
		Node currentConfigNode;
		for (int i=0; i<loggerConfigNodes.getLength(); i++) {
			currentConfigNode = loggerConfigNodes.item(i);
			if ("outputModule".compareToIgnoreCase(currentConfigNode.getNodeName())!=0) {
				continue;
			}
			try {
				@SuppressWarnings("unchecked")
				Class<? extends OutputModule> outputClass = (Class<? extends OutputModule>) Class.forName(currentConfigNode.getAttributes().getNamedItem("class").getNodeValue());
				OutputModule outputModule = outputClass.newInstance();
				if (outputModule.configure(currentConfigNode)) {
					this.addOutputModule(outputModule);
				}
			} catch (Exception e) {
				log.error("Got exception while loading output module: ",e);
			}
		}
		
		Collections.sort(configRegs);
		Collections.sort(dataRegs);
		
		buildScanGroupQueue();
		
		setOnline(true);
		
		return true;
	}
	
	@Override
	public void execute() {
		this.run();
	}
	
	public ArrayList<RealRegister> getAllRegisters() {
		synchronized (registerLock) {
			ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
			for (DataRegister d : this.dataRegs) {
				DataRegister n = new DataRegister(d);
				ret.add(n);
			}
			for (ConfigRegister c : this.configRegs) {
				ConfigRegister n = new ConfigRegister(c);
				ret.add(n);
			}
			return ret;
		}
	}
	
	public String getDescription() {
		return this.description;
	}
	public int getLogInterval() {
		return 1;
	}
	@Override
	public long getNextRun() {
		if (scanGroups.size() > 0) {
			return scanGroups.peek().getNextRun();
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	public int getScanRate() {
		return scanRate;
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
	
	public void setChannel(ModbusChannel chan) {
		this.channel = chan;
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
	private void buildScanGroupQueue() {
		scanGroups = new SchedulerQueue();
		//Get all registers ordered by scan rate, then type, then address
		SortedSet<RealRegister> regs = getAllRegistersByScanRate();
		log.trace("All registers by scan rate: "+regs);
		
		//Combine all the registers with the same scan rate into scan groups
		ScanGroup scanGroup = null;
		for (RealRegister reg : regs) {
			if (scanGroup != null) {
				if (reg.getScanRate() == scanGroup.getScanRate()) {
					scanGroup.addRegister(reg);
				} else {
					log.trace(scanGroup.getRegisters());
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
				scanGroup.setNextRun(System.nanoTime()+(1000l*1000000l)); //We'll start in 1 second
				scanGroups.add(scanGroup);
			} 
		}
	}
	private void checkConfigRegisters() {
		for (ConfigRegister c : configRegs) {
			if (!c.dataIsGood()) {
				log.debug("Config register has wrong value: "+c);
				ModbusRequest req = c.getWriteRequest();
				req.setUnitID(this.unitId);
				try {
					log.trace("Executing request: "+req);
					ModbusResponse resp = channel.executeRequest(req);
					log.trace("Got response: "+resp);
					if (!(req instanceof WriteMultipleRegistersRequest))
						c.setData(resp);
					else //optimistically assume the write was successful
						c.setData(c.getValue());
				} catch (ModbusException e) {
					log.error("Got modbus exception (writing config register): ", e);
					return;	//don't try the rest of the config variables
				} catch (Exception e) {
					log.error("Got exception (writing config register): ", e);
					return;
				}
			}
		}
	}
	
	private ArrayList<ModbusResponse> executeRequests(ArrayList<ModbusRequest> requests) {
		ArrayList<ModbusResponse> responses = new ArrayList<ModbusResponse>();
		for (ModbusRequest request : requests) {
			try {
				log.debug("Executing request: "+request);
				ModbusResponse resp = channel.executeRequest(request);
				responses.add(resp);
			} catch (ModbusException e) {
				log.error("Got modbus exception while executing request: "+request,e);
				break;
			} catch (Exception e) {
				log.error("Got no response while executing request: "+request, e);
				break;
			}
		}
		
		return responses;
	}

	private SortedSet<RealRegister> getAllRegistersByScanRate() {
		SortedSet<RealRegister> ret = new TreeSet<RealRegister>(new RealRegister.ScanRateComparator());
		ret.addAll(configRegs);
		ret.addAll(dataRegs);
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
	
	private ModbusRequest getModbusRequest(RealRegister from, RealRegister to) {
		//log.trace("Creating modbus request from: "+from+" to: "+to);
		int size = to.getAddress() - from.getAddress() + to.getSize();
		ModbusRequest ret = from.getRequest(size);
		ret.setUnitID(this.unitId);
		//log.trace("Created Request: "+ret);
		return ret;
	}
	
	private ArrayList<RealRegister> getRegistersToUpdate(ArrayList<RealRegister> registerList, ModbusResponse response) {
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		int numWords = getDataLength(response);
		RegisterType type = RegisterType.fromResponse(response);
		Collections.sort(registerList);
		for (int address = response.getReference(); address < response.getReference()+numWords; address++) {
			Iterator<RealRegister> registerIterator = registerList.iterator();
			RealRegister reg;
			if (registerIterator.hasNext()) { //avoid NoSuchElementException
				reg = registerIterator.next();
			} else {
				break;
			}
			boolean foundType = false;
			while (reg != null) {
				if (type != reg.getRegisterType()) {
					if (foundType)
						break;	//we're past all the registers of the correct type
				} else {
					foundType = true;
					if (address == reg.getAddress()) {
						//log.trace("Adding register (add : "+address+", type: "+type+"): "+reg);
						ret.add(reg);
					}
				}
				if (registerIterator.hasNext()) {
					reg = registerIterator.next();
				} else {
					break;
				}
			}
		}
		return ret;
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

	@SuppressWarnings("unused")
	private void resetRegisterSamplings() {
		synchronized (registerLock) {
			ArrayList<RealRegister> regs = new ArrayList<RealRegister>();
			regs.addAll(dataRegs);
			regs.addAll(configRegs);
			resetRegisterSamplings(regs);
		}
	}
	
	private void resetRegisterSamplings(ArrayList<RealRegister> regs) {
		for (RealRegister r : regs) {
			r.resetSampling();
		}
	}
	
	private void run() {
		
		if (this.channel == null) {
			return;
		}
		
		//If we're not online, retry every retryTimeSeconds
		if (!isOnline()) {
			synchronized (isOnline) {
				if ((System.nanoTime() - offlineTime)/(1000000l) >= currentRetryInterval) {
					//Fall through and continue executing to see if we're online now
				} else {
					return;	//Give up, we're probably still offline
				}
			}
		}
		
		SortedSet<RealRegister> registersToProcess = new TreeSet<RealRegister>();
		List<ScanGroup> scanGroupsToAdd = new ArrayList<ScanGroup>();
		long nextRunCutoff = System.nanoTime() + (10l*1000000l);	//Add a 10ms fudge factor
		while (scanGroups.size() > 0 && getNextRun() - nextRunCutoff < 0) {
			ScanGroup nextGroup = (ScanGroup) scanGroups.poll();
			nextGroup.execute();
			scanGroupsToAdd.add(nextGroup);
			registersToProcess.addAll(nextGroup.getRegisters());
		}
		
		for (ScanGroup sg : scanGroupsToAdd) {
			scanGroups.add(sg);
		}
		
		//wait until after we execute the scangroups
		if (!this.channel.isReady()) {
			return;
		}
		
		if (registersToProcess.size() == 0) {
			log.warn("Registers to process: "+registersToProcess);
			return;
		}
		
		synchronized (registerLock) {
			
			ArrayList<ModbusRequest> requests = getRequests(registersToProcess);
			
			long then = System.nanoTime();
			ArrayList<ModbusResponse> responses = executeRequests(requests);
			long now = System.nanoTime();
			long measurementTimeMillis = (long) (((then+now)*0.5d)/1000000l);
			
			if (responses.size() == 0) {
				setOnline(false);
				
				if (maxRetryInterval > 0) {
					currentRetryInterval += retryIntervalPeriod;
					if (currentRetryInterval > maxRetryInterval) {
						currentRetryInterval = maxRetryInterval;
					}
				} else {
					currentRetryInterval = retryIntervalPeriod;
				}
				
				log.error("ERROR: Device appears to be offline.  Retrying in "+(currentRetryInterval/1000)+" seconds");
				return;
			}
			
			setOnline(true);
			currentRetryInterval = 0;
			
			ArrayList<RealRegister> registerList = new ArrayList<RealRegister>();
			registerList.addAll(dataRegs);
			registerList.addAll(configRegs);
			
			updateRegisters(registerList, responses, measurementTimeMillis, true);
			
			for (OutputModule outputModule : outputModules) {
				log.trace("Processing output module: "+outputModule.getDescription());
				RegisterCollection collect = outputModule.getRegisterCollection();
				updateRegisters(collect.beginUpdating(), responses, measurementTimeMillis, outputModule.useRegisterSampling());
				collect.finishUpdating();
			}
		}

		//If a config register's value is invalid, write the correct one
		checkConfigRegisters();
		
	}

	private void setRegisterData(RealRegister reg, ModbusResponse response, Long timestamp, boolean useSampling) {
		int offset = reg.getAddress() - response.getReference();
		//log.trace("Setting register: "+reg);
		if (response instanceof ReadCoilsResponse) {
			if (reg.getRegisterType() != RegisterType.OUTPUT_COIL) {
				log.error("Register type mismatch for ReadCoilsResponse: "+reg);
				reg.setNull();
			} else if (offset > getDataLength(response)) {
				log.error("Invalid offset for ReadCoilsResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" "+reg);
				reg.setNull();
			} else {
				RegisterData temp = new RegisterData();
				temp.setData(((ReadCoilsResponse)response).getCoilStatus(offset), timestamp);
				if (useSampling)
					reg.setDataWithSampling(temp);
				else
					reg.setData(temp);
			}
		}
		if (response instanceof ReadInputDiscretesResponse) {
			if (reg.getRegisterType() != RegisterType.INPUT_COIL) {
				log.error("Register type mismatch for ReadInputDiscretesResponse: "+reg);
				reg.setNull();
			} else if (offset > getDataLength(response)) {
				log.error("Invalid offset for ReadInputDiscretesResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" "+reg);
				reg.setNull();
			} else {
				RegisterData temp = new RegisterData();
				temp.setData(((ReadInputDiscretesResponse)response).getDiscreteStatus(offset), timestamp);
				if (useSampling)
					reg.setDataWithSampling(temp);
				else
					reg.setData(temp);
			}
		}
		if (response instanceof ReadInputRegistersResponse) {
			if (reg.getRegisterType() != RegisterType.INPUT_REGISTER) {
				log.error("Register type mismatch for ReadInputRegisterResponse: "+reg);
				reg.setNull();
			} else if (getDataLength(response) >= offset+reg.getSize() && reg.getSize() == 1) {
				RegisterSizeType sizeType = reg.getSizeType();
				int val;
				if (sizeType == RegisterSizeType.SIGNED) {
					short shortVal = (short) ((ReadInputRegistersResponse)response).getRegister(offset).getValue();
					val = shortVal;
				} else {
					val = ((ReadInputRegistersResponse)response).getRegister(offset).getValue();
				}
				RegisterData temp = new RegisterData();
				temp.setData(new Integer(val), timestamp);
				if (reg.getFunctionClass() != null) {
					DataFunction func = DataProcessingManager.getDataFunction(reg.getFunctionClass());
					func.process(temp, reg.getDataFunctionArgument());
				}
				if (useSampling)
					reg.setDataWithSampling(temp);
				else
					reg.setData(temp);
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
				temp.setDataFloat(val, timestamp);
				if (reg.getFunctionClass() != null) {
					DataFunction func = DataProcessingManager.getDataFunction(reg.getFunctionClass());
					func.process(temp, reg.getDataFunctionArgument());
				}
				if (useSampling)
					reg.setDataWithSampling(temp);
				else
					reg.setData(temp);
			} else { //not 1 or 2 words
				log.error("Invalid offset for ReadInputRegisterResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" Size: "+reg.getSize()+" "+reg);
				reg.setNull();
			}
		}
		if (response instanceof ReadMultipleRegistersResponse) {
			if (reg.getRegisterType() != RegisterType.OUTPUT_REGISTER) {
				log.error("Register type mismatch for ReadMultipleRegistersResponse: "+reg);
				reg.setNull();
			} else if (getDataLength(response) >= offset+reg.getSize() && reg.getSize() == 1) {
				RegisterSizeType sizeType = reg.getSizeType();
				int val;
				if (sizeType == RegisterSizeType.SIGNED) {
					short shortVal = (short) ((ReadMultipleRegistersResponse)response).getRegister(offset).getValue();
					val = shortVal;
				} else {
					val = ((ReadMultipleRegistersResponse)response).getRegister(offset).getValue();
				}
				RegisterData temp = new RegisterData();
				temp.setData(new Integer(val), timestamp);
				if (reg.getFunctionClass() != null) {
					DataFunction func = DataProcessingManager.getDataFunction(reg.getFunctionClass());
					func.process(temp, reg.getDataFunctionArgument());
				}
				if (useSampling)
					reg.setDataWithSampling(temp);
				else
					reg.setData(temp);
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
				temp.setDataFloat(val, timestamp);
				if (reg.getFunctionClass() != null) {
					DataFunction func = DataProcessingManager.getDataFunction(reg.getFunctionClass());
					func.process(temp, reg.getDataFunctionArgument());
				}
				if (useSampling)
					reg.setDataWithSampling(temp);
				else
					reg.setData(temp);
			} else { //not 1 or 2 words
				log.error("Invalid offset for ReadMultipleRegistersResponse - Offset: "+offset+" DataLength: "+getDataLength(response)+" Size: "+reg.getSize()+" "+reg);
				reg.setNull();
			}
		}
		//log.trace("Setting register (NEW): "+reg);
	}

	private void updateRegisters(ArrayList<RealRegister> registerList, ArrayList<ModbusResponse> responses, Long timestamp, boolean useSamplings) {
		for (ModbusResponse response : responses) {
			ArrayList<RealRegister> regsToUpdate = getRegistersToUpdate(registerList, response);
			for (RealRegister regToUpdate : regsToUpdate) {
				//log.trace("Got Register To Update: "+regToUpdate);
				this.setRegisterData(regToUpdate, response, timestamp, useSamplings);
				//if (regToUpdate instanceof ConfigRegister) {
					//log.trace("Set config register: "+regToUpdate);
				//}
			}
		}
	}

	public ArrayList<OutputModule> getOutputModules() {
		ArrayList<OutputModule> ret = new ArrayList<OutputModule>();
		ret.addAll(outputModules);
		return ret;
	}

	public void stop() {
		setOnline(false);
		for (OutputModule output : outputModules) {
			output.close();
		}
	}
	
	public void setOnline(boolean online) {
		synchronized (isOnline) {
			isOnline.set(online);
			if (!online) {
				offlineTime = System.nanoTime();
			}
		}
	}

	@Override
	public boolean isOnline() {
		synchronized (isOnline) {
			return isOnline.get();
		}
	}
}
