package org.openmrs.module.htmlformentry.handler;

import org.w3c.dom.Node;

public interface TagValidator {
	
	/**
	 * Validates a document node and returns a {@link TagAnalysis} object
	 * 
	 * @param node to validate
	 * @return {@link TagAnalysis}
	 */
	TagAnalysis validate(Node node);
}
