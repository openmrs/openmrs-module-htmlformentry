package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
        ValidationUtils.rejectIfEmpty(errors, "form", "error.null");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "error.null");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "xmlData", "error.null");

        HtmlForm hf = (HtmlForm) obj;
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
