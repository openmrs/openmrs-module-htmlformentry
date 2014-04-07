package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;


/**
 * Make a test against the logicTest/velocityTest to include/exclude the content within
 * <p/>
 * For example the following input:
 * <p/>
 * <pre>
 * {@code
 * <htmlform>
 *      <includeIf logicTest="FEMALE">
 * 		<obs conceptId="123" labelText="Pregnant?"/>
 * 	</includeIf>
 * </htmlform>
 * }
 * </pre>
 * <p/>
 * Would include the following only if the logicTest="FEMALE" success
 * <p/>
 * <pre>
 * {@code
 * <htmlform>
 * 		<obs conceptId="123" labelText="Pregnant?"/>
 * </htmlform>
 * }
 * </pre>
 *
 */
public class IncludeIfTagHandler extends LogicConditionalIncludeTagHandler {

    @Override
    public boolean shouldIncludeContent(FormEntrySession session, Node parent, Node node) throws BadFormDesignException {
        return shouldIncludeContentHelper(session, parent, node);
    }

}
