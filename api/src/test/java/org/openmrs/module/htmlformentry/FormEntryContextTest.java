package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FormEntryContextTest {

    private FormEntryContext context;

    @Before
    public void setUp() {
        context = new FormEntryContext(FormEntryContext.Mode.ENTER);
    }

    @Test
    public void testGetWidgetByFieldName() {
        DropdownWidget widget = new DropdownWidget();
        context.registerWidget(widget);
        String fieldName = context .getFieldName(widget);
        Widget w = context.getWidgetByFieldName(fieldName);
        assertNotNull(w);
    }

    @Test
    public void testNullGetWidgetByFieldName() {
        Widget w = context.getWidgetByFieldName("test");
        assertNull(w);
    }

    @Test
    public void testGetErrorDivIds() {
        DateWidget startDateWidget;
        ErrorWidget startDateErrorWidget;
        startDateWidget = new DateWidget();
        startDateErrorWidget = new ErrorWidget();
        context.registerWidget(startDateWidget);
        context.registerErrorWidget(startDateWidget, startDateErrorWidget);
        assertNotNull(context.getErrorDivIds());
    }

    @Test
    public void testWidgetSequenceVal_shouldAssignValidSequenceForWidgets() {
        DateWidget w1 = new DateWidget();
        CheckboxWidget w2 = new CheckboxWidget();
        context.registerWidget(w1);
        context.registerWidget(w2);

        Assert.assertEquals("w1", context.getFieldName(w1));
        Assert.assertEquals("w2", context.getFieldName(w2));
    }
}
