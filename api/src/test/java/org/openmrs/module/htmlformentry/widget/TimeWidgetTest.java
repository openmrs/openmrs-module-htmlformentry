package org.openmrs.module.htmlformentry.widget;


import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeWidgetTest {

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

}