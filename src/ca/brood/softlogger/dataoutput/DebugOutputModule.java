package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.register.RealRegister;

public class DebugOutputModule extends AbstractOutputModule {
	private Logger log;
	private String description;
	
	public DebugOutputModule() {
		super();
		log = Logger.getLogger(DebugOutputModule.class);
		description = "DebugOutputModule";
	}
	
	public DebugOutputModule(DebugOutputModule o) {
		super(o);
		log = Logger.getLogger(DebugOutputModule.class);
		description = o.description;
	}
	
	@Override
	public DebugOutputModule clone() {
		return new DebugOutputModule(this);
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

	@Override
	public String getDescription() {
		return description;
	}
	
	public String toString() {
		return "DebugOutputModule - description: "+this.description +" period: "+ this.getPeriod();
	}

}
