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
	private int address = Integer.MAX_VALUE;
	private String description = "";
	private ArrayList<ConfigRegister> configRegs;
	private ArrayList<DataRegister> dataRegs;
	//private ArrayList<VirtualRegister> virtualRegs;
	private ModbusChannel channel = null;
	private ScheduledFuture<?> future = null;
	private int poll = 0;
	private int defaultPoll = 0;
	private static volatile int nextId = 1;
	
	public Device(int channelId) {
		this.id = getNextDeviceId();
		log = Logger.getLogger("Device: "+id+" on Channel: "+channelId);
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
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("address".compareToIgnoreCase(configNode.getNodeName())==0)) {
				this.address = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				log.debug("Device address: "+this.address);
			} else if (("description".compareToIgnoreCase(configNode.getNodeName())==0)) {
				this.description = configNode.getFirstChild().getNodeValue();
				log.debug("Device description: "+this.description);
			} else if (("configregister".compareToIgnoreCase(configNode.getNodeName())==0)) {
				ConfigRegister c = new ConfigRegister(this.id);
				if (c.configure(configNode))
					configRegs.add(c);
			} else if (("dataregister".compareToIgnoreCase(configNode.getNodeName())==0)) {
				DataRegister d = new DataRegister(this.id);
				if (d.configure(configNode))
					dataRegs.add(d);
			} else if (("poll".compareToIgnoreCase(configNode.getNodeName())==0)) {
				try {
					this.poll = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid device poll: "+configNode.getFirstChild().getNodeValue());
					this.poll = 0;
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (this.address == Integer.MAX_VALUE) {
			log.error("Error: Device does not have an address configured.");
			return false;
		}
		
		//Sort the registers
		RealRegister.organizeRegisters(configRegs);
		RealRegister.organizeRegisters(dataRegs);
		
		log.debug("Device's configuration registers: "+configRegs.toString());
		log.debug("Device's data registers: "+dataRegs.toString());
		return true;
	}
	public int getPollRate() {
		if (poll != 0)
			return poll;
		return defaultPoll;
	}
	public void setDefaultPoll(int poll) {
		log.info("Setting device's default poll rate to "+poll);
		this.defaultPoll = poll;
	}

	@Override
	public void run() {
		//TODO device run function
		if (this.channel == null) {
			return;
		}
		log.trace("Checking Config Registers");
		//Treat the config registers as data registers - read them
		for (int i=0; i<configRegs.size(); i++) {
			ConfigRegister c = configRegs.get(i);
			ModbusRequest req = c.getRequest();
			req.setUnitID(this.address);
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
				req.setUnitID(this.address);
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
		for (int i=0; i<dataRegs.size(); i++) {
			DataRegister d = dataRegs.get(i);
			ModbusRequest req = d.getRequest();
			req.setUnitID(this.address);
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
		log.debug(this.configRegs);
		log.debug(this.dataRegs);
	}
}
