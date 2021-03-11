package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class OrderWidgetTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(OrderWidgetTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void testDrugOrdersTag_htmlShouldRenderCorrectlyWithDefaultFormValues() {
		FormTester formTester = FormTester.buildForm("orderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(2);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		formSessionTester.assertHtmlContains("orderwidget-element");
		formSessionTester.assertHtmlContains("orderwidget-order-section");
		formSessionTester.assertHtmlContains("orderwidget-selector-section");
		formSessionTester.assertHtmlContains("orderwidget-order-form");
		log.trace(formSessionTester.getHtmlToDisplay());
	}
	
	@Test
	public void shouldRenderTemplateWithAllWidgets() {
		FormTester formTester = FormTester.buildForm("orderTestForm.xml");
		FormSessionTester fst = formTester.openNewForm(2);
		OrderWidget widget = fst.getWidgets(OrderWidget.class).get(0);
		String[] properties = { "concept", "drug", "drugNonCoded", "action", "previousOrder", "careSetting", "dosingType",
		        "orderReason", "orderReasonNonCoded", "dosingInstructions", "dose", "doseUnits", "route", "frequency",
		        "asNeeded", "instructions", "urgency", "dateActivated", "scheduledDate", "duration", "durationUnits",
		        "quantity", "quantityUnits", "numRefills", "discontinueReason", "discontinueReasonNonCoded" };
		assertThat(widget.getWidgets().size(), is(properties.length));
		List<Widget> widgets = new ArrayList<>(widget.getWidgets().values());
		for (int i = 0; i < widgets.size(); i++) {
			String property = properties[i];
			Widget propertyWidget = widgets.get(i);
			fst.assertHtmlContains("<div class=\"order-field order-" + property + "\"");
			fst.assertHtmlContains("<div class=\"order-field-label order-" + property + "\"");
			fst.assertHtmlContains("<div class=\"order-field-widget order-" + property);
			fst.assertHtmlContains(propertyWidget.generateHtml(fst.getFormEntrySession().getContext()));
		}
	}
}
