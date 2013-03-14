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
package ca.brood.softlogger.dataoutput;

import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PrettyPeriodicSchedulable;
import ca.brood.softlogger.util.Util;

public class DBOutputModule extends AbstractOutputModule {
	private Logger log;
	private PrettyPeriodicSchedulable logSchedulable;
	private boolean firstIntervalOutputted = false;
	private String dbUser = "";
	private String database = "";
	private String dbPassword = "";
	private String dbHost = "";
	private int dbPort = 3306;
	private Connection connection = null;
	
	public DBOutputModule() {
		super();
		log = Logger.getLogger(DBOutputModule.class);
		logSchedulable = new PrettyPeriodicSchedulable();
		logSchedulable.setAction(this);
	}
	
	public DBOutputModule(DBOutputModule o) {
		super(o);
		log = Logger.getLogger(DBOutputModule.class);
		logSchedulable = new PrettyPeriodicSchedulable(o.logSchedulable);
		logSchedulable.setAction(this);
		firstIntervalOutputted = o.firstIntervalOutputted;
		dbUser = o.dbUser;
		database = o.database;
		dbPassword = o.dbPassword;
		dbHost = o.dbHost;
		dbPort = o.dbPort;
	}
	
	@Override
	public DBOutputModule clone() {
		return new DBOutputModule(this);
	}

	@Override
	public String getDescription() {
		return "DBOutputModule";
	}
	
	@Override
	protected void setConfigValue(String name, String value) {
		if ("logIntervalSeconds".equalsIgnoreCase(name)) {
			try {
				logSchedulable.setPeriod(Util.parseInt(value) * 1000);
			} catch (Exception e) {
				log.error("Error in config file: "+name+" = "+value+" ",e);
			}
		} else if ("dbUser".equalsIgnoreCase(name)) {
			dbUser = value;
		} else if ("database".equalsIgnoreCase(name)) {
			database = value;
		} else if ("dbHost".equalsIgnoreCase(name)) {
			dbHost = value;
		} else if ("dbPassword".equalsIgnoreCase(name)) {
			dbPassword = value;
		} else if ("dbPort".equalsIgnoreCase(name)) {
			try {
				dbPort = Util.parseInt(value);
			} catch (Exception e) {
				//dbPort keeps its default value
				log.error("Error in config file: "+name+" = "+value+" ",e);
			}
		} else {
			log.warn("Got unexpected config value: "+name+" = "+value);
		}
	}

	@Override
	public long getNextRun() {
		return logSchedulable.getNextRun();
	}

	@Override
	public void execute() {
		logSchedulable.execute();
	}

	@Override
	public void run() {
		log.info("Running"); 
		
		//Skip first interval of data
		if (!firstIntervalOutputted) {
			firstIntervalOutputted = true;
			this.resetRegisterSamplings();
			return;
		}
		
		//TODO: Write values to DB
		ArrayList<RealRegister> re = this.m_Registers.readRegisters();
		this.resetRegisterSamplings();
		
		checkConnection();
	}
	
	private boolean checkConnection() {
		
		if (connection == null) {
			String url = "jdbc:mysql://"+dbHost+":"+dbPort+"/"+database;
			try {
				connection = DriverManager.getConnection(url, dbUser, dbPassword);
				Statement st = connection.createStatement();
	            ResultSet rs = st.executeQuery("SELECT VERSION()");

	            if (rs.next()) {
	                log.info(rs.getString(1));
	            }
	            
	            rs.close();
	            
	            st.close();
	            
	            connection.close();
	            connection = null;
			} catch (SQLException e) {
				log.error("URL: "+url+" user: "+dbUser+" pass: "+dbPassword.replaceAll(".", "*"));
				log.error("Error setting up DB connection", e);
				return false;
			}
		}
		
		
		
		return true;
	}

	@Override
	public void close() {
		if (connection != null) {
			try {
				if (!connection.isClosed()) {
					connection.close();
				}
			} catch (Exception e) {
				log.error("Exception while closing DB connection", e);
			}
		}
	}

}
