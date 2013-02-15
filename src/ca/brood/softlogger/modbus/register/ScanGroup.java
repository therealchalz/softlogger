package ca.brood.softlogger.modbus.register;

import java.util.SortedSet;
import java.util.TreeSet;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public class ScanGroup extends PeriodicSchedulable {
	private SortedSet<RealRegister> registers;
	
	public ScanGroup(int scanRate) {
		super(scanRate);
		registers = new TreeSet<RealRegister>();
	}
	
	public int getScanRate() {
		return this.getPeriod();
	}
	
	public void addRegister(RealRegister add) {
		registers.add(add);
	}
	
	public SortedSet<RealRegister> getRegisters() {
		return registers;
	}
	
	public String toString() {
		return "ScanGroup: TTL: "+(System.currentTimeMillis()-this.getNextRun())+" scanRate: "+this.getScanRate();
	}
	
}
