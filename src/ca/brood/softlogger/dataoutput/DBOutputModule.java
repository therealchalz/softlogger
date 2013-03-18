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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RealRegister;
import ca.brood.softlogger.scheduler.PrettyPeriodicSchedulable;
import ca.brood.softlogger.util.Util;

public class DBOutputModule extends AbstractOutputModule  implements Runnable {
	private Logger log;
	private PrettyPeriodicSchedulable logSchedulable;
	private boolean firstIntervalOutputted = false;
	private String dbUser = "";
	private String dbSchema = "";
	private String dbPassword = "";
	private String dbHost = "";
	private int dbPort = 3306;
	private Connection connection = null;
	private boolean isClosed = false;
	private boolean tableCreated = false;
	
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
		dbSchema = o.dbSchema;
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
		} else if ("dbSchema".equalsIgnoreCase(name)) {
			dbSchema = value;
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
		
		if (!m_OutputDevice.isOnline()) {
			return;
		}
		
		//Skip first interval of data
		if (!firstIntervalOutputted) {
			firstIntervalOutputted = true;
			this.resetRegisterSamplings();
			return;
		}
		
		if (!isClosed) {
			try {
				checkConnection();
				checkTable();
				updateRegisters();
			} catch (Exception e) {
				log.error(e);
			}
		}

	}
	
	private String sqlSanitize(String in) {
		//replace all whitespace with _ and invalid characters with $
		String ret = in.replaceAll("\\s", "_");
		ret = ret.replaceAll("[^\\w$]", "\\$");
		return ret;
	}
	
	private void updateRegisters() throws SQLException {
		String tableName = getTableName();
		DateFormat sqlDateFormat = new SimpleDateFormat(Util.SQL_DATE_FORMAT_STRING);
		for (RealRegister r : this.getRegisterCollection().readRegisters()) {
			PreparedStatement s = connection.prepareStatement("UPDATE "+dbSchema+"."+tableName
					+" SET `value`=?,`t_stamp`=?,`date`=? WHERE `address`=?;");
			s.setDouble(1, r.getFloat());
			s.setLong(2, r.getData().getTimestamp());
			s.setString(3, sqlDateFormat.format(new Date(r.getData().getTimestamp())));
			s.setInt(4, r.getLongAddress());
			s.execute();
			s.close();
		}
	}
	
	private String getTableName() {
		return sqlSanitize(m_OutputDevice.getDescription());		
	}
	
	private void checkTable() throws Exception {
		if (!tableCreated) {
			String tableName = getTableName();
			createTable(tableName);
			tableCreated = true;
		}
	}
	
	private void createTable(String tableName) throws SQLException {
		Statement st = connection.createStatement();
		st.execute("DROP TABLE IF EXISTS "+dbSchema+"."+tableName);
		st.execute("CREATE  TABLE "+dbSchema+"."+tableName+" (  `id` INT(11) unsigned NOT NULL AUTO_INCREMENT,  `name` VARCHAR(64) NULL ,  `address` INT(6) NULL ,  `value` DOUBLE NULL ,  `t_stamp` BIGINT NULL, `date` DATETIME NULL,  PRIMARY KEY (`id`) );");
		for (RealRegister r : this.getRegisterCollection().readRegisters()) {
			PreparedStatement s = connection.prepareStatement("INSERT INTO "+dbSchema+"."+tableName+" ( `name`, `address`) values (?, ?);");
			s.setString(1, r.getFieldName());
			s.setString(2, String.format("%06d", r.getLongAddress()));
			s.execute();
			s.close();
		}
		st.close();
		log.info("Table "+dbSchema+"."+tableName+" created.");
	}
	
	private void checkConnection() throws SQLException {
		if (!isClosed && connection == null || !connection.isValid(1000)) {
			String url = "jdbc:mysql://"+dbHost+":"+dbPort+"/"+dbSchema;
			
			connection = DriverManager.getConnection(url, dbUser, dbPassword);
			
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT VERSION()");

			if (rs.next()) {
				log.info("Server version: "+rs.getString(1));
			}
			rs.close();
			st.close();
		}
		
		//log.error("URL: "+url+" user: "+dbUser+" pass: "+dbPassword.replaceAll(".", "*"));
	
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
		isClosed = true;
	}

	@Override
	public boolean useRegisterSampling() {
		return false;
	}
}
