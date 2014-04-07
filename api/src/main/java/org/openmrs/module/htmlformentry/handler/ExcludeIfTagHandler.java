package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

/**
 * This is the opposite of IncludeIfTagHandler
 */
public class ExcludeIfTagHandler extends LogicConditionalIncludeTagHandler {

    @Override
    public boolean shouldIncludeContent(FormEntrySession session, Node parent, Node node) throws BadFormDesignException {
        return !shouldIncludeContentHelper(session, parent, node);
    }

}
