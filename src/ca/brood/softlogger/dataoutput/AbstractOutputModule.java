package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public abstract class AbstractOutputModule 
		extends PeriodicSchedulable 
		implements OutputModule {
	
	protected ArrayList<RealRegister> m_Registers;
	
	public AbstractOutputModule() {
		m_Registers = new ArrayList<RealRegister>();
	}
	
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
