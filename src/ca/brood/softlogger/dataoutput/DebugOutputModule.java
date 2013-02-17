package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.register.RealRegister;

public class DebugOutputModule extends AbstractOutputModule implements Runnable {
	private Logger log;
	private String description;
	
	public DebugOutputModule(Device d) {
		super();
		this.setPeriod(d.getLogInterval()*1000);
		d.addOutputModule(this);
		
		description = d.getDescription();
		
		log = Logger.getLogger(DebugOutputModule.class);
		
		this.setAction(this);
		this.setRegisters(d.getAllRegisters());
	}

	@Override
	public void run() {
		printDeviceData();
	}
	
	private void printDeviceData() {
		ArrayList<RealRegister> registers = this.getRegisters();
		log.info("Printing "+description);
		for (RealRegister register : registers) {
			try {
				if (!register.isNull())
					log.info(register.getFieldName()+"("+register.getAddress()+"): "+register.getFloat());
				else
					log.info(register.getFieldName()+"("+register.getAddress()+"): <null>");
			} catch (Exception e) {
				log.info("Exception on print: ", e);
			}
		}
		this.resetRegisterSamplings();
	}

	@Override
	public boolean configure(Node rootNode) {
		// TODO Auto-generated method stub
		return true;
	}

}
