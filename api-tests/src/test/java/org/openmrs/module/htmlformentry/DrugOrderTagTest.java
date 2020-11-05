package org.openmrs.module.htmlformentry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.tester.DrugOrderFieldTester;
import org.openmrs.module.htmlformentry.tester.FormResultsTester;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class DrugOrderTagTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(DrugOrderTagTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void testDrugOrdersTag_htmlShouldRenderCorrectlyWithDefaultFormValues() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(2);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		formSessionTester.assertHtmlContains("drugorders-element");
		formSessionTester.assertHtmlContains("drugorders-order-section");
		formSessionTester.assertHtmlContains("drugorders-selector-section");
		formSessionTester.assertHtmlContains("drugorders-order-form");
		formSessionTester.assertStartingFormValue("order-field-label orderType", "1");
		log.trace(formSessionTester.getHtmlToDisplay());
	}
	
	@Test
	public void testDrugOrdersTag_shouldCreateNewInpatientAndOutpatientOrders() {
		for (String careSettingId : new String[] { "1", "2" }) {
			FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting(careSettingId).urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			triomuneField.quantity("10").quantityUnits("51").numRefills("1");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
			DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
			assertThat(order.getCareSetting().getId().toString(), is(careSettingId));
		}
	}
	
	@Test
	public void testDrugOrdersTag_shouldCreateNewFreeTextOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		results.assertFreeTextDosing(order, "Triomune instructions");
		results.assertSimpleDosingFieldsAreNull(order);
		results.assertDurationFieldsNull(order);
		results.assertDispensingFieldsNull(order);
	}
	
	@Test
	public void testDrugOrdersTag_shouldCreateNewSimpleOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.simpleDosing("2", "51", "1", "22");
		triomuneField.asNeeded("true").instructions("TBD");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		results.assertSimpleDosing(order, 2.0, 51, 1, 22, true, "TBD");
		results.assertFreeTextDosingFieldsNull(order);
		results.assertDurationFieldsNull(order);
		results.assertDispensingFieldsNull(order);
	}
	
	@Test
	public void testDrugOrdersTag_shouldCreateRoutineOrderWithDefaultDateActivated() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
	}
	
	@Test
	public void testDrugOrdersTag_shouldCreateRoutineOrderWithSpecificDateActivated() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.dateActivated("2020-04-15");
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-04-15 00:00:00");
	}
	
	@Test
	public void testDrugOrdersTag_shouldCreateScheduledOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2");
		triomuneField.urgency(Order.Urgency.ON_SCHEDULED_DATE.name()).scheduledDate("2020-05-02");
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
		TestUtil.assertDate(order.getScheduledDate(), "yyyy-MM-dd HH:mm:ss", "2020-05-02 00:00:00");
		assertThat(order.getUrgency(), is(Order.Urgency.ON_SCHEDULED_DATE));
	}
	
	@Test
	public void testDrugOrdersTag_reviseShouldVoidIfWithinSameEncounterForSameDate() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
		DrugOrder originalOrder = results.assertDrugOrder(Order.Action.NEW, 2);
		
		FormSessionTester reviseTester = formSessionTester.reopenForEditing(results);
		DrugOrderFieldTester revisedTriomuneField = DrugOrderFieldTester.forDrug(2, reviseTester);
		revisedTriomuneField.orderAction("REVISE").previousOrder(originalOrder.getId().toString());
		revisedTriomuneField.freeTextDosing("Revised Triomune instructions");
		FormResultsTester revisedResults = reviseTester.submitForm();
		
		// Since it was voided, then this should not be a revise with previous order, but void with new order
		revisedResults.assertNoErrors().assertOrderCreatedCount(2).assertNonVoidedOrderCount(1).assertVoidedOrderCount(1);
		DrugOrder revisedOrder = results.assertDrugOrder(Order.Action.NEW, 2);
		assertThat(revisedOrder.getPreviousOrder(), nullValue());
		assertThat(revisedOrder.getDosingInstructions(), is("Revised Triomune instructions"));
		
		Order originalOrderRetrieved = Context.getOrderService().getOrder(originalOrder.getId());
		assertThat(originalOrderRetrieved.getVoided(), is(true));
		assertThat(originalOrderRetrieved.getVoidReason(), is("Voided by htmlformentry"));
	}
	
	@Test
	public void testDrugOrdersTag_reviseShouldCloseAndLinkPrevious() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		
		Integer initialOrderId;
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o1 = results.assertDrugOrder(Order.Action.NEW, 2);
			TestUtil.assertDate(o1.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
			assertThat(o1.getDosingInstructions(), is("Triomune instructions"));
			assertThat(o1.getDateStopped(), nullValue());
			initialOrderId = o1.getOrderId();
		}
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-05-15", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.orderAction("REVISE").previousOrder(initialOrderId.toString());
			triomuneField.freeTextDosing("Revised Triomune instructions");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o2 = results.assertDrugOrder(Order.Action.REVISE, 2);
			TestUtil.assertDate(o2.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-05-15 00:00:00");
			assertThat(o2.getDateStopped(), nullValue());
			
			DrugOrder o1 = (DrugOrder) Context.getOrderService().getOrder(initialOrderId);
			assertThat(o2.getPreviousOrder(), is(o1));
			TestUtil.assertDate(o1.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-05-14 23:59:59");
			assertThat(o1.getDosingInstructions(), is("Triomune instructions"));
			assertThat(o2.getDosingInstructions(), is("Revised Triomune instructions"));
		}
	}
	
	@Test
	public void testDrugOrdersTag_renewShouldCloseAndLinkPrevious() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		
		Integer initialOrderId;
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting("1").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			triomuneField.quantity("30").quantityUnits("51").numRefills("2");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o1 = results.assertDrugOrder(Order.Action.NEW, 2);
			TestUtil.assertDate(o1.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
			results.assertDispensing(o1, 30.0, 51, 2);
			assertThat(o1.getDateStopped(), nullValue());
			initialOrderId = o1.getOrderId();
		}
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-05-15", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.careSetting("1").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.orderAction("RENEW").previousOrder(initialOrderId.toString());
			triomuneField.freeTextDosing("Triomune instructions");
			triomuneField.quantity("50").quantityUnits("51").numRefills("3");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o2 = results.assertDrugOrder(Order.Action.RENEW, 2);
			TestUtil.assertDate(o2.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-05-15 00:00:00");
			assertThat(o2.getDateStopped(), nullValue());
			
			DrugOrder o1 = (DrugOrder) Context.getOrderService().getOrder(initialOrderId);
			assertThat(o2.getPreviousOrder(), is(o1));
			TestUtil.assertDate(o1.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-05-14 23:59:59");
			assertThat(o2.getDosingInstructions(), is(o1.getDosingInstructions()));
			results.assertDispensing(o2, 50.0, 51, 3);
		}
	}
	
	@Test
	public void testDrugOrdersTag_discontinueShouldCloseAndLinkPrevious() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		
		Integer initialOrderId;
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o1 = results.assertDrugOrder(Order.Action.NEW, 2);
			TestUtil.assertDate(o1.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
			assertThat(o1.getDateStopped(), nullValue());
			initialOrderId = o1.getOrderId();
		}
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-05-15", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.orderAction("DISCONTINUE").previousOrder(initialOrderId.toString());
			triomuneField.discontinueReason("556");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o2 = results.assertDrugOrder(Order.Action.DISCONTINUE, 2);
			TestUtil.assertDate(o2.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-05-15 00:00:00");
			assertThat(o2.getDateStopped(), nullValue());
			
			DrugOrder o1 = (DrugOrder) Context.getOrderService().getOrder(initialOrderId);
			assertThat(o2.getPreviousOrder(), is(o1));
			TestUtil.assertDate(o1.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-05-14 23:59:59");
		}
	}
}
