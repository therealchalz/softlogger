package ca.brood.softlogger.datafunction;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RegisterData;

public class ExpressionParserDataFunction implements DataFunction {
	private Logger log;
	
	public ExpressionParserDataFunction() {
		log = Logger.getLogger(ExpressionParserDataFunction.class);
	}
	
	@Override
	public void process(RegisterData data, String funcArg) {
		log.info("Processing: "+funcArg);
	}

}
