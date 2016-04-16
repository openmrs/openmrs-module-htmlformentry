/**
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
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.AdministrationServiceImpl;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class DateWidgetTest {
    private FormEntryContext context;
    private static final Date sampleDate = new Date(1460669115000L);

    @Before
    public void setUp() throws Exception {
        context = mock(FormEntryContext.class);

        AdministrationService administrationService = mock(AdministrationServiceImpl.class);
        mockStatic(Context.class);
        when(Context.getAdministrationService()).thenReturn(administrationService);
        when(Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_DATE_FORMAT)).
                thenReturn("yyyy-MM-dd HH:mm:ss");
        when(Context.getLocale()).thenReturn(new Locale("en"));
    }

    @Test
    public void testView_shouldReturnSpanWithDate() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.VIEW);

        DateWidget widget = new DateWidget();
        widget.setInitialValue(sampleDate);

        String html = widget.generateHtml(context);
        assertEquals("<span class=\"value\">2016-04-14 16:25:15</span>", html);
    }

    @Test
    public void testEdit_shouldReturnTextInput() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);

        DateWidget widget = new DateWidget();
        widget.setInitialValue(sampleDate);

        String html = widget.generateHtml(context);
        assertEquals("<input type=\"text\" size=\"10\" id=\"null-display\"/><input type=\"hidden\" name=\"null\" id=\"null\" /><script>setupDatePicker('yy-mm-dd HH:mm:ss', 'null','en', '#null-display', '#null', '2016-04-14')</script>", html);
    }

    @Test
    public void testEditHidden_shouldReturnHiddenInput() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);

        DateWidget widget = new DateWidget();
        widget.setInitialValue(sampleDate);
        widget.setHidden(true);

        String html = widget.generateHtml(context);
        assertEquals("<input type=\"hidden\" name=\"null\" id=\"null\" value=\"2016-04-14\" />", html);
    }
}
