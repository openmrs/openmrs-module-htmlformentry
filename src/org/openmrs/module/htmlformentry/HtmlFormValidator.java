package org.openmrs.module.htmlformentry;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class HtmlFormValidator implements Validator {

    public boolean supports(Class clazz) {
        return HtmlForm.class.equals(clazz);
    }

    public void validate(Object obj, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "form", "error.null");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "error.null");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "xmlData", "error.null");

        HtmlForm hf = (HtmlForm) obj;
        if (hf.getXmlData() != null) {
            try {
                FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), hf.getXmlData());
            } catch (Exception ex) {
                errors.rejectValue("xmlData", null, ex.getMessage());
            }
        }
    }

}
