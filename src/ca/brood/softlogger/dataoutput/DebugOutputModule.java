package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.register.DataRegister;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public class DebugOutputModule extends AbstractOutputModule implements Runnable {
	private Device device;
	private Logger log;
	
	public DebugOutputModule(Device d) {
		super();
		this.setPeriod(d.getLogInterval()*1000);
		
		log = Logger.getLogger(DebugOutputModule.class);
		
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

	@Override
	public boolean configure(Node rootNode) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public ArrayList<DataRegister> getRegisters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRegisters(ArrayList<DataRegister> regs) {
		// TODO Auto-generated method stub
		
	}

}
