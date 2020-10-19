package org.openmrs.module.htmlformentry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.SimpleDosingInstructions;

public class DrugOrdersTagTest extends BaseHtmlFormEntryTest {
	
	private static Log log = LogFactory.getLog(DrugOrdersTagTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/drugOrderElement.xml");
	}
	
	@Test
	public void testDrugOrdersTag_shouldLoadHtmlCorrectly() throws Exception {
		final DrugOrdersRegressionTestHelper helper = new DrugOrdersRegressionTestHelper() {
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(html);
			}
		};
		helper.run();
	}
	
	@Test
	public void testNewDrugOrder_simpleDosingRoutineOutpatient() throws Exception {
		final DrugOrdersRegressionTestHelper test = new DrugOrdersRegressionTestHelper() {
			
			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.NEW.name());
				p.setCareSetting("OUTPATIENT");
				p.setDosingType(SimpleDosingInstructions.class.getName());
				p.setDose("2");
				p.setDoseUnits("51");
				p.setRoute("22");
				p.setFrequency("1");
				p.setAsNeeded("true");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDateActivated(dateAsString(getEncounterDate()));
				p.setDuration("10");
				p.setDurationUnits("28");
				p.setQuantity("20");
				p.setQuantityUnits("51");
				p.setInstructions("Take with water");
				p.setNumRefills("2");
				p.setVoided("");
				p.setDiscontinueReason("");
				return Arrays.asList(p);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(1));
				DrugOrder order = (DrugOrder) orders.get(0);
				assertThat(order.getDateActivated(), is(getEncounterDate()));
				assertThat(order.getAction(), is(Order.Action.NEW));
				assertThat(order.getUrgency(), is(Order.Urgency.ROUTINE));
				assertThat(order.getScheduledDate(), nullValue());
				assertThat(order.getDosingType(), is(SimpleDosingInstructions.class));
				assertThat(order.getDosingInstructions(), nullValue());
				assertThat(order.getDose(), is(2.0));
				assertThat(order.getDoseUnits().getConceptId(), is(51));
				assertThat(order.getRoute().getConceptId(), is(22));
				assertThat(order.getFrequency().getOrderFrequencyId(), is(1));
				assertThat(order.getDuration(), is(10));
				assertThat(order.getDurationUnits().getConceptId(), is(28));
				assertThat(order.getQuantity(), is(20.0));
				assertThat(order.getQuantityUnits().getConceptId(), is(51));
				assertThat(order.getInstructions(), is("Take with water"));
				assertThat(order.getNumRefills(), is(2));
			}
		};
		
		test.run();
	}
	
	@Test
	public void testNewDrugOrder_freeTextDosingScheduledInpatientDefaultToEncounterDate() throws Exception {
		final DrugOrdersRegressionTestHelper test = new DrugOrdersRegressionTestHelper() {
			
			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.NEW.name());
				p.setCareSetting("INPATIENT");
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My dose instructions");
				p.setUrgency("ON_SCHEDULED_DATE");
				p.setScheduledDate(dateAsString(daysAfterEncounterDate(7)));
				return Arrays.asList(p);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(1));
				DrugOrder order = (DrugOrder) orders.get(0);
				assertThat(order.getDateActivated(), is(getEncounterDate()));
				assertThat(order.getAction(), is(Order.Action.NEW));
				assertThat(order.getUrgency(), is(Order.Urgency.ON_SCHEDULED_DATE));
				assertThat(order.getScheduledDate(), is(daysAfterEncounterDate(7)));
				assertThat(order.getDosingType(), is(FreeTextDosingInstructions.class));
				assertThat(order.getDosingInstructions(), is("My dose instructions"));
				assertThat(order.getDose(), nullValue());
				assertThat(order.getDoseUnits(), nullValue());
				assertThat(order.getRoute(), nullValue());
				assertThat(order.getFrequency(), nullValue());
				assertThat(order.getDuration(), nullValue());
				assertThat(order.getDurationUnits(), nullValue());
				assertThat(order.getQuantity(), nullValue());
				assertThat(order.getQuantityUnits(), nullValue());
				assertThat(order.getNumRefills(), nullValue());
			}
			
		};
		
		test.run();
	}
	
	@Test
	public void testEditDrugOrder_shouldReviseExistingOrderAndCreateNewOrder() throws Exception {
		final DrugOrdersRegressionTestHelper test = new DrugOrdersRegressionTestHelper() {
			
			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.NEW.name());
				p.setCareSetting("INPATIENT");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My dose instructions");
				return Arrays.asList(p);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(1));
				DrugOrder order = (DrugOrder) orders.get(0);
				assertThat(order.getAction(), is(Order.Action.NEW));
				assertThat(order.getDateActivated(), is(getEncounterDate()));
				assertThat(order.getDosingInstructions(), is("My dose instructions"));
				assertThat(order.getEffectiveStopDate(), nullValue());
			}
			
			@Override
			public List<DrugOrderRequestParams> getDrugOrderEditRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.REVISE.name());
				p.setCareSetting("INPATIENT");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My revised dose instructions");
				return Arrays.asList(p);
			}
			
			@Override
			public void testEditFormHtml(String html) {
				log.trace(html);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(2));
				DrugOrder originalOrder = (DrugOrder) orders.get(0);
				assertThat(originalOrder.getAction(), is(Order.Action.NEW));
				assertThat(originalOrder.getDateActivated(), is(getEncounterDate()));
				assertThat(originalOrder.getDosingInstructions(), is("My dose instructions"));
				assertThat(originalOrder.getDateStopped(), is(adjustMillis(getEncounterDate(), -1000)));
				DrugOrder newOrder = (DrugOrder) orders.get(1);
				assertThat(newOrder.getAction(), is(Order.Action.REVISE));
				assertThat(newOrder.getDateActivated(), is(getEncounterDate()));
				assertThat(newOrder.getDosingInstructions(), is("My revised dose instructions"));
				assertThat(newOrder.getEffectiveStopDate(), nullValue());
			}
		};
		
		test.run();
	}

	@Test
	public void testEditDrugOrder_voidPreviousShouldCreateNew() throws Exception {
		final DrugOrdersRegressionTestHelper test = new DrugOrdersRegressionTestHelper() {

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.NEW.name());
				p.setCareSetting("INPATIENT");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My dose instructions");
				return Arrays.asList(p);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(1));
				DrugOrder order = (DrugOrder) orders.get(0);
				assertThat(order.getAction(), is(Order.Action.NEW));
				assertThat(order.getDateActivated(), is(getEncounterDate()));
				assertThat(order.getDosingInstructions(), is("My dose instructions"));
				assertThat(order.getEffectiveStopDate(), nullValue());
			}

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEditRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.REVISE.name());
				p.setCareSetting("INPATIENT");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My revised dose instructions");
				p.setVoided("true");
				return Arrays.asList(p);
			}

			@Override
			public void testEditFormHtml(String html) {
				log.trace(html);
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(2));
				DrugOrder originalOrder = (DrugOrder) orders.get(0);
				assertThat(originalOrder.getVoided(), is(true));
				DrugOrder newOrder = (DrugOrder) orders.get(1);
				assertThat(newOrder.getAction(), is(Order.Action.NEW));
				assertThat(newOrder.getDateActivated(), is(getEncounterDate()));
				assertThat(newOrder.getDosingInstructions(), is("My revised dose instructions"));
				assertThat(newOrder.getEffectiveStopDate(), nullValue());
			}
		};

		test.run();
	}

	@Test
	public void testEditDrugOrder_shouldNotAllowRenewIfFreeTextDosingInstructionsChanged() throws Exception {
		final DrugOrdersRegressionTestHelper test = new DrugOrdersRegressionTestHelper() {

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.NEW.name());
				p.setCareSetting("INPATIENT");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My dose instructions");
				return Arrays.asList(p);
			}

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEditRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.RENEW.name());
				p.setCareSetting("INPATIENT");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDosingType(FreeTextDosingInstructions.class.getName());
				p.setDosingInstructions("My revised dose instructions");
				return Arrays.asList(p);
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertErrors(1);
			}
		};

		test.run();
	}

	@Test
	public void testEditDrugOrder_shouldRenewNoDosingInstructionsChanged() throws Exception {
		final DrugOrdersRegressionTestHelper test = new DrugOrdersRegressionTestHelper() {

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.NEW.name());
				p.setCareSetting("OUTPATIENT");
				p.setDosingType(SimpleDosingInstructions.class.getName());
				p.setDose("2");
				p.setDoseUnits("51");
				p.setRoute("22");
				p.setFrequency("1");
				p.setAsNeeded("true");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDateActivated(dateAsString(getEncounterDate()));
				p.setDuration("10");
				p.setDurationUnits("28");
				p.setQuantity("20");
				p.setQuantityUnits("51");
				p.setInstructions("Take with water");
				p.setNumRefills("2");
				return Arrays.asList(p);
			}

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEditRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams(0);
				p.setAction(Order.Action.RENEW.name());
				p.setCareSetting("OUTPATIENT");
				p.setDosingType(SimpleDosingInstructions.class.getName());
				p.setDose("2");
				p.setDoseUnits("51");
				p.setRoute("22");
				p.setFrequency("1");
				p.setAsNeeded("true");
				p.setUrgency(Order.Urgency.ROUTINE.name());
				p.setDateActivated(dateAsString(getEncounterDate()));
				p.setDuration("10");
				p.setDurationUnits("28");
				p.setQuantity("50");
				p.setQuantityUnits("51");
				p.setInstructions("Take with water");
				p.setNumRefills("3");
				return Arrays.asList(p);
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(2));
				DrugOrder order = (DrugOrder) orders.get(0);
				assertThat(order.getAction(), is(Order.Action.NEW));
				assertThat(order.getQuantity(), is(20.0));
				assertThat(order.getNumRefills(), is(2));
				DrugOrder renewOrder = (DrugOrder) orders.get(1);
				assertThat(renewOrder.getAction(), is(Order.Action.RENEW));
				assertThat(renewOrder.getQuantity(), is(50.0));
				assertThat(renewOrder.getNumRefills(), is(3));
			}
		};

		test.run();
	}
}
