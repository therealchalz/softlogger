package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.RealRegister;

public interface OutputableDevice {
	public void addOutputModule(OutputModule m);
	public void deleteAllOutputModules();
	public ArrayList<RealRegister> getAllRegisters();
	public String getDescription();
}
