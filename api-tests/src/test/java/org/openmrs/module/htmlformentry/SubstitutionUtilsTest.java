package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.substitution.HtmlFormSubstitutionUtils;
import org.openmrs.test.Verifies;

/***
 * Test agaist standardTestData.xml from org.openmrs.include + Data from HtmlFormEntryTest-data.xml
 */
public class SubstitutionUtilsTest extends BaseHtmlFormEntryTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	/**
	 * see {@link HtmlFormSubstitutionUtils#replaceIdsWithUuids(HtmlForm)}
	 */
	@Test
	@Verifies(value = "should convert ids to uuids", method = "replaceConceptIdsWithUuids(HtmlForm)")
	public void replaceConceptIdsWithUuids_shouldReplaceConceptIdsWithUuids() throws Exception {
		
		HtmlForm form = new HtmlForm();
		
		form.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingTestForm.xml"));
		HtmlFormSubstitutionUtils.replaceIdsWithUuids(form);
		
		Location location1 = Context.getLocationService().getLocation(1);
		
		TestUtil.assertFuzzyContains("<encounterLocation default=\"9356400c-a5a2-4532-8f2b-2361b3446eb8\" order=\""
		        + location1.getUuid() + ",9356400c-a5a2-4532-8f2b-2361b3446eb8,Never Never Land\"",
		    form.getXmlData());
		TestUtil.assertFuzzyContains("<encounterProvider default=\"c04ee3c8-b68f-43cc-bff3-5a831ee7225f\" role=\"Provider\"",
		    form.getXmlData());
		
		// test to make sure that underlying matcher is case-insensitive
		TestUtil.assertFuzzyContains("GROUPINGCONCEPTID=\"32296060-03aa-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"32296060-03aa-102d-b0e3-001ec94a0cc4\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"32296060-03aa-102d-b0e3-001ec94a0cc3\"", form.getXmlData());
		TestUtil.assertFuzzyContains(
		    "answerConceptIds=\"32296060-03aa-102d-b0e3-001ec94a0cc5,XYZ:HT,32296060-03aa-102d-b0e3-001ec94a0cc6,32296060-03aa-102d-b0e3-001ec94a0cc7\"",
		    form.getXmlData());
		TestUtil.assertFuzzyContains("programId=\"67bad8f4-5140-11e1-a3e3-00248140a5eb\"", form.getXmlData());
		TestUtil.assertFuzzyContains(
		    "stateIds=\"67337cdc-53ad-11e1-8cb6-00248140a5eb,6de7ed10-53ad-11e1-8cb6-00248140a5eb\"", form.getXmlData());
		TestUtil.assertFuzzyContains("workflowId=\"72a90efc-5140-11e1-a3e3-00248140a5eb\"", form.getXmlData());
		TestUtil.assertFuzzyContains("stateId=\"89d1a292-5140-11e1-a3e3-00248140a5eb\"", form.getXmlData());
		TestUtil.assertFuzzyContains("identifierTypeId=\"1a339fe9-38bc-4ab3-b180-320988c0b968\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"aa52296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("stateIds=\"67337cdc-53ad-11e1-8cb6-00248140a5eb,XYZ: Test Code\"", form.getXmlData());
	}
	
	/**
	 * see {@link HtmlFormSubstitutionUtils#replaceIdsWithUuids(HtmlForm)}
	 * 
	 * @throws Exception
	 */
	@Test
	@Verifies(value = "should convert ids to uuids within repeat tags", method = "replaceConceptIdsWithUuids(HtmlForm)")
	public void replaceConceptIdsWithUuids_shouldReplaceConceptIdsWithUuidsWithinRepeatTags() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		
		HtmlForm form = new HtmlForm();
		
		form.setXmlData(new TestUtil()
		        .loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingWithRepeatTestForm.xml"));
		HtmlFormSubstitutionUtils.replaceIdsWithUuids(form);
		
		// make sure it's left the keys alone
		TestUtil.assertFuzzyContains("groupingConceptId=\"\\{allergyGroup\\}\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"\\{allergy\\}\"", form.getXmlData());
		TestUtil.assertFuzzyContains("answerConceptIds=\"\\{allergyAnswers\\}\"", form.getXmlData());
		
		// test that the first render tag has been substituted
		TestUtil.assertFuzzyContains("allergyGroup=\"32296060-03aa-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("allergy=\"32296060-03aa-102d-b0e3-001ec94a0cc4\"", form.getXmlData());
		TestUtil.assertFuzzyContains(
		    "allergyAnswers=\"32296060-03aa-102d-b0e3-001ec94a0cc5,32296060-03aa-102d-b0e3-001ec94a0cc6\"",
		    form.getXmlData());
		
		// test that the second render tag has been substituted
		TestUtil.assertFuzzyContains("allergyGroup=\"42296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains("allergy=\"aa52296060-03-102d-b0e3-001ec94a0cc1\"", form.getXmlData());
		TestUtil.assertFuzzyContains(
		    "allergyAnswers=\"32296060-03aa-102d-b0e3-001ec94a0cc6,32296060-03aa-102d-b0e3-001ec94a0cc7\"",
		    form.getXmlData());
		
	}
	
	/**
	 * see {@link HtmlFormSubstitutionUtils#replaceIdsWithUuids(HtmlForm)}
	 * 
	 * @throws Exception
	 */
	@Test
	@Verifies(value = "should convert ids to uuids within repeat tags", method = "replaceConceptIdsWithUuids(HtmlForm)")
	public void replaceConceptIdsWithUuids_shouldReplaceConceptIdsWithUuidsWithMacros() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		
		HtmlForm form = new HtmlForm();
		
		form.setXmlData(new TestUtil()
		        .loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingWithMacrosTestForm.xml"));
		HtmlFormSubstitutionUtils.replaceIdsWithUuids(form);
		
		// make sure it's left the macro references alone
		TestUtil.assertFuzzyContains("groupingConceptId=\"\\$allergyGroup\"", form.getXmlData());
		TestUtil.assertFuzzyContains("conceptId=\"\\$allergy\"", form.getXmlData());
		TestUtil.assertFuzzyContains("answerConceptIds=\"\\$allergyAnswers\"", form.getXmlData());
		
		// test that the macros themselves have been substituted
		TestUtil.assertFuzzyContains("allergyGroup=32296060-03aa-102d-b0e3-001ec94a0cc1", form.getXmlData());
		TestUtil.assertFuzzyContains("allergy=32296060-03aa-102d-b0e3-001ec94a0cc4", form.getXmlData());
		TestUtil.assertFuzzyContains(
		    "allergyAnswers=32296060-03aa-102d-b0e3-001ec94a0cc5,32296060-03aa-102d-b0e3-001ec94a0cc6", form.getXmlData());
		
	}
	
	/**
	 * see {@link HtmlFormSubstitutionUtils#replaceProgramNamesWithUuids(HtmlForm)}
	 * 
	 * @throws Exception
	 */
	@Test
	@Verifies(value = "should replace program names with uuids", method = "replaceProgramNamesWithUuids(HtmlForm)")
	public void replaceProgamNamesWithUuids_shouldReplaceProgramNamesWithUuids() throws Exception {
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingTestForm.xml"));
		HtmlFormSubstitutionUtils.replaceProgramNamesWithUuids(form);
		
		// make the program is no longer referenced by name
		TestUtil.assertFuzzyDoesNotContain("programId=\"MDR-TB PROGRAM\"", form.getXmlData());
		
		// make sure the program is now referenced by uuid
		TestUtil.assertFuzzyContains("programId=\"71779c39-d289-4dfe-91b5-e7cfaa27c78b\"", form.getXmlData());
	}
	
}
