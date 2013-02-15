package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.register.DataRegister;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public class DataOutputSchedulable extends PeriodicSchedulable implements Runnable{
	private Device device;
	private Logger log;
	
	public DataOutputSchedulable(Device d) {
		super(d.getLogInterval()*1000);
		
		log = Logger.getLogger(DataOutputSchedulable.class);
		
		this.setAction(this);
		device = d;
	}

	@Override
	public void run() {
		printDeviceData(device);
	}
	
	private void printDeviceData(Device d) {
		ArrayList<DataRegister> registers = d.getDataRegistersAndResetSamplings();
		log.info("Printing "+d.getDescription());
		for (DataRegister register : registers) {
			try {
				if (!register.isNull())
					log.info(register.getFieldName()+"("+register.getAddress()+"): "+register.getFloat());
				else
					log.info(register.getFieldName()+"("+register.getAddress()+"): <null>");
			} catch (Exception e) {
				log.info("Exception on print: ", e);
			}
		}
	}

}
