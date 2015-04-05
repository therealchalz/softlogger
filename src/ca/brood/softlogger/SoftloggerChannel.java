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
package ca.brood.softlogger;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.thread.ThreadPerformanceMonitor;
import ca.brood.brootils.xml.XMLConfigurable;
import ca.brood.softlogger.dataoutput.OutputModule;
import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.channel.*;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;
import ca.brood.softlogger.scheduler.Scheduler;


public class SoftloggerChannel implements Runnable, XMLConfigurable {
	private Logger log;
	private ArrayList<Device> devices = null;
	private final int id;
	private ModbusChannel channel = null;
	private int scanRate = 0;
	private int retryInterval = 10000;
	private static int nextId = 1;
	private Scheduler deviceScheduler;
	private PeriodicSchedulable mySchedulable;
	private final AtomicBoolean shouldRun;
	
	public SoftloggerChannel() {
		this.id = getNextId();
		log = Logger.getLogger(SoftloggerChannel.class+" ID: "+id);
		devices = new ArrayList<Device>();
		shouldRun = new AtomicBoolean(false);
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
		for (Device d : devices) {
			d.setDefaultScanRate(getScanRate());
			
		}
	}
	@Override
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0)||
					("outputModule".compareToIgnoreCase(configNode.getNodeName())==0)||
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
			} else if ("defaultScanRate".compareToIgnoreCase(configNode.getNodeName())==0){
				try {
					this.scanRate = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid scan rate: "+configNode.getFirstChild().getNodeValue());
					this.scanRate = 0;
				}
			} else if ("retryInterval".compareToIgnoreCase(configNode.getNodeName())==0){
				try {
					this.retryInterval = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid retryInterval: "+configNode.getFirstChild().getNodeValue());
				}
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		
		if (this.channel == null) {
			log.error("Error: Device has no channel (either serial or TCP) defined.");
			return false;
		}
		
		for (Device d : devices) {
			d.setChannel(this.channel);
		}
		
		//Load the channel output modules
		NodeList loggerConfigNodes = serverNode.getChildNodes();
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
					for (Device d : devices) {
						d.addOutputModule(outputModule.clone());
					}
				}
			} catch (Exception e) {
				log.error("Got exception while loading output module: ",e);
			}
		}
		
		mySchedulable = new PeriodicSchedulable(retryInterval, this);
		
		return true;
	}
	
	private boolean getShouldRun() {
		return shouldRun.get();
	}
	
	private void setShouldRun(boolean b) {
		shouldRun.set(b);
	}
	
	@Override
	public void run() {
		//The softlogger channel is setup so that this run method gets
		//scheduled every retryInterval(ms) by the same scheduler that handles the
		//devices.  The idea is that the channel can check if the devices
		//should run - if not then the channel can stop the scheduler
		//temporarily then restart it when the comm link comes back, for
		//example.
		
		boolean stopped = false;
		
		while (getShouldRun()) {
			if (!channel.isReady()) {
				if (!stopped) {
					log.info("Channel is down!");
					deviceScheduler.stop();
					for (Device d : devices) {
						d.setOnline(false);
					}
					stopped = true;
				}
			} else {
				if (stopped) {
					deviceScheduler.start();
				}
				stopped = false;
				break;
			}
			
			try {
				//Since we're running in the thread context of the channel's
				//scheduler, we don't want to pollute our stats so we keep 
				//them updated:
				ThreadPerformanceMonitor.threadStopping();
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			} finally {
				ThreadPerformanceMonitor.threadStarting();
			}
		}
	}
	
	public void start() {
		if (channel == null)	//Not configured yet
			return;
		
		deviceScheduler = new Scheduler();
		deviceScheduler.setThreadName("Scheduler - Channel "+this.id);
		
		mySchedulable.setNextRun(System.nanoTime());
		deviceScheduler.addSchedulee(mySchedulable);
		
		for (Device d : devices) {
			deviceScheduler.addSchedulee(d);
		}
		
		setShouldRun(true);
		
		//Run the channel before any of the devices
		
		deviceScheduler.start();
		
	}
	
	public void stop() {
		
		setShouldRun(false);
		
		if (deviceScheduler != null) {
			deviceScheduler.stop();
		}
		
		for (Device d : devices) {
			d.stop();
		}
		
		log.debug("Done issuing stop commands.");
		
		this.channel.close();
		
	}

	public void printAll() {
		if (this.scanRate > 0)
			log.info("Scan rate: "+scanRate);
		
		this.channel.printAll();
		
		for (Device d : this.devices) {
			d.printAll();
		}
	}
}
