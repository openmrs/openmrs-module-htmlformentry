package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Drug;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;
import org.openmrs.module.htmlformentry.util.JsonObject;

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

	@Test
	public void shouldIndicateWhenDrugOrderHistoryDrugIsRetired() {
		FormTester formTester = FormTester.buildForm("orderTestForm.xml");

		// Patient 2 has an existing, open order for drug 2, which is not retired
		FormSessionTester fst = formTester.openNewForm(2);
		OrderWidget widget = fst.getWidgets(OrderWidget.class).get(0);
		JsonObject config = widget.constructJavascriptConfig(fst.getFormEntrySession().getContext());
		JsonObject historyOrder = config.getObjectArray("history").get(0);
		assertThat(historyOrder.getObject("drug").getString("retired"), is("false"));

		// Once the drug is retired, the history entry for the existing order should reflect this
		Drug drug = Context.getConceptService().getDrug(2);
		drug.setRetired(true);
		Context.getConceptService().saveDrug(drug);

		FormSessionTester fstAfterRetiring = formTester.openNewForm(2);
		OrderWidget widgetAfterRetiring = fstAfterRetiring.getWidgets(OrderWidget.class).get(0);
		JsonObject configAfterRetiring = widgetAfterRetiring
		        .constructJavascriptConfig(fstAfterRetiring.getFormEntrySession().getContext());
		JsonObject historyOrderAfterRetiring = configAfterRetiring.getObjectArray("history").get(0);
		assertThat(historyOrderAfterRetiring.getObject("drug").getString("retired"), is("true"));
	}

	@Test
	public void shouldKeepRetiredDrugAsASelectableOptionButFlagItAsRetired() {
		FormTester formTester = FormTester.buildForm("orderTestForm.xml");

		Drug drug = Context.getConceptService().getDrug(2);
		drug.setRetired(true);
		Context.getConceptService().saveDrug(drug);

		FormSessionTester fst = formTester.openNewForm(2);
		OrderWidget widget = fst.getWidgets(OrderWidget.class).get(0);

		// The retired drug remains a selectable Option on the underlying widget, but is flagged as
		// retired so the UI (JS) can exclude it from being offered as a choice for a NEW order
		Option drugOption = widget.getWidgetConfig().getOption("drug", "2");
		assertThat(drugOption, notNullValue());
		assertThat(drugOption.isRetired(), is(true));

		// The same retired flag is surfaced to the UI in the per-concept drug list used to filter
		// the drug dropdown for a NEW order
		JsonObject config = widget.constructJavascriptConfig(fst.getFormEntrySession().getContext());
		JsonObject drugConcept = config.getObjectArray("concepts").stream()
		        .filter(c -> c.getString("conceptId").equals(drug.getConcept().getConceptId().toString())).findFirst()
		        .orElse(null);
		assertThat(drugConcept, notNullValue());
		JsonObject drugJson = drugConcept.getObjectArray("drugs").stream()
		        .filter(d -> d.getString("drugId").equals("2")).findFirst().orElse(null);
		assertThat(drugJson, notNullValue());
		assertThat(drugJson.getString("retired"), is("true"));
	}
}
