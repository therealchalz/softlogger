package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.util.Util;

public class DebugOutputModule extends AbstractOutputModule {
	private Logger log;
	private String description;
	
	public DebugOutputModule() {
		super();
		log = Logger.getLogger(DebugOutputModule.class);
		description = "DebugOutputModule";
	}
	
	public DebugOutputModule(DebugOutputModule o) {
		super(o);
		log = Logger.getLogger(DebugOutputModule.class);
		description = o.description;
	}
	
	@Override
	public DebugOutputModule clone() {
		return new DebugOutputModule(this);
	}

	@Override
	public void run() {
		printDeviceData();
	}
	
	private void printDeviceData() {
		ArrayList<RealRegister> registers = this.m_Registers.readRegisters();
		log.info("Printing "+description);
		for (RealRegister register : registers) {
			try {
				if (!register.isNull())
					log.info(register.getFieldName()+"("+register.getAddress()+"): "+register.getFloat());
				else
					log.info(register.getFieldName()+"("+register.getAddress()+"): <null>");
			} catch (Exception e) {
				log.info("Exception on print: ", e);
			}
		}
		this.resetRegisterSamplings();
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
	public String getDescription() {
		return description;
	}
	
	public String toString() {
		return "DebugOutputModule - description: "+this.description +" period: "+ this.getPeriod();
	}

}
