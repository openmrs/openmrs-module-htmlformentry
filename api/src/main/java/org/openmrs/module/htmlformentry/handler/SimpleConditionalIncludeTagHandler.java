/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

/**
 * Helper for subclasses that want to decide whether or not to include certain content. (This doesn't support having the
 * module do any further substitutions on the contents of this tag.)
 */
public abstract class SimpleConditionalIncludeTagHandler extends AbstractTagHandler {

    /**
     * @param session
     * @param parent
     * @param node
     * @return whether or not to include the content
     */
    public abstract boolean shouldIncludeContent(FormEntrySession session, Node parent, Node node) throws BadFormDesignException;

    /**
     * @param session
     * @param parent
     * @param node
     * @return what content to include, or null (the default implementation) to indicate processing the tag's contents
     */
    public String getContentsToInclude(FormEntrySession session, Node parent, Node node) {
        return null;
    }

    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        if (shouldIncludeContent(session, parent, node)) {
            String toInclude = getContentsToInclude(session, parent, node);
            if (toInclude == null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        // do nothing
    }
}
