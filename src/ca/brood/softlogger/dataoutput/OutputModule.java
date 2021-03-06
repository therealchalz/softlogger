/*******************************************************************************
 * Copyright (c) 2013-2016 Charles Hache <chache@cygnustech.ca>.  
 * All rights reserved. 
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
 * along with softlogger.  If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 * 
 * Contributors:
 *     Charles Hache <chache@cygnustech.ca> - initial API and implementation
 ******************************************************************************/

package ca.brood.softlogger.dataoutput;

import ca.brood.brootils.xml.XMLConfigurable;
import ca.brood.softlogger.modbus.register.RegisterCollection;
import ca.brood.softlogger.scheduler.Schedulable;

public interface OutputModule extends XMLConfigurable, Schedulable {
	/** This is used to identify the output module when logging / for debugging.
	 * This should identify this class of output module (so that it's the same
	 * for all instances)
	 * @return A useful description of this output module.
	 */
	public String getDescription();
	/** Get a working copy of this output module.  The output module manager
	 * uses this method to make a copy of each output module for each device.
	 * This method must create a deep copy of the existing module and duplicate
	 * all necessary and relevant config, parameters etc.
	 * @return A deep copy of this output module.
	 */
	public OutputModule clone();
	/** Get the reference to this output module's register collection. This must
	 * not be a deep copy of the registers because the Device uses the instance
	 * returned from this method to update the registers with the latest values.
	 * @return The register collection associated with this Output Module.
	 */
	public RegisterCollection getRegisterCollection();
	/** Associates the passed in RegisterCollection with this Output Module. Each
	 * device will use this method to set the Output Module register list to match
	 * the device's.
	 * @param reg The new RegisterCollection.
	 */
	public void setRegisterCollection(RegisterCollection reg);
	/** Notifies the output module that its shutdown is imminent.
	 * This is where files and connections should be closed etc.
	 */
	public void close();
	/** Indicates whether the configured register sampling function
	 * is to be used for data updates to this output module.
	 * @return true if the register sampling function is to be used.
	 */
	public boolean useRegisterSampling();
	/** Sets the device that this output module is outputting for.
	 * Provides the output module with some metadata.
	 * @param dev The outputable device.
	 */
	public void setOutputableDevice(OutputableDevice dev);
}
