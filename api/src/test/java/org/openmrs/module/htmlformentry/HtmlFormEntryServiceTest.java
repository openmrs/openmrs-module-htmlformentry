package org.openmrs.module.htmlformentry;


import java.util.Date;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.element.PersonStub;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class HtmlFormEntryServiceTest extends BaseModuleContextSensitiveTest {

	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_SERVICE_DATASET = "htmlFormEntryServiceDataSet";
	
	private HtmlFormEntryService service;
	
	@Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_SERVICE_DATASET));
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
	
	/**
     * @see {@link HtmlFormEntryService#getProviderStub()}
     */
    @Test
    @Verifies(value = "should return all ProviderStubs", method = "getProviderStub()")
    public void getProviderStub_shouldReturnAllProviderStubs() throws Exception {
        Assert.assertEquals(1, service.getUsersAsPersonStubs("Provider").size());
        //make sure getDisplayValue is working:
        Assert.assertEquals("Hippocrates of Cos", service.getUsersAsPersonStubs("Provider").get(0).getDisplayValue());
        Assert.assertEquals(0, service.getUsersAsPersonStubs("Clinician").size());
        
        //Create the Clinician role and add to the existing user, and see if user is returned.
        Integer userId = service.getUsersAsPersonStubs("Provider").get(0).getId();
        Role role = new Role();
        role.setUuid(java.util.UUID.randomUUID().toString());
        role.setRole("Clinician");
        role.setRetired(false);
        role.setName("Clinician");
        role.setDateCreated(new Date());
        role.setDescription("boo");
        role.setCreator(Context.getAuthenticatedUser());
        Context.getUserService().saveRole(role);
        User user = Context.getUserService().getUser(userId);
        user.addRole(role);
        user.getPersonName().setFamilyName(null);
        user.getPersonName().setGivenName(null);
        user.getPersonName().setMiddleName("middleName");
        Context.getUserService().saveUser(user, null);
        Assert.assertEquals(1, service.getUsersAsPersonStubs("Clinician").size());
        
        //lets look at the PersonStub for the Clinician:
        PersonStub ps = service.getUsersAsPersonStubs("Clinician").get(0);
        Assert.assertNull(ps.getGivenName());
        Assert.assertNull(ps.getFamilyName());
        Assert.assertNotNull(ps.getId());
 
    }
	
}