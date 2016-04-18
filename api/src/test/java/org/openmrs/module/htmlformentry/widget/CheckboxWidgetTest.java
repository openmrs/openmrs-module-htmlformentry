package org.openmrs.module.htmlformentry.widget;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckboxWidgetTest {

    @Test
    public void testHidden() throws Exception {
        CheckboxWidget cb = new CheckboxWidget();
        cb.setLabel("checkboxTextLabel");
        cb.setValue("checkboxValue");

        cb.setInitialValue(cb);

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(cb)).thenReturn("w1");

        String html = cb.generateHtml(formEntryContext);
        assertThat(html, is("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"checkboxValue\" checked=\"true\"/><label for=\"w1\">checkboxTextLabel</label><input type=\"hidden\" name=\"_w1\"/>"));
    }
}