package org.openmrs.module.htmlformentry.widget;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateWidgetTest {

    @Test
    public void testHidden() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2014-10-01");

        DateWidget widget = new DateWidget();
        widget.setHidden(true);
        widget.setInitialValue(date);

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);
        assertThat(html, is("<input type=\"hidden\" name=\"w1\" id=\"w1\" value=\"2014-10-01\" />"));
    }

    @Test
    public void testNonHiddenByDefault() throws Exception {
        DateWidget widget = new DateWidget();
        assertThat(widget.isHidden(), is(false));
    }

    @Test
    public void testSetOnChangeFunction() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2014-10-01");

        DateWidget widget = new DateWidget();
        widget.setHidden(true);
        widget.setInitialValue(date);
        widget.setDateFormat("yyyy-MM-dd");
        widget.setOnChangeFunction("changeFunc");

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);
        assertThat(html, is("<input type=\"hidden\" name=\"w1\" id=\"w1\" onChange=\"changeFunc\"  value=\"2014-10-01\" />"));
    }
}