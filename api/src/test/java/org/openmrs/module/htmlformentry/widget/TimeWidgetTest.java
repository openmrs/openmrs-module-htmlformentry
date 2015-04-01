package org.openmrs.module.htmlformentry.widget;


import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeWidgetTest extends BaseModuleContextSensitiveTest {

    @Test
    public void testHidden() throws Exception {
        Date date = new SimpleDateFormat("HH:mm:ss").parse("18:09:35");

        TimeWidget widget = new TimeWidget();
        widget.setHidden(true);
        widget.setInitialValue(date);

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);

        assertTrue(html.contains("<input type=\"hidden\" class=\"hfe-hours\" name=\"w1hours\" value=\"18\"/>"));
        assertTrue(html.contains("<input type=\"hidden\" class=\"hfe-minutes\" name=\"w1minutes\" value=\"09\"/>"));
        assertTrue(html.contains("<input type=\"hidden\" class=\"hfe-seconds\" name=\"w1seconds\" value=\"35\"/>"));
    }

    @Test
    public void testTimeFormat() throws Exception {
        Date date = new SimpleDateFormat("HH:mm:ss").parse("18:09:35");

        TimeWidget widget = new TimeWidget();
        widget.setInitialValue(date);

        // Test default if GP is missing
        setGlobalProperty("");
        testTimeFormat(widget, "18:09");

        // Test specific GP value
        setGlobalProperty("h:mm a");
        testTimeFormat(widget, "6:09 PM");

        // Test specific widget value
        widget.setTimeFormat("mm.ss");
        testTimeFormat(widget, "09.35");
    }

    protected void testTimeFormat(TimeWidget widget, String expectedValue) {
        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getMode()).thenReturn(FormEntryContext.Mode.VIEW);
        String html = widget.generateHtml(formEntryContext);
        String expected = "<span class=\"value\">" + expectedValue + "</span>";
        assertEquals(expected, html);
    }

    protected void setGlobalProperty(String format) {
        GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(HtmlFormEntryConstants.GP_TIME_FORMAT);
        if (gp == null) {
            gp = new GlobalProperty(HtmlFormEntryConstants.GP_TIME_FORMAT);
        }
        gp.setPropertyValue(format);
        Context.getAdministrationService().saveGlobalProperty(gp);
    }
}