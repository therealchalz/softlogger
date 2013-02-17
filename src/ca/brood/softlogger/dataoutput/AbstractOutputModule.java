package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public abstract class AbstractOutputModule 
		extends PeriodicSchedulable 
		implements OutputModule {
	
	protected ArrayList<RealRegister> m_Registers;
	
	public AbstractOutputModule() {
		super();
		m_Registers = new ArrayList<RealRegister>();
		this.setAction(this);
	}
	
	public AbstractOutputModule(AbstractOutputModule other) {
		super(other);
		m_Registers = new ArrayList<RealRegister>();
		for(RealRegister r:other.m_Registers) {
			m_Registers.add(r.clone());
		}
		this.setAction(this);
	}
	
	@Override
	abstract public AbstractOutputModule clone();

	public ArrayList<RealRegister> getRegisters() {
		synchronized (m_Registers) {
			return m_Registers;
		}
	}
	public void setRegisters(ArrayList<? extends RealRegister> regs) {
		synchronized (m_Registers) {
			m_Registers = new ArrayList<RealRegister>();
			m_Registers.addAll(regs);
		}
	}
	public void resetRegisterSamplings() {
		synchronized (m_Registers) {
			for (RealRegister reg : m_Registers) {
				reg.resetSampling();
			}
		}
	}
}
