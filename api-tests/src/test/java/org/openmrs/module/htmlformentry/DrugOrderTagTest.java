package org.openmrs.module.htmlformentry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.SimpleDosingInstructions;

public class DrugOrderTagTest extends BaseHtmlFormEntryTest {
	
	private static Log log = LogFactory.getLog(DrugOrderTagTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/drugOrderElement.xml");
	}
	
	@Test
	public void testNewDrugOrder_simpleDosingRoutineOutpatient() throws Exception {
		final DrugOrderRegressionTestHelper test = new DrugOrderRegressionTestHelper() {
			
			@Override
			public List<Map<String, String>> getDrugOrderTags() {
				return Arrays.asList(toMap("drug", "2"));
			}
			
			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.NEW.name());
				p.setDose("2");
				p.setDoseUnits("51");
				p.setRoute("22");
				p.setFrequency("1");
				p.setAsNeeded("true");
				p.setStartDate(dateAsString(getEncounterDate()));
				p.setDuration("10");
				p.setDurationUnits("28");
				p.setQuantity("20");
				p.setQuantityUnits("51");
				p.setInstructions("Take with water");
				p.setNumRefills("2");
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
	public void testNewDrugOrder_freeTextDosingScheduledInpatient() throws Exception {
		final DrugOrderRegressionTestHelper test = new DrugOrderRegressionTestHelper() {
			
			@Override
			public List<Map<String, String>> getDrugOrderTags() {
				return Arrays.asList(toMap("drug", "2", "dosingType", "freeText", "careSetting", "INPATIENT"));
			}
			
			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.NEW.name());
				p.setDosingInstructions("My dose instructions");
				p.setStartDate(dateAsString(daysAfterEncounterDate(7)));
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
	public void testEditDrugOrder_shouldVoidAndRecreateIfChangedInSameEncounter() throws Exception {
		final DrugOrderRegressionTestHelper test = new DrugOrderRegressionTestHelper() {

			@Override
			public List<Map<String, String>> getDrugOrderTags() {
				return Arrays.asList(toMap("drug", "2", "dosingType", "freeText", "careSetting", "INPATIENT"));
			}

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.NEW.name());
				p.setDosingInstructions("My dose instructions");
				p.setStartDate(dateAsString(getEncounterDate()));
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
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.EDIT.name());
				p.setDosingInstructions("My revised dose instructions");
				p.setStartDate(dateAsString(getEncounterDate()));
				return Arrays.asList(p);
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
				assertThat(originalOrder.getVoided(), is(true));
				assertThat(originalOrder.getVoidReason(), notNullValue());
				DrugOrder newOrder = (DrugOrder) orders.get(1);
				assertThat(newOrder.getAction(), is(Order.Action.NEW));
				assertThat(newOrder.getDateActivated(), is(is(ymdToDate(dateAsString(getEncounterDate())))));
				assertThat(newOrder.getDosingInstructions(), is("My revised dose instructions"));
				assertThat(newOrder.getEffectiveStopDate(), nullValue());
			}
		};

		test.run();
	}

	@Test
	public void testEditDrugOrder_shouldDiscontinueInSameEncounter() throws Exception {
		final DrugOrderRegressionTestHelper test = new DrugOrderRegressionTestHelper() {

			@Override
			public List<Map<String, String>> getDrugOrderTags() {
				return Arrays.asList(toMap("drug", "2", "dosingType", "freeText", "careSetting", "INPATIENT"));
			}

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.NEW.name());
				p.setDosingInstructions("My dose instructions");
				p.setStartDate(dateAsString(getEncounterDate()));
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
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.DISCONTINUE.name());
				p.setDiscontinuedReason("556");
				return Arrays.asList(p);
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
				assertThat(originalOrder.getEffectiveStopDate(), is(DateUtils.addSeconds(getEncounterDate(), -1)));
				DrugOrder newOrder = (DrugOrder) orders.get(1);
				assertThat(newOrder.getAction(), is(Order.Action.DISCONTINUE));
				assertThat(newOrder.getDateActivated(), is(is(ymdToDate(dateAsString(getEncounterDate())))));
				assertThat(newOrder.getOrderReason().getConceptId(), is(556));
				assertThat(newOrder.getPreviousOrder(), is(originalOrder));
			}
		};

		test.run();
	}

	@Test
	public void testDeleteDrugOrder_shouldVoid() throws Exception {
		final DrugOrderRegressionTestHelper test = new DrugOrderRegressionTestHelper() {

			@Override
			public List<Map<String, String>> getDrugOrderTags() {
				return Arrays.asList(toMap("drug", "2", "dosingType", "freeText", "careSetting", "INPATIENT"));
			}

			@Override
			public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.NEW.name());
				p.setDosingInstructions("My dose instructions");
				p.setStartDate(dateAsString(getEncounterDate()));
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
				DrugOrderRequestParams p = new DrugOrderRequestParams();
				p.setAction(Action.DELETE.name());
				return Arrays.asList(p);
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				List<Order> orders = new ArrayList<>(encounter.getOrders());
				assertThat(orders.size(), is(1));
				DrugOrder originalOrder = (DrugOrder) orders.get(0);
				assertThat(originalOrder.getAction(), is(Order.Action.NEW));
				assertThat(originalOrder.getVoided(), is(true));
				assertThat(originalOrder.getVoidReason(), notNullValue());
			}
		};

		test.run();
	}
}
