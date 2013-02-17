package ca.brood.softlogger.util;

import org.w3c.dom.Node;

public interface XmlConfigurable {
	/** Configures this class based on xml element rootNode.
	 * @param rootNode the root node of this class' configuration
	 * @return true on success, false on fatal error (when execution of the program should stop).
	 */
	public boolean configure(Node rootNode);
}
