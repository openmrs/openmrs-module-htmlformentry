package org.openmrs.module.htmlformentry;


import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class HtmlFormEntryServiceTest extends BaseModuleContextSensitiveTest {

	private HtmlFormEntryService service;
	
	@Before
	public void before() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/HtmlFormEntryService-data.xml");
		service = Context.getService(HtmlFormEntryService.class);
	}
	
	/**
	 * @see {@link HtmlFormEntryService#getAllHtmlForms()}
	 */
	@Test
	@Verifies(value = "should return all html forms", method = "getAllHtmlForms()")
	public void getAllHtmlForms_shouldReturnAllHtmlForms() throws Exception {
		Assert.assertEquals(2, service.getAllHtmlForms().size());
	}
}