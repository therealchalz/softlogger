package ca.brood.softlogger;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.channel.*;
import java.util.concurrent.*;

public class Channel implements Runnable {
	private Logger log;
	
	private ScheduledExecutorService threadBoss;
	
	private ArrayList<Device> devices = null;
	
	private ModbusChannel channel = null;
	private int poll = 0;
	private int defaultPoll = 0;
	
	
	public Channel() {
		log = Logger.getLogger(Channel.class);
		devices = new ArrayList<Device>();
		threadBoss = new ScheduledThreadPoolExecutor(1); //1 thread
	}
	public int getPollRate() {
		if (poll != 0)
			return poll;
		return defaultPoll;
	}
	public void setDefaultPoll(int defaultPoll) {
		log.info("Setting Channel's default poll rate to "+defaultPoll);
		this.defaultPoll = defaultPoll;
		if (channel != null) {
			channel.setDefaultPoll(defaultPoll);
			log.debug("Channel is getting it's poll rate from the modbus comm channel: "+channel.getPollRate());
			this.defaultPoll = channel.getPollRate();
		}
		for (int i=0; i<devices.size(); i++) {
			devices.get(i).setDefaultPoll(getPollRate());
		}
	}
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("device".compareToIgnoreCase(configNode.getNodeName())==0))	{
				Device d = new Device();
				d.configure(configNode);
				devices.add(d);
			} else if (("serial".compareToIgnoreCase(configNode.getNodeName())==0))	{
				this.channel = new ModbusSerialChannel();
				if (!this.channel.configure(configNode)) {
					return false;
				}
			} else if (("tcp".compareToIgnoreCase(configNode.getNodeName())==0))	{
				this.channel = new ModbusTcpChannel();
				if (!this.channel.configure(configNode)) {
					return false;
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
		if (!channel.open())	//Can't open comms
			return;
		
		//Schedule the devices in the thread pool
		for (int i=0; i<devices.size(); i++) {
			Device d = devices.get(i);
			d.setFuture(threadBoss.scheduleAtFixedRate(d, 0, d.getPollRate(), TimeUnit.SECONDS));
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
}
