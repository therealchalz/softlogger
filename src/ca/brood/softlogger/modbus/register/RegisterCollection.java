package ca.brood.softlogger.modbus.register;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


public class RegisterCollection {
	private ArrayList<RealRegister> readRegisters;
	private ArrayList<RealRegister> updateRegisters;
	private ArrayList<RealRegister> registerSetA;
	private ArrayList<RealRegister> registerSetB;
	private int whichRegisterSet;	//0-> read=A, !0-> read=B;
	private ReentrantLock readRegisterLock;
	private ReentrantLock updateRegisterLock;
	
	public RegisterCollection() {
		this(new ArrayList<RealRegister>());
	}
	
	public RegisterCollection(ArrayList<? extends RealRegister> regs) {
		registerSetA = new ArrayList<RealRegister>();
		registerSetB = new ArrayList<RealRegister>();
		for (RealRegister r : regs) {
			registerSetA.add(r.clone());
			registerSetB.add(r.clone());
		}
		readRegisterLock = new ReentrantLock();
		updateRegisterLock = new ReentrantLock();
		
		whichRegisterSet = 0;
		readRegisters = registerSetA;
		updateRegisters = registerSetB;
	}
	
	public RegisterCollection(RegisterCollection o) {
		this(o.readRegisters());
	}
	
	public ArrayList<RealRegister> readRegisters() {
		readRegisterLock.lock();
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		for (RealRegister r : readRegisters) {
			ret.add(r.clone());
		}
		readRegisterLock.unlock();
		return ret;
	}
	
	public ArrayList<RealRegister> beginUpdating() {
		ArrayList<RealRegister> ret = new ArrayList<RealRegister>();
		updateRegisterLock.lock();
		ret.addAll(updateRegisters);
		return ret;
	}
	
	public void finishUpdating() {
		toggleRegisterSet();
		updateRegisterLock.unlock();
	}
	
	public void resetRegisterSamplings() {
		ArrayList<RealRegister> regs = beginUpdating();
		for (RealRegister r : regs) {
			r.resetSampling();
		}
		finishUpdating();
	}
	
	private void toggleRegisterSet(){
		//Lock all the registers, then synchronize the read registers
		//with the updated ones.
		readRegisterLock.lock();
		updateRegisterLock.lock();
		
		readRegisters.clear();
		for (int i = 0; i < updateRegisters.size(); i++) {
			readRegisters.add(updateRegisters.get(i).clone());
		}
		
		if (whichRegisterSet == 0) {
			whichRegisterSet = 1;		
			readRegisters = registerSetB;
			updateRegisters = registerSetA;
		} else {
			whichRegisterSet = 0;
			readRegisters = registerSetA;
			updateRegisters = registerSetB;
		}
		
		readRegisterLock.unlock();
		updateRegisterLock.unlock();
	}
}
