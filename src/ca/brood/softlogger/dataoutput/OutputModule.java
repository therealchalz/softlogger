package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.DataRegister;
import ca.brood.softlogger.scheduler.Schedulable;
import ca.brood.softlogger.util.XmlConfigurable;

public interface OutputModule extends XmlConfigurable, Schedulable{
	public ArrayList<DataRegister> getRegisters();
	public void setRegisters(ArrayList<DataRegister> regs);
}
