package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.ToggleWidget;
import org.openmrs.module.htmlformentry.widget.UploadWidget;

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
        Assert.assertEquals("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"true\"/><input type=\"hidden\" name=\"_w1\"/>", cw.generateHtml(context));
        cw.setInitialValue("Something");
        Assert.assertEquals("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"true\" checked=\"true\"/><input type=\"hidden\" name=\"_w1\"/>", cw.generateHtml(context));
        cw.setLabel("This is a label");
        Assert.assertEquals("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"true\" checked=\"true\"/><label for=\"w1\">This is a label</label><input type=\"hidden\" name=\"_w1\"/>", cw.generateHtml(context));
    }
    
    @Test
    public void dropdownWidgetShouldProduceHtml() {
        DropdownWidget dw = new DropdownWidget();
        context.registerWidget(dw);
        dw.addOption(new Option("Extra Large", "XL", false));
        dw.addOption(new Option("Large", "L", true));
        Assert.assertEquals("<select id=\"w1\" name=\"w1\"><option value=\"XL\">Extra Large</option><option value=\"L\" selected=\"true\">Large</option></select>", dw.generateHtml(context));
    }

    @Test
    public void UploadWidgetShouldProduceHtml(){
        UploadWidget uw = new UploadWidget();
        context.registerWidget(uw);
        Assert.assertEquals("<input type=\"file\" class=\"uploadWidget\" id=\"w1\" name=\"w1\"/>",uw.generateHtml(context));
    }
    
    @Test
    public void checkboxWidgetShouldProduceHtmlWithToggleSimple() {
		ToggleWidget toggleWidget = new ToggleWidget("hatColors");
		CheckboxWidget cw = new CheckboxWidget("Has a hat?", "true", toggleWidget.getTargetId(), toggleWidget.isToggleDim());
        context.registerWidget(cw);
        Assert.assertEquals("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"true\" toggleHide=\"hatColors\"/><label for=\"w1\">Has a hat?</label><input type=\"hidden\" name=\"_w1\"/>", cw.generateHtml(context));
    }
    
    @Test
    public void checkboxWidgetShouldProduceHtmlWithToggleDim() {
		ToggleWidget toggleWidget = new ToggleWidget("{id: 'hatColors', style: 'dim'}");
		CheckboxWidget cw = new CheckboxWidget("Has a hat?", "true", toggleWidget.getTargetId(), toggleWidget.isToggleDim());
        context.registerWidget(cw);
        Assert.assertEquals("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"true\" toggleDim=\"hatColors\"/><label for=\"w1\">Has a hat?</label><input type=\"hidden\" name=\"_w1\"/>", cw.generateHtml(context));
    }
    
    @Test
    public void checkboxWidgetShouldProduceHtmlWithToggleHide() {
		ToggleWidget toggleWidget = new ToggleWidget("{id: 'hatColors', style: 'hide'}");
		CheckboxWidget cw = new CheckboxWidget("Has a hat?", "true", toggleWidget.getTargetId(), toggleWidget.isToggleDim());
        context.registerWidget(cw);
        Assert.assertEquals("<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"true\" toggleHide=\"hatColors\"/><label for=\"w1\">Has a hat?</label><input type=\"hidden\" name=\"_w1\"/>", cw.generateHtml(context));
    }
    
	@Test
	public void toggleWidgetShouldParseSimpleAttribute() {
		ToggleWidget toggleWidget = new ToggleWidget("hatColors");
		Assert.assertEquals(toggleWidget.getTargetId(), "hatColors");
		Assert.assertNull(toggleWidget.getTargetClass());
		Assert.assertNull(toggleWidget.getStyle());
		Assert.assertFalse(toggleWidget.isToggleDim());
	}
    
	@Test
	public void toggleWidgetShouldParseComplexHideAttribute() {
		ToggleWidget toggleWidget = new ToggleWidget("{id: 'hatColors', style: 'hide'}");
		Assert.assertEquals(toggleWidget.getTargetId(), "hatColors");
		Assert.assertNull(toggleWidget.getTargetClass());
		Assert.assertEquals(toggleWidget.getStyle(), "hide");
		Assert.assertFalse(toggleWidget.isToggleDim());
	}

	@Test
	public void toggleWidgetShouldParseComplexDimAttribute() {
		ToggleWidget toggleWidget = new ToggleWidget("{id: 'hatColors', style: 'dim'}");
		Assert.assertEquals(toggleWidget.getTargetId(), "hatColors");
		Assert.assertNull(toggleWidget.getTargetClass());
		Assert.assertEquals(toggleWidget.getStyle(), "dim");
		Assert.assertTrue(toggleWidget.isToggleDim());
	}
    
}
