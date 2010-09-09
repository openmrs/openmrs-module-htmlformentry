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
		HtmlForm form = new HtmlForm();
		form.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "metadataSharingTestForm.xml"));
		
		ShareableHtmlForm formClone = new ShareableHtmlForm(form);
		Collection<OpenmrsObject> dependencies = formClone.getDependencies();
		
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
	}
}
