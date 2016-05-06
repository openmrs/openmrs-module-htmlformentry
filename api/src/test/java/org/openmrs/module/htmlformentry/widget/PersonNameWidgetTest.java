package org.openmrs.module.htmlformentry.widget;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import org.openmrs.PersonName;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersonNameWidgetTest {

    @Test
    public void testConstructor() throws Exception {
        PersonNameWidget widget = new PersonNameWidget();

        widget.setInitialValue(new PersonName("given", "middle", "family"));

        FormEntryContext formEntryContext = mock(FormEntryContext.class);
        when(formEntryContext.getFieldName(widget)).thenReturn("w1");

        String html = widget.generateHtml(formEntryContext);
        assertThat(html, is("Family name: <input type=\"text\" id=\"w1_family\" name=\"w1_family\" value=\"family\"/>Given name: <input type=\"text\" id=\"w1_given\" name=\"w1_given\" value=\"given\"/>"));
    }
}
