package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.htmlformentry.handler.TagAnalysis;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.module.htmlformentry.handler.TagValidator;
import org.openmrs.validator.FormValidator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Spring validator for an HTML Form object.
 */
public class HtmlFormValidator implements Validator {

	protected final Log log = LogFactory.getLog(getClass());
	
	private List<String> htmlFormWarnings = new ArrayList<String>();

	public List<String> getHtmlFormWarnings() {
		return htmlFormWarnings;
	}

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
                FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), hf.getXmlData(), null); // can't access an HttpSession here
                if (hf.getForm() != null) {
					if (hf.getForm().getEncounterType() != null && hasEncounterTypeTag(hf.getXmlData())) {
						 throw new FormEntryException("encounterType tag is not allowed for a form that is already associated to encounter type");
					}
				}
                HtmlFormEntryGenerator htmlGenerator = new HtmlFormEntryGenerator();
                String xml = hf.getXmlData();
                xml = htmlGenerator.substituteCharacterCodesWithAsciiCodes(xml);
                xml = htmlGenerator.stripComments(xml);
                xml = htmlGenerator.convertSpecialCharactersWithinLogicAndVelocityTests(xml);
                xml = htmlGenerator.applyRoleRestrictions(xml);
                xml = htmlGenerator.applyMacros(session, xml);
                xml = htmlGenerator.applyRepeats(xml);
                Document document = HtmlFormEntryUtil.stringToDocument(xml);
                validateTags(document, errors, null);
            } catch (Exception ex) {
                errors.rejectValue("xmlData", null, ex.getMessage());
                log.warn("Error in HTML form", ex);
            }
        }
    }

    public void validateTags(Node node, Errors errors, Map<String, TagHandler> tagHandlerCache) {
        if (tagHandlerCache == null) {
            tagHandlerCache = new HashMap<String, TagHandler>();
        }
        TagHandler handler = null;
        if (node.getNodeName() != null) {
            if (tagHandlerCache.containsKey(node.getNodeName())) {
                handler = tagHandlerCache.get(node.getNodeName());
            } else {
                handler = HtmlFormEntryUtil.getService().getHandlerByTagName(node.getNodeName());
                tagHandlerCache.put(node.getNodeName(), handler);
            }
        }
        if (handler != null && handler instanceof TagValidator) {
            TagAnalysis analysis = ((TagValidator) handler).validate(node);
            if (analysis.getWarnings().size() > 0 || analysis.getErrors().size() > 0) {
                htmlFormWarnings.addAll(analysis.getWarnings());
                for (String errorMsg : analysis.getErrors()) {
                    errors.reject(errorMsg);
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            validateTags(children.item(i), errors, tagHandlerCache);
        }
    }

    private boolean hasEncounterTypeTag(String xml) throws Exception {
		Document doc = HtmlFormEntryUtil.stringToDocument(xml);
		Node formNode = HtmlFormEntryUtil.findChild(doc, "htmlform");
		return HtmlFormEntryUtil.findChild(formNode, "encounterType") != null;
	}
}
