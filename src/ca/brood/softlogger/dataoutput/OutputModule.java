package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.modbus.register.RegisterCollection;
import ca.brood.softlogger.scheduler.Schedulable;
import ca.brood.softlogger.util.XmlConfigurable;

public interface OutputModule extends XmlConfigurable, Schedulable, Runnable {
	public String getDescription();
	public OutputModule clone();
	public RegisterCollection getRegisterCollection();
	public void setRegisterCollection(RegisterCollection reg);
}
