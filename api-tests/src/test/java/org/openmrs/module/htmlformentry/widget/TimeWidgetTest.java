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

package org.openmrs.module.htmlformentry.widget;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class TimeWidgetTest {

    FormEntryContext context;

    MessageSourceService messageSourceService;

    @Before
    public void setUp() throws Exception {
        context = mock(FormEntryContext.class);
        when(context.isAutomaticClientSideValidation()).thenReturn(true);
        when(context.isClientSideValidationHints()).thenReturn(true);

        messageSourceService = mock(MessageSourceService.class);
        mockStatic(Context.class);
        when(Context.getMessageSourceService()).thenReturn(messageSourceService);
    }

    @Test
    public void testTimeWidgetCreation() {
        TimeWidget widget = new TimeWidget();
        String html = widget.generateHtml(context);
        assertThat(getAttribute(html, "class"), is("hfe-hours"));
        assertThat(widget.isHidden(), is(false));
    }

    @Test
    public void testTimeWidgetHide() {
        TimeWidget widget = new TimeWidget();
        widget.setHidden(true);
        assertThat(widget.isHidden(), is(true));
    }

    @Test
    public void testTimeWidgetTimeFormat() {
        TimeWidget widget = new TimeWidget();
        widget.setTimeFormat("dd-MM-yyyy");
        assertThat(widget.getTimeFormat(), is("dd-MM-yyyy"));
    }

    @Test
    public void testTimeWidgetInitialValue() {
        TimeWidget widget = new TimeWidget();
        Date date = new Date();

        widget.setInitialValue(date);
        assertThat(widget.getInitialValue(), is(date));
    }

    /**
     * Hacky, incomplete, but good enough for these tests
     */
    private String getAttribute(String html, String attr) {
        int index = html.indexOf(attr + "=\"");
        if (index < 0) {
            return null;
        }
        int startIndex = html.indexOf("\"", index) + 1;
        int endingQuote = html.indexOf("\"", startIndex);
        return html.substring(startIndex, endingQuote);
    }

}
