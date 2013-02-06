package ca.brood.softlogger.modbus;
import ca.brood.softlogger.modbus.channel.ModbusChannel;
import ca.brood.softlogger.modbus.register.*;

import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Device implements Runnable {
	private Logger log;
	private final int id;
	private int unitId = Integer.MAX_VALUE;
	private String description = "";
	private SortedSet<ConfigRegister> configRegs;
	private SortedSet<DataRegister> dataRegs;
	private ModbusChannel channel = null;
	private PriorityQueue<ScanGroup> scanGroups;
	
	private int scanRate = 0;
	private int logInterval = 0;
	private static volatile int nextId = 1;
	
	public Device(int channelId) {
		this.id = getNextDeviceId();
		log = Logger.getLogger(Device.class+": "+id+" on Channel: "+channelId);
		configRegs = new TreeSet<ConfigRegister>();
		dataRegs = new TreeSet<DataRegister>();
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
				if (c.configure(configNode))
					configRegs.add(c);
			} else if (("dataregister".compareToIgnoreCase(configNode.getNodeName())==0)) {
				DataRegister d = new DataRegister(this.id);
				if (d.configure(configNode))
					dataRegs.add(d);
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

	@Override
	public void run() {
		//TODO device run function
		if (this.channel == null) {
			return;
		}
		
		int ttl = getTtl();
		long time = System.currentTimeMillis();
		log.info("Time: "+time+" TTL: "+ttl);
		
		while (getTtl() < 10 && getTtl() >= 0) {
			SortedSet<RealRegister> registersToProcess = new TreeSet<RealRegister>();
			ScanGroup next = scanGroups.poll();
			next.reset();
			scanGroups.add(next);
			registersToProcess.addAll(next.getRegisters());
			
			log.info("Registers to process: "+registersToProcess);
		}
		
		
		/*
		log.trace("Checking Config Registers");
		//Treat the config registers as data registers - read them
		for (ConfigRegister c : configRegs) {
			ModbusRequest req = c.getRequest();
			req.setUnitID(this.unitId);
			try {
				ModbusResponse resp = channel.executeRequest(req);
				c.setData(resp);
			} catch (ModbusException e) {
				log.trace("Got modbus exception: "+e.getMessage());
			} catch (Exception e) {
				log.trace("Got no response....");
				return; //Couldn't do a modbus request
			}
		}
		//If a config register's value is invalid, write the correct one
		log.trace("Writing config registers");
		for (ConfigRegister c : configRegs) {
			if (!c.dataIsGood()) {
				log.debug("Config register has wrong value: "+c.toString());
				ModbusRequest req = c.getWriteRequest();
				req.setUnitID(this.unitId);
				try {
					ModbusResponse resp = channel.executeRequest(req);
					c.setData(resp);
				} catch (ModbusException e) {
					log.trace("Got modbus exception: "+e.getMessage());
				} catch (Exception e) {
					log.trace("Got no response....");
					return; //Couldn't do a modbus request
				}
			}
		}
		//Read the data registers
		log.trace("Checking data Registers");
		for (DataRegister d : dataRegs) {
			ModbusRequest req = d.getRequest();
			req.setUnitID(this.unitId);
			try {
				ModbusResponse resp = channel.executeRequest(req);
				d.setData(resp);
			} catch (ModbusException e) {
				log.trace("Got modbus exception: "+e.getMessage());
			} catch (Exception e) {
				log.trace("Got no response....");
				return; //Couldn't do a modbus request
			}
		}
		*/
		//Work on the virtual registers
		//All done
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
