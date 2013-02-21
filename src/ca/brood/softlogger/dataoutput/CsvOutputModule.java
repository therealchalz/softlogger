package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.util.CsvFileWriter;
import ca.brood.softlogger.util.Util;

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
	
	private void setConfigValue(String name, String value) {
		if ("logInterval".equalsIgnoreCase(name)) {
			this.setPeriod(Util.parseInt(value));
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}

	@Override
	public boolean configure(Node rootNode) {
		NodeList configNodes = rootNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0) || 
					("#comment".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if (("configValue".compareToIgnoreCase(configNode.getNodeName())==0)) {
				String name = configNode.getAttributes().getNamedItem("name").getNodeValue();
				String value = configNode.getFirstChild().getNodeValue();
				setConfigValue(name, value);
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		return true;
	}

	@Override
	public void run() {
		log.info("Running");
		ArrayList<String> heads = new ArrayList<String>();
		heads.add("datetime");
		heads.add("timestamp");
		ArrayList<RealRegister> re = this.m_Registers.readRegisters();
		for (RealRegister r : re) {
			heads.add(r.getFieldName());
		}
		writer.setHeaders(heads);
		ArrayList<String> values = new ArrayList<String>();
		Calendar cal = Calendar.getInstance();
		values.add(String.format("%1$tY%1$tm%1$td-%1$tT", cal));
		values.add(""+(System.currentTimeMillis()));
		
		boolean atLeastOneGoodValue = false;
		
		for (RealRegister r : re) {
			if (r.isNull())
				values.add("NULL");
			else {
				values.add(""+r.getInteger());
				atLeastOneGoodValue = true;
			}
		}
		if (atLeastOneGoodValue)
			writer.writeData(values);
		this.resetRegisterSamplings();
	}

	@Override
	public AbstractOutputModule clone() {
		return new CsvOutputModule(this);
	}

}
