package ca.brood.softlogger.datafunction;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RegisterData;

public class LookupTableDataFunction implements DataFunction {
	private Logger log;
	
	public LookupTableDataFunction() {
		log = Logger.getLogger(LookupTableDataFunction.class);
	}
	
	@Override
	public void process(RegisterData data, String funcArg) {
		// TODO Auto-generated method stub
		log.info("Processing: "+funcArg);
	}
	
}
