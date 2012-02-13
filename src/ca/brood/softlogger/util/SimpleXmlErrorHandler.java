package ca.brood.softlogger.util;

import org.apache.log4j.Logger;
import org.xml.sax.*;

public class SimpleXmlErrorHandler implements ErrorHandler {
	private Logger log;
	private XmlErrorCallback ec;
	public SimpleXmlErrorHandler (Logger log, XmlErrorCallback ec) {
		this.log = log;
		this.ec = ec;
	}
    public void warning(SAXParseException e) throws SAXException {
        log.warn(e.getMessage());
    }

    public void error(SAXParseException e) throws SAXException {
    	log.error(e.getMessage());
    	ec.setConfigValid(false);
    }

    public void fatalError(SAXParseException e) throws SAXException {
    	log.fatal(e.getMessage());
    	ec.setConfigValid(false);
    }
}