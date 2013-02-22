package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.modbus.register.RegisterCollection;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public abstract class AbstractOutputModule 
		extends PeriodicSchedulable 
		implements OutputModule {
	
	protected OutputableDevice m_outputDevice;
	protected RegisterCollection m_Registers;
	
	public AbstractOutputModule() {
		super();
		this.setAction(this);
		m_outputDevice = null;
		m_Registers = new RegisterCollection();
	}
	
	public AbstractOutputModule(AbstractOutputModule other) {
		super(other);
		m_Registers = new RegisterCollection(other.m_Registers);
		m_outputDevice = other.m_outputDevice;
		this.setAction(this);
	}
	
	@Override
	abstract public AbstractOutputModule clone();

	@Override
	public RegisterCollection getRegisterCollection() {
		return m_Registers;
	}
	
	@Override
	public void setRegisterCollection(RegisterCollection reg) {
		m_Registers = reg;
	}
	
	public void resetRegisterSamplings() {
		m_Registers.resetRegisterSamplings();
	}
/*	
	public void setDeviceToOutputA(OutputableDevice dev) {
		m_outputDevice = dev;
		this.setRegisters(m_outputDevice.getAllRegisters());
	}*/
}
