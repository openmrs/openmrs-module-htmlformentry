package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

/***
 * Test agaist standardTestData.xml from org.openmrs.include + 
 * Data from HtmlFormEntryTest-data3.xml 
 */
public class HtmlFormEntryUtilTest extends BaseModuleContextSensitiveTest{
	
    protected final Log log = LogFactory.getLog(getClass());
    
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
    
    protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data3.xml";
    
    @Before
    public void setupDatabase() throws Exception {
        initializeInMemoryDatabase();
        authenticate();
        executeDataSet(XML_DATASET_PACKAGE_PATH);
    }
    
	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)}
	 * id test 
	 */
	@Test
	@Verifies(value = "should find a location by its id", method = "getLocation(String)")
	public void getLocation_shouldFindALocationByItsId() throws Exception {
		Assert.assertEquals("Xanadu", HtmlFormEntryUtil.getLocation("2").getName());
	}

	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)}
	 * mapping test
	 */
	@Test
	@Verifies(value = "should find a location by its name", method = "getLocation(String)")
	public void getLocation_shouldFindALocationByItsName() throws Exception {
		Assert.assertEquals("2", HtmlFormEntryUtil.getLocation("Xanadu").getId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getLocation(String)}
	 * this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a location by its uuid", method = "getLocation(String)")
	public void getLocation_shouldFindALocationByItsUuid() throws Exception {
		Assert.assertEquals("Xanadu", HtmlFormEntryUtil.getLocation("9356400c-a5a2-4532-8f2b-2361b3446eb8").getName());
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
	 * @see {@link HtmlFormEntryUtil#getProgram(String)}
	 * id test 
	 */
	@Test
	@Verifies(value = "should find a program by its Id", method = "getProgram(String)")
	public void getProgram_shouldFindAProgramByItsId() throws Exception {
		Assert.assertEquals("MDR program", HtmlFormEntryUtil.getProgram("2").getName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getProgram(String)}
	 * this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a program by its uuid", method = "getProgram(String)")
	public void getLocation_shouldFindAProgramByItsUuid() throws Exception {
		Assert.assertEquals("MDR program", HtmlFormEntryUtil.getProgram("71779c39-d289-4dfe-91b5-e7cfaa27c78b").getName());
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
	 * @see {@link HtmlFormEntryUtil#getPerson(String)}
	 * id test 
	 */
	@Test
	@Verifies(value = "should find a person by Id", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonById() throws Exception {
		Assert.assertEquals("Hornblower", HtmlFormEntryUtil.getPerson("2").getFamilyName());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getPerson(String)}
	 * this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a person by uuid", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonByUuid() throws Exception {
		Assert.assertEquals("Hornblower", HtmlFormEntryUtil.getPerson("da7f524f-27ce-4bb2-86d6-6d1d05312bd5").getFamilyName());
	}

	/**
	 * @see {@link HtmlFormEntryUtil#getPerson(String)}
	 * this is the username test
	 */
	@Test
	@Verifies(value = "should find a person by username", method = "getPerson(String)")
	public void getPerson_shouldFindAPersonByUsername() throws Exception {			
		Assert.assertEquals("502", HtmlFormEntryUtil.getPerson("butch").getId().toString());
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
	 * @see {@link HtmlFormEntryUtil#getConcept(String)}
	 * id test 
	 */
	@Test
	@Verifies(value = "should find a concept by its conceptId", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptByItsConceptId() throws Exception {
		String id = "3";
		Assert.assertEquals("3",HtmlFormEntryUtil.getConcept(id).getConceptId().toString());
	}
	
	/**
	 * @see {@link HtmlFormEntryUtil#getConcept(String)}
	 * mapping test
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
	 * @see {@link HtmlFormEntryUtil#getConcept(String)}
	 * this is the uuid test
	 */
	@Test
	@Verifies(value = "should find a concept by its uuid", method = "getConcept(String)")
	public void getConcept_shouldFindAConceptByItsUuid() throws Exception {
		//the uuid from standardTestDataset
		String id = "0cbe2ed3-cd5f-4f46-9459-26127c9265ab";
		Assert.assertEquals(id, HtmlFormEntryUtil.getConcept(id).getUuid());
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
	 * see {@link HtmlFormEntryUtil#replaceIdsWithUuids(HtmlForm)}
	 */
	@Test
	@Verifies(value = "should convert ids to uuids", method = "replaceConceptIdsWithUuids(HtmlForm)")
	public void replaceConceptIdsWithUuids_shouldReplaceConceptIdsWithUuids() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data.xml");
		
		HtmlForm form = new HtmlForm();
		
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		HtmlFormEntryUtil.replaceIdsWithUuids(form);
		
		TestUtil.assertFuzzyContains("<encounterLocation default=\"9356400c-a5a2-4532-8f2b-2361b3446eb8\" order=\"dc5c1fcc-0459-4201-bf70-0b90535ba362,9356400c-a5a2-4532-8f2b-2361b3446eb8\"",form.getXmlData());
		TestUtil.assertFuzzyContains("<encounterProvider role=\"Provider\" default=\"c04ee3c8-b68f-43cc-bff3-5a831ee7225f\"", form.getXmlData());
		TestUtil.assertFuzzyContains("groupingConceptId=\"32296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"32296060-03-102d-b0e3-001ec94a0cc4\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"32296060-03-102d-b0e3-001ec94a0cc3\"", form.getXmlData());
		TestUtil.assertFuzzyContains("answerConceptIds=\"32296060-03-102d-b0e3-001ec94a0cc5,someSource:someKey,32296060-03-102d-b0e3-001ec94a0cc6,32296060-03-102d-b0e3-001ec94a0cc7\"", form.getXmlData());
		TestUtil.assertFuzzyContains("programId=\"da4a0391-ba62-4fad-ad66-1e3722d16380\"", form.getXmlData());
	}
	
	/**
	 * see {@link HtmlFormEntryUtil#replaceIdsWithUuids(HtmlForm)}
	 * @throws Exception 
	 */
	@Test
	@Verifies(value = "should convert ids to uuids within repeat tags", method = "replaceConceptIdsWithUuids(HtmlForm)")
	public void replaceConceptIdsWithUuids_shouldReplaceConceptIdsWithUuidsWithinRepeatTags() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data.xml");
		
		HtmlForm form = new HtmlForm();
		
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingWithRepeatTestForm.xml"));
		HtmlFormEntryUtil.replaceIdsWithUuids(form);
		
		// make sure it's left the keys alone
		TestUtil.assertFuzzyContains("groupingConceptId=\"\\{allergyGroup\\}\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"\\{allergy\\}\"", form.getXmlData());
		TestUtil.assertFuzzyContains("answerConceptIds=\"\\{allergyAnswers\\}\"", form.getXmlData());

		// test that the first render tag has been substituted
		TestUtil.assertFuzzyContains("allergyGroup=\"32296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("allergy=\"32296060-03-102d-b0e3-001ec94a0cc4\"", form.getXmlData());
		TestUtil.assertFuzzyContains("allergyAnswers=\"32296060-03-102d-b0e3-001ec94a0cc5,32296060-03-102d-b0e3-001ec94a0cc6\"", form.getXmlData());		
	
		// test that the second render tag has been substituted
		TestUtil.assertFuzzyContains("allergyGroup=\"42296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("allergy=\"52296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("allergyAnswers=\"32296060-03-102d-b0e3-001ec94a0cc6,32296060-03-102d-b0e3-001ec94a0cc7\"", form.getXmlData());		
	
	}
	
}