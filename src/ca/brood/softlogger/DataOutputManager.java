package ca.brood.softlogger;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.register.DataRegister;
import ca.brood.softlogger.util.ThreadPerformanceMonitor;

public class DataOutputManager implements Runnable {
	private ArrayList<Device> devices;
	private Thread outManagerThread = null;
	private Boolean keepRunning = false;
	private Logger log;
	
	public DataOutputManager(ArrayList<Device> devs) {
		devices = devs;
		log = Logger.getLogger(DataOutputManager.class);
	}
	
	private void setKeepRunning(boolean b) {
		synchronized (keepRunning) {
			keepRunning = b;
		}
	}
	
	private boolean getKeepRunning() {
		synchronized (keepRunning) {
			return keepRunning;
		}
	}
	
	public void start() {
		if (outManagerThread == null) {
			outManagerThread = new Thread(this);
			setKeepRunning(true);
			outManagerThread.setName("DataOutManagerThread");
			outManagerThread.start();
			return;
		}
	}

	@Override
	public void run() {
		ThreadPerformanceMonitor.threadStarting();
		log.info("Starting");
		while (getKeepRunning()){
			try {
				ThreadPerformanceMonitor.threadStopping();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			ThreadPerformanceMonitor.threadStarting();
			for (Device d: devices) {
				printDeviceData(d);
			}
		}
	}
	
	private void printDeviceData(Device d) {
		ArrayList<DataRegister> registers = d.getDataRegisters();
		log.info("Printing "+d.getDescription());
		for (DataRegister register : registers) {
			try {
				if (!register.isNull())
					log.info(register.getFieldName()+"("+register.getAddress()+"): "+register.getInteger());
				else
					log.info(register.getFieldName()+"("+register.getAddress()+"): <null>");
			} catch (Exception e) {
				log.info("Exception on print: ", e);
			}
		}
	}
	
	public void stop() {
		log.info("Received Stop Request");
		setKeepRunning(false);
		synchronized(outManagerThread) {
			outManagerThread.interrupt();
		}
	}
}
