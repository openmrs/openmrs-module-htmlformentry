package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.PersonStub;

public class DropdownWidgetTest extends BaseHtmlFormEntryTest {
	
	protected FormEntryContext getContext() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		session.getHtmlToDisplay();
		return session.getContext();
	}
	
	@Test
	public void testSingleOption() throws Exception {
		FormEntryContext context = getContext();
		DropdownWidget widget = new DropdownWidget();
		
		List<Option> option = new ArrayList<Option>();
		option.add(new Option("label1", "value1", false));
		
		widget.setOptions(option);
		widget.setInitialValue(null);
		
		context.registerWidget(widget);
		String html = widget.generateHtml(context);
		assertThat(html, is("<select id=\"w3\" name=\"w3\"><option value=\"value1\">label1</option></select>"));
	}
	
	@Test
	public void testMultipleOptions() throws Exception {
		FormEntryContext context = getContext();
		DropdownWidget widget = new DropdownWidget();
		
		List<Option> option = new ArrayList<Option>();
		
		option.add(new Option("label1", "value1", false));
		option.add(new Option("label2", "value2", true));
		option.add(new Option("label3", "value3", false));
		option.add(new Option("label4", "value4", false));
		option.add(new Option("label5", "value5", false));
		
		widget.setOptions(option);
		widget.setInitialValue("value2");
		context.registerWidget(widget);
		
		String html = widget.generateHtml(context);
		assertThat(html, is(
		    "<select id=\"w3\" name=\"w3\"><option value=\"value1\">label1</option><option value=\"value2\" selected=\"true\">label2</option><option value=\"value3\">label3</option><option value=\"value4\">label4</option><option value=\"value5\">label5</option></select>"));
	}
	
	@Test
	public void testMultipleOptionsWithGroupsAndClasses() throws Exception {
		FormEntryContext context = getContext();
		DropdownWidget widget = new DropdownWidget();
		
		List<Option> options = new ArrayList<Option>();
		options.add(groupOption("label1", "value1", false, "", "", ""));
		options.add(groupOption("label2", "value2", true, "", "Group 1", "class-1"));
		options.add(groupOption("label3", "value3", false, "oc1", "Group 1", "class-1"));
		options.add(groupOption("label4", "value4", false, "oc2", "Group 2", "class-2"));
		options.add(groupOption("label5", "value5", false, "", "Group 2", "class-2"));
		
		widget.setOptions(options);
		widget.setInitialValue("value2");
		context.registerWidget(widget);
		
		String html = widget.generateHtml(context);
		StringBuilder expected = new StringBuilder();
		expected.append("<select id=\"w3\" name=\"w3\">");
		expected.append("<option value=\"").append("value1").append("\">").append("label1").append("</option>");
		expected.append("<optgroup label=\"").append("Group 1").append("\" class=\"").append("class-1").append("\">");
		expected.append("<option value=\"").append("value2").append("\" selected=\"true\">").append("label2")
		        .append("</option>");
		expected.append("<option value=\"").append("value3").append("\" class=\"").append("oc1").append("\">")
		        .append("label3").append("</option>");
		expected.append("</optgroup>");
		expected.append("<optgroup label=\"").append("Group 2").append("\" class=\"").append("class-2").append("\">");
		expected.append("<option value=\"").append("value4").append("\" class=\"").append("oc2").append("\">")
		        .append("label4").append("</option>");
		expected.append("<option value=\"").append("value5").append("\">").append("label5").append("</option>");
		expected.append("</optgroup>");
		expected.append("</select>");
		assertThat(html, is(expected.toString()));
	}
	
	@Test
	public void generateHtml_shouldRenderInitialValueAsOptionIfMissing() throws Exception {
		
		// Test String option
		assertInitialValue("4", "4", "4");
		
		// Test Concept option
		Concept c = Context.getConceptService().getConcept(88);
		assertInitialValue(c, c.getId(), c.getDisplayString());
		
		// Test Metadata option
		Location l = Context.getLocationService().getLocation(3);
		assertInitialValue(l, l.getId(), l.getName());
		
		// Test Value Stub option
		PersonStub s = new PersonStub(Context.getPersonService().getPerson(2));
		assertInitialValue(s, s.getId(), s.getDisplayValue());
		
		// Test OpenmrsData option
		Obs o = Context.getObsService().getObs(7);
		assertInitialValue(o, o.getId(), o.toString());
	}
	
	@Test
	public void generateHtml_shouldNotRenderInitialValueAsNewOptionIfAlreadyPresent() throws Exception {
		
		// Test String option
		assertNotInitialValue("4", "4", true);
		
		// Test Concept option
		Concept c = Context.getConceptService().getConcept(88);
		assertNotInitialValue(c, c.getId(), true);
		
		// Test Metadata option
		Location l = Context.getLocationService().getLocation(2);
		assertNotInitialValue(l, l.getId(), true);
		
		// Test Value Stub option
		PersonStub s = new PersonStub(Context.getPersonService().getPerson(2));
		assertNotInitialValue(s, s.getId(), false);
		
		// Test OpenmrsData option
		Obs o = Context.getObsService().getObs(7);
		assertNotInitialValue(o, o.getId(), true);
	}
	
	protected void assertInitialValue(Object val, Object idExpected, String labelExpected) throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		session.getHtmlToDisplay();
		FormEntryContext context = session.getContext();
		
		DropdownWidget widget = new DropdownWidget();
		widget.addOption(new Option("", "", false));
		assertThat(widget.getOptions().size(), is(1));
		widget.setInitialValue(val);
		context.registerWidget(widget);
		String html = widget.generateHtml(context);
		assertThat(widget.getOptions().size(), is(2));
		String expectedOption = "<option value=\"" + idExpected + "\" selected=\"true\">" + labelExpected + "</option>";
		assertThat(html, containsString(expectedOption));
	}
	
	protected void assertNotInitialValue(Object val, Object idExpected, boolean selected) throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		session.getHtmlToDisplay();
		FormEntryContext context = session.getContext();
		
		DropdownWidget widget = new DropdownWidget();
		widget.addOption(new Option("4", "4", false));
		widget.addOption(new Option("88", "88", false));
		widget.addOption(new Option("3", "3", false));
		widget.addOption(new Option("2", "2", false));
		widget.addOption(new Option("7", "7", false));
		
		assertThat(widget.getOptions().size(), is(5));
		widget.setInitialValue(val);
		context.registerWidget(widget);
		String html = widget.generateHtml(context);
		assertThat(widget.getOptions().size(), is(5));
		String selectedText = (selected ? " selected=\"true\"" : "");
		String expectedOption = "<option value=\"" + idExpected + "\"" + selectedText + ">" + idExpected + "</option>";
		assertThat(html, containsString(expectedOption));
	}
	
	protected Option groupOption(String label, String value, boolean selected, String optionClass, String groupLabel,
	        String groupCssClass) {
		Option o = new Option(label, value, selected);
		o.setCssClass(optionClass);
		o.setGroupLabel(groupLabel);
		o.setGroupCssClass(groupCssClass);
		return o;
	}
}
