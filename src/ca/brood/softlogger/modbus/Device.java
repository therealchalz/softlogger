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
	
	private int address = Integer.MAX_VALUE;
	private String description = "";
	private ArrayList<ConfigRegister> configRegs;
	private ArrayList<DataRegister> dataRegs;
	private ArrayList<VirtualRegister> virtualRegs;
	private ModbusChannel channel = null;
	private ScheduledFuture<?> future = null;
	private int poll = 0;
	private int defaultPoll = 0;
	
	public Device() {
		log = Logger.getLogger(Device.class);
		configRegs = new ArrayList<ConfigRegister>();
		dataRegs = new ArrayList<DataRegister>();
		virtualRegs = new ArrayList<VirtualRegister>();
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
				ConfigRegister c = new ConfigRegister();
				if (c.configure(configNode))
					configRegs.add(c);
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
		log.trace("Device is running: "+this.description);
		//Treat the config registers as data registers - read them
		for (int i=0; i<configRegs.size(); i++) {
			ConfigRegister c = configRegs.get(i);
			ReadMultipleRegistersRequest req = c.getRequest();
			req.setUnitID(this.address);
			try {
				ReadMultipleRegistersResponse resp = (ReadMultipleRegistersResponse) channel.executeRequest(req);
				log.debug("Got response: "+resp.getRegisterValue(0));
			} catch (ModbusException e) {
				log.debug("Got modbus exception: "+e.getMessage());
			} catch (Exception e) {
				log.debug("Got no response....");
				return; //Couldn't do a modbus request
			}
		}
		//If a config register's value is invalid, write the correct one
		//Read the data registers
		//Work on the virtual registers
		//All done
	}
}
