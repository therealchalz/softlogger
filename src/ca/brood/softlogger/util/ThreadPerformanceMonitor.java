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
package ca.brood.softlogger.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class ThreadPerformanceMonitor {
	//TODO:
	//Set maximum number of threads to store data for
	//If we run out of space, then delete from the list of exited
	//threads, starting with the ones with the lightest duty cycle.
	private static Map<Thread,ThreadPerformanceData> performanceData;
	private static Logger log;
	
	static {
		performanceData = new HashMap<Thread, ThreadPerformanceData>();
		log = Logger.getLogger(ThreadPerformanceMonitor.class);
	}
	
	private static class ThreadPerformanceData {
		private long lastStart = 0;
		private long lastStop = 0;
		private long onTime = 0;
		private long offTime = 0;
		private boolean running = false;
		private boolean didExit = false;
		
		public ThreadPerformanceData() {
			
		}
		
		public void starting() {
			if (!running) {
				long theTime = System.nanoTime();
				if (lastStop > 0) {
					long elapsed = theTime - lastStop;
					
					offTime += elapsed;
				}
				lastStart = theTime;
				running = true;
			}
		}
		
		public void stopping() {
			if (running) {
				long theTime = System.nanoTime();
				if (lastStart > 0) {
					long elapsed = theTime - lastStart;
					
					onTime += elapsed;
				}
				lastStop = theTime;
				running = false;
			}
		}
		
		public double getDutyCycle() {
			long extraOn = 0;
			long extraOff = 0;
			if (running) {
				extraOn = System.nanoTime() - lastStart;
			} else {
				extraOff = System.nanoTime() - lastStop;
			}
			if (onTime+offTime+extraOn+extraOff > 0)
				return (double)(onTime+extraOn)/(double)(onTime+offTime+extraOn+extraOff)*100.0;
			return 0;
		}
		
		public void threadExited() {
			didExit = true;
		}
		
		public boolean exited() {
			return didExit;
		}
	}
	
	public static synchronized void threadStarting() {
		Thread t = Thread.currentThread();
		if (!performanceData.containsKey(t)) {
			ThreadPerformanceData n = new ThreadPerformanceData();
			performanceData.put(t, n);
		}
		
		ThreadPerformanceData pd = performanceData.get(t);
		pd.starting();
	}
	public static synchronized void threadStopping() {
		Thread t = Thread.currentThread();
		if (!performanceData.containsKey(t)) {
			ThreadPerformanceData n = new ThreadPerformanceData();
			performanceData.put(t, n);
		}
		
		ThreadPerformanceData pd = performanceData.get(t);
		pd.stopping();
	}
	
	public static synchronized void printPerformanceData() {
		log.info("Performance Numbers: ");
		Set<Thread> keys = performanceData.keySet();
		for (Thread t : keys) {
			ThreadPerformanceData pd = performanceData.get(t);
			log.info(t.getName()+"(exited: "+pd.exited()+"): "+pd.getDutyCycle()+"%");
		}
	}
	
	public static synchronized void threadExit() {
		Thread t = Thread.currentThread();
		if (!performanceData.containsKey(t)) {
			ThreadPerformanceData n = new ThreadPerformanceData();
			performanceData.put(t, n);
		}
		
		ThreadPerformanceData pd = performanceData.get(t);
		pd.stopping();
		pd.threadExited();
	}
}
