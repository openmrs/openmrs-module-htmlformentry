package org.openmrs.module.htmlformentry.widget;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class RemoteJsonAutocompleteWidgetTest {

    @Test
    public void testGenerateHtml() throws Exception {
        RemoteJsonAutocompleteWidget widget = new RemoteJsonAutocompleteWidget("drug.form");

        FormEntryContext fec = mock(FormEntryContext.class);
        when(fec.getFieldName(widget)).thenReturn("w17");

        String html = widget.generateHtml(fec);
        assertThat(html, containsString("<input id=\"w17-display\"/>"));
        assertThat(html, containsString("<input id=\"w17-value\" type=\"hidden\" name=\"w17\"/>"));
        assertThat(html, containsString("var displayTemplatew17 ="));
        assertThat(html, containsString("var valueTemplatew17 ="));
    }

}
