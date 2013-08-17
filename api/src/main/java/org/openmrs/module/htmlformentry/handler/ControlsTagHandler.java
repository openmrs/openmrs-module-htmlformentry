package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.w3c.dom.Node;

import java.io.PrintWriter;

/**
 * Usage is something like this:
 <pre>
    <obsgroup groupingConceptId="PIH:HUM Disposition construct">
        Disposition:
        <obs id="disposition" conceptId="PIH:HUM Disposition categories" answerConceptIds="PIH:Transfer out of hospital,...">
            <controls>
                <when value="PIH:Transfer out of hospital" thenDisplay="#transfer-out-location"/>
            </controls>
        </obs>
        <p id="transfer-out-location">
            Transfer to:
            <obs conceptId="PIH:Transfer out location" answerConceptIds="PIH:ZL-supported site,PIH:Non-ZL supported site"/>
        </p>
    </obsgroup>
 </pre>
 */
public class ControlsTagHandler extends AbstractTagHandler {

    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        if (session.getContext().getHighestOnStack(ObsSubmissionElement.class) == null) {
            throw new IllegalStateException("controlsSections tag must be inside an obs tag");
        }
        return true;
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
    }

}
