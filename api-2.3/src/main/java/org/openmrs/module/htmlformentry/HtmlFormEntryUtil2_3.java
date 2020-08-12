package org.openmrs.module.htmlformentry;

import org.openmrs.FormRecordable;
import org.springframework.util.StringUtils;

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
		
		// Validate if getFormFieldPath exists.
		if (StringUtils.isEmpty(openmrsData.getFormFieldPath())) {
			throw new IllegalStateException(
			        "A form recordable object was found to have no form namespace and path set, its control id in the form could not be determined.");
		}
		
		// get the control id
		String controlId = openmrsData.getFormFieldPath().split("/")[1];
		return controlId.split("-[^-]*$")[0];
		
	}
	
}
