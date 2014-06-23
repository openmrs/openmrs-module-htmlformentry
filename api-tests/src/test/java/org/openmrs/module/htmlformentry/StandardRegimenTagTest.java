package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.regimen.RegimenUtil;
import org.openmrs.order.DrugOrderSupport;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.mock.web.MockHttpServletRequest;


public class StandardRegimenTagTest extends BaseModuleContextSensitiveTest {
	
protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	protected static final String XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET = "RegimenUtilsTest.xml";
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	protected static final String XML_DRUG_ORDER_ELEMENT_DATASET = "drugOrderElementDataSet";

	@Before
    public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_DRUG_ORDER_ELEMENT_DATASET));
		
		String xml = (new TestUtil()).loadXmlFromFile(XML_DATASET_PATH + XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET);	
		GlobalProperty gp = new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_STANDARD_DRUG_REGIMENS);
		gp.setPropertyValue(xml);
		Context.getAdministrationService().saveGlobalProperty(gp);
    }
	
	@Test
	public void testStandardRegimenTag_shouldCreateStandardRegimen() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:"};
				
			}
			
			@Override
			public void testBlankFormHtml(String html){
				//System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the standardRegimen tag
				request.addParameter("w7", "all3");
				//start date
				request.addParameter("w9", dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Encounter e = results.getEncounterCreated();
				Assert.assertTrue(e != null);
				Assert.assertTrue(e.getOrders() != null);
				
				// build the list of drugs that should be in the order in the order
				Set<Integer> drugsInOrder = new HashSet<Integer>();
				drugsInOrder.add(2);
				drugsInOrder.add(3);
				drugsInOrder.add(11);
				
				Assert.assertTrue(e.getOrders().size() == 3);
				for (Order o : e.getOrders()){
					if (o instanceof DrugOrder == false) {
						Assert.assertTrue(false);
					}
					if (!drugsInOrder.remove(((DrugOrder) o).getDrug().getId())) {
						Assert.assertTrue(false);
					}
					Assert.assertTrue(dateAsString(o.getStartDate()).equals(dateAsString(date)));
				}
			}
		}.run();
	}
	
	@Test
	public void testStandardRegimenTag_shouldCreateEncounterWithNoRegimenCodeSelectedWithNoOrders() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:"};
				
			}
			
			@Override
			public void testBlankFormHtml(String html){
				//System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the standardRegimen tag
				//request.addParameter("w7", "all3");
				//start date
				request.addParameter("w9", dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Assert.assertTrue(results.getEncounterCreated().getOrders().size() == 0);
			}
		}.run();
	}
	
	@Test
	public void testStandardRegimenTag_shouldNotValidateWithoutStartDateIfRegimenCodeSelected() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:"};
				
			}
			
			@Override
			public void testBlankFormHtml(String html){
				//System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the standardRegimen tag
				request.addParameter("w7", "all3");
				//start date
				//request.addParameter("w9", dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
			}
		}.run();
	}
	
	
	@Test
	public void testStandardRegimenTag_shouldCreateStandardRegimenUsingAllAttributes() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:"};
				
			}
			
			@Override
			public void testBlankFormHtml(String html){
				//System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the standardRegimen tag
				request.addParameter("w7", "all3");
				//start date
				request.addParameter("w9", dateAsString(date));
				//discontinue date
				request.addParameter("w11", dateAsString(new Date(date.getTime() + 1)));
				//discontinue reason
				request.addParameter("w13", "102");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Encounter e = results.getEncounterCreated();
				Assert.assertTrue(e != null);
				Assert.assertTrue(e.getOrders() != null);
				Assert.assertTrue(e.getOrders().size() == 3);
				for (Order o : e.getOrders()){
					if (o instanceof DrugOrder == false)
						Assert.assertTrue(false);
					Assert.assertTrue(dateAsString(o.getStartDate()).equals(dateAsString(date)));
					Assert.assertTrue(o.isDiscontinued(new Date(date.getTime() + 5000)));
					Assert.assertTrue(o.getDiscontinuedReason().getConceptId().equals(102));
				}
			}
		}.run();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testStandardRegimenTag_shouldThrowIllegalArgException() throws Exception {
		new RegressionTestHelper() {		
			
			@Override
			public String getFormName() {
				//this form contains a standardRegimenCode not found in RegimenUtilsTest.xml helper file
				return "standardRegimenTestFormBad";
			}
			
		}.run();
	}
	
	
	@Test
	public void testStandardRegimenTag_shouldRecognizeStandardRegimenInEditMode_AllFields() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}
			
			@Override
			public Encounter getEncounterToView(){
				Encounter e = new Encounter();
				Patient p = Context.getPatientService().getPatient(2);
				e.setPatient(p);
				e.setEncounterDatetime(date);
				e.setProvider(Context.getPersonService().getPerson(502));
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setLocation(Context.getLocationService().getLocation(2));

				//add standard regimen to encounter:
				List<RegimenSuggestion> rs = DrugOrderSupport.getInstance().getStandardRegimens();
				RegimenSuggestion rsug = RegimenUtil.getStandardRegimenByCode(rs, "all3");
				Set<Order> dors = RegimenUtil.standardRegimenToDrugOrders(rsug, date, p);
				for (Order o : dors){
					o.setDiscontinuedDate(new Date(date.getTime() + 10));
					o.setDiscontinuedBy(Context.getAuthenticatedUser());
					o.setDiscontinuedReason(Context.getConceptService().getConcept(102));
					o.setDiscontinued(true);
					o.setDiscontinuedReasonNonCoded("non-coded reason");
					e.addOrder(o);
				}	
				//save so interceptor sets missing mandatory values
				return Context.getEncounterService().saveEncounter(e);
			}
			
			@Override
			public void testViewingEncounter(Encounter e, String html){
				//System.out.println(html);
				Assert.assertTrue(html.contains("<span class=\"value\">all 3 drugs</span>"));
				Assert.assertTrue(html.contains("toxicity"));
				Assert.assertTrue(html.contains("__") == false); //i.e., there are no blank fields.
			}
		}.run();
	}
	
	@Test
	public void testStandardRegimenTag_shouldRecognizeAllFields_StartDateAndRegimenOnly() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}
			
			@Override
			public Encounter getEncounterToView(){
				Encounter e = new Encounter();
				Patient p = Context.getPatientService().getPatient(2);
				e.setPatient(p);
				e.setEncounterDatetime(date);
				e.setProvider(Context.getPersonService().getPerson(502));
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setLocation(Context.getLocationService().getLocation(2));

				//add standard regimen to encounter:
				List<RegimenSuggestion> rs = Context.getOrderService().getStandardRegimens();
				RegimenSuggestion rsug = RegimenUtil.getStandardRegimenByCode(rs, "drug2and3");
				Set<Order> dors = RegimenUtil.standardRegimenToDrugOrders(rsug, date, p);
				for (Order o : dors){
					e.addOrder(o);
				}	
				//save so interceptor sets missing mandatory values
				return Context.getEncounterService().saveEncounter(e);
			}
			
			@Override
			public void testViewingEncounter(Encounter e, String html){
				//System.out.println(html);
				Assert.assertTrue(html.contains("<span class=\"value\">Drug 2 and 3</span>"));
				Assert.assertTrue(html.contains("toxicity") == false);
				Assert.assertTrue(html.contains("__") == true); //blank fields.
			}
		}.run();
	}
}
