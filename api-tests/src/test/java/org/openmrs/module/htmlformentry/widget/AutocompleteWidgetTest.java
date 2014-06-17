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

import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class AutocompleteWidgetTest extends BaseModuleContextSensitiveTest {


    @Test
	public void autocompleteWidget_shouldAcceptLocationOptionsWithSingleOrDoubleQuotesInMiddle() throws Exception {

        AutocompleteWidget autocompleteWidget = null;
        String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
        FormEntryContext enterContext = session.getContext();
        Map<Widget, String> widgets = enterContext.getFieldNames();
        Set<Map.Entry<Widget,String>> entries = widgets.entrySet();

        for(Map.Entry<Widget,String> entry : entries){
            if(entry.getKey().getClass().equals(AutocompleteWidget.class)){
              autocompleteWidget = (AutocompleteWidget)entry.getKey();
            }
        }

        String locationWithDoubleQuotes = "Te\"st LocationDou";
        String locationWithSingleQuotes = "Te\'st LocationSin";
        Option optionDouble = new Option(locationWithDoubleQuotes, "" ,false);
        Option optionSingle = new Option(locationWithSingleQuotes, "", false);

        if (autocompleteWidget != null) {
            autocompleteWidget.addOption(optionDouble);
            autocompleteWidget.addOption(optionSingle);
            String generatedHtml = autocompleteWidget.generateHtml(enterContext);
            Assert.assertTrue(generatedHtml.indexOf("Unknown Location,Xanadu,Te\\'st LocationDou,Te\\'st LocationSin") > -1);
        }

    }

    @Test
	public void autocompleteWidget_shouldAcceptLocationOptionsWithSpecialCharacters() throws Exception {

        AutocompleteWidget autocompleteWidget = null;
        String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
        FormEntryContext enterContext = session.getContext();
        Map<Widget, String> widgets = enterContext.getFieldNames();
        Set<Map.Entry<Widget,String>> entries = widgets.entrySet();

        for(Map.Entry<Widget,String> entry : entries){
            if(entry.getKey().getClass().equals(AutocompleteWidget.class)){
              autocompleteWidget = (AutocompleteWidget)entry.getKey();
            }
        }

        String locationWithSpecCharacters = "Tést Locãtion Doùblê";
        Option optionDouble = new Option(locationWithSpecCharacters, "" ,false);

        if (autocompleteWidget != null) {
            autocompleteWidget.addOption(optionDouble);
            String generatedHtml = autocompleteWidget.generateHtml(enterContext);
            Assert.assertTrue(generatedHtml.indexOf("Unknown Location,Xanadu,Tést Locãtion Doùblê") > -1);
        }

    }
}
