package org.openmrs.module.htmlformentry;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openmrs.FormResource;
import org.openmrs.api.FormService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class HtmlFormFromFileLoaderTest extends BaseHtmlFormEntryTest {
	
	@Autowired
	private HtmlFormFromFileLoader htmlFormFromFileLoader;

	@Autowired
	FormService formService;

	@Autowired
	private HtmlFormEntryService htmlFormEntryService;

	// Expected values from form xml
	String formUuid = "203fa4f8-28f3-11eb-bc37-0242ac110002";
	String formName = "Test Form 1";
	String formDescription = "Test Form With All Attributes";
	String formVersion = "1.3";
	String formEncounterType = "61ae96f4-6afe-4351-b6f8-cd4fc383cce1";
	String htmlformUuid = "26ddfe02-28f3-11eb-bc37-0242ac110002";
	String renderPageValue = "standardUi";
	String categoryValue = "Test Forms";

	private File getFormFile(String resourcePath) {
		String path = Objects.requireNonNull(getClass().getClassLoader().getResource(resourcePath)).getPath();
		return new File(path);
	}
	
	@Test
	public void saveHtmlForm_shouldSaveNewForm() throws Exception {
		assertThat(formService.getForm(formUuid), nullValue());
		assertThat(htmlFormEntryService.getHtmlFormByUuid(htmlformUuid), nullValue());
		File formFile = getFormFile("org/openmrs/module/htmlformentry/htmlFormFromFile1.xml");
		htmlFormFromFileLoader.saveHtmlForm(formFile);
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
		assertThat(f.getXmlData().trim(), equalTo(FileUtils.readFileToString(formFile, "UTF-8").trim()));
		assertThat(formService.getFormResourcesForForm(f.getForm()).size(), equalTo(2));
		FormResource renderPage = formService.getFormResource(f.getForm(), "renderPage");
		assertThat(renderPage, notNullValue());
		assertThat(renderPage.getValueReference(), equalTo(renderPageValue));
		FormResource category = formService.getFormResource(f.getForm(), "category");
		assertThat(category, notNullValue());
		assertThat(category.getValueReference(), equalTo(categoryValue));
	}

	@Test
	public void saveHtmlForm_shouldUpdateForm() throws Exception {
		assertThat(formService.getForm(formUuid), nullValue());
		assertThat(htmlFormEntryService.getHtmlFormByUuid(htmlformUuid), nullValue());
		File formFile = getFormFile("org/openmrs/module/htmlformentry/htmlFormFromFile1.xml");
		htmlFormFromFileLoader.saveHtmlForm(formFile);
		File updatedFormFile = getFormFile("org/openmrs/module/htmlformentry/htmlFormFromFile2.xml");
		htmlFormFromFileLoader.saveHtmlForm(updatedFormFile);
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
		assertThat(f.getXmlData().trim(), equalTo(FileUtils.readFileToString(updatedFormFile, "UTF-8").trim()));
		assertThat(formService.getFormResourcesForForm(f.getForm()).size(), equalTo(2));
		FormResource renderPage = formService.getFormResource(f.getForm(), "renderPage");
		assertThat(renderPage, notNullValue());
		assertThat(renderPage.getValueReference(), equalTo("simpleUi"));
		FormResource category = formService.getFormResource(f.getForm(), "category");
		assertThat(category, nullValue());
		FormResource condition = formService.getFormResource(f.getForm(), "condition");
		assertThat(condition, notNullValue());
		assertThat(condition.getValueReference(), equalTo("${patient.gender === 'F'}"));
	}
}
