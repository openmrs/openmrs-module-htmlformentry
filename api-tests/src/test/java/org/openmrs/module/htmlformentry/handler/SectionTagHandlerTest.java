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

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.schema.HtmlFormSection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SectionTagHandlerTest {

    public static final String LABEL_TEXT = "the label";
    public static final String SECTION_CONTENT = "Some content";

    private FormEntrySession formEntrySession;
    private FormEntryContext formEntryContext;
    private HtmlFormSection section;
    private CapturingPrintWriter out;
    private SectionTagHandler handler;

    @Before
    public void before() throws Exception {
        section = new HtmlFormSection();

        formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getActiveSection()).thenReturn(section);

        formEntrySession = mock(FormEntrySession.class);
        when(formEntrySession.getContext()).thenReturn(formEntryContext);

        out = new CapturingPrintWriter();
        handler = new SectionTagHandler();

    }

    @Test
    public void testDefaultBehavior() throws Exception {
        Node sectionNode = getSectionNode("<section headerLabel=\"" + LABEL_TEXT + "\"> " + SECTION_CONTENT + " </section>");

        assertTrue(handler.doStartTag(formEntrySession, out, sectionNode.getParentNode(), sectionNode));
        TestUtil.assertFuzzyEquals(out.getContent(), "<div class=\"section\"> <span class=\"sectionHeader\">" + LABEL_TEXT + "</span> ");

        out.reset();
        handler.doEndTag(formEntrySession, out, sectionNode.getParentNode(), sectionNode);
        TestUtil.assertFuzzyEquals(out.getContent(), "</div>");
    }

    @Test
    public void testSpecifyingSectionAndHeaderTags() throws Exception {
        Node sectionNode = getSectionNode("<section sectionTag=\"section\" headerTag=\"h1\" headerLabel=\"" + LABEL_TEXT + "\"> " + SECTION_CONTENT + " </section>");

        assertTrue(handler.doStartTag(formEntrySession, out, sectionNode.getParentNode(), sectionNode));
        TestUtil.assertFuzzyEquals(out.getContent(), "<section class=\"section\"> <h1 class=\"sectionHeader\">" + LABEL_TEXT + "</h1>");

        out.reset();
        handler.doEndTag(formEntrySession, out, sectionNode.getParentNode(), sectionNode);
        TestUtil.assertFuzzyEquals(out.getContent(), "</section>");
    }

    private Node getSectionNode(String xml) throws Exception {
        Document document = HtmlFormEntryUtil.stringToDocument("<htmlform>" + xml + "</htmlform>");
        Node section = HtmlFormEntryUtil.findDescendant(document, "section");
        if (section == null) {
            throw new IllegalArgumentException("Could not find <section> tag");
        }
        return section;
    }

}
