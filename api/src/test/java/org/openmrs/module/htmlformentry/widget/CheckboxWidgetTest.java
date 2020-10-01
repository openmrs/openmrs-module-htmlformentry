package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;

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
		assertThat(html, is(
		    "<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"checkboxValue\" checked=\"true\"/><label for=\"w1\">checkboxTextLabel</label><input type=\"hidden\" name=\"_w1\"/>"));
	}
	
	@Test
	public void testDisabled() throws Exception {
		CheckboxWidget cb = new CheckboxWidget();
		cb.setLabel("checkboxTextLabel");
		cb.setValue("checkboxValue");
		cb.setDisabled(true);
		
		cb.setInitialValue(cb);
		
		FormEntryContext formEntryContext = mock(FormEntryContext.class);
		when(formEntryContext.getFieldName(cb)).thenReturn("w1");
		
		String html = cb.generateHtml(formEntryContext);
		assertThat(html, is(
		    "<input type=\"checkbox\" id=\"w1\" name=\"w1\" value=\"checkboxValue\" checked=\"true\" disabled=\"disabled\"/><label for=\"w1\">checkboxTextLabel</label><input type=\"hidden\" name=\"_w1\"/>"));
	}
	
	@Test
	public void testConstructorValue() throws Exception {
		CheckboxWidget cb = new CheckboxWidget("33");
		cb.setLabel("checkboxTextLabel");
		
		cb.setInitialValue(cb);
		
		FormEntryContext formEntryContext = mock(FormEntryContext.class);
		when(formEntryContext.getFieldName(cb)).thenReturn("w1");
		
		String html = cb.generateHtml(formEntryContext);
		
		assertThat(cb.getValue(), is("33"));
		assertThat(cb.getLabel(), is("checkboxTextLabel"));
	}
	
	@Test
	public void testConstructorLabelValue() throws Exception {
		CheckboxWidget cb = new CheckboxWidget("labelText", "valueText");
		
		assertThat(cb.getValue(), is("valueText"));
		assertThat(cb.getLabel(), is("labelText"));
	}
	
	@Test
	public void testConstructorLabelValueToggle() throws Exception {
		CheckboxWidget cb = new CheckboxWidget("labelText", "valueText", "toggleText");
		
		assertThat(cb.getValue(), is("valueText"));
		assertThat(cb.getLabel(), is("labelText"));
		assertThat(cb.getToggleTarget(), is("toggleText"));
	}
	
	@Test
	public void testConstructorLabelValueToggleDim() throws Exception {
		CheckboxWidget cb = new CheckboxWidget("labelText", "valueText", "toggleText", true);
		
		assertThat(cb.getValue(), is("valueText"));
		assertThat(cb.getLabel(), is("labelText"));
		assertThat(cb.getToggleTarget(), is("toggleText"));
		assertThat(cb.isToggleDimInd(), is(true));
	}
	
	@Test
	public void testConstructorLabelValueToggleDimDisabled() throws Exception {
		CheckboxWidget cb = new CheckboxWidget("labelText", "valueText", "toggleText", true, true);
		
		assertThat(cb.getValue(), is("valueText"));
		assertThat(cb.getLabel(), is("labelText"));
		assertThat(cb.getToggleTarget(), is("toggleText"));
		assertThat(cb.isToggleDimInd(), is(true));
		assertThat(cb.isDisabled(), is(true));
	}
}
