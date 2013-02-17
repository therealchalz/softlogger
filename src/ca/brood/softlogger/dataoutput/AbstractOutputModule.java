package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import ca.brood.softlogger.modbus.register.DataRegister;
import ca.brood.softlogger.scheduler.PeriodicSchedulable;

public abstract class AbstractOutputModule 
	extends PeriodicSchedulable 
	implements OutputModule {
	protected ArrayList<DataRegister> dataRegisters;
}
