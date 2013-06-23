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
package ca.brood.softlogger.scheduler;


import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import ca.brood.brootils.thread.ThreadPerformanceMonitor;

public class Scheduler {
	private SchedulerRunner runner;
	
	public Scheduler () {
		runner = new SchedulerRunner();
	}
	
	public void addSchedulee(Schedulable sch) {
		runner.addSchedulee(sch);
	}
	
	public void start() {
		runner.beginRunner();
	}
	
	public void stop() {
		runner.stopRunner();
	}
	
	public void setThreadName(String name) {
		runner.setName(name);
	}
	
	private class SchedulerRunner implements Runnable {
		private SchedulerQueue schedulerQueue;
		private final AtomicBoolean shouldRun;
		private Logger log;
		private String name;
		private Thread myRunner;
		
		public SchedulerRunner() {
			schedulerQueue = new SchedulerQueue();
			shouldRun = new AtomicBoolean(false);
			log = Logger.getLogger(SchedulerRunner.class);
			name = "Unnamed Thread Scheduler";
		}
		public void addSchedulee(Schedulable sch) {
			schedulerQueue.add(sch);
		}
		
		public void setName(String name) {
			this.name = name;
		}
		public void beginRunner() {
			log.info(name+"- Starting");
			boolean start = !getShouldRun();
			setShouldRun(true);
			if (start) {
				myRunner = new Thread(this);
				myRunner.setName(name);
				myRunner.start();
			}
		}
		
		public void stopRunner() {
			log.info(name+"- Stopping");
			if (myRunner != null) {
				boolean interrupt = getShouldRun() | myRunner.isAlive();
				setShouldRun(false);
				if (interrupt) {
					myRunner.interrupt();
				}
			}
		}
		
		private boolean getShouldRun() {
			return shouldRun.get();
		}
		
		private void setShouldRun(boolean should) {
			shouldRun.set(should);
		}
		@Override
		public void run() {
			
			if (schedulerQueue.peek() == null) {
				log.error("No schedulees configured.  Exiting thread.");
				this.setShouldRun(false);
				return;
			}
			
			ThreadPerformanceMonitor.threadStarting();
			log.info("Running");

			long currentTime;
			
			while (getShouldRun()) {
				currentTime = System.currentTimeMillis();
				
				while (schedulerQueue.peek().getNextRun() <= currentTime && getShouldRun()) {
					 Schedulable s = schedulerQueue.poll();
					 s.execute();
					 schedulerQueue.add(s);
				}
				
				//The schedulable's action could take a while, so it's possible
				//that we get stopped during the action.  If so, we want to bail
				//as soon as the action is finished:
				if (!getShouldRun()) {
					break;
				}
				
				long wakeTime = schedulerQueue.peek().getNextRun();
				long sleepTime = wakeTime - System.currentTimeMillis();
				if (sleepTime > 10) {
					try {
						log.trace("Sleeping for "+sleepTime+" milliseconds.");
						ThreadPerformanceMonitor.threadStopping();
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
					}
				}
				ThreadPerformanceMonitor.threadStarting();
			}
			log.info("Thread exiting");
			ThreadPerformanceMonitor.threadExit();
		}
		
	}
}
