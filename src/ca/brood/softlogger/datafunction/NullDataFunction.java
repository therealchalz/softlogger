package ca.brood.softlogger.datafunction;

import org.w3c.dom.Node;

import ca.brood.softlogger.modbus.register.RegisterData;

public class NullDataFunction implements DataFunction {

	@Override
	public void process(RegisterData data, String funcArg) {
		//Do nothing
	}

}
