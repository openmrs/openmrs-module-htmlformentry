package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.openmrs.CodedOrFreeText;
import org.openmrs.FormRecordable;

/**
 * HTML Form Entry utility methods with Core 2.3 compatibility
 */
public class HtmlFormEntryUtil2_3 {
	
	/**
	 * Returns the control id part out of an OpenMRS data object's form namespace and path. Eg:
	 * "my_condition_tag" out of "HtmlFormEntry^MyForm.1.0/my_condition_tag-0"
	 *
	 * @param openmrsData The form recordable OpenMRS data object
	 * @return The control id or null if the form recordable has no form namespace and path set
	 */
	public static String getControlId(FormRecordable openmrsData) {
		
		// Validate if getFormFieldPath exists.
		if (StringUtils.isEmpty(openmrsData.getFormFieldPath())) {
			return null;
		}
		
		// Get the control id
		String controlId = openmrsData.getFormFieldPath().split("/")[1];
		String[] controlIdSplitted = controlId.split("-(?!.*-)");
		
		// Check if it has a control counter
		if (NumberUtils.isDigits(controlIdSplitted[1])) {
			return controlIdSplitted[0];
		} else {
			return controlId;
		}
	}
	
	/**
	 * Tells whether a CodedOrFreeText instance is empty.
	 *
	 * @param codedOrFreeText The CodedOrFreeText instance to check.
	 * @return true if the underlying coded concept is null
	 * @return true if the underlying non-coded string value is blank
	 * @return true if the underlying specific concept name is null
	 */
	public static boolean isEmpty(CodedOrFreeText codedOrFreeText) {
		
		if (codedOrFreeText.getCoded() != null) {
			return false;
		}
		
		if (StringUtils.isNotBlank(codedOrFreeText.getNonCoded())) {
			return false;
		}
		
		if (codedOrFreeText.getSpecificName() != null) {
			return false;
		}
		
		return true;
	}
}
