package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

public abstract class LogicConditionalIncludeTagHandler extends SimpleConditionalIncludeTagHandler {
	
	public boolean shouldIncludeContentHelper(FormEntrySession session, Node parent, Node node)
	        throws BadFormDesignException {
		
		String velocityTestStr = getAttribute(node, "velocityTest", null);
		
		if (velocityTestStr != null) {
			return processVelocityIncludeLogic(session, velocityTestStr);
		} else {
			throw new BadFormDesignException("Include/exclude tag must have a velocityTest attribute");
		}
		
	}
	
	/**
	 * given a test string, parse the string to return a boolean value for Velocity result
	 *
	 * @param session
	 * @param testStr for ex. = "velocityTest='#if($patient.getPatientIdentifier(5))true #else false
	 *            #end")' >"
	 * @return a boolean value if this patient is a female
	 * @throws org.openmrs.module.htmlformentry.BadFormDesignException <strong>Should</strong> return a
	 *             correct boolean value for logic test string
	 */
	protected boolean processVelocityIncludeLogic(FormEntrySession session, String testStr) throws BadFormDesignException {
		
		//("#if($patient.getPatientIdentifier(5))true #else false #end"));
		testStr = "#if (" + testStr + ") true #else false #end";
		return session.evaluateVelocityExpression(testStr).trim().equals("true");
		
	}
	
}
