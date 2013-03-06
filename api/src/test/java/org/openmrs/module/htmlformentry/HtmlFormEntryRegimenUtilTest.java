package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.regimen.RegimenUtil;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsConstants;



public class HtmlFormEntryRegimenUtilTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
    
	protected static final String XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET = "RegimenUtilsTest.xml";
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
    
	@Before
    public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		
		String xml = (new TestUtil()).loadXmlFromFile(XML_DATASET_PATH + XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET);	
		GlobalProperty gp = new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_STANDARD_DRUG_REGIMENS);
		gp.setPropertyValue(xml);
		Context.getAdministrationService().saveGlobalProperty(gp);
    }

	
	@Test
	@Verifies(value = "should create the correct number of DrugOrders, and they should save correctly", method = "standardRegimenToDrugOrders(RegimenSuggestion, Date, Patient)")
	public void standardRegimenToDrugOrders_shouldCreateDrugOrders() throws Exception {
		List<RegimenSuggestion> regList = Context.getOrderService().getStandardRegimens();
		Assert.assertTrue(regList.size() > 0);
		
		//Add regimens to an encounter:
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = new Date();
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		
		//And, add some drugOrders
		Patient p = Context.getPatientService().getPatient(2);
		Set<Order> dos = RegimenUtil.standardRegimenToDrugOrders(regList.get(0), new Date(), p);
		Assert.assertTrue(dos.size() == 2);
		for (Order o : dos)
			e.addOrder(o);
		
		//save the encounter
		e = Context.getEncounterService().saveEncounter(e);
		Integer encId = e.getId();
		Context.flushSession();
		Context.clearSession();
		
		//now retrieve it and make sure that there are two orders in the Encounter  (checks to ensure adequate values are set in creation of DrugOrder for not null constraints)
		Encounter enc = Context.getEncounterService().getEncounter(encId);
		Assert.assertTrue(enc.getOrders().size() == 2);
	}
	
	
	@Test
	@Verifies(value = "should match drugOrders to standard regimen correctly", method = "findStrongestStandardRegimenInDrugOrders(List<RegimenSuggestion>, List<Order>)")
	public void findStrongestStandardRegimenInDrugOrders_shouldMatchLargeRegimenCorrectly() throws Exception {
		
		//Build drug orders according to drug2and3
		Integer encId = regimenTestBuildEncounterHelper("drug2and3");
		Context.flushSession();
		Context.clearSession();
		
		//Check to see that method returns drug2and3
		Encounter e = Context.getEncounterService().getEncounter(encId);
		List<Order> dors = new ArrayList<Order>();
		dors.addAll(e.getOrders());
		Map<RegimenSuggestion, List<DrugOrder>>  m = RegimenUtil.findStrongestStandardRegimenInDrugOrders(Context.getOrderService().getStandardRegimens(),dors);
		Assert.assertTrue(m.size() > 0);
		RegimenSuggestion rs = m.keySet().iterator().next();
		log.debug("findStrongestStandardRegimenInDrugOrders found standard regimen " + rs.getCodeName());
		Assert.assertTrue(rs.getCodeName().equals("drug2and3"));
		
	}
	
	@Test
	@Verifies(value = "should match drugOrders to standard regimen correctly", method = "findStrongestStandardRegimenInDrugOrders(List<RegimenSuggestion>, List<Order>)")
	public void findStrongestStandardRegimenInDrugOrders_shouldMatchSmallRegimenCorrectly() throws Exception {
		//Build drug orders according to drug2only
		Integer encId = regimenTestBuildEncounterHelper("drug2only");
		Context.flushSession();
		Context.clearSession();
		
		//check to see that method returns drug2only
		Encounter e = Context.getEncounterService().getEncounter(encId);
		List<Order> dors = new ArrayList<Order>();
		dors.addAll(e.getOrders());
		Map<RegimenSuggestion, List<DrugOrder>>  m = RegimenUtil.findStrongestStandardRegimenInDrugOrders(Context.getOrderService().getStandardRegimens(),dors);
		Assert.assertTrue(m.size() > 0);
		RegimenSuggestion rs = m.keySet().iterator().next();
		log.debug("findStrongestStandardRegimenInDrugOrders found standard regimen " + rs.getCodeName());
		Assert.assertTrue(rs.getCodeName().equals("drug2only"));
	}
	
	@Test
	@Verifies(value = "should match drugOrders to standard regimen correctly", method = "findStrongestStandardRegimenInDrugOrders(List<RegimenSuggestion>, List<Order>)")
	public void findStrongestStandardRegimenInDrugOrders_shouldMatchLargestRegimenCorrectly() throws Exception {
		//Build drug orders according to drug2only
		Integer encId = regimenTestBuildEncounterHelper("all3");
		Context.flushSession();
		Context.clearSession();
		
		//check to see that method returns drug2only
		Encounter e = Context.getEncounterService().getEncounter(encId);
		List<Order> dors = new ArrayList<Order>();
		dors.addAll(e.getOrders());
		Map<RegimenSuggestion, List<DrugOrder>>  m = RegimenUtil.findStrongestStandardRegimenInDrugOrders(Context.getOrderService().getStandardRegimens(),dors);
		Assert.assertTrue(m.size() > 0);
		RegimenSuggestion rs = m.keySet().iterator().next();
		log.debug("findStrongestStandardRegimenInDrugOrders found standard regimen " + rs.getCodeName());
		Assert.assertTrue(rs.getCodeName().equals("all3"));
	}
	
	@Test
	@Verifies(value = "should not match multiple drug orders with different start dates", method = "findStrongestStandardRegimenInDrugOrders(List<RegimenSuggestion>, List<Order>)")
	public void findStrongestStandardRegimenInDrugOrders_shouldReturnSingle() throws Exception {
		//Build drug orders according to drug2only
		Integer encId = regimenTestBuildEncounterHelper("all3");
		Context.flushSession();
		Context.clearSession();
		
		//update orders to have different start dates
		Encounter e = Context.getEncounterService().getEncounter(encId);
		int aDay = 1000*60*60*24;
		int counter = 1;
		for (Order dor : e.getOrders()){
			dor.setStartDate(new Date((dor.getStartDate().getTime()) - (aDay * counter)));
			counter ++;
			log.debug("drugOrder now has start date " + dor.getStartDate());
		}
		Context.getEncounterService().saveEncounter(e);
		Context.flushSession();
		Context.clearSession();
		
		//check to see that method returns drug2only
		e = Context.getEncounterService().getEncounter(encId);
		List<Order> dors = new ArrayList<Order>();
		dors.addAll(e.getOrders());
		Map<RegimenSuggestion, List<DrugOrder>>  m = RegimenUtil.findStrongestStandardRegimenInDrugOrders(Context.getOrderService().getStandardRegimens(),dors);
		Assert.assertTrue(m.size() > 0);
		RegimenSuggestion rs = m.keySet().iterator().next();
		log.debug("findStrongestStandardRegimenInDrugOrders found standard regimen " + rs.getCodeName());
		Assert.assertTrue(rs.getCodeName().equals("drug2only"));
	}
	
	
	/**
	 * Builds an Encounter for Patient (id = 2), and adds DrugOrders to the Patient according to which StandardRegimen you want.
	 */
	private static Integer regimenTestBuildEncounterHelper(String code){
		List<RegimenSuggestion> regList = Context.getOrderService().getStandardRegimens();
		Assert.assertTrue(regList.size() > 0);
		
		//Add regimens to an encounter:
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = new Date();
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		
		//And, add some drugOrders
		Patient p = Context.getPatientService().getPatient(2);
		Set<Order> dos = RegimenUtil.standardRegimenToDrugOrders(RegimenUtil.getStandardRegimenByCode(regList, code), new Date(), p);
		for (Order o : dos)
			e.addOrder(o);
		
		//save the encounter
		e = Context.getEncounterService().saveEncounter(e);
		return e.getId();
	}
	
}
