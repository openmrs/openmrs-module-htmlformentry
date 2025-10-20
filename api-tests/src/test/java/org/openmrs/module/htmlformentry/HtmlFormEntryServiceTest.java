package org.openmrs.module.htmlformentry;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.FormResource;
import org.openmrs.Patient;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.customdatatype.datatype.RegexValidatedTextDatatype;
import org.openmrs.module.htmlformentry.element.PersonStub;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsClassLoader;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class HtmlFormEntryServiceTest extends BaseHtmlFormEntryTest {
	
	@Autowired
	private HtmlFormEntryService htmlFormEntryService;
	
	@Autowired
	FormService formService;
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
		htmlFormEntryService.clearConceptMappingCache();
	}
	
	/**
	 * @see {@link HtmlFormEntryService#getAllHtmlForms()}
	 */
	@Test
	@Verifies(value = "should return all html forms", method = "getAllHtmlForms()")
	public void getAllHtmlForms_shouldReturnAllHtmlForms() throws Exception {
		Assert.assertEquals(2, htmlFormEntryService.getAllHtmlForms().size());
	}
	
	/**
	 * @see {@link HtmlFormEntryService#getHtmlFormByUuid(String)}
	 */
	@Test
	@Verifies(value = "should return the HtmlForm with the given uuid", method = "getHtmlFormByUuid()")
	public void getHtmlFormByUuid_shouldReturnTheHtmlFormWithTheGivenUuid() throws Exception {
		Assert.assertNotNull(htmlFormEntryService.getHtmlFormByUuid("12e41146-e162-11df-9195-001e378eb67e"));
	}
	
	/**
	 * @see {@link HtmlFormEntryService#getUsersAsPersonStubs(String)}
	 */
	@Test
	@Verifies(value = "should return all ProviderStubs", method = "getProviderStub()")
	public void getProviderStub_shouldReturnAllProviderStubs() throws Exception {
		Assert.assertEquals(1, htmlFormEntryService.getUsersAsPersonStubs("Provider").size());
		//make sure getDisplayValue is working:
		Assert.assertEquals("Hippocrates of Cos",
		    htmlFormEntryService.getUsersAsPersonStubs("Provider").get(0).getDisplayValue());
		Assert.assertEquals(0, htmlFormEntryService.getUsersAsPersonStubs("Clinician").size());
		
		//Create the Clinician role and add to the existing user, and see if user is returned.
		Integer userId = htmlFormEntryService.getUsersAsPersonStubs("Provider").get(0).getId();
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
		user.getPersonName().setFamilyName("UserFamily");
		user.getPersonName().setGivenName("UserGiven");
		user.getPersonName().setMiddleName("middleName");
		Context.getUserService().saveUser(user);
		Assert.assertEquals(1, htmlFormEntryService.getUsersAsPersonStubs("Clinician").size());
		
		//lets look at the PersonStub for the Clinician:
		PersonStub ps = htmlFormEntryService.getUsersAsPersonStubs("Clinician").get(0);
		Assert.assertEquals("UserGiven", ps.getGivenName());
		Assert.assertEquals("UserFamily", ps.getFamilyName());
		Assert.assertEquals("middleName", ps.getMiddleName());
		Assert.assertNotNull(ps.getId());
		
	}
	
	@Test
	@Verifies(value = "Should save archived form to the database", method = "reprocessArchivedForm")
	public void reprocessArchivedForm_shouldProcessForm() throws Exception {
		EncounterService encService = Context.getEncounterService();
		
		String path = OpenmrsClassLoader.getInstance().getResource("archivedFormData.xml").getPath();
		System.out.println("Path: " + path);
		
		//Get the SerializableFormObject
		SerializableFormObject formObject = SerializableFormObject.deserializeXml(path);
		Assert.assertEquals("da7f524f-27ce-4bb2-86d6-6d1d05312bd5", formObject.getPatientUuid());
		
		Patient patient = Context.getPatientService().getPatientByUuid("da7f524f-27ce-4bb2-86d6-6d1d05312bd5");
		
		int noEnc = encService.getEncountersByPatient(patient).size();
		htmlFormEntryService.reprocessArchivedForm(path);
		int newNoEnc = encService.getEncountersByPatient(patient).size();
		
		Assert.assertEquals(noEnc + 1, newNoEnc);
	}
	
	@Test
	public void getConceptByMapping_shouldRetrieveConceptByMapping() throws Exception {
		Concept concept = htmlFormEntryService.getConceptByMapping("XYZ:HT");
		Assert.assertEquals(5090, concept.getConceptId().intValue());
	}
	
	@Test
	public void getConceptByMapping_shouldReturnNullIfInvalidMappingSpecified() throws Exception {
		Concept concept = htmlFormEntryService.getConceptByMapping("XYZ-HT");
		Assert.assertNull(concept);
		concept = htmlFormEntryService.getConceptByMapping("XYZ123:HT");
		Assert.assertNull(concept);
	}
	
	@Test
	public void getStartingFormXml_shouldGetBasicFormXmlTemplate() throws Exception {
		String xmlTemplate = htmlFormEntryService.getStartingFormXml(new HtmlForm());
		Assert.assertTrue(xmlTemplate.contains("2. Demographic Information"));
	}
	
	@Test
	public void saveHtmlFormFromXml_shouldSaveNewForm() throws Exception {
		String formUuid = "203fa4f8-28f3-11eb-bc37-0242ac110002";
		String formName = "Test Form 1";
		String formDescription = "Test Form With All Attributes";
		String formVersion = "1.3";
		String formEncounterType = "61ae96f4-6afe-4351-b6f8-cd4fc383cce1";
		String htmlformUuid = "26ddfe02-28f3-11eb-bc37-0242ac110002";
		String renderPageValue = "standardUi";
		String categoryValue = "Test Forms";
		assertThat(formService.getForm(formUuid), nullValue());
		assertThat(htmlFormEntryService.getHtmlFormByUuid(htmlformUuid), nullValue());
		String formXml = getFormXml("org/openmrs/module/htmlformentry/htmlFormFromFile1.xml");
		htmlFormEntryService.saveHtmlFormFromXml(formXml);
		HtmlForm f = htmlFormEntryService.getHtmlFormByUuid(htmlformUuid);
		assertThat(f, notNullValue());
		assertThat(f.getForm().getUuid(), equalTo(formUuid));
		assertThat(f.getName(), equalTo(formName));
		assertThat(f.getDescription(), equalTo(formDescription));
		assertThat(f.getForm().getVersion(), equalTo(formVersion));
		assertThat(f.getForm().getPublished(), equalTo(true));
		assertThat(f.getForm().getRetired(), equalTo(false));
		assertThat(f.getRetired(), equalTo(false));
		assertThat(f.getForm().getEncounterType().getUuid(), equalTo(formEncounterType));
		assertThat(f.getXmlData().trim(), equalTo(formXml.trim()));
		assertThat(formService.getFormResourcesForForm(f.getForm()).size(), equalTo(3));
		FormResource renderPage = formService.getFormResource(f.getForm(), "renderPage");
		assertThat(renderPage, notNullValue());
		assertThat(renderPage.getValueReference(), equalTo(renderPageValue));
		assertThat(renderPage.getDatatypeClassname(), equalTo(FreeTextDatatype.class.getName()));
		FormResource category = formService.getFormResource(f.getForm(), "category");
		assertThat(category, notNullValue());
		assertThat(category.getValueReference(), equalTo(categoryValue));
		assertThat(category.getDatatypeClassname(), equalTo(FreeTextDatatype.class.getName()));
		FormResource maxNum = formService.getFormResource(f.getForm(), "maxNumberPerDay");
		assertThat(maxNum, notNullValue());
		assertThat(maxNum.getValueReference(), equalTo("2"));
		assertThat(maxNum.getDatatypeClassname(), equalTo(RegexValidatedTextDatatype.class.getName()));
		assertThat(maxNum.getDatatypeConfig(), equalTo("^\\\\d+$"));
		assertThat(maxNum.getPreferredHandlerClassname(), equalTo(TestDataTypeHandler.class.getName()));
		assertThat(maxNum.getHandlerConfig(), equalTo("xxx"));
	}
	
	@Test
	public void saveHtmlForm_shouldUpdateForm() throws Exception {
		String formUuid = "203fa4f8-28f3-11eb-bc37-0242ac110002";
		String formEncounterType = "61ae96f4-6afe-4351-b6f8-cd4fc383cce1";
		String htmlformUuid = "26ddfe02-28f3-11eb-bc37-0242ac110002";
		assertThat(formService.getForm(formUuid), nullValue());
		assertThat(htmlFormEntryService.getHtmlFormByUuid(htmlformUuid), nullValue());
		String formXml = getFormXml("org/openmrs/module/htmlformentry/htmlFormFromFile1.xml");
		htmlFormEntryService.saveHtmlFormFromXml(formXml);
		String updatedXml = getFormXml("org/openmrs/module/htmlformentry/htmlFormFromFile2.xml");
		htmlFormEntryService.saveHtmlFormFromXml(updatedXml);
		HtmlForm f = htmlFormEntryService.getHtmlFormByUuid(htmlformUuid);
		assertThat(f, notNullValue());
		assertThat(f.getForm().getUuid(), equalTo(formUuid));
		assertThat(f.getName(), equalTo("Test Form 2"));
		assertThat(f.getDescription(), equalTo("Test Form With All Attributes Modified"));
		assertThat(f.getForm().getVersion(), equalTo("2.0"));
		assertThat(f.getForm().getPublished(), equalTo(true));
		assertThat(f.getForm().getRetired(), equalTo(false));
		assertThat(f.getRetired(), equalTo(false));
		assertThat(f.getForm().getEncounterType().getUuid(), equalTo(formEncounterType));
		assertThat(f.getXmlData().trim(), equalTo(updatedXml.trim()));
		assertThat(formService.getFormResourcesForForm(f.getForm()).size(), equalTo(3));
		// Test changed resource value
		FormResource renderPage = formService.getFormResource(f.getForm(), "renderPage");
		assertThat(renderPage, notNullValue());
		assertThat(renderPage.getValueReference(), equalTo("simpleUi"));
		// Test existing resource that is not present in the form is left alone
		FormResource category = formService.getFormResource(f.getForm(), "category");
		assertThat(category, notNullValue());
		assertThat(category.getValueReference(), equalTo("Test Forms"));
		// Test existing resource that is present with no value is deleted
		FormResource maxNum = formService.getFormResource(f.getForm(), "maxNumberPerDay");
		assertThat(maxNum, nullValue());
		// Test new resource is added
		FormResource condition = formService.getFormResource(f.getForm(), "condition");
		assertThat(condition, notNullValue());
		assertThat(condition.getValueReference(), equalTo("${patient.gender === 'F'}"));
	}
	
	private String getFormXml(String resourcePath) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			return IOUtils.toString(Objects.requireNonNull(in), StandardCharsets.UTF_8);
		}
	}
}
