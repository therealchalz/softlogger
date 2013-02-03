package ca.brood.softlogger;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.channel.*;
import java.util.concurrent.*;

public class SoftloggerChannel implements Runnable {
	private Logger log;
	
	private ScheduledExecutorService threadBoss;
	
	private ArrayList<Device> devices = null;
	private final int id;
	private ModbusChannel channel = null;
	private int scanRate = 0;
	private int logInterval = 0;
	private static int nextId = 1;
	
	public SoftloggerChannel() {
		this.id = getNextId();
		log = Logger.getLogger(SoftloggerChannel.class+" ID: "+id);
		devices = new ArrayList<Device>();
		threadBoss = new ScheduledThreadPoolExecutor(1); //TODO: 1 thread. Maybe for TCP we should have a thread for each device?
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
		
		//Schedule the devices in the thread pool
		for (int i=0; i<devices.size(); i++) {
			Device d = devices.get(i);
			d.setFuture(threadBoss.scheduleAtFixedRate(d, 0, d.getScanRate(), TimeUnit.SECONDS));
		}
		
	}
	
	public void stop() {
		threadBoss.shutdown();
		for (int i=0; i<devices.size(); i++) {
			Device d = devices.get(i);
			if (d.getFuture() != null) {
				if (!d.getFuture().isDone() && !d.getFuture().isCancelled()) {
					log.info("Stopping device: "+d.getDescription());
					d.getFuture().cancel(false);
				} else {
					log.info("Device is already stopped :"+d.getDescription());
				}
			}
		}
		log.debug("Done issuing stop commands, check status of threads");
		//I'm willing to wait a maximum of 5 seconds for the threads to close cleanly
		try {
			threadBoss.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		log.debug("Done checking threads.  Killing all devices now");
		this.kill();
	}
	public void kill() {
		for (int i=0; i<devices.size(); i++) {
			Device d = devices.get(i);
			if (d.getFuture() != null) {
				if (!d.getFuture().isDone() && !d.getFuture().isCancelled()) {
					log.info("Killing device: "+d.getDescription());
					d.getFuture().cancel(true);
				}
			}
		}
		threadBoss.shutdownNow();
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
