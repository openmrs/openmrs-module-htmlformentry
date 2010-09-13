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
		
		TestUtil.assertFuzzyContains("groupingConceptId=\"32296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"32296060-03-102d-b0e3-001ec94a0cc4\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"32296060-03-102d-b0e3-001ec94a0cc3\"", form.getXmlData());
		TestUtil.assertFuzzyContains("answerConceptIds=\"32296060-03-102d-b0e3-001ec94a0cc5,someSource:someKey,32296060-03-102d-b0e3-001ec94a0cc6,32296060-03-102d-b0e3-001ec94a0cc7\"", form.getXmlData());
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