package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.validator.FormValidator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Spring validator for an HTML Form object.
 */
public class HtmlFormValidator implements Validator {

	protected final Log log = LogFactory.getLog(getClass());
	
	/** 
     * Tests whether the validator supports the specified class
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean supports(Class clazz) {
        return HtmlForm.class.equals(clazz);
    }

    /**
     * Validates the specified HTML Form, placing any errors in the Errors object passed to it
     * 
     * @should reject xml containing encounter type tag for a form with an encounter type
	 * @should allow xml containing encounter type tag for a form with no encounter type
     */
    @Override
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
                if (hf.getForm() != null) {
					if (hf.getForm().getEncounterType() != null && hasEncounterTypeTag(hf.getXmlData())) {
						 throw new FormEntryException("encounterType tag is not allowed for a form that is already associated to encounter type");
					}
				}
            } catch (Exception ex) {
                errors.rejectValue("xmlData", null, ex.getMessage());
                log.warn("Error in HTML form", ex);
            }
        }
    }

    private boolean hasEncounterTypeTag(String xml) throws Exception {
		Document doc = HtmlFormEntryUtil.stringToDocument(xml);
		Node formNode = HtmlFormEntryUtil.findChild(doc, "htmlform");
		return HtmlFormEntryUtil.findChild(formNode, "encounterType") != null;
	}
}
