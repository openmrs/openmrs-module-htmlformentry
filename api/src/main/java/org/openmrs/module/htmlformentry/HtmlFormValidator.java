package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.validator.FormValidator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Spring validator for an HTML Form object.
 */
public class HtmlFormValidator implements Validator {

	protected final Log log = LogFactory.getLog(getClass());
	
	/** 
     * Tests whether the validator supports the specified class
     */
    @SuppressWarnings("unchecked")
    public boolean supports(Class clazz) {
        return HtmlForm.class.equals(clazz);
    }

    /**
     * Validates the specified HTML Form, placing any errors in the Errors object passed to it
     */
    public void validate(Object obj, Errors errors) {
        HtmlForm hf = (HtmlForm) obj;
        // can't use ValidationUtil for this because toString of a new non-null form is ""
        if (hf.getForm() == null)
        	errors.rejectValue("form", "error.null");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "error.null");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "xmlData", "error.null");

        if (hf.getForm() != null) {
	        errors.pushNestedPath("form");
	        new FormValidator().validate(hf.getForm(), errors);
	        errors.popNestedPath();
        }
        if (hf.getXmlData() != null) {
            try {
                @SuppressWarnings("unused")
                FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), hf.getXmlData());
            } catch (Exception ex) {
                errors.rejectValue("xmlData", null, ex.getMessage());
                log.warn("Error in HTML form", ex);
            }
        }
    }

}
