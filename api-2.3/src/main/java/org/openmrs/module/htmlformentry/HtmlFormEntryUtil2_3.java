package org.openmrs.module.htmlformentry;

import org.openmrs.FormRecordable;

/**
 * HTML Form Entry utility methods for version 2.3
 */
public class HtmlFormEntryUtil2_3 {
	
	/**
	 * Return control id from FormRecordable.getFormFieldPath.
	 * HtmlFormEntry^MyForm.1.0/<b>my_condition_tag</b>-0
	 *
	 * @param openmrsData
	 * @return
	 */
	public static String getControlId(FormRecordable openmrsData) {
		if (openmrsData.getFormFieldPath() == null) {
			return null;
		} else {
			String controlId = openmrsData.getFormFieldPath().split("/")[1];
			return controlId.split("-")[0];
		}
	}
	
}
