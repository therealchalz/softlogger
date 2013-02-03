package ca.brood.softlogger.modbus;
import ca.brood.softlogger.modbus.channel.ModbusChannel;
import ca.brood.softlogger.modbus.register.*;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.msg.*;

public class Device implements Runnable {
	private Logger log;
	private final int id;
	private int unitId = Integer.MAX_VALUE;
	private String description = "";
	private ArrayList<ConfigRegister> configRegs;
	private ArrayList<DataRegister> dataRegs;
	//private ArrayList<VirtualRegister> virtualRegs;
	private ModbusChannel channel = null;
	private ScheduledFuture<?> future = null;
	private int scanRate = 0;
	private int logInterval = 0;
	private static volatile int nextId = 1;
	
	public Device(int channelId) {
		this.id = getNextDeviceId();
		log = Logger.getLogger(Device.class+": "+id+" on Channel: "+channelId);
		configRegs = new ArrayList<ConfigRegister>();
		dataRegs = new ArrayList<DataRegister>();
		//virtualRegs = new ArrayList<VirtualRegister>();
	}
	
	private static synchronized int getNextDeviceId() {
		return nextId++;
	}
	
	public void setFuture(ScheduledFuture<?> fut) {
		this.future = fut;
	}
	public ScheduledFuture<?> getFuture() {
		return future;
	}
	public String getDescription() {
		return this.description;
	}
	
	public void setChannel(ModbusChannel chan) {
		this.channel = chan;
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
		
		//Sort the registers
		RealRegister.organizeRegisters(configRegs);
		RealRegister.organizeRegisters(dataRegs);
		
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
		for (int i=0; i<configRegs.size(); i++) {
			ConfigRegister c = configRegs.get(i);
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
