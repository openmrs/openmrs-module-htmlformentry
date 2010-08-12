package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.Option;

/**
 * Basic test cases for HTML Form Entry widgets
 */
public class WidgetTest {

    FormEntryContext context;
    
    @Before
    public void setupContext() {
        if (context == null) {
            context = new FormEntryContext(Mode.ENTER);
        }
    }
    
    @Test
    public void checkboxWidgetShouldProduceHtml() {
        CheckboxWidget cw = new CheckboxWidget();
        context.registerWidget(cw);
        Assert.assertEquals("<input type=\"hidden\" name=\"_w1\"/><input type=\"checkbox\" name=\"w1\" value=\"true\"/>", cw.generateHtml(context));
        cw.setInitialValue("Something");
        Assert.assertEquals("<input type=\"hidden\" name=\"_w1\"/><input type=\"checkbox\" name=\"w1\" value=\"true\" checked=\"true\"/>", cw.generateHtml(context));
        cw.setLabel("This is a label");
        Assert.assertEquals("<input type=\"hidden\" name=\"_w1\"/><input type=\"checkbox\" name=\"w1\" value=\"true\" checked=\"true\"/>This is a label", cw.generateHtml(context));
    }
    
    @Test
    public void dropdownWidgetShouldProduceHtml() {
        DropdownWidget dw = new DropdownWidget();
        context.registerWidget(dw);
        dw.addOption(new Option("Extra Large", "XL", false));
        dw.addOption(new Option("Large", "L", true));
        Assert.assertEquals("<select id=\"w1\" name=\"w1\"><option value=\"XL\">Extra Large</option><option value=\"L\" selected=\"true\">Large</option></select>", dw.generateHtml(context));
    }
    
}
