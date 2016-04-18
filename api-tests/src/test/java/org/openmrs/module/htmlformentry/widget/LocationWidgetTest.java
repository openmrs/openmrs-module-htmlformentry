/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.widget;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class LocationWidgetTest {
    private FormEntryContext context;
    private List<Location> testLocations;

    @Before
    public void setUp() throws Exception {
        context = mock(FormEntryContext.class);

        LocationService locationService = mock(LocationService.class);
        MessageSourceService messageSourceService = mock(MessageSourceService.class);

        mockStatic(Context.class);
        when(Context.getLocationService()).thenReturn(locationService);
        when(Context.getService(MessageSourceService.class)).thenReturn(messageSourceService);
        when(Context.getMessageSourceService()).thenReturn(messageSourceService);
        when(Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation")).thenReturn("Location");
        when(Context.getLocale()).thenReturn(new Locale("en"));

        testLocations = new ArrayList<Location>();
        Location l1 = new Location();
        l1.setName("Location1");
        l1.setId(1);
        testLocations.add(l1);
        Location l2 = new Location();
        l2.setName("Location2");
        l2.setId(2);
        testLocations.add(l2);
    }

    @Test
    public void testEdit_shouldReturnSelectOptions() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
        LocationWidget widget = new LocationWidget();
        widget.setOptions(testLocations);
        String html = widget.generateHtml(context);

        Assert.assertEquals("<select id=\"null\" name=\"null\">\n" +
                "<option value=\"\">Location</option>\n" +
                "<option value=\"1\">Location1</option>\n" +
                "<option value=\"2\">Location2</option></select>", html);
    }

    @Test
    public void testEditAutocomplete_shouldReturnInputWithJavaScript() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
        LocationWidget widget = new LocationWidget();
        widget.setOptions(testLocations);
        widget.setType("autocomplete");
        String html = widget.generateHtml(context);

        Assert.assertEquals("<input type=\"text\" id=\"display_null\" value=\"\" onblur=\"updateLocationFields(this)\" placeholder=\"null\" />\n" +
                "<input type=\"hidden\" id=\"null\" name=\"null\" value=\"\" />\n" +
                "<script>\n" +
                "var locationNameIdMap = new Object();\n" +
                "locationNameIdMap[\"Location1\"] = 1;\n" +
                "locationNameIdMap[\"Location2\"] = 2;\n" +
                "\n" +
                "function updateLocationFields(displayField){\n" +
                "\tif(locationNameIdMap[$j.trim($j(displayField).val())] == undefined)\n" +
                "\t\t$j(displayField).val('');\n" +
                "\tif($j.trim($j(displayField).val()) == '')\n" +
                "\t\t$j(\"#null\").val('');\n" +
                "}\n" +
                "\n" +
                "$j('input#display_null').autocomplete({\n" +
                "\tsource:[\"Location1\",\"Location2\"],\n" +
                "\tselect: function(event, ui) {\n" +
                "\t\t\t\t$j(\"#null\").val(locationNameIdMap[ui.item.value]);\n" +
                "\t\t\t}\n" +
                "});</script>", html);
    }
}
