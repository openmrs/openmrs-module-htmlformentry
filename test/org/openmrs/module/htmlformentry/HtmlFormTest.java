package org.openmrs.module.htmlformentry;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.OpenmrsObject;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class HtmlFormTest extends BaseModuleContextSensitiveTest  {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/RegressionTest-data.xml";
	
	@Before
	public void setupDatabase() throws Exception {
		initializeInMemoryDatabase();
		authenticate();
		executeDataSet(XML_DATASET_PACKAGE_PATH);
	}
	
	/**
	 * @see HtmlForm#onSave(Map)
	 * @verifies should replace uuids
	 */
	@Test
	public void onSave_shouldShouldReplaceUuids() throws Exception {
		// include this set so that we get the mapping concept
		executeDataSet("org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data3.xml");
		
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
	 * @verifies throw exception if incoming uuid has not 36 characters
	 */
	@Test(expected = IllegalArgumentException.class)
	public void onSave_shouldThrowExceptionIfIncomingUuidHasNot36Characters() throws Exception {
		Map<OpenmrsObject, OpenmrsObject> incomingToExisting = new HashMap<OpenmrsObject, OpenmrsObject>();
		
		Concept incoming = new Concept();
		incoming.setUuid("a");
		Concept existing = new Concept();
		existing.setUuid("b");
		
		incomingToExisting.put(incoming, existing);
		
		HtmlForm htmlForm = new HtmlForm();
		htmlForm.onSave(incomingToExisting);
	}
}