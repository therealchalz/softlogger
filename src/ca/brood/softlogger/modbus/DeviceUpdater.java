package ca.brood.softlogger.modbus;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;

public class DeviceUpdater implements Runnable {
	
	private ArrayList<Device> devices;
	private ScheduledFuture<?> future = null;
	private Logger log;
	
	public DeviceUpdater(ArrayList<Device> devices, int pwnerId) {
		log = Logger.getLogger(DeviceUpdater.class+" - Channel #"+pwnerId+":");
		this.devices = devices;
		
		//Prepare
	}
	
	public void setFuture(ScheduledFuture<?> fut) {
		this.future = fut;
	}
	public ScheduledFuture<?> getFuture() {
		return future;
	}
	
	@Override
	public void run() {
		log.info("Running");
		
		for (Device d : devices) {
			d.run();
		}
	}
}
