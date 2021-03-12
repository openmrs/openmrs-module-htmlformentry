package org.openmrs.module.htmlformentry.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.OrderSubmissionElement;
import org.openmrs.module.htmlformentry.schema.CareSettingAnswer;
import org.openmrs.module.htmlformentry.schema.ConceptOption;
import org.openmrs.module.htmlformentry.schema.OrderField;
import org.openmrs.module.htmlformentry.schema.OrderFrequencyAnswer;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.OrderWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

/**
 * Tests that the OrderTagHandler successfully parses the XML and instantiates correctly configured
 * fields, widgets, and submission elements.
 */
public class OrderTagHandlerTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(OrderTagHandlerTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void shouldAddDrugOrderWidgetToContext() {
		assertThat(getDrugOrderWidgets("orderTestForm.xml").size(), is(1));
	}
	
	@Test
	public void shouldAddDrugOrderSubmissionElementToContext() {
		assertThat(getDrugOrderSubmissionElements("orderTestForm.xml").size(), is(1));
	}
	
	@Test
	public void shouldSupportDrugConfigurationProperties() {
		OrderWidget widget = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		OrderField f = widget.getOrderField();
		assertThat(f.getDrugOrderAnswers().get(0).getDrug().getDrugId(), is(2)); // Lookup by id
		assertThat(f.getDrugOrderAnswers().get(1).getDrug().getDrugId(), is(3)); // Lookup by uuid
		assertThat(f.getDrugOrderAnswers().get(2).getDrug().getDrugId(), is(6)); // Lookup by name
		assertThat(f.getDrugOrderAnswers().get(0).getDisplayName(), is("Triomune-30")); // Default label
		assertThat(f.getDrugOrderAnswers().get(1).getDisplayName(), is("Aspirin 325mg Tablet")); // Explicit label
		assertThat(f.getDrugOrderAnswers().get(2).getDisplayName(), is("HTML Form Entry")); // Translated label
		List<Option> options = widget.getWidgetConfig().getOrderPropertyOptions("drug");
		assertThat(options.size(), is(3));
		assertValueAttribute(widget, "drug", "2");
	}
	
	@Test
	public void shouldSupportDefaultDrugsIfNoneExplicitlyConfigured() {
		OrderWidget widget = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		OrderField f = widget.getOrderField();
		List<Drug> allDrugs = Context.getConceptService().getAllDrugs(false);
		assertThat(f.getDrugOrderAnswers().size(), is(0));
		List<Option> drugOptions = widget.getWidgetConfig().getOrderPropertyOptions("drug");
		assertThat(drugOptions.size(), is(allDrugs.size()));
	}
	
	@Test
	public void shouldSupportActionConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("action").size(), is(3));
		assertOrderPropertyOption(w, "action", 1, "NEW", "New"); // Default
		assertOrderPropertyOption(w, "action", 2, "REVISE", "Revise this Order"); // Explicit
		assertOrderPropertyOption(w, "action", 3, "DISCONTINUE", "HTML Form Entry"); // Translated
		assertValueAttribute(w, "action", "NEW");
	}
	
	@Test
	public void shouldSupportDefaultActionsIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("action").size(), is(4));
		for (int i = 0; i < Order.Action.values().length; i++) {
			Order.Action action = Order.Action.values()[i];
			Option option = w.getWidgetConfig().getOrderPropertyOptions("action").get(i);
			assertThat(option.getValue(), is(action.name()));
			String label = HtmlFormEntryUtil.translate("htmlformentry.orders.action." + action.name().toLowerCase());
			assertThat(option.getLabel(), is(label));
		}
	}
	
	@Test
	public void shouldSupportCareSettingConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "careSetting", 1, "1", "OPD"); // Lookup by uuid
		assertOrderPropertyOption(w, "careSetting", 2, "2", "HTML Form Entry"); // Translated label
		List<CareSettingAnswer> answers = w.getOrderField().getCareSettingAnswers();
		assertThat(answers.size(), is(2));
		assertThat(answers.get(0).getCareSetting().getUuid(), is("6f0c9a92-6f24-11e3-af88-005056821db0"));
		assertThat(answers.get(0).getDisplayName(), is("OPD"));
		assertThat(answers.get(1).getCareSetting().getName(), is("INPATIENT"));
		assertThat(answers.get(1).getDisplayName(), is("HTML Form Entry"));
		assertValueAttribute(w, "careSetting", "1");
	}
	
	@Test
	public void shouldSupportDefaultCareSettingsIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		List<CareSetting> all = Context.getOrderService().getCareSettings(false);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("careSetting").size(), is(all.size()));
		for (int i = 0; i < all.size(); i++) {
			CareSetting expected = all.get(i);
			Option option = w.getWidgetConfig().getOrderPropertyOptions("careSetting").get(i);
			assertThat(option.getValue(), is(expected.getId().toString()));
			assertThat(option.getLabel(), is(expected.getName()));
		}
	}
	
	@Test
	public void shouldSupportDosingTypeConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "dosingType", 1, "org.openmrs.SimpleDosingInstructions", "Structured");
		assertOrderPropertyOption(w, "dosingType", 2, "org.openmrs.FreeTextDosingInstructions", "HTML Form Entry"); // Translated Label
		assertValueAttribute(w, "dosingType", "org.openmrs.SimpleDosingInstructions");
	}
	
	@Test
	public void shouldSupportDefaultDosingTypesIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("dosingType").size(), is(2));
		Class[] types = { SimpleDosingInstructions.class, FreeTextDosingInstructions.class };
		String[] labels = { "simple", "freetext" };
		for (int i = 0; i < types.length; i++) {
			Class type = types[i];
			Option option = w.getWidgetConfig().getOrderPropertyOptions("dosingType").get(i);
			assertThat(option.getValue(), is(type.getName()));
			String label = HtmlFormEntryUtil.translate("htmlformentry.orders.dosingType." + labels[i]);
			assertThat(option.getLabel(), is(label));
		}
	}
	
	@Test
	public void shouldSupportDoseUnitsConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "doseUnits", 1, "50", "Milligrams");
		assertOrderPropertyOption(w, "doseUnits", 2, "51", "HTML Form Entry");
		List<ConceptOption> answers = w.getOrderField().getDoseUnitAnswers();
		assertThat(answers.size(), is(2));
		assertThat(answers.get(0).getConcept().getConceptId(), is(50));
		assertThat(answers.get(0).getDisplayName(), is("Milligrams"));
		assertThat(answers.get(1).getConcept().getUuid(), is("5a2aa3db-68a3-11e3-bd76-0800271c1b75"));
		assertThat(answers.get(1).getDisplayName(), is("HTML Form Entry")); // Translated Label
		assertValueAttribute(w, "doseUnits", "50");
	}
	
	@Test
	public void shouldSupportDefaultDoseUnitsIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		List<Concept> all = Context.getOrderService().getDrugDosingUnits();
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("doseUnits").size(), is(all.size()));
		for (int i = 0; i < all.size(); i++) {
			Concept expected = all.get(i);
			Option option = w.getWidgetConfig().getOrderPropertyOptions("doseUnits").get(i);
			assertThat(option.getValue(), is(expected.getId().toString()));
			assertThat(option.getLabel(), is(expected.getDisplayString()));
		}
	}
	
	@Test
	public void shouldSupportRouteConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "route", 1, "9", "HTML Form Entry");
		assertOrderPropertyOption(w, "route", 2, "10", "IV");
		List<ConceptOption> answers = w.getOrderField().getRouteAnswers();
		assertThat(answers.size(), is(2));
		assertThat(answers.get(0).getConcept().getConceptId(), is(9));
		assertThat(answers.get(0).getDisplayName(), is("HTML Form Entry"));
		assertThat(answers.get(1).getConcept().getUuid(), is("0abca361-f6bf-49cc-97de-b2f37f099dde"));
		assertThat(answers.get(1).getDisplayName(), is("IV")); // Translated Label
		assertValueAttribute(w, "route", "9");
	}
	
	@Test
	public void shouldSupportDefaultRoutesIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		List<Concept> all = Context.getOrderService().getDrugRoutes();
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("route").size(), is(all.size()));
		for (int i = 0; i < all.size(); i++) {
			Concept expected = all.get(i);
			Option option = w.getWidgetConfig().getOrderPropertyOptions("route").get(i);
			assertThat(option.getValue(), is(expected.getId().toString()));
			assertThat(option.getLabel(), is(expected.getDisplayString()));
		}
	}
	
	@Test
	public void shouldSupportFrequencyConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "frequency", 1, "1", "1 / day");
		assertOrderPropertyOption(w, "frequency", 2, "2", "HTML Form Entry");
		List<OrderFrequencyAnswer> answers = w.getOrderField().getFrequencyAnswers();
		assertThat(answers.size(), is(2));
		assertThat(answers.get(0).getOrderFrequency().getId(), is(1));
		assertThat(answers.get(0).getDisplayName(), is("1 / day"));
		assertThat(answers.get(1).getOrderFrequency().getUuid(), is("38090760-7c38-11e3-baa7-0800200c9a66"));
		assertThat(answers.get(1).getDisplayName(), is("HTML Form Entry")); // Translated Label
		assertValueAttribute(w, "frequency", "1");
	}
	
	@Test
	public void shouldSupportDefaultFrequenciesIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		List<OrderFrequency> all = Context.getOrderService().getOrderFrequencies(false);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("frequency").size(), is(all.size()));
		for (int i = 0; i < all.size(); i++) {
			OrderFrequency expected = all.get(i);
			Option option = w.getWidgetConfig().getOrderPropertyOptions("frequency").get(i);
			assertThat(option.getValue(), is(expected.getId().toString()));
			assertThat(option.getLabel(), is(expected.getName()));
		}
	}
	
	@Test
	public void shouldSupportUrgencyConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("urgency").size(), is(3));
		assertOrderPropertyOption(w, "urgency", 1, "ROUTINE", "Routine"); // Default
		assertOrderPropertyOption(w, "urgency", 2, "STAT", "ASAP"); // Explicit
		assertOrderPropertyOption(w, "urgency", 3, "ON_SCHEDULED_DATE", "HTML Form Entry"); // Translated
		assertValueAttribute(w, "urgency", "ROUTINE");
	}
	
	@Test
	public void shouldSupportDefaultUrgenciesIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("urgency").size(), is(3));
		for (int i = 0; i < Order.Urgency.values().length; i++) {
			Order.Urgency urgency = Order.Urgency.values()[i];
			Option option = w.getWidgetConfig().getOrderPropertyOptions("urgency").get(i);
			assertThat(option.getValue(), is(urgency.name()));
			String label = HtmlFormEntryUtil.translate("htmlformentry.orders.urgency." + urgency.name().toLowerCase());
			assertThat(option.getLabel(), is(label));
		}
	}
	
	@Test
	public void shouldSupportDurationUnitsConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "durationUnits", 1, "28", "Days");
		List<ConceptOption> answers = w.getOrderField().getDurationUnitAnswers();
		assertThat(answers.size(), is(1));
		assertThat(answers.get(0).getConcept().getConceptId(), is(28));
		assertThat(answers.get(0).getDisplayName(), is("Days"));
	}
	
	@Test
	public void shouldSupportDefaultDurationUnitsIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		List<Concept> all = Context.getOrderService().getDurationUnits();
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("durationUnits").size(), is(all.size()));
		for (int i = 0; i < all.size(); i++) {
			Concept expected = all.get(i);
			Option option = w.getWidgetConfig().getOrderPropertyOptions("durationUnits").get(i);
			assertThat(option.getValue(), is(expected.getId().toString()));
			assertThat(option.getLabel(), is(expected.getDisplayString()));
		}
	}
	
	@Test
	public void shouldSupportQuantityUnitsConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "quantityUnits", 1, "51", "Tablets");
		List<ConceptOption> answers = w.getOrderField().getQuantityUnitAnswers();
		assertThat(answers.size(), is(1));
		assertThat(answers.get(0).getConcept().getConceptId(), is(51));
		assertThat(answers.get(0).getDisplayName(), is("Tablets"));
	}
	
	@Test
	public void shouldSupportDefaultQuantityUnitsIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		List<Concept> all = Context.getOrderService().getDrugDispensingUnits();
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("quantityUnits").size(), is(all.size()));
		for (int i = 0; i < all.size(); i++) {
			Concept expected = all.get(i);
			Option option = w.getWidgetConfig().getOrderPropertyOptions("quantityUnits").get(i);
			assertThat(option.getValue(), is(expected.getId().toString()));
			assertThat(option.getLabel(), is(expected.getDisplayString()));
		}
	}
	
	@Test
	public void shouldSupportDiscontinueReasonConfigurationProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertOrderPropertyOption(w, "discontinueReason", 1, "16", "Patient Died");
		assertOrderPropertyOption(w, "discontinueReason", 2, "22", "HTML Form Entry");
		List<ConceptOption> answers = w.getOrderField().getDiscontinueReasonAnswers();
		assertThat(answers.size(), is(2));
		assertThat(answers.get(0).getConcept().getConceptId(), is(16));
		assertThat(answers.get(0).getDisplayName(), is("Patient Died"));
		assertThat(answers.get(1).getConcept().getConceptId(), is(22));
		assertThat(answers.get(1).getDisplayName(), is("HTML Form Entry")); // Translated label
	}
	
	@Test
	public void shouldSupportAttributesOnOrderProperties() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOrderProperties.xml").get(0);
		assertValueAttribute(w, "dose", "350.0");
		assertValueAttribute(w, "asNeeded", "true");
		assertValueAttribute(w, "instructions", "Instructions");
		assertValueAttribute(w, "dosingInstructions", "Dosing");
		assertValueAttribute(w, "duration", "30");
		assertValueAttribute(w, "quantity", "0");
		assertValueAttribute(w, "numRefills", "0");
		assertAttribute(w, "instructions", "textArea", "true");
		assertAttribute(w, "instructions", "textAreaRows", "4");
		assertAttribute(w, "instructions", "textAreaColumns", "50");
		assertAttribute(w, "dosingInstructions", "textArea", "true");
		assertAttribute(w, "dosingInstructions", "textAreaRows", "10");
		assertAttribute(w, "dosingInstructions", "textAreaColumns", "100");
	}
	
	@Test
	public void shouldSupportOptionGroupsWithQuestionAnswers() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormOptionGroups.xml").get(0);
		List<Option> options = w.getWidgetConfig().getOrderPropertyOptions("discontinueReason");
		assertThat(options.size(), is(2));
		assertThat("TOXICITY", is(options.get(0).getLabel()));
		assertThat("TRANSFERRED OUT", is(options.get(1).getLabel()));
	}
	
	@Test
	public void shouldSupportDefaultDiscontinueReasonIfNoneExplicitlyConfigured() {
		OrderWidget w = getDrugOrderWidgets("orderTestFormAllDefaults.xml").get(0);
		assertThat(w.getWidgetConfig().getOrderPropertyOptions("discontinueReason").size(), is(0));
	}
	
	@Test
	public void shouldHandleHtmlWithinTemplate() {
		FormTester formTester = FormTester.buildForm("orderTestFormOrderProperties.xml");
		FormSessionTester fst = formTester.openNewForm(6);
		fst.assertHtmlContains("<h3>Please enter your drug orders below:</h3>");
		fst.assertHtmlContains(
		    "<h3>There are some hidden properties here.  You can see how this can be used along with defaults...</h3>");
	}
	
	private List<OrderWidget> getDrugOrderWidgets(String form) {
		List<OrderWidget> ret = new ArrayList<>();
		FormTester formTester = FormTester.buildForm(form);
		FormSessionTester fst = formTester.openNewForm(6);
		assertNoBadFormDesignException(fst);
		for (Widget widget : fst.getFormEntrySession().getContext().getFieldNames().keySet()) {
			int numFound = 0;
			if (widget instanceof OrderWidget) {
				ret.add((OrderWidget) widget);
			}
		}
		return ret;
	}
	
	private List<OrderSubmissionElement> getDrugOrderSubmissionElements(String form) {
		List<OrderSubmissionElement> ret = new ArrayList<>();
		FormTester formTester = FormTester.buildForm(form);
		FormSessionTester fst = formTester.openNewForm(6);
		assertNoBadFormDesignException(fst);
		for (FormSubmissionControllerAction action : fst.getFormEntrySession().getSubmissionController().getActions()) {
			if (action instanceof OrderSubmissionElement) {
				ret.add((OrderSubmissionElement) action);
			}
		}
		return ret;
	}
	
	private void assertOrderPropertyOption(OrderWidget widget, String property, int num, String value, String label) {
		List<Option> options = widget.getWidgetConfig().getOrderPropertyOptions(property);
		Option option = options.get(num - 1);
		assertThat(option.getValue(), is(value));
		assertThat(option.getLabel(), is(label));
	}
	
	private void assertValueAttribute(OrderWidget widget, String property, String value) {
		assertAttribute(widget, property, "value", value);
	}
	
	private void assertAttribute(OrderWidget widget, String property, String attributeName, String value) {
		assertThat(widget.getWidgetConfig().getAttributes(property).get(attributeName), is(value));
	}
	
	private void assertNoBadFormDesignException(FormSessionTester fst) {
		fst.assertHtmlDoesNotContain("org.openmrs.module.htmlformentry.BadFormDesignException");
	}
}
