package ca.brood.softlogger;

import java.util.Vector;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.channel.*;
import java.util.concurrent.*;

public class Channel implements Runnable {
	private Logger log;
	
	private ScheduledExecutorService threadBoss;
	
	private Vector<Device> devices = null;
	
	private ModbusChannel channel = null;
	
	
	public Channel() {
		log = Logger.getLogger(Channel.class);
		devices = new Vector<Device>();
		threadBoss = new ScheduledThreadPoolExecutor(1); //1 thread
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
			threadBoss.scheduleAtFixedRate(devices.get(i), 0, devices.get(i).getPollRate(), TimeUnit.SECONDS);
		}
	}
}
