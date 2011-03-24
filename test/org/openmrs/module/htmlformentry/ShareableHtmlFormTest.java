package org.openmrs.module.htmlformentry;

import java.util.Collection;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class ShareableHtmlFormTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/RegressionTest-data.xml";
	
	@Before
	public void setupDatabase() throws Exception {
		initializeInMemoryDatabase();
		authenticate();
		executeDataSet(XML_DATASET_PACKAGE_PATH);
	}
	
	@Test
	@Verifies(value = "should create cloned form to export with appropriate dependencies", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneWithDependencies() throws Exception {
		
		// include this set so that we get the mapping concept
		executeDataSet("org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data3.xml");
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		ShareableHtmlForm formClone = new ShareableHtmlForm(form, true, true, true, true);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03-102d-b0e3-001ec94a0cc4")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03-102d-b0e3-001ec94a0cc5")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03-102d-b0e3-001ec94a0cc6")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03-102d-b0e3-001ec94a0cc3")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConcept("5")));
		
		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-0370-102d-b0e3-123456789011")));
		
		// make sure the program has been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getProgramByUuid(
			"da4a0391-ba62-4fad-ad66-1e3722d16380")));
		
		// make sure the drugs have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getDrugByUuid(
			"3cfcf118-931c-46f7-8ff6-7b876f0d4202")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getDrugByUuid(
			"05ec820a-d297-44e3-be6e-698531d9dd3f")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getDrugByUuid(
			"7e2323fa-0fa0-461f-9b59-6765997d849e")));
		
		// make sure the locations have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocationByUuid(
			"dc5c1fcc-0459-4201-bf70-0b90535ba362")));
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocationByUuid(
			"9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocationByUuid(
			"167ce20c-4785-4285-9119-d197268f7f4a")));
		
		// make sure the provider has been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(Context.getPersonService().getPersonByUuid(
			"c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
	}
	
	@Test
	@Verifies(value = "should create cloned form without mapping concepts, drugs, locations and providers", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneButSkipLocationsAndProviders() throws Exception {
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		ShareableHtmlForm formClone = new ShareableHtmlForm(form, false, false, false, false);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure the provider has NOT been added to the list of dependencies
		Assert.assertFalse(dependencies.contains(Context.getPersonService().getPersonByUuid(
			"c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
		
		// make sure the mapping concept and locations have NOT been added to the dependencies
		Assert.assertFalse(dependencies.contains(Context.getLocationService().getLocationByUuid(
			"dc5c1fcc-0459-4201-bf70-0b90535ba362")));
		Assert.assertFalse(dependencies.contains(Context.getLocationService().getLocationByUuid(
			"9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		Assert.assertFalse(dependencies.contains(Context.getConceptService().getConcept("5")));
		
		// make sure the drug referenced by name has NOT been added to the dependencies
		Assert.assertFalse(dependencies.contains(Context.getConceptService().getDrugByUuid(
			"05ec820a-d297-44e3-be6e-698531d9dd3f")));
		
	}
}
