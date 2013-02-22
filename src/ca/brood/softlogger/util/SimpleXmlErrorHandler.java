/*******************************************************************************
 * Copyright (c) 2013 Charles Hache. All rights reserved. 
 * 
 * This file is part of the softlogger project.
 * softlogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * softlogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with softlogger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache - initial API and implementation
 ******************************************************************************/
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
