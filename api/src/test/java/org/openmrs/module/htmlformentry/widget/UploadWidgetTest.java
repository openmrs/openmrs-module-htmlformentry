package org.openmrs.module.htmlformentry.widget;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import org.openmrs.Obs;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadWidgetTest {

    @Test
    public void testConstructor() throws Exception {
        UploadWidget widget = new UploadWidget();

        widget.setInitialValue(new Obs());

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);
        assertThat(html, is("<input type=\"file\" class=\"uploadWidget\" id=\"w1\" name=\"w1\"/>"));
    }
}
