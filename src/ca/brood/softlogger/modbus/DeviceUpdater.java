package ca.brood.softlogger.modbus;

import java.util.ArrayList;
import org.apache.log4j.Logger;

public class DeviceUpdater extends Thread {
	
	private ArrayList<Device> devices;
	private Logger log;
	private Boolean shouldRun = false;
	
	public DeviceUpdater(ArrayList<Device> devices, int pwnerId) {
		log = Logger.getLogger(DeviceUpdater.class+" - Channel #"+pwnerId+":");
		this.devices = devices;
		
		//Prepare
		shouldRun = true;
	}
	
	private boolean getShouldRun() {
		return shouldRun;
	}
	
	private void setShouldRun(boolean should) {
		synchronized (shouldRun) {
			shouldRun = should;
		}
	}
	
	public void beginUpdating() {
		setShouldRun(true);
		if (!this.isAlive()) {
			this.start();
		}
	}
	
	public void stopUpdating() {
		setShouldRun(false);
		if (this.isAlive()) {
			this.interrupt();
		}
	}
	
	@Override
	public void run() {
		log.info("Running");
		
		long timer = System.currentTimeMillis();
		long elapsedMillis = 0;
		
		while (getShouldRun()) {
			elapsedMillis = System.currentTimeMillis() - timer;
			
			for (Device d : devices) {
				d.elapsed(elapsedMillis);
			}
			
			timer = System.currentTimeMillis();
			for (Device d : devices) {
				elapsedMillis = System.currentTimeMillis() - timer;
				
				for (Device d2 : devices) {
					d2.elapsed(elapsedMillis);
				}
				
				d.run(); //this call 'takes a while'
			}
			
			long sleepTime = Long.MAX_VALUE;
			
			elapsedMillis = System.currentTimeMillis() - timer;
			for (Device d: devices) {
				d.elapsed(elapsedMillis);
				if (d.getTtl() < sleepTime) {
					sleepTime = d.getTtl();
				}
			}
			
			timer = System.currentTimeMillis();
			if (sleepTime > 10) {
				try {
					log.trace("Sleeping for "+sleepTime+" milliseconds.");
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
