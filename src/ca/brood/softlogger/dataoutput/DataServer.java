/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chache@brood.ca>. All rights reserved. 
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
 *     Charles Hache <chache@brood.ca> - initial API and implementation
 ******************************************************************************/
package ca.brood.softlogger.dataoutput;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.xml.XMLConfigurable;

public class DataServer implements XMLConfigurable{
	private Logger log;
	
	private String username = "";
	private String host = "";
	private String password = "";
	private String keyfile = "";
	private String homeFolder = "";
	private int port = 22;
	private int configPoll = 0;
	public DataServer() {
		log = Logger.getLogger(DataServer.class);
	}
	@Override
	public boolean configure(Node serverNode) {
		NodeList configNodes = serverNode.getChildNodes();
		for (int i=0; i<configNodes.getLength(); i++) {
			Node configNode = configNodes.item(i);
			if (("#text".compareToIgnoreCase(configNode.getNodeName())==0))	{
				continue;
			} else if ("#comment".compareTo(configNode.getNodeName())==0) {
				continue;
			} else if (("user".compareToIgnoreCase(configNode.getNodeName())==0))	{
				username = configNode.getFirstChild().getNodeValue();
			} else if (("host".compareToIgnoreCase(configNode.getNodeName())==0))	{
				host = configNode.getFirstChild().getNodeValue();
			} else if (("keyfile".compareToIgnoreCase(configNode.getNodeName())==0))	{
				keyfile = configNode.getFirstChild().getNodeValue();
			} else if (("password".compareToIgnoreCase(configNode.getNodeName())==0))	{
				password = configNode.getFirstChild().getNodeValue();
			} else if (("configpoll".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					configPoll = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid server poll config period.  Using default of 3600.");
					configPoll = 3600;
				}
			} else if (("port".compareToIgnoreCase(configNode.getNodeName())==0))	{
				try {
					port = Integer.parseInt(configNode.getFirstChild().getNodeValue());
				} catch (NumberFormatException e) {
					log.error("Invalid server port number: "+configNode.getFirstChild().getNodeValue()+".  Using default of 22.");
					port = 22;
				}
			} else if (("homeFolder".compareToIgnoreCase(configNode.getNodeName())==0))	{
				homeFolder = configNode.getFirstChild().getNodeValue();
			} else {
				log.warn("Got unknown node in config: "+configNode.getNodeName());
			}
		}
		if (username.equals("")) { 
			log.fatal("Missing user in server definition.");
			return false;
		}
		if (host.equals("")) { 
			log.fatal("Missing host in server definition.");
			return false;
		}
		if (homeFolder.equals("")) { 
			log.fatal("Missing home folder in server definition.");
			return false;
		}
		if (configPoll < 1) {
			log.error("Server poll config is out of range (must be larger than 0).  Using default of 3600.");
			configPoll = 3600;
		}
		if (port < 1 && port > 65535) {
			log.error("Server port is invalid.  Using default of 22.");
			configPoll = 22;
		}
		if (password.equals("") && keyfile.equals("")) {
			log.fatal("Must specify a keyfile or a password in the server definition");
			return false;
		}
		return true;
	}
	
	public void printAll() {
		log.info("Server username: "+this.username);
		log.info("Server host: "+this.host);
		log.info("Server port: "+this.port);
		log.info("Server poll config period: "+this.configPoll);
		log.info("Server home folder: "+this.homeFolder);
		if (this.password.equals("")) {
			log.info("Server keyfile: "+this.keyfile);
		} else {
			//log.info("Server password: "+this.password.replaceAll(".", "*"));
			log.info("Server password: ************");
		}
	}
}
