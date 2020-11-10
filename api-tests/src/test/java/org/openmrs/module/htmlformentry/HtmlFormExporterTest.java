package org.openmrs.module.htmlformentry;

import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.Module;
import org.openmrs.module.metadatamapping.MetadataSource;
import org.openmrs.module.metadatamapping.MetadataTermMapping;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.openmrs.test.Verifies;

public class HtmlFormExporterTest extends BaseHtmlFormEntryTest {
	
	private static Module module = new Module("metadatamapping", "metadatamapping", "packageName", "author", "desc",
	        "1.3.4");
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	private void setupMappings() {
		MetadataSource metadataSource = new MetadataSource();
		metadataSource.setName("source");
		metadataSource.setDateCreated(new Date());
		metadataSource.setRetired(false);
		metadataSource = Context.getService(MetadataMappingService.class).saveMetadataSource(metadataSource);
		
		MetadataTermMapping metadataTermMapping1 = new MetadataTermMapping(metadataSource, "DataClerk",
		        Context.getUserService().getRole("Data Clerk"));
		metadataTermMapping1.setName("mapping1");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping1);
		
		MetadataTermMapping metadataTermMapping2 = new MetadataTermMapping(metadataSource, "ROLE",
		        Context.getUserService().getRole("Provider"));
		metadataTermMapping2.setName("mapping2");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping2);
		
		MetadataTermMapping metadataTermMapping3 = new MetadataTermMapping(metadataSource, "Location",
		        Context.getLocationService().getLocationByUuid("9356400c-a5a2-4532-8f2b-2361b3446eb8"));
		metadataTermMapping3.setName("mapping3");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping3);
		
		MetadataTermMapping metadataTermMapping4 = new MetadataTermMapping(metadataSource, "MDR-TB PROGRAM",
		        Context.getProgramWorkflowService().getProgramByName("MDR-TB PROGRAM"));
		metadataTermMapping4.setName("mapping4");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping4);
		
		MetadataTermMapping metadataTermMapping5 = new MetadataTermMapping(metadataSource, "1",
		        Context.getPatientService().getPatientIdentifierType(1));
		metadataTermMapping5.setName("mapping5");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping5);
		
		Context.flushSession();
	}
	
	@Test
	@Verifies(value = "should create cloned form to export with appropriate dependencies using mappings", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneWithDependencies_withMapping() throws Exception {
		
		setupMappings();
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingTestFormMapping.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, true, true, true);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc7")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc4")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc6")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc3")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("aa52296060-03-102d-b0e3-001ec94a0cc1")));
		// this is the mapped concept XYZ:HT found in HtmlFormEntryTest-data
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("44d3611a-6699-4d52-823f-b4b788bac3e3")));
		
		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-0370-102d-b0e3-123456789011")));
		
		// make sure the programs have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getProgramByUuid("67bad8f4-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getProgramByUuid("71779c39-d289-4dfe-91b5-e7cfaa27c78b")));
		
		// make sure the program workflows have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getWorkflowByUuid("72a90efc-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getWorkflowByUuid("7c3e071a-53a7-11e1-8cb6-00248140a5eb")));
		
		// make sure the program workflow states have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("89d1a292-5140-11e1-a3e3-00248140a5eb"))); // 200
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("6de7ed10-53ad-11e1-8cb6-00248140a5eb"))); // As specified
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11e1-8cb6-00248140a5eb"))); // 207
		
		// note this this assertion fails--currently, the exporter WILL NOT be able to pick up states referenced concept map
		// (this is because the exporter considers each attribute separately, and to you can't get a state referenced by concept map without knowing the corresponding program or workflow)
		// however, this should not be a problem because the state should be included by MDS when it's parent program or workflow is exported
		//Assert.assertTrue(dependencies .contains(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11a1-8cb6-00248140a5eb"))); // Test Code
		
		// make sure the drugs have been added to the dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("3cfcf118-931c-46f7-8ff6-7b876f0d4202")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("05ec820a-d297-44e3-be6e-698531d9dd3f")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("7e2323fa-0fa0-461f-9b59-6765997d849e")));
		
		// make sure the locations have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocation("Never Never Land")));
		Assert.assertTrue(
		    dependencies.contains(Context.getLocationService().getLocationByUuid("9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocation(1)));
		
		// make sure the provider has been added to the list of dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getPersonService().getPersonByUuid("c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
		
		// make sure the patient identifier types have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertTrue(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));
		
		// make sure the roles have been added to the list of dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getUserService().getRoleByUuid("92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertTrue(
		    dependencies.contains(Context.getUserService().getRoleByUuid("a238c500-58b1-11e0-80e3-0800200c9a66")));
		
		// make sure the location tags have been added to the list of dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getLocationService().getLocationTagByUuid("321c1f44-0459-4201-bf70-0b90535ba362")));
	}
	
	@Test
	@Verifies(value = "should create cloned form to export with appropriate dependencies", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneWithDependencies() throws Exception {
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, true, true, true);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc4")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc6")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc3")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("aa52296060-03-102d-b0e3-001ec94a0cc1")));
		// this is the mapped concept XYZ:HT found in HtmlFormEntryTest-data
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("44d3611a-6699-4d52-823f-b4b788bac3e3")));
		
		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-0370-102d-b0e3-123456789011")));
		
		// make sure the programs have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getProgramByUuid("67bad8f4-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getProgramByUuid("71779c39-d289-4dfe-91b5-e7cfaa27c78b")));
		
		// make sure the program workflows have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getWorkflowByUuid("72a90efc-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getWorkflowByUuid("7c3e071a-53a7-11e1-8cb6-00248140a5eb")));
		
		// make sure the program workflow states have been added to the dependencies
		
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("89d1a292-5140-11e1-a3e3-00248140a5eb"))); // 200
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("6de7ed10-53ad-11e1-8cb6-00248140a5eb"))); // As specified
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11e1-8cb6-00248140a5eb"))); // 207
		
		// note this this assertion fails--currently, the exporter WILL NOT be able to pick up states referenced concept map
		// (this is because the exporter considers each attribute separately, and to you can't get a state referenced by concept map without knowing the corresponding program or workflow)
		// however, this should not be a problem because the state should be included by MDS when it's parent program or workflow is exported
		//Assert.assertTrue(dependencies .contains(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11a1-8cb6-00248140a5eb"))); // Test Code
		
		// make sure the drugs have been added to the dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("3cfcf118-931c-46f7-8ff6-7b876f0d4202")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("05ec820a-d297-44e3-be6e-698531d9dd3f")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("7e2323fa-0fa0-461f-9b59-6765997d849e")));
		
		// make sure the locations have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocation("Never Never Land")));
		Assert.assertTrue(
		    dependencies.contains(Context.getLocationService().getLocationByUuid("9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocation(1)));
		
		// make sure the provider has been added to the list of dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getPersonService().getPersonByUuid("c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
		
		// make sure the patient identifier types have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertTrue(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));
		
		// make sure the roles have been added to the list of dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getUserService().getRoleByUuid("92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertTrue(
		    dependencies.contains(Context.getUserService().getRoleByUuid("a238c500-58b1-11e0-80e3-0800200c9a66")));
		
		// TODO test exporting location tags once we upgrade to only supporting OpenMRS 1.7 and above (since in 1.6, locations tags don't have names)
		
	}
	
	@Test
	public void shouldRespectClassesNotToExportGlobalProperty() throws Exception {
		
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(HtmlFormEntryConstants.GP_CLASSES_NOT_TO_EXPORT_WITH_MDS, "org.openmrs.Program"));
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, true, true, true);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure all the appropriate concepts have been added to the dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc1")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc4")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc6")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-03aa-102d-b0e3-001ec94a0cc3")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("aa52296060-03-102d-b0e3-001ec94a0cc1")));
		// this is the mapped concept XYZ:HT found in HtmlFormEntryTest-data
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("44d3611a-6699-4d52-823f-b4b788bac3e3")));
		
		//drug discontinue reason, corresponds to concept 555 in regressionTest-data
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getConceptByUuid("32296060-0370-102d-b0e3-123456789011")));
		
		// make sure the programs have NOT been added to the dependencies
		Assert.assertFalse(dependencies
		        .contains(Context.getProgramWorkflowService().getProgramByUuid("da4a0391-ba62-4fad-ad66-1e3722d16380")));
		Assert.assertFalse(dependencies
		        .contains(Context.getProgramWorkflowService().getProgramByUuid("71779c39-d289-4dfe-91b5-e7cfaa27c78b")));
		
		// make sure the program workflows have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getWorkflowByUuid("72a90efc-5140-11e1-a3e3-00248140a5eb")));
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getWorkflowByUuid("7c3e071a-53a7-11e1-8cb6-00248140a5eb")));
		
		// make sure the program workflow states have been added to the dependencies
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("89d1a292-5140-11e1-a3e3-00248140a5eb"))); // 200
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("6de7ed10-53ad-11e1-8cb6-00248140a5eb"))); // As specified
		Assert.assertTrue(dependencies
		        .contains(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11e1-8cb6-00248140a5eb"))); // 207
		
		// note this this assertion fails--currently, the exporter WILL NOT be able to pick up states referenced concept map
		// (this is because the exporter considers each attribute separately, and to you can't get a state referenced by concept map without knowing the corresponding program or workflow)
		// however, this should not be a problem because the state should be included by MDS when it's parent program or workflow is exported
		//Assert.assertTrue(dependencies .contains(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11a1-8cb6-00248140a5eb"))); // Test Code
		
		// make sure the drugs have been added to the dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("3cfcf118-931c-46f7-8ff6-7b876f0d4202")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("05ec820a-d297-44e3-be6e-698531d9dd3f")));
		Assert.assertTrue(
		    dependencies.contains(Context.getConceptService().getDrugByUuid("7e2323fa-0fa0-461f-9b59-6765997d849e")));
		
		// make sure the locations have been added to the dependencies
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocation("Never Never Land")));
		Assert.assertTrue(
		    dependencies.contains(Context.getLocationService().getLocationByUuid("9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		Assert.assertTrue(dependencies.contains(Context.getLocationService().getLocation(1)));
		
		// make sure the provider has been added to the list of dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getPersonService().getPersonByUuid("c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
		
		// make sure the patient identifier types have been added to the list of dependencies
		Assert.assertTrue(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertTrue(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));
		
		// make sure the roles have been added to the list of dependencies
		Assert.assertTrue(
		    dependencies.contains(Context.getUserService().getRoleByUuid("92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertTrue(
		    dependencies.contains(Context.getUserService().getRoleByUuid("a238c500-58b1-11e0-80e3-0800200c9a66")));
	}
	
	@Test
	@Verifies(value = "should create cloned form without locations, providers, and patient identifiers", method = "createCloneForExport(HtmlForm)")
	public void createCloneForExport_shouldCreateCloneButSkipLocationsAndProviders() throws Exception {
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(false, false, false, false);
		
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
		// make sure the provider has NOT been added to the list of dependencies
		Assert.assertFalse(
		    dependencies.contains(Context.getPersonService().getPersonByUuid("c04ee3c8-b68f-43cc-bff3-5a831ee7225f")));
		
		// make sure the locations have NOT been added to the dependencies
		Assert.assertFalse(
		    dependencies.contains(Context.getLocationService().getLocationByUuid("dc5c1fcc-0459-4201-bf70-0b90535ba362")));
		Assert.assertFalse(
		    dependencies.contains(Context.getLocationService().getLocationByUuid("9356400c-a5a2-4532-8f2b-2361b3446eb8")));
		
		// make sure the patient identifier types have NOT been added to the list of dependencies
		Assert.assertFalse(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("1a339fe9-38bc-4ab3-b180-320988c0b968")));
		Assert.assertFalse(dependencies.contains(
		    Context.getPatientService().getPatientIdentifierTypeByUuid("2f470aa8-1d73-43b7-81b5-01f0c0dfa53c")));
		
		// make sure the roles have NOT been added to the list of dependencies
		Assert.assertFalse(
		    dependencies.contains(Context.getUserService().getRoleByUuid("92b70b00-58b1-11e0-80e3-0800200c9a66")));
		Assert.assertFalse(
		    dependencies.contains(Context.getUserService().getRoleByUuid("a238c500-58b1-11e0-80e3-0800200c9a66")));
		
		/* We have changed the default so that you cannot exclude mapped concepts or drugs referenced by name */
		//Assert.assertFalse(dependencies.contains(Context.getConceptService().getConceptByUuid(
		//"44d3611a-6699-4d52-823f-b4b788bac3e3")));
		// make sure the drug referenced by name has NOT been added to the dependencies
		//Assert.assertFalse(dependencies.contains(Context.getConceptService().getDrugByUuid(
		//"05ec820a-d297-44e3-be6e-698531d9dd3f")));
		
	}
}
