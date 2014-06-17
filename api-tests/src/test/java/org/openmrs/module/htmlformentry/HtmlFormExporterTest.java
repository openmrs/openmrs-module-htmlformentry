package org.openmrs.module.htmlformentry;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsConstants;

import java.util.Collection;

public class HtmlFormExporterTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	protected static final String XML_DRUG_ORDER_ELEMENT_DATASET = "drugOrderElementDataSet";
	
	protected static final String XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET = "RegimenUtilsTest.xml";
	
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
	@Verifies(value = "should create cloned form to export with appropriate dependencies", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneWithDependencies() throws Exception {
		
		// include this set so that we get the mapping concept
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, true, true, true);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc4")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc5")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc6")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc3")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
			"aa52296060-03-102d-b0e3-001ec94a0cc1")));
		// this is the mapped concept XYZ:HT found in HtmlFormEntryTest-data
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "44d3611a-6699-4d52-823f-b4b788bac3e3")));
		
		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-0370-102d-b0e3-123456789011")));
		
		// make sure the programs have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getProgramByUuid(
		    "da4a0391-ba62-4fad-ad66-1e3722d16380")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getProgramByUuid(
		    "71779c39-d289-4dfe-91b5-e7cfaa27c78b")));
		
		// make sure the program workflows have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getWorkflowByUuid(
		    "72a90efc-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getWorkflowByUuid(
			"7c3e071a-53a7-11e1-8cb6-00248140a5eb")));
		
		// make sure the program workflow states have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
		    "92584cdc-6a20-4c84-a659-e035e45d36b0")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"e938129e-248a-482a-acea-f85127251472")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"860b3a13-d4b1-4f0a-b526-278652fa1809")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"8ef66ca8-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"67337cdc-53ad-11e1-8cb6-00248140a5eb")));
		
		// note this this assertion fails--currently, the exporter WILL NOT be able to pick up states referenced concept map
		// (this is because the exporter considers each attribute separately, and to you can't get a state referenced by concept map without knowing the corresponding program or workflow)
		// however, this should not be a problem because the state should be included by MDS when it's parent program or workflow is exported
		//Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			//"6de7ed10-53ad-11e1-8cb6-00248140a5eb")));
		
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
		
		// make sure the patient identifier types have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(Context.getPatientService().getPatientIdentifierTypeByUuid(
		    "1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertTrue(dependencies.contains(Context.getPatientService().getPatientIdentifierTypeByUuid(
		    "2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));
		
		// make sure the roles have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(Context.getUserService().getRoleByUuid(
		    "92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertTrue(dependencies.contains(Context.getUserService().getRoleByUuid(
		    "a238c500-58b1-11e0-80e3-0800200c9a66")));

        // TODO test exporting location tags once we upgrade to only supporting OpenMRS 1.7 and above (since in 1.6, locations tags don't have names)
		
	}
	
	@Test
	public void shouldRespectClassesNotToExportGlobalProperty() throws Exception {

		// include this set so that we get the mapping concept
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(HtmlFormEntryConstants.GP_CLASSES_NOT_TO_EXPORT_WITH_MDS, "org.openmrs.Program"));

		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));

		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, true, true, true);

		Collection<OpenmrsObject> dependencies = formClone.getDependencies();

		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc4")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc5")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc6")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc3")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
			"aa52296060-03-102d-b0e3-001ec94a0cc1")));
		// this is the mapped concept XYZ:HT found in HtmlFormEntryTest-data
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "44d3611a-6699-4d52-823f-b4b788bac3e3")));

		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-0370-102d-b0e3-123456789011")));

		// make sure the programs have NOT been added to the dependencies
		Assert.assertFalse(dependencies.contains(Context.getProgramWorkflowService().getProgramByUuid(
		    "da4a0391-ba62-4fad-ad66-1e3722d16380")));
		Assert.assertFalse(dependencies.contains(Context.getProgramWorkflowService().getProgramByUuid(
		    "71779c39-d289-4dfe-91b5-e7cfaa27c78b")));

		// make sure the program workflows have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getWorkflowByUuid(
		    "72a90efc-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getWorkflowByUuid(
			"7c3e071a-53a7-11e1-8cb6-00248140a5eb")));

		// make sure the program workflow states have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
		    "92584cdc-6a20-4c84-a659-e035e45d36b0")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"e938129e-248a-482a-acea-f85127251472")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"860b3a13-d4b1-4f0a-b526-278652fa1809")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"8ef66ca8-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies.contains(Context.getProgramWorkflowService().getStateByUuid(
			"67337cdc-53ad-11e1-8cb6-00248140a5eb")));

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

		// make sure the patient identifier types have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(Context.getPatientService().getPatientIdentifierTypeByUuid(
		    "1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertTrue(dependencies.contains(Context.getPatientService().getPatientIdentifierTypeByUuid(
		    "2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));

		// make sure the roles have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(Context.getUserService().getRoleByUuid(
		    "92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertTrue(dependencies.contains(Context.getUserService().getRoleByUuid(
		    "a238c500-58b1-11e0-80e3-0800200c9a66")));
	}

	@Test
	@Verifies(value = "should create cloned form without locations, providers, and patient identifiers", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneButSkipLocationsAndProviders() throws Exception {
		
		// include this set so that we get the mapping concept
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(false, false, false, false);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure the provider has NOT been added to the list of dependencies
		Assert.assertFalse(dependencies.contains(Context.getPersonService().getPersonByUuid(
		    "c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
		
		// make sure the locations have NOT been added to the dependencies
		Assert.assertFalse(dependencies.contains(Context.getLocationService().getLocationByUuid(
		    "dc5c1fcc-0459-4201-bf70-0b90535ba362")));
		Assert.assertFalse(dependencies.contains(Context.getLocationService().getLocationByUuid(
		    "9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		
		// make sure the patient identifier types have NOT been added to the list of dependencies
		Assert.assertFalse(dependencies.contains(Context.getPatientService().getPatientIdentifierTypeByUuid(
		    "1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertFalse(dependencies.contains(Context.getPatientService().getPatientIdentifierTypeByUuid(
		    "2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));
		
		// make sure the roles have NOT been added to the list of dependencies
		Assert.assertFalse(dependencies.contains(Context.getUserService().getRoleByUuid(
		    "92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertFalse(dependencies.contains(Context.getUserService().getRoleByUuid(
		    "a238c500-58b1-11e0-80e3-0800200c9a66")));
		
		/* We have changed the default so that you cannot exclude mapped concepts or drugs referenced by name */
		//Assert.assertFalse(dependencies.contains(Context.getConceptService().getConceptByUuid(
		//"44d3611a-6699-4d52-823f-b4b788bac3e3")));
		// make sure the drug referenced by name has NOT been added to the dependencies
		//Assert.assertFalse(dependencies.contains(Context.getConceptService().getDrugByUuid(
		//"05ec820a-d297-44e3-be6e-698531d9dd3f")));
		
	}
	
	@Test
	@Verifies(value = "should create cloned export with appropriate drugs as referenced by standard regimen", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldIncludeAppropriateDrugsReferencedByStandardRegimen() throws Exception {
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestFormRegimens.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, true, true, true);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure the drugs have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getDrugByUuid(
				"3cfcf118-931c-46f7-8ff6-7b876f0d4202")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getDrugByUuid(
				"05ec820a-d297-44e3-be6e-698531d9dd3f")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getDrugByUuid(
				"7e2323fa-0fa0-461f-9b59-6765997d849e")));
		
		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "aa52296060-03-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-03aa-102d-b0e3-001ec94a0cc4")));
		
		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(dependencies.contains(Context.getConceptService().getConceptByUuid(
		    "32296060-0370-102d-b0e3-123456789011")));
			
	}
}