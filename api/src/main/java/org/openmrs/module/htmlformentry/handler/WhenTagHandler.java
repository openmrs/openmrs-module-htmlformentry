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

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> attributes) throws BadFormDesignException {
        String value = attributes.get(WHEN_VALUE);
        String thenDisplay = attributes.get(THEN_DISPLAY);
        if (value == null || thenDisplay == null) {
            throw new IllegalArgumentException("when tag must have '" + WHEN_VALUE + "' and '" + THEN_DISPLAY + "' attributes");
        }
        if (!thenDisplay.startsWith("#") && !thenDisplay.startsWith(".")) {
            throw new IllegalArgumentException("'" + THEN_DISPLAY + "' attribute must be a DOM #id or .class");
        }

        ObsSubmissionElement obs = session.getContext().getHighestOnStack(ObsSubmissionElement.class);

        // for now we only implement the case where value is a concept id/uuid/code
        Concept concept = HtmlFormEntryUtil.getConcept(value);
        obs.whenValueThenDisplaySection(concept, thenDisplay);
        return "";
    }

}
