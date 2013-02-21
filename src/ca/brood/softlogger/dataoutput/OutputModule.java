package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.RegisterCollection;
import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.Schedulable;
import ca.brood.softlogger.util.XmlConfigurable;

public interface OutputModule extends XmlConfigurable, Schedulable, Runnable {
//	public ArrayList<RealRegister> getRegisters();
//	public void setRegisters(ArrayList<? extends RealRegister> regs);
//	public void setDeviceToOutput(OutputableDevice dev);
	public String getDescription();
	public OutputModule clone();
	public RegisterCollection getRegisterCollection();
	public void setRegisterCollection(RegisterCollection reg);
}
