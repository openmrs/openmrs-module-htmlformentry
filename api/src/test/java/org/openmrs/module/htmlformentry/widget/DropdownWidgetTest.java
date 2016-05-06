package org.openmrs.module.htmlformentry.widget;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

public class DropdownWidgetTest {

    @Test
    public void testSingleOption() throws Exception {
        DropdownWidget widget = new DropdownWidget();

        List<Option> option = new ArrayList<Option>();

        option.add(new Option("label1", "value1", false));

        widget.setOptions(option);
        widget.setInitialValue("choose one");

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);
        assertThat(html, is("<select id=\"w1\" name=\"w1\"><option value=\"value1\">label1</option></select>"));
    }

    @Test
    public void testMultipleOptions() throws Exception {
        DropdownWidget widget = new DropdownWidget();

        List<Option> option = new ArrayList<Option>();

        option.add(new Option("label1", "value1", false));
        option.add(new Option("label2", "value2", true));
        option.add(new Option("label3", "value3", false));
        option.add(new Option("label4", "value4", false));
        option.add(new Option("label5", "value5", false));

        widget.setOptions(option);
        widget.setInitialValue("choose one");

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);
        assertThat(html, is("<select id=\"w1\" name=\"w1\"><option value=\"value1\">label1</option><option value=\"value2\" selected=\"true\">label2</option><option value=\"value3\">label3</option><option value=\"value4\">label4</option><option value=\"value5\">label5</option></select>"));
    }
}
