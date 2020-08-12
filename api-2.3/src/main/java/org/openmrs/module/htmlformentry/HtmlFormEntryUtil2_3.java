package org.openmrs.module.htmlformentry;

import org.openmrs.FormRecordable;
import org.springframework.util.StringUtils;

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
		
		// Validate if getFormFieldPath exists.
		if (StringUtils.isEmpty(openmrsData.getFormFieldPath())) {
			throw new IllegalStateException(
			        "A form recordable object was found to have no form namespace and path set, its control id in the form could not be determined.");
		}
		
		// get the control id
		String controlId = openmrsData.getFormFieldPath().split("/")[1];
		return controlId.split("(-)(?!.*-)")[0];
		
	}
	
}
