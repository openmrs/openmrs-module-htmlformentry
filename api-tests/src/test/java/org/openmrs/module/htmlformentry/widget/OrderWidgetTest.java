package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
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
	
	@Test
	public void shouldRenderStaticHtmlForExistingOrdersInViewMode() {
		FormTester formTester = FormTester.buildForm("orderTestForm.xml");
		FormSessionTester fst = formTester.openExistingToView(3);
		String html = fst.getHtmlToDisplay();
		// Orders should be pre-rendered as static HTML — no script tag needed
		assertThat(html, not(containsString("orderWidget.initialize")));
		// Section structure must mirror what renderOrdersForRevision() produces so CSS rules apply correctly
		assertThat(html, containsString("orderwidget-existing-order-section"));
		assertThat(html, containsString("orderwidget-new-order-section"));
		assertThat(html, containsString("orderwidget-orderable-section"));
		assertThat(html, containsString("orderwidget-history-section"));
		// Active/inactive and encounter-context CSS classes should be applied
		// Encounter 3 date is 2008-08-01, but orders' dateActivated are after that date,
		// so isOrderActive() correctly classifies them as inactive
		assertThat(html, containsString("order-view-inactive"));
		assertThat(html, containsString("order-view-current-encounter"));
		// Configured fields and their values should be rendered
		assertThat(html, containsString("order-field-widget order-drug"));
		assertThat(html, containsString("Drug 3"));
	}

	@Test
	public void shouldRenderTemplateWithWidgetsForTestOrder() {
		FormTester formTester = FormTester.buildForm("orderLabTestForm.xml");
		FormSessionTester fst = formTester.openNewForm(2);
		OrderWidget widget = fst.getWidgets(OrderWidget.class).get(0);
		String[] properties = { "concept", "action", "previousOrder", "careSetting", "orderReason", "orderReasonNonCoded",
		        "instructions", "urgency", "dateActivated", "scheduledDate", "specimenSource", "laterality",
		        "clinicalHistory", "frequency", "numberOfRepeats", "location", "discontinueReason",
		        "discontinueReasonNonCoded" };
		
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
