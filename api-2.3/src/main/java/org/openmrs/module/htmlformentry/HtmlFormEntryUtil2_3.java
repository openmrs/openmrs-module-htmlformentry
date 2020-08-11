package org.openmrs.module.htmlformentry;

import org.openmrs.Condition;

/**
 * HTML Form Entry utility methods for version 2.3
 */
public class HtmlFormEntryUtil2_3 {
	
	/**
	 * Return control id from Condition.formFieldPath.
	 * HtmlFormEntry^MyForm.1.0/<b>my_condition_tag</b>-0
	 *
	 * @param condition
	 * @return
	 */
	public static String getControlId(Condition condition) {
		if (condition.getFormFieldPath() == null) {
			return null;
		} else {
			String controlId = condition.getFormFieldPath().split("/")[1];
			return controlId.split("-")[0];
		}
	}
	
}
