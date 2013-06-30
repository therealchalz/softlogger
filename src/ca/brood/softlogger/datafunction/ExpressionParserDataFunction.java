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
package ca.brood.softlogger.datafunction;

import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RegisterData;
import org.nfunk.jep.JEP;

public class ExpressionParserDataFunction implements DataFunction {
	private Logger log;
	
	public ExpressionParserDataFunction() {
		log = Logger.getLogger(ExpressionParserDataFunction.class);
	}
	
	@Override
	public void process(RegisterData data, String funcArg) {
		log.trace("Processing: "+funcArg);
		JEP j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addConstant("$", new Double(data.getFloat()));
		
		j.parseExpression(funcArg);
		
		if (j.hasError()) {
			log.error((j.getErrorInfo()));
			data.setNull();
		} else {
			Double value = j.getValue();
			
			if (j.hasError()) {
				log.error(j.getErrorInfo());
			} else {
				data.setDataFloat(value.floatValue());
				log.trace("Evaluated to : "+value);
			}
		}
	}

}
