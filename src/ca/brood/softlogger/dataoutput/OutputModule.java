package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.Schedulable;
import ca.brood.softlogger.util.XmlConfigurable;

public interface OutputModule extends XmlConfigurable, Schedulable{
	public ArrayList<RealRegister> getRegisters();
	public void setRegisters(ArrayList<? extends RealRegister> regs);
	public void resetRegisterSamplings();
}
