package ca.brood;
import org.apache.commons.logging.*;

public class Softlogger {
	private Log log;
	public Softlogger() {
		
	}
	public void run() {
		log = LogFactory.getLog(Softlogger.class);
		log.fatal("Test");
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Starting...");
		Softlogger s = new Softlogger();
		s.run();
	}

}
