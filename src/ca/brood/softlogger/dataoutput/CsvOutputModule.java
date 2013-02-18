package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.util.CsvFileWriter;

public class CsvOutputModule extends AbstractOutputModule {
	private Logger log;
	private CsvFileWriter writer;
	
	public CsvOutputModule() {
		super();
		log = Logger.getLogger(CsvOutputModule.class);
		writer = new CsvFileWriter("testOut.csv");
	}
	
	public CsvOutputModule(CsvOutputModule o) {
		super(o);
		log = Logger.getLogger(CsvOutputModule.class);
		writer = new CsvFileWriter(o.writer);
	}


	@Override
	public String getDescription() {
		return "CsvOutputModule";
	}

	@Override
	public boolean configure(Node rootNode) {
		return true;
	}

	@Override
	public void run() {
		log.info("Running");
		ArrayList<String> heads = new ArrayList<String>();
		heads.add("datetime");
		heads.add("timestamp");
		ArrayList<RealRegister> re = this.getRegisters();
		for (RealRegister r : re) {
			heads.add(r.getFieldName());
		}
		writer.setHeaders(heads);
		ArrayList<String> values = new ArrayList<String>();
		Calendar cal = Calendar.getInstance();
		values.add(String.format("%1$tY%1$tm%1$td-%1$tT", cal));
		values.add(""+(System.currentTimeMillis()));
		
		for (RealRegister r : re) {
			values.add(""+r.getInteger());
		}
		writer.writeData(values);
		this.resetRegisterSamplings();
	}

	@Override
	public AbstractOutputModule clone() {
		return new CsvOutputModule(this);
	}

}
