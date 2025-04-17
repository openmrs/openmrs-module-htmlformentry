package org.openmrs.module.htmlformentry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.api.FormService;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that saves or updates and HtmlForm based on an xml definition file on the file system
 */
@Component
public class HtmlFormFromFileLoader {

	public static final String HTML_FORM_TAG = "htmlform";

	public static final String FORM_UUID_ATTRIBUTE = "formUuid";

	public static final String FORM_NAME_ATTRIBUTE = "formName";

	public static final String FORM_DESCRIPTION_ATTRIBUTE = "formDescription";

	public static final String FORM_VERSION_ATTRIBUTE = "formVersion";

	public static final String FORM_PUBLISHED_ATTRIBUTE = "formPublished";

	public static final String FORM_RETIRED_ATTRIBUTE = "formRetired";

	public static final String FORM_ENCOUNTER_TYPE_ATTRIBUTE = "formEncounterType";

	public static final String HTML_FORM_UUID_ATTRIBUTE = "htmlformUuid";

	@Autowired
	private FormService formService;

	@Autowired
	private HtmlFormEntryService htmlFormEntryService;

	/**
	 * For the given form definition located at the given path relative to the application data directory,
	 * parse it and update a Form, HtmlForm, and any related FormResources
	 */
	public HtmlForm saveHtmlForm(String filePathRelativeToAppDataDir) {
		File formFile = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), filePathRelativeToAppDataDir).toFile();
		if (!formFile.exists()) {
			throw new RuntimeException("Form file does not exist: " + formFile.getAbsolutePath());
		}
		return saveHtmlForm(formFile);
	}

	/**
	 * For the given form definition file,
	 * parse it and update a Form, HtmlForm, and any related FormResources
	 */
	public HtmlForm saveHtmlForm(File file) {
		boolean isNewForm = false;
		boolean hasChanges = false;
		String xmlData;
		try {
			xmlData = FileUtils.readFileToString(file, "UTF-8");
		}
		catch (Exception e) {
			throw new RuntimeException("Error reading file: " + file.getAbsolutePath(), e);
		}
		Document doc;
		try {
			doc = HtmlFormEntryUtil.stringToDocument(xmlData);
		}
		catch (Exception e) {
			throw new RuntimeException("Error parsing XML: " + xmlData, e);
		}
		Node htmlFormNode = HtmlFormEntryUtil.findChild(doc, HTML_FORM_TAG);
		if (htmlFormNode == null) {
			throw new RuntimeException("Could not find tag: " + HTML_FORM_TAG);
		}

		Map<String, String> htmlFormAttributes = HtmlFormEntryUtil.getNodeAttributes(htmlFormNode);

		// Create or update the form

		String formUuid = htmlFormAttributes.remove(FORM_UUID_ATTRIBUTE);
		if (formUuid == null) {
			throw new IllegalArgumentException(FORM_UUID_ATTRIBUTE + " is required");
		}
		Form form = formService.getFormByUuid(formUuid);
		if (form == null) {
			form = new Form();
			form.setUuid(formUuid);
			isNewForm = true;
			hasChanges = true;
		}
		String formName = htmlFormAttributes.remove(FORM_NAME_ATTRIBUTE);
		if (!OpenmrsUtil.nullSafeEquals(form.getName(), formName)) {
			form.setName(formName);
			hasChanges = true;
		}
		String formDescription = htmlFormAttributes.remove(FORM_DESCRIPTION_ATTRIBUTE);
		if (!OpenmrsUtil.nullSafeEquals(form.getDescription(), formDescription)) {
			form.setDescription(formDescription);
			hasChanges = true;
		}
		String formVersion = htmlFormAttributes.remove(FORM_VERSION_ATTRIBUTE);
		if (!OpenmrsUtil.nullSafeEquals(form.getVersion(), formVersion)) {
			form.setVersion(formVersion);
			hasChanges = true;
		}
		Boolean formPublished = "true".equalsIgnoreCase(htmlFormAttributes.remove(FORM_PUBLISHED_ATTRIBUTE));
		if (!OpenmrsUtil.nullSafeEquals(form.getPublished(), formPublished)) {
			form.setPublished(formPublished);
			hasChanges = true;
		}
		Boolean formRetired = "true".equalsIgnoreCase(htmlFormAttributes.remove(FORM_RETIRED_ATTRIBUTE));
		if (!OpenmrsUtil.nullSafeEquals(form.getRetired(), formRetired)) {
			form.setRetired(formRetired);
			if (formRetired && StringUtils.isBlank(form.getRetireReason())) {
				form.setRetireReason("Retired set in form xml");
			}
			hasChanges = true;
		}
		String formEncounterType = htmlFormAttributes.remove(FORM_ENCOUNTER_TYPE_ATTRIBUTE);
		EncounterType encounterType = null;
		if (formEncounterType != null) {
			encounterType = HtmlFormEntryUtil.getEncounterType(formEncounterType);
		}
		if (encounterType != null && !OpenmrsUtil.nullSafeEquals(form.getEncounterType(), encounterType)) {
			form.setEncounterType(encounterType);
			hasChanges = true;
		}

		// Create or update the htmlform

		HtmlForm htmlForm = (isNewForm ? null : htmlFormEntryService.getHtmlFormByForm(form));
		if (htmlForm == null) {
			htmlForm = new HtmlForm();
			hasChanges = true;
		}
		if (hasChanges) {
			htmlForm.setForm(form);
		}

		// if there is a html form uuid specified, make sure the htmlform uuid is set to that value
		String htmlformUuid = htmlFormAttributes.remove(HTML_FORM_UUID_ATTRIBUTE);
		if (StringUtils.isNotBlank(htmlformUuid) && !OpenmrsUtil.nullSafeEquals(htmlformUuid, htmlForm.getUuid())) {
			htmlForm.setUuid(htmlformUuid);
			hasChanges = true;
		}

		if (!OpenmrsUtil.nullSafeEquals(htmlForm.getRetired(), formRetired)) {
			htmlForm.setRetired(formRetired);
			if (formRetired && StringUtils.isBlank(htmlForm.getRetireReason())) {
				htmlForm.setRetireReason("Retired set in form xml");
			}
			hasChanges = true;
		}

		if (!StringUtils.trimToEmpty(htmlForm.getXmlData()).equals(StringUtils.trimToEmpty(xmlData))) {
			// trim because if the file ends with a newline the db will have trimmed it
			htmlForm.setXmlData(xmlData);
			hasChanges = true;
		}

		// Remaining form attributes saved as form resources

		Map<String, FormResource> resourcesByName = new HashMap<>();
		if (!isNewForm) {
			for (FormResource formResource : formService.getFormResourcesForForm(form)) {
				if (htmlFormAttributes.containsKey(formResource.getName())) {
					resourcesByName.put(formResource.getName(), formResource);
				}
				else {
					hasChanges = true;
				}
			}
		}
		for (String attributeName : htmlFormAttributes.keySet()) {
			String attributeValue = htmlFormAttributes.get(attributeName);
			FormResource resource = resourcesByName.get(attributeName);
			if (resource == null) {
				resource = new FormResource();
				resource.setName(attributeName);
				resource.setForm(form);
				resource.setValueReferenceInternal(attributeValue);
				resource.setDatatypeClassname(FreeTextDatatype.class.getName());
				resourcesByName.put(attributeName, resource);
				hasChanges = true;
			}
			else {
				if (!attributeValue.equals(resource.getValueReference())) {
					resource.setValueReferenceInternal(attributeValue);
					hasChanges = true;
				}
			}
		}

		if (!hasChanges) {
			return htmlForm;
		}
		return htmlFormEntryService.saveHtmlForm(htmlForm, resourcesByName.values());
	}
	
}
