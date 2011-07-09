package org.openmrs.module.htmlformentry.schema;


import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.schema.FormSchemaUpdater;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class FormSchemaUpdaterTest extends BaseModuleContextSensitiveTest {
	
	private HtmlFormEntryService htmlService;
	
	
	@Before
	public void before() throws Exception {
		executeDataSet("test/org/openmrs/module/htmlformentry/include/FormSchemaUpdaterTest-data.xml");
		htmlService = Context.getService(HtmlFormEntryService.class);
	}

	/**
	 * @see FormSchemaUpdater#updateSchema()
	 * @verifies add new form fields if they don't exist in the schema
	 */
	@Test
	public void updateSchema_shouldAddNewFormFieldsIfTheyDontExistInTheSchema()
			throws Exception {   
        HtmlForm htmlForm = htmlService.getHtmlForm(1);
        
        int count = htmlForm.getForm().getFormFields().size();

        htmlForm.setXmlData(new TestUtil().loadXmlFromFile("test/org/openmrs/module/htmlformentry/include/UpdateSchemaBaseHtmlForm.xml"));
        
        htmlService.saveHtmlForm(htmlForm);
 
        int newCount = htmlForm.getForm().getFormFields().size();
        
        Assert.assertEquals(count+1, newCount);        
   	}

	/**
	 * @see FormSchemaUpdater#updateSchema()
	 * @verifies create an entire form schema from the html code
	 */
	@Test
	public void updateSchema_shouldCreateAnEntireFormSchemaFromTheHtmlCode()
			throws Exception {
		//TODO auto-generated
		Assert.fail("Not yet implemented");
	}

	/**
	 * @see FormSchemaUpdater#updateSchema()
	 * @verifies not remove any created form fields
	 */
	@Test
	public void updateSchema_shouldNotRemoveAnyCreatedFormFields()
			throws Exception {
		//TODO auto-generated
		Assert.fail("Not yet implemented");
	}
}