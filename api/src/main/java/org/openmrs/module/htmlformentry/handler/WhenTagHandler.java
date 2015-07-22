package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;

import java.util.Map;

/**
 * @see ControlsTagHandler
 */
public class WhenTagHandler extends SubstitutionTagHandler {

    public static final String WHEN_VALUE = "value";
    public static final String THEN_DISPLAY = "thenDisplay";
    public static final String THEN_JS = "thenJavaScript";
    public static final String ELSE_JS = "elseJavaScript";

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> attributes) throws BadFormDesignException {
        String value = attributes.get(WHEN_VALUE);
        String thenDisplay = attributes.get(THEN_DISPLAY);
        String thenJavaScript = attributes.get(THEN_JS);
        String elseJavaScript = attributes.get(ELSE_JS);
        if (value == null) {
            throw new IllegalArgumentException("when tag must have '" + WHEN_VALUE + "'");
        }
        if (thenDisplay == null && thenJavaScript == null && elseJavaScript == null) {
            throw new IllegalArgumentException("when tag must have at least one of '" + THEN_DISPLAY + "', '"
                    + THEN_JS + "', and '" + ELSE_JS + "' attributes");
        }
        if (thenDisplay != null && !thenDisplay.startsWith("#") && !thenDisplay.startsWith(".")) {
            throw new IllegalArgumentException("'" + THEN_DISPLAY + "' attribute must be a DOM #id or .class");
        }

        ObsSubmissionElement obs = session.getContext().getHighestOnStack(ObsSubmissionElement.class);

        // for now we only implement the case where value is a concept id/uuid/code or a Boolean
        
        Object whenCondition = null;

		if ( ! ("true".equalsIgnoreCase(value.toString()) || "false".equalsIgnoreCase(value.toString()))) {
			whenCondition = HtmlFormEntryUtil.getConcept(value);
		}
		else {
			whenCondition = value.toString();
		}

		if (thenDisplay != null) {
			obs.whenValueThenDisplaySection(whenCondition, thenDisplay);
		}
		if (thenJavaScript != null) {
			obs.whenValueThenJavaScript(whenCondition, thenJavaScript);
		}
		if (elseJavaScript != null) {
			obs.whenValueElseJavaScript(whenCondition, elseJavaScript);
		}
        return "";
    }

}
