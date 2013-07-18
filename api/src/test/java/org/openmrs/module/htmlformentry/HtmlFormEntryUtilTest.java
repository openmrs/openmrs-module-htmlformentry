package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.obs.ComplexData;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.springframework.mock.web.MockHttpSession;
import org.w3c.dom.Document;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/***
 * Test agaist standardTestData.xml from org.openmrs.include + Data from HtmlFormEntryTest-data.xml
 */
public class HtmlFormEntryUtilTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)} id test
	 */
	@Test
	@Verifies(value = "should find a location by its id", method = "getLocation(String)")
	public void getLocation_shouldFindALocationByItsId() throws Exception {
		Assert.assertEquals("Xanadu", HtmlFormEntryUtil.getLocation("2").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)} mapping test
	 */
	@Test
	@Verifies(value = "should find a location by its name", method = "getLocation(String)")
	public void getLocation_shouldFindALocationByItsName() throws Exception {
		Assert.assertEquals("2", HtmlFormEntryUtil.getLocation("Xanadu").getId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)} this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a location by its uuid", method = "getLocation(String)")
	public void getLocation_shouldFindALocationByItsUuid() throws Exception {
		Assert.assertEquals("Xanadu", HtmlFormEntryUtil.getLocation("9356400c-a5a2-4532-8f2b-2361b3446eb8").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)} this is the id|name test
	 */
	@Test
	@Verifies(value = "should find a location by in Id|Name format", method = "getLocation(String)")
	public void getLocation_shouldFindALocationInIdNameFormat() throws Exception {
		Assert.assertEquals("2", HtmlFormEntryUtil.getLocation("2 - Xanadu").getId().toString());
	}

    /**
     * @see {@link HtmlFormEntryUtil#getLocation(String, FormEntryContext)}
     */
    @Test
    @Verifies(value = "should find a location by session attribute", method = "getLocation(String,FormEntrySession)")
    public void getLocation_shouldFindALocationBySessionAttribute() throws Exception {
        String attrName = "emr.sessionLocation";
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(attrName, "2");

        FormEntryContext formEntryContext = new FormEntryContext(FormEntryContext.Mode.ENTER);
        formEntryContext.setHttpSession(httpSession);

        Assert.assertEquals("2", HtmlFormEntryUtil.getLocation("SessionAttribute:" + attrName, formEntryContext).getId().toString());
    }

    /**
     * @see {@link HtmlFormEntryUtil#getLocation(String, FormEntryContext)}
     * @verifies not fail if trying to find a location by session attribute and we have no session
     */
    @Test
    public void getLocation_shouldNotFailIfTryingToFindALocationBySessionAttriubteAndWeHaveNoSession() {
        FormEntryContext formEntryContext = new FormEntryContext(FormEntryContext.Mode.ENTER);
        Assert.assertNull(HtmlFormEntryUtil.getLocation("SessionAttribute:someSessionAttribute", formEntryContext));
    }

    /**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)}
	 */
	@Test
	@Verifies(value = "should return null otherwise", method = "getLocation(String)")
	public void getLocation_shouldReturnNullOtherwise() throws Exception {
		String id = null;
		Assert.assertNull(HtmlFormEntryUtil.getLocation(id));
		
		id = "";
		Assert.assertNull(HtmlFormEntryUtil.getLocation(id));
		
		id = "100000";//not exist in the standardTestData
		Assert.assertNull(HtmlFormEntryUtil.getLocation(id));
		
		id = "ASDFASDFEAF";//random string
		Assert.assertNull(HtmlFormEntryUtil.getLocation(id));
		
		id = "-";//uuid style
		Assert.assertNull(HtmlFormEntryUtil.getLocation(id));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)} id test
	 */
	@Test
	@Verifies(value = "should find a program by its Id", method = "getProgram(String)")
	public void getProgram_shouldFindAProgramByItsId() throws Exception {
		Assert.assertEquals("MDR program", HtmlFormEntryUtil.getProgram("2").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)} this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a program by its uuid", method = "getProgram(String)")
	public void getProgram_shouldFindAProgramByItsUuid() throws Exception {
		Assert.assertEquals("MDR program", HtmlFormEntryUtil.getProgram("71779c39-d289-4dfe-91b5-e7cfaa27c78b").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)} this is the name test
	 */
	@Test
	@Verifies(value = "should find a program by its name", method = "getProgram(String)")
	public void getProgram_shouldFindAProgramByItsName() throws Exception {
		Assert.assertEquals("71779c39-d289-4dfe-91b5-e7cfaa27c78b", HtmlFormEntryUtil.getProgram("MDR-TB PROGRAM").getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)}
	 */
	@Test
	@Verifies(value = "should return null otherwise", method = "getProgram(String)")
	public void getProgram_shouldReturnNullOtherwise() throws Exception {
		String id = null;
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "";
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "100000";//not exist in the standardTestData
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "ASDFASDFEAF";//random string
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "-";//uuid style
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPerson(String)} id test
	 */
	@Test
	@Verifies(value = "should find a person by Id", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonById() throws Exception {
		Assert.assertEquals("Hornblower", HtmlFormEntryUtil.getPerson("2").getFamilyName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPerson(String)} this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a person by uuid", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonByUuid() throws Exception {
		Assert.assertEquals("Hornblower", HtmlFormEntryUtil.getPerson("da7f524f-27ce-4bb2-86d6-6d1d05312bd5")
		        .getFamilyName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPerson(String)} this is the username test
	 */
	@Test
	@Verifies(value = "should find a person by username", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonByUsername() throws Exception {
		Assert.assertEquals("502", HtmlFormEntryUtil.getPerson("butch").getId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPerson(String)} this is the username test
	 */
	@Test
	@Verifies(value = "should find a person in id|name format", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonInIdNameFormat() throws Exception {
		Assert.assertEquals("Hornblower", HtmlFormEntryUtil.getPerson("2 - Horatio Hornblower").getFamilyName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)}
	 */
	@Test
	@Verifies(value = "should return null otherwise", method = "getProgram(String)")
	public void getPerson_shouldReturnNullOtherwise() throws Exception {
		String id = null;
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "";
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "100000";//not exist in the standardTestData
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "ASDFASDFEAF";//random string
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
		
		id = "-";//uuid style
		Assert.assertNull(HtmlFormEntryUtil.getProgram(id));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)} id test
	 */
	@Test
	@Verifies(value = "should find a concept by its conceptId", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptByItsConceptId() throws Exception {
		String id = "3";
		Assert.assertEquals("3", HtmlFormEntryUtil.getConcept(id).getConceptId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)} mapping test
	 */
	@Test
	@Verifies(value = "should find a concept by its mapping", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptByItsMapping() throws Exception {
		String id = "XYZ:HT";
		Concept cpt = HtmlFormEntryUtil.getConcept(id);
		Assert.assertEquals("XYZ", cpt.getConceptMappings().iterator().next().getSource().getName());
		Assert.assertEquals("HT", cpt.getConceptMappings().iterator().next().getSourceCode());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)} this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a concept by its uuid", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptByItsUuid() throws Exception {
		//the uuid from standardTestDataset
		String id = "0cbe2ed3-cd5f-4f46-9459-26127c9265ab";
		Assert.assertEquals(id, HtmlFormEntryUtil.getConcept(id).getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)} tests a uuid that is 36 characters long but
	 *      has no dashes
	 */
	@Test
	@Verifies(value = "should find a concept by its uuid", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptWithNonStandardUuid() throws Exception {
		// concept from HtmlFormEntryTest-data.xml
		String id = "1000AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		Assert.assertEquals(id, HtmlFormEntryUtil.getConcept(id).getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)} tests a uuid that is in invalid format
	 *      (less than 36 characters)
	 */
	@Test
	@Verifies(value = "should not find a concept with invalid uuid", method = "getConcept(String)")
	public void getConcept_shouldNotFindAConceptWithInvalidUuid() throws Exception {
		// concept from HtmlFormEntryTest-data.xml
		String id = "1000";
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)}
	 */
	@Test
	@Verifies(value = "should return null otherwise", method = "getConcept(String)")
	public void getConcept_shouldReturnNullOtherwise() throws Exception {
		String id = null;
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
		
		id = "";
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
		
		id = "100000";//not exist in the standardTestData
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
		
		id = "ASDFASDFEAF";//random string
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
		
		id = ":"; //mapping style
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
		
		id = "-";//uuid style
		Assert.assertNull(HtmlFormEntryUtil.getConcept(id));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)}
	 */
	@Test
	@Verifies(value = "should find a concept by its mapping with a space in between", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptByItsMappingWithASpaceInBetween() throws Exception {
		String id = "XYZ: HT";
		Concept cpt = HtmlFormEntryUtil.getConcept(id);
		Assert.assertEquals("XYZ", cpt.getConceptMappings().iterator().next().getSource().getName());
		Assert.assertEquals("HT", cpt.getConceptMappings().iterator().next().getSourceCode());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPatientIdentifierType(String)} id test
	 */
	@Test
	@Verifies(value = "should find a patient identifier type by its Id", method = "getPatientIdentifierType(String)")
	public void getPatientIdentifierType_shouldFindAPatientIdentifierTypeByItsId() throws Exception {
		Assert.assertEquals("OpenMRS Identification Number", HtmlFormEntryUtil.getPatientIdentifierType("1").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPatientIdentifierType(String)} this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a patient identifier type by its uuid", method = "getPatientIdentifierType(String)")
	public void getPatientIdentifierType_shouldFindAPatientIdentifierTypeByItsUuid() throws Exception {
		Assert.assertEquals("OpenMRS Identification Number",
		    HtmlFormEntryUtil.getPatientIdentifierType("1a339fe9-38bc-4ab3-b180-320988c0b968").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPatientIdentifierType(String)} this is the name test
	 */
	@Test
	@Verifies(value = "should find a program by its name", method = "getPatientIdentifierType(String)")
	public void getPatientIdentifierType_shouldFindAPatientIdentifierTypeByItsName() throws Exception {
		Assert.assertEquals("1a339fe9-38bc-4ab3-b180-320988c0b968",
		    HtmlFormEntryUtil.getPatientIdentifierType("OpenMRS Identification Number").getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)}
	 */
	@Test
	@Verifies(value = "should return null otherwise", method = "getProgram(String)")
	public void getPatientIdentifierType_shouldReturnNullOtherwise() throws Exception {
		String id = null;
		Assert.assertNull(HtmlFormEntryUtil.getPatientIdentifierType(id));
		
		id = "";
		Assert.assertNull(HtmlFormEntryUtil.getPatientIdentifierType(id));
		
		id = "100000";//not exist in the standardTestData
		Assert.assertNull(HtmlFormEntryUtil.getPatientIdentifierType(id));
		
		id = "ASDFASDFEAF";//random string
		Assert.assertNull(HtmlFormEntryUtil.getPatientIdentifierType(id));
		
		id = "-";//uuid style
		Assert.assertNull(HtmlFormEntryUtil.getPatientIdentifierType(id));
	}
	
	@Test
	@Verifies(value = "shoud return true valid uuid format", method = "isValidUuidFormat(String)")
	public void isValidUuidFormat_shouldReturnTrueIfNotValidUuidFormat() throws Exception {
		Assert.assertTrue(HtmlFormEntryUtil.isValidUuidFormat("1000AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")); // 36 characters
		Assert.assertTrue(HtmlFormEntryUtil.isValidUuidFormat("1000AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")); // 38 characters
	}
	
	@Test
	@Verifies(value = "shoud return false if not valid uuid format", method = "isValidUuidFormat(String)")
	public void isValidUuidFormat_shouldReturnFalseIfNotValidUuidFormat() throws Exception {
		Assert.assertFalse(HtmlFormEntryUtil.isValidUuidFormat("afasdfasd")); // less than 36 characters
		Assert.assertFalse(HtmlFormEntryUtil.isValidUuidFormat("012345678901234567890123456789012345678")); // more than 38 characters
		Assert.assertFalse(HtmlFormEntryUtil.isValidUuidFormat("1000AAAAAA AAAAAAAAA AAAAAAAAAA AAAA")); // includes whitespace
	}
	
	@Test
	@Verifies(value = "should return encounter with all child objects voided according to schema", method = "voidEncounterByHtmlFormSchema")
	public void testVoidEncounterByHtmlFormSchema_shouldReturnEncounterVoided() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = Context.getDateFormat().parse("01/02/2003");
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		
		//add a bunch of obs...
		TestUtil.addObs(e, 2474, Context.getConceptService().getConcept(656), date); //matches
		
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH
		        + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
		HtmlFormEntryUtil.voidEncounterByHtmlFormSchema(e, htmlform, null);
		
		//this is going to test out the voided state of the obs in the encounter after processing:
		Assert.assertTrue(e.isVoided());
		
	}
	
	@Test
	@Verifies(value = "should return encounter with all child objects voided according to schema", method = "voidEncounterByHtmlFormSchema")
	public void testVoidEncounterByHtmlFormSchema_shouldReturnEncounterCorrectly() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = Context.getDateFormat().parse("01/02/2003");
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		
		//add a bunch of obs...
		TestUtil.addObs(e, 2474, Context.getConceptService().getConcept(656), date); //matches
		TestUtil.addObs(e, 3017, Context.getConceptService().getConcept(767), date); //matches
		TestUtil.addObs(e, 3032, new Date(), date); //matches
		TestUtil.addObs(e, 1, 5000, date); //   matches
		TestUtil.addObs(e, 2, 5000, date); //not in form schema
		TestUtil.addObs(e, 3, 5000, date); //not in form schema
		TestUtil.addObs(e, 6, "blah blah", date); //   matches
		//1004 is ANOTHER ALLERGY CONSTRUCT, 1005 is HYPER-ALLERGY CODED, 1001 is PENICILLIN
		TestUtil.addObsGroup(e, 1004, new Date(), 1005, Context.getConceptService().getConcept(1001), new Date()); //matches
		//7 IS ALLERGY CONSTRUCT, 1000 IS ALLERGY CODED, 1003 IS OPENMRS
		TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date()); //matches
		TestUtil.addObsGroup(e, 1000, new Date(), 7, Context.getConceptService().getConcept(1003), new Date()); //does not match	    
		Context.getEncounterService().saveEncounter(e);
		
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH
		        + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
		HtmlFormEntryUtil.voidEncounterByHtmlFormSchema(e, htmlform, null);
		
		//this is going to test out the voided state of the obs in the encounter after processing:
		Assert.assertTrue(!e.isVoided());
		for (Obs o : e.getAllObs(true)) {
			if (o.getConcept().getConceptId().equals(2474))
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(3017))
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(3032))
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(1))
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(2))
				Assert.assertTrue(!o.isVoided()); //not matched
			if (o.getConcept().getConceptId().equals(3))
				Assert.assertTrue(!o.isVoided());//not matched
			if (o.getConcept().getConceptId().equals(6))
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(1004))
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(1005))
				Assert.assertTrue(o.isVoided());
			
			//obsGroups
			if (o.getConcept().getConceptId().equals(7) && o.isObsGrouping())
				Assert.assertTrue(o.isVoided());
			if (o.getConcept().getConceptId().equals(7) && !o.isObsGrouping())
				Assert.assertTrue(!o.isVoided());//not matched
			if (o.getConcept().getConceptId().equals(1000) && o.isObsGrouping())
				Assert.assertTrue(!o.isVoided());//not matched
			if (o.getConcept().getConceptId().equals(1000) && !o.isObsGrouping())
				Assert.assertTrue(o.isVoided());
		}
	}
	
	@Test
	@Verifies(value = "should return encounter with all child objects voided according to schema", method = "voidEncounterByHtmlFormSchema")
	public void testVoidEncounterByHtmlFormSchema_shouldHandleDrugOrderCorrectly() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = Context.getDateFormat().parse("01/02/2003");
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		TestUtil.addObs(e, 1, 5000, date); //a matching obs
		
		DrugOrder dor = new DrugOrder();
		dor.setVoided(false);
		dor.setConcept(Context.getConceptService().getConcept(792));
		dor.setCreator(Context.getUserService().getUser(1));
		dor.setDateCreated(new Date());
		dor.setDiscontinued(false);
		dor.setDrug(Context.getConceptService().getDrug(2));
		dor.setOrderType(Context.getOrderService().getOrderType(1));
		dor.setPatient(Context.getPatientService().getPatient(2));
		dor.setStartDate(new Date());
		e.addOrder(dor);
		
		Context.getEncounterService().saveEncounter(e);
		
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH
		        + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
		
		HtmlFormEntryUtil.voidEncounterByHtmlFormSchema(e, htmlform, "test void reason");
		
		//this is going to test out the voided state of the obs in the encounter after processing:
		//order was matched, so order was voided, and because that's the only thing in the encounter, encounter was voided too.
		Assert.assertTrue(e.isVoided());
		Assert.assertTrue(e.getVoidReason().equals("test void reason"));
		for (Order o : e.getOrders()) {
			Assert.assertTrue(o.isVoided());
			Assert.assertTrue(o.getVoidReason().equals("test void reason"));
		}
		for (Obs o : e.getAllObs(true)) {
			Assert.assertTrue(o.getVoidReason().equals("test void reason"));
		}
	}
	
	@Test
	@Verifies(value = "should return encounter with all child objects voided according to schema", method = "voidEncounterByHtmlFormSchema")
	public void testVoidEncounterByHtmlFormSchema_shouldHandleDrugOrderAndObsCorrectly() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = Context.getDateFormat().parse("01/02/2003");
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		TestUtil.addObs(e, 3, 5000, date);//adding an un-matched Obs
		
		DrugOrder dor = new DrugOrder();
		dor.setVoided(false);
		dor.setConcept(Context.getConceptService().getConcept(792));
		dor.setCreator(Context.getUserService().getUser(1));
		dor.setDateCreated(new Date());
		dor.setDiscontinued(false);
		dor.setDrug(Context.getConceptService().getDrug(2));
		dor.setOrderType(Context.getOrderService().getOrderType(1));
		dor.setPatient(Context.getPatientService().getPatient(2));
		dor.setStartDate(new Date());
		e.addOrder(dor);
		
		Context.getEncounterService().saveEncounter(e);
		
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH
		        + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
		
		HtmlFormEntryUtil.voidEncounterByHtmlFormSchema(e, htmlform, null);
		
		//order was matched, obs was not, so order should be voided, obs not, encounter not.
		Assert.assertTrue(!e.isVoided());
		for (Order o : e.getOrders()) {
			Assert.assertTrue(o.isVoided());
		}
		for (Obs o : e.getObs()) {
			Assert.assertTrue(!o.isVoided());
		}
	}
	
	@Test
	@Verifies(value = "should delete encounter correctly", method = "voidEncounterByHtmlFormSchema")
	public void testVoidEncounterByHtmlFormSchema_shouldDeleteEncounter() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Encounter e = new Encounter();
		e.setPatient(Context.getPatientService().getPatient(2));
		Date date = Context.getDateFormat().parse("01/02/2003");
		e.setDateCreated(new Date());
		e.setEncounterDatetime(date);
		e.setLocation(Context.getLocationService().getLocation(2));
		e.setProvider(Context.getPersonService().getPerson(502));
		TestUtil.addObs(e, 3, 5000, date);//adding an un-matched, voided Obs
		for (Obs o : e.getAllObs(true)) {
			o.setVoided(true);
			o.setVoidedBy(Context.getUserService().getUser(1));
			o.setVoidReason("blah");
			o.setDateVoided(new Date());
		}
		
		//and adding a voided drug order
		DrugOrder dor = new DrugOrder();
		dor.setVoided(false);
		dor.setConcept(Context.getConceptService().getConcept(792));
		dor.setCreator(Context.getUserService().getUser(1));
		dor.setDateCreated(new Date());
		dor.setDiscontinued(false);
		dor.setDrug(Context.getConceptService().getDrug(2));
		dor.setOrderType(Context.getOrderService().getOrderType(1));
		dor.setPatient(Context.getPatientService().getPatient(2));
		dor.setVoided(true);
		dor.setVoidedBy(Context.getUserService().getUser(1));
		dor.setVoidReason("blah");
		dor.setDateVoided(new Date());
		dor.setStartDate(new Date());
		e.addOrder(dor);
		
		Context.getEncounterService().saveEncounter(e);
		
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH
		        + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
		
		HtmlFormEntryUtil.voidEncounterByHtmlFormSchema(e, htmlform, null);
		
		//encounter had no non-voided objects, should be voided
		Assert.assertTrue(e.isVoided());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 */
	@Test
	@Verifies(value = "should return a Date with current date, but time of 00:00:00:00, for 'today'", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldReturnDateForToday() throws Exception {
		
		Date testDate = HtmlFormEntryUtil.translateDatetimeParam("today", null);
		Assert.assertTrue(HtmlFormEntryUtil.translateDatetimeParam("today", null) instanceof java.util.Date);
		
		java.util.Calendar referenceCalendar = Calendar.getInstance();
		referenceCalendar.setTime(new java.util.Date());
		java.util.Calendar testCal = Calendar.getInstance();
		testCal.setTime(testDate);
		// date matches today?
		Assert.assertEquals(referenceCalendar.get(java.util.Calendar.YEAR), testCal.get(java.util.Calendar.YEAR));
		Assert.assertEquals(referenceCalendar.get(java.util.Calendar.DAY_OF_YEAR),
		    testCal.get(java.util.Calendar.DAY_OF_YEAR));
		
		// check the time fields are zeroed out
		Assert.assertEquals(0, testCal.get(java.util.Calendar.HOUR));
		Assert.assertEquals(0, testCal.get(java.util.Calendar.MINUTE));
		Assert.assertEquals(0, testCal.get(java.util.Calendar.SECOND));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 * @see wiki.openmrs.org/display/docs/HTML+Form+Entry+Module+HTML+Reference for the date format
	 *      the Obs defaultDatetime and defaultObsDatetime attributes support
	 */
	@Test
	@Verifies(value = "should return a Date object matching the value param if a format is specified", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldParseDate() throws Exception {
		String datetimeFormat = "yyyy-MM-dd-HH-mm"; // this is the date format
		                                            // the Obs defaultDatetime
		                                            // attribute uses
		String dateString = "1990-01-02-13-59";
		Date refDate = (new java.text.SimpleDateFormat(datetimeFormat)).parse(dateString);
		Assert.assertEquals(refDate, HtmlFormEntryUtil.translateDatetimeParam(dateString, datetimeFormat));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 */
	@Test
	@Verifies(value = "should return null for null value", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldReturnNullForNullValue() throws Exception {
		Assert.assertNull(HtmlFormEntryUtil.translateDatetimeParam(null, "yyyy-MM-dd-HH-mm"));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 */
	@Test
	@Verifies(value = "should  return a Date object with current date and time for 'now'", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldReturnDateForNow() throws Exception {
		Date referenceDate = new Date();
		Date testDate = HtmlFormEntryUtil.translateDatetimeParam("now", null);
		
		Assert.assertNotNull(testDate);
		Assert.assertTrue(HtmlFormEntryUtil.translateDatetimeParam("now", null) instanceof java.util.Date);
		// some millis elapsed between Date() calls - allow it a 1000 ms buffer
		Assert.assertEquals(referenceDate.getTime() / 1000, testDate.getTime() / 1000);
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 */
	@Test
	@Verifies(value = "should return null if format is null and value not in [ null, 'now', 'today' ]", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldReturnNullForNullFormat() throws Exception {
		Assert.assertNull(HtmlFormEntryUtil.translateDatetimeParam("1990-01-02-13-59", null));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 */
	@Test(expected = java.lang.IllegalArgumentException.class)
	@Verifies(value = "should return null if format is null and value not in [ null, 'now', 'today' ]", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldReturnNullForInvalidDate() throws Exception {
		// java.text.SimpleDateFormat parses invalid numerical dates without
		// error, e.g. 9999-99-99-99-99-99 is parsed to Mon Jun 11 04:40:39 EDT
		// 10007
		// Text strings are unparseable, however.
		HtmlFormEntryUtil.translateDatetimeParam("c'est ne pas une date", "yyyy-MM-dd-HH-mm");
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#translateDatetimeParam(String,String)}
	 */
	@Test(expected = java.lang.IllegalArgumentException.class)
	@Verifies(value = "should fail if date parsing fails", method = "translateDatetimeParam(String,String)")
	public void translateDatetimeParam_shouldFailForBadDateFormat() throws Exception {
		HtmlFormEntryUtil.translateDatetimeParam("1990-01-02-13-59", "a bogus date format that will throw an error");
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@Test
	@Verifies(value = "should return the workflow with the matching id", method = "getWorkflow(String)")
	public void getWorkflow_shouldReturnTheWorkflowWithTheMatchingId() throws Exception {
		Assert.assertEquals("84f0effa-dd73-46cb-b931-7cd6be6c5f81", HtmlFormEntryUtil.getWorkflow("1").getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@Test
	@Verifies(value = "should return the workflow with the matching uuid", method = "getWorkflow(String)")
	public void getWorkflow_shouldReturnTheWorkflowWithTheMatchingUuid() throws Exception {
		Assert.assertEquals("1", HtmlFormEntryUtil.getWorkflow("84f0effa-dd73-46cb-b931-7cd6be6c5f81").getId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@Test
	@Verifies(value = "should look up a workflow by a concept mapping", method = "getWorkflow(String)")
	public void getWorkflow_shouldLookUpAWorkflowByAConceptMapping() throws Exception {
		// load this data set so that we get the additional patient program with concept mapping
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		
		Assert.assertEquals("7c3e071a-53a7-11e1-8cb6-00248140a5eb", HtmlFormEntryUtil.getWorkflow("SNOMED CT: Test Workflow Code").getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@Test
	@Verifies(value = "should return the state with the matching id", method = "getState(String,Program)")
	public void getStateProgram_shouldReturnTheStateWithTheMatchingId() throws Exception {
		Assert.assertEquals("92584cdc-6a20-4c84-a659-e035e45d36b0",
		    HtmlFormEntryUtil.getState("1", Context.getProgramWorkflowService().getProgram(1)).getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@Test
	@Verifies(value = "should return the state with the matching uuid", method = "getState(String,Program)")
	public void getStateProgram_shouldReturnTheStateWithTheMatchingUuid() throws Exception {
		Assert.assertEquals("1",HtmlFormEntryUtil.getState("92584cdc-6a20-4c84-a659-e035e45d36b0", Context.getProgramWorkflowService().getProgram(1)).getId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@Test
	@Verifies(value = "should look up a state by a concept mapping", method = "getState(String,Program)")
	public void getStateProgram_shouldLookUpAStateByAConceptMapping() throws Exception {
		// load this data set so that we get the additional patient program with concept mapping
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Assert.assertEquals("6de7ed10-53ad-11e1-8cb6-00248140a5eb",HtmlFormEntryUtil.getState("SNOMED CT: Test Code", Context.getProgramWorkflowService().getProgram(10)).getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@SuppressWarnings("deprecation")
    @Test
	@Verifies(value = "should return the state with the matching id", method = "getState(String,ProgramWorkflow)")
	public void getStateWorkflow_shouldReturnTheStateWithTheMatchingId() throws Exception {
		Assert.assertEquals("92584cdc-6a20-4c84-a659-e035e45d36b0",
		    HtmlFormEntryUtil.getState("1", Context.getProgramWorkflowService().getWorkflow(1)).getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@SuppressWarnings("deprecation")
    @Test
	@Verifies(value = "should return the state with the matching uuid", method = "getState(String,ProgramWorkflow)")
	public void getStateWorkflow_shouldReturnTheStateWithTheMatchingUuid() throws Exception {
		Assert.assertEquals("1",HtmlFormEntryUtil.getState("92584cdc-6a20-4c84-a659-e035e45d36b0", Context.getProgramWorkflowService().getWorkflow(1)).getId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getState(String,Program)}
	 */
	@SuppressWarnings("deprecation")
    @Test
	@Verifies(value = "should look up a state by a concept mapping", method = "getState(String,ProgramWorkflow)")
	public void getStateWorkflow_shouldLookUpAStateByAConceptMapping() throws Exception {
		// load this data set so that we get the additional patient program with concept mapping
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		Assert.assertEquals("6de7ed10-53ad-11e1-8cb6-00248140a5eb",HtmlFormEntryUtil.getState("SNOMED CT: Test Code", Context.getProgramWorkflowService().getWorkflow(108)).getUuid());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#isEnrolledInProgram(Patient,Program,Date)}
	 */
	@Test
	@Verifies(value = "should return false if the patient is not enrolled in the program", method = "isEnrolledInProgram(Patient,Program,Date)")
	public void isEnrolledInProgram_shouldReturnFalseIfThePatientIsNotEnrolledInTheProgram() throws Exception {
		Patient patient = Context.getPatientService().getPatient(6);
		Program program = Context.getProgramWorkflowService().getProgram(1);
		Assert.assertFalse(HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, new Date()));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#isEnrolledInProgram(Patient,Program,Date)}
	 */
	@Test
	@Verifies(value = "should return false if the program was completed", method = "isEnrolledInProgram(Patient,Program,Date)")
	public void isEnrolledInProgram_shouldReturnFalseIfTheProgramWasCompleted() throws Exception {
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Patient patient = Context.getPatientService().getPatient(2);
		
		//for test purposes, lets set a program as complete
		PatientProgram pp = pws.getPatientProgram(1);
		Assert.assertSame(patient, pp.getPatient());
		pp.setDateCompleted(new Date());
		Thread.sleep(100);
		pws.savePatientProgram(pp);
		
		Assert.assertFalse(HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, pws.getProgram(1), new Date()));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#isEnrolledInProgram(Patient,Program,Date)}
	 */
	@Test
	@Verifies(value = "should return true if the patient is enrolled in the program at the specified date", method = "isEnrolledInProgram(Patient,Program,Date)")
	public void isEnrolledInProgram_shouldReturnTrueIfThePatientIsEnrolledInTheProgramAtTheSpecifiedDate() throws Exception {
		Patient patient = Context.getPatientService().getPatient(2);
		Program program = Context.getProgramWorkflowService().getProgram(1);
		Assert.assertTrue(HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, new Date()));
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#isEnrolledInProgram(Patient,Program,Date)}
	 */
	@Test
	@Verifies(value = "should return false if the date is before the existing patient program enrollment date", method = "isEnrolledInProgram(Patient,Program,Date)")
	public void isEnrolledInProgram_shouldReturnFalseIfTheDateIsBeforeTheExistingPatientProgramEnrollmentDateIgnoringTimeFields()
	    throws Exception {//2008-08-01 00:00:00.0
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Patient patient = Context.getPatientService().getPatient(2);
		Program program = pws.getProgram(1);
		PatientProgram pp = pws.getPatientProgram(1);
		
		Calendar cal = Calendar.getInstance();
		cal.set(2008, 6, 31);
		Date newEnrollmentDate = cal.getTime();
		Assert.assertTrue(newEnrollmentDate.before(pp.getDateEnrolled()));//sanity check
		Assert.assertFalse(HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, newEnrollmentDate));
	}
	
	@Test
	@Verifies(value = "should return null if no program enrollment after specified date", method = "getClosestFutureProgramEnrollment(Patient,Program,Date)") 
	public void getClosestFutureProgramEnrollment_shouldReturnNullIfNoProgramEnrollmentAfterSpecifiedDate() throws Exception {
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Patient patient = Context.getPatientService().getPatient(2);
		Program program = pws.getProgram(1);
		Assert.assertNull(HtmlFormEntryUtil.getClosestFutureProgramEnrollment(patient, program, new Date()));
	}
	
	@Test
	@Verifies(value = "should return program enrollment after specified date", method = "getClosestFutureProgramEnrollment(Patient,Program,Date)") 
	public void shouldReturnPatientProgramWithEnrollmentAfterSpecifiedDate() throws Exception {
		// load this data set so that we get the additional patient program created in this data case
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Patient patient = Context.getPatientService().getPatient(2);
		Program program = pws.getProgram(1);
		
		Calendar cal = Calendar.getInstance();
		cal.set(2001, 6, 31);
		Date date = cal.getTime();
		
		PatientProgram pp = HtmlFormEntryUtil.getClosestFutureProgramEnrollment(patient, program, date);
		Assert.assertEquals("32296060-03aa-102d-b0e3-001ec94a0cc5", pp.getUuid());
		
		// now, if we roll the date back a year earlier, it should get the earlier of the two programs for this patient
		cal.set(2000, 6, 31);
		date = cal.getTime();
		
		pp = HtmlFormEntryUtil.getClosestFutureProgramEnrollment(patient, program, date);
		Assert.assertEquals("32596060-03aa-102d-b0e3-001ec94a0cc5", pp.getUuid());
	}
	
	@Test
	@Verifies(value = "should return null if program enrollment date same as specified date", method = "getClosestFutureProgramEnrollment(Patient,Program,Date)") 
	public void getClosestFutureProgramEnrollment_shouldReturnNullIfProgramEnrollmentSameAsSpecifiedDate() throws Exception {
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Patient patient = Context.getPatientService().getPatient(2);
		Program program = pws.getProgram(1);
		Date date = pws.getPatientProgram(1).getDateEnrolled();
		Assert.assertNull(HtmlFormEntryUtil.getClosestFutureProgramEnrollment(patient, program, date));
	}
	
	@Test
	@Verifies(value = "stringToDocument should handle ascii-escaped characters correctly", method = "stringToDocument(String xml)")
	public void stringToDocument_shouldHandleAsciiEsapeChars(){
		String str = "<htmlform> <input type=\"text\" value=\"HI!\" /> &#160; </htmlform>";
		try {
			Document doc = HtmlFormEntryUtil.stringToDocument(str);
			Assert.assertNotNull(doc);
		} catch (Exception ex){
			System.out.println(ex);
			Assert.assertTrue(false);
		}
	}
	
	@Test
	@Verifies(value = "stringToDocument should handle escaped characters correctly if defined as an entity", method = "stringToDocument(String xml)")
	public void stringToDocument_shouldHandleEsapeCharsWhenDefinedAsEntity(){
		String str = "<?xml version=\"1.0\"?> <!DOCTYPE some_name [ <!ENTITY nbsp \"&#160;\">]> <htmlform> <input type=\"text\" value=\"HI!\" /> &nbsp; </htmlform>";
		try {
			Document doc = HtmlFormEntryUtil.stringToDocument(str);
			Assert.assertNotNull(doc);
		} catch (Exception ex){
			System.out.println(ex);
			Assert.assertTrue(false);
		}
	}
	
	@Test
	@Verifies(value="shouldSetTheValueComplexOfObsIfConceptIsComplex",method="createObs(Concept concept, Object value, Date datetime, String accessionNumber)")
	public void createObs_shouldSetTheValueComplexOfObsIfConceptIsComplex(){
		
		ComplexData complexData=new ComplexData("test", null);
		Concept c=new Concept();
		ConceptDatatype cd=new ConceptDatatype();
		cd.setUuid(HtmlFormEntryConstants.COMPLEX_UUID);
		c.setDatatype(cd);
		Obs o=HtmlFormEntryUtil.createObs(c, complexData, null, null);
	    Assert.assertEquals(o.getValueComplex(),complexData.getTitle());
		
	}

    @Test
    @Verifies(value="shouldReturnMetadataNameIfNoFormatterPresent", method="format(OpenmrsMetadata md, Locale locale)")
    public void sholdReturnMetadataNameIfNoFormatterPresent() {

        Location location = new Location();
        location.setName("Test name");

        Assert.assertTrue(HtmlFormEntryUtil.format(location, Locale.US).equals("Test name"));
    }

    @Test
    @Verifies(value="shouldFetchLocationTagByName", method="getLocationTag(String identifier)")
    public void shouldFetchLocationTagByName() throws Exception {
        // this tag is in the regression test dataset
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));

        LocationTag tag = HtmlFormEntryUtil.getLocationTag("Some Tag");
        Assert.assertNotNull(tag);
        Assert.assertEquals("Some Tag", tag.getTag());
    }

    @Test
    @Verifies(value="shouldFetchLocationTagById", method="getLocationTag(String identifier)")
    public void shouldFetchLocationTagById() throws Exception {
        // this tag is in the regression test dataset
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));

        LocationTag tag = HtmlFormEntryUtil.getLocationTag("1001");
        Assert.assertNotNull(tag);
        Assert.assertEquals("Some Tag", tag.getTag());
    }

}
