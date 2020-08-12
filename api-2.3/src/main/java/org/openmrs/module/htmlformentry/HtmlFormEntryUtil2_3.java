package org.openmrs.module.htmlformentry;

import org.openmrs.FormRecordable;

/**
 * HTML Form Entry utility methods with Core 2.3 compatibility
 */
public class HtmlFormEntryUtil2_3 {
	
	/**
	 * Returns the control id part out of an OpenMRS data object's form namespace and path.
	 * Eg: "my_condition_tag" out of "HtmlFormEntry^MyForm.1.0/my_condition_tag-0"
	 *
	 * @param openmrsData The form recordable OpenMRS data object
	 * @return The control id
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
