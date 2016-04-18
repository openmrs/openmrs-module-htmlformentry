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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class RadioButtonsWidgetTest {

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

    private static RadioButtonsWidget createRadioButtons(int numOptions) {
        RadioButtonsWidget widget = new RadioButtonsWidget();
        for (int i=0; i<numOptions; i++) {
            widget.addOption(new Option(String.valueOf(i), "", false));
        }

        return widget;
    }

    @Test
    public void testOneOptionSingleSeparator_shouldNotHaveAnySeparator() {
        RadioButtonsWidget widget = createRadioButtons(1);
        widget.setAnswerSeparator(null);

        String html = widget.generateHtml(context);
        Assert.assertEquals("<input type=\"radio\" id=\"null_0\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_0\">0</label>", html);
    }

    @Test
    public void testTwoOptionsNullSeparator_shouldFallBackToDefaultSeparator() {
        RadioButtonsWidget widget = createRadioButtons(2);
        widget.setAnswerSeparator(null);

        String html = widget.generateHtml(context);
        Assert.assertEquals("<input type=\"radio\" id=\"null_0\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_0\">0</label>&#160;<input type=\"radio\" id=\"null_1\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_1\">1</label>", html);
    }

    @Test
    public void testTwoOptionsSingleSeparator_shouldHaveSingleSeparator() {
        RadioButtonsWidget widget = createRadioButtons(2);
        widget.setAnswerSeparator(" ");

        String html = widget.generateHtml(context);
        Assert.assertEquals("<input type=\"radio\" id=\"null_0\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_0\">0</label> <input type=\"radio\" id=\"null_1\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_1\">1</label>", html);
    }

    @Test
    public void testTwoOptionsTwoSeparator_shouldHaveTwoSeparator() {
        RadioButtonsWidget widget = createRadioButtons(2);
        widget.setAnswerSeparator("TT");

        String html = widget.generateHtml(context);
        Assert.assertEquals("<input type=\"radio\" id=\"null_0\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_0\">0</label>TT<input type=\"radio\" id=\"null_1\" name=\"null\" value=\"\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/><label for=\"null_1\">1</label>", html);
    }
}
