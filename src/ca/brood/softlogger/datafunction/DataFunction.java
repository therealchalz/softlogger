package ca.brood.softlogger.datafunction;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.modbus.register.RegisterData;
import ca.brood.softlogger.util.XmlConfigurable;

public interface DataFunction {
	public void process(RegisterData data, String funcArg);
}
