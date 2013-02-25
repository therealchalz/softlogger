package ca.brood.softlogger.datafunction;

import org.apache.log4j.Logger;

import ca.brood.softlogger.lookuptable.LookupTable;
import ca.brood.softlogger.lookuptable.LookupTableManager;
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
		try {
			LookupTable lut = LookupTableManager.getLookupTable(funcArg);
			data.setDataFloat(lut.lookup(data.getInt()).floatValue());
		} catch (Exception e) {
			log.error("Error in table: "+funcArg);
		}
	}
	
}
