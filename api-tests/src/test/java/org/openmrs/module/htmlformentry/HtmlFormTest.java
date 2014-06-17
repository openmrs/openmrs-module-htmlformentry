package org.openmrs.module.htmlformentry;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Location;
import org.openmrs.OpenmrsObject;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class HtmlFormTest extends BaseModuleContextSensitiveTest  {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}
	
	/**
	 * @see HtmlForm#onSave(Map)
	 * @verifies should replace uuids
	 */
	@Test
	public void onSave_shouldShouldReplaceUuids() throws Exception {
		// include this set so that we get the mapping concept
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(false, false, false, false);
		
		String incomingUuid1 = "3cfcf118-931c-46f7-8ff6-7b876f0d4202";
		String incomingUuid2 = "32296060-0370-102d-b0e3-123456789011";
		
		String existingUuid1 = "XXXXXXXX-931c-46f7-8ff6-7b876f0d4202";
		String existingUuid2 = "XXXXXXXX-0370-102d-b0e3-123456789011";
		
		Assert.assertTrue(formClone.getXmlData().contains(incomingUuid1));
		Assert.assertTrue(formClone.getXmlData().contains(incomingUuid2));
		Assert.assertFalse(formClone.getXmlData().contains(existingUuid1));
		Assert.assertFalse(formClone.getXmlData().contains(existingUuid2));
		
		Concept incoming1 = new Concept();
		incoming1.setUuid(incomingUuid1);
		Concept incoming2 = new Concept();
		incoming2.setUuid(incomingUuid2);
		Concept existing1 = new Concept();
		existing1.setUuid(existingUuid1);
		Concept existing2 = new Concept();
		existing2.setUuid(existingUuid2);
		
		Map<OpenmrsObject, OpenmrsObject> incomingToExisting = new HashMap<OpenmrsObject, OpenmrsObject>();
		incomingToExisting.put(incoming1, existing1);
		incomingToExisting.put(incoming2, existing2);
		
		formClone.onSave(incomingToExisting);
		
		Assert.assertFalse(formClone.getXmlData().contains(incomingUuid1));
		Assert.assertFalse(formClone.getXmlData().contains(incomingUuid2));
		Assert.assertTrue(formClone.getXmlData().contains(existingUuid1));
		Assert.assertTrue(formClone.getXmlData().contains(existingUuid2));
	}
	
	/**
	 * @see HtmlForm#onSave(Map)
	 * @verifies should replace uuids
	 */
	@Test
	public void onSave_shouldShouldReplaceNames() throws Exception {
		// include this set so that we get the mapping concept
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(true, false, true, true);
		
		String incomingIdentifierTypeName = "Old Identification Number";
		String existingIdentifierTypeName = "Social Security Number";
		
		String incomingLocationName = "Never Never Land";
		String existingLocationName = "Xanadu";
		
		String incomingDrugName = "Aspirin";
		String existingDrugName = "NyQuil";

		String incomingRoleName = "Data Clerk";
		String existingRoleName = "System Administrator";
		
		Assert.assertTrue(formClone.getXmlData().contains(incomingIdentifierTypeName));
		Assert.assertFalse(formClone.getXmlData().contains(existingIdentifierTypeName));
		
		Assert.assertTrue(formClone.getXmlData().contains(incomingLocationName));
		Assert.assertFalse(formClone.getXmlData().contains(existingLocationName));
		
		Assert.assertTrue(formClone.getXmlData().contains(incomingDrugName));
		Assert.assertFalse(formClone.getXmlData().contains(existingDrugName));
		
		Assert.assertTrue(formClone.getXmlData().contains(incomingRoleName));
		Assert.assertFalse(formClone.getXmlData().contains(existingRoleName));
		
		PatientIdentifierType incoming1 = Context.getPatientService().getPatientIdentifierType(2);  // Old Identifier Type
		PatientIdentifierType existing1 = Context.getPatientService().getPatientIdentifierType(4); // Social Security Number
		
		Location incoming2 = Context.getLocationService().getLocation(3);  // Never Never Land
		Location existing2 = Context.getLocationService().getLocation(2); // Xanadu
		
		Drug incoming3 = Context.getConceptService().getDrug(3);  // Aspirin
		Drug existing3 = Context.getConceptService().getDrug(11); // NyQuil
		
		Role incoming4 = Context.getUserService().getRole(incomingRoleName);
		Role existing4 = Context.getUserService().getRole(existingRoleName);
		
		Map<OpenmrsObject, OpenmrsObject> incomingToExisting = new HashMap<OpenmrsObject, OpenmrsObject>();
		incomingToExisting.put(incoming1, existing1);
		incomingToExisting.put(incoming2, existing2);
		incomingToExisting.put(incoming3, existing3);
		incomingToExisting.put(incoming4, existing4);
		
		formClone.onSave(incomingToExisting);
		
		Assert.assertFalse(formClone.getXmlData().contains(incomingIdentifierTypeName));
		Assert.assertTrue(formClone.getXmlData().contains(existingIdentifierTypeName));
		
		Assert.assertFalse(formClone.getXmlData().contains(incomingLocationName));
		Assert.assertTrue(formClone.getXmlData().contains(existingLocationName));
		
		Assert.assertFalse(formClone.getXmlData().toLowerCase().contains(incomingDrugName.toLowerCase()));
		Assert.assertTrue(formClone.getXmlData().toLowerCase().contains(existingDrugName.toLowerCase()));
		
		Assert.assertFalse(formClone.getXmlData().contains(incomingRoleName));
		Assert.assertTrue(formClone.getXmlData().contains(existingRoleName));
	}
	
	/**
	 * @see HtmlForm#onSave(Map)
	 * @verifies throw exception if incoming uuid has not 36 characters
	 */
	@Test(expected = IllegalArgumentException.class)
	public void onSave_shouldThrowExceptionIfExistingUuidHasNot36Characters() throws Exception {
		// include this set so that we get the mapping concept
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		HtmlFormExporter exporter = new HtmlFormExporter(form);
		HtmlForm formClone = exporter.export(false, false, false, false);
		
		String incomingUuid1 = "3cfcf118-931c-46f7-8ff6-7b876f0d4202";
		String incomingUuid2 = "32296060-0370-102d-b0e3-123456789011";
		
		String existingUuid1 = "a";
		String existingUuid2 = "b";
		
		Concept incoming1 = new Concept();
		incoming1.setUuid(incomingUuid1);
		Concept incoming2 = new Concept();
		incoming2.setUuid(incomingUuid2);
		Concept existing1 = new Concept();
		existing1.setUuid(existingUuid1);
		Concept existing2 = new Concept();
		existing2.setUuid(existingUuid2);
		
		Map<OpenmrsObject, OpenmrsObject> incomingToExisting = new HashMap<OpenmrsObject, OpenmrsObject>();
		incomingToExisting.put(incoming1, existing1);
		incomingToExisting.put(incoming2, existing2);
		
		formClone.onSave(incomingToExisting);
	}
}