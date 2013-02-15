package ca.brood.softlogger.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class ThreadPerformanceMonitor {
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
			log.info(t.getName()+": "+pd.getDutyCycle()+"%");
		}
	}
}
