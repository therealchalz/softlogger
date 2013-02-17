package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.scheduler.Scheduler;

public class DataOutputManager {
	private Scheduler deviceScheduler;
	private Logger log;
	
	public DataOutputManager(ArrayList<Device> devs) {
		deviceScheduler = new Scheduler();
		for (Device d : devs) {
			deviceScheduler.addSchedulee(new DebugOutputModule(d));
		}
		deviceScheduler.setThreadName("DataOutputManager");
		log = Logger.getLogger(DataOutputManager.class);
	}
	
	public void start() {
		log.info("Starting");
		deviceScheduler.start();
	}
	
	public void stop() {
		log.info("Received Stop Request");
		deviceScheduler.stop();
	}
}
