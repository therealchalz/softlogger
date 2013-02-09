package ca.brood.softlogger;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.DeviceUpdater;
import ca.brood.softlogger.modbus.channel.*;


public class SoftloggerChannel implements Runnable {
	private Logger log;
	
	private ArrayList<Device> devices = null;
	private DeviceUpdater deviceUpdater;
	private final int id;
	private ModbusChannel channel = null;
	private int scanRate = 0;
	private int logInterval = 0;
	private static int nextId = 1;
	
	public SoftloggerChannel() {
		this.id = getNextId();
		log = Logger.getLogger(SoftloggerChannel.class+" ID: "+id);
		devices = new ArrayList<Device>();
	}
	public ArrayList<Device> getDevices() {
		return devices;
	}
	public static synchronized int getNextId() {
		return nextId++;
	}
	public int getScanRate() {
		return scanRate;
	}
	public void setDefaultScanRate(int scanRate) {
		if (this.scanRate == 0)
			this.scanRate = scanRate;
		for (int i=0; i<devices.size(); i++) {
			devices.get(i).setDefaultScanRate(getScanRate());
		}
	}
	public void setDefaultLogInterval(int lo) {
		if (this.logInterval == 0)
			this.logInterval = lo;
		for (int i=0; i<devices.size(); i++) {
			devices.get(i).setDefaultLogInterval(logInterval);
		}
	}
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("device".compareToIgnoreCase(configNode.getNodeName())==0))	{
				Device d = new Device(this.id);
				d.configure(configNode);
				devices.add(d);
			} else if (("serial".compareToIgnoreCase(configNode.getNodeName())==0))	{
				this.channel = new ModbusSerialChannel(this.id);
				if (!this.channel.configure(configNode)) {
					return false;
				}
			} else if (("tcp".compareToIgnoreCase(configNode.getNodeName())==0))	{
				this.channel = new ModbusTcpChannel(this.id);
				if (!this.channel.configure(configNode)) {
					return false;
				}
			} else if ("logInterval".compareToIgnoreCase(configNode.getNodeName())==0){
				//log.debug("Default logging interval: "+configNode.getFirstChild().getNodeValue());
				try {
					this.logInterval = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid log interval: "+configNode.getFirstChild().getNodeValue());
					this.logInterval = 0;
				}
			}  else if ("defaultScanRate".compareToIgnoreCase(configNode.getNodeName())==0){
				//log.debug("Default scan rate: "+configNode.getFirstChild().getNodeValue());
				try {
					this.scanRate = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid scan rate: "+configNode.getFirstChild().getNodeValue());
					this.scanRate = 0;
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		
		if (this.channel == null) {
			log.error("Error: Device has no channel (either serial or TCP) defined.");
			return false;
		}
		
		for (int index=0; index < devices.size(); index++) {
			devices.get(index).setChannel(this.channel);
		}
		
		return true;
	}
	@Override
	public void run() {
		if (channel == null)	//Not configured yet
			return;
		channel.open();
		
		deviceUpdater = new DeviceUpdater(devices, this.id);
		
		deviceUpdater.setName("Channel-"+this.id);
		
		deviceUpdater.beginUpdating();
		
	}
	
	public void stop() {
		
		if (deviceUpdater != null) {
			deviceUpdater.stopUpdating();
		}
		
		log.debug("Done issuing stop commands.");
		
		this.channel.close();
		
	}

	public void printAll() {
		if (this.scanRate > 0)
			log.info("Scan rate: "+scanRate);
		if (this.logInterval > 0)
			log.info("Log interval: "+logInterval);
		
		this.channel.printAll();
		
		for (Device d : this.devices) {
			d.printAll();
		}
	}
}
