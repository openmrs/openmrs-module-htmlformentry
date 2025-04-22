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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class that saves or updates and HtmlForm based on an xml definition file on the file
 * system
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
	
	public static final String FORM_RESOURCE_PREFIX = "resource";
	
	public static final String FORM_RESOURCE_DELIMITER = "-";
	
	public static final String FORM_RESOURCE_NAME = "name";
	
	public static final String FORM_RESOURCE_VALUE = "value";
	
	public static final String FORM_RESOURCE_DATA_TYPE_CLASS = "datatype-class";
	
	public static final String FORM_RESOURCE_DATA_TYPE_CONFIG = "datatype-config";
	
	public static final String FORM_RESOURCE_HANDLER_CLASS = "handler-class";
	
	public static final String FORM_RESOURCE_HANDLER_CONFIG = "handler-config";
	
	@Autowired
	private FormService formService;
	
	@Autowired
	private HtmlFormEntryService htmlFormEntryService;
	
	/**
	 * For the given form definition file, parse it and update a Form, HtmlForm, and any related
	 * FormResources
	 */
	public HtmlForm saveHtmlForm(File file) {
		String xmlData;
		try {
			xmlData = FileUtils.readFileToString(file, "UTF-8");
			return saveHtmlForm(xmlData);
		}
		catch (Exception e) {
			throw new RuntimeException("Error reading file: " + file.getAbsolutePath(), e);
		}
	}
	
	/**
	 * For the given form xml, parse this into an htmlform and save
	 */
	public HtmlForm saveHtmlForm(String xmlData) {
		boolean isNewForm = false;
		boolean hasChanges = false;
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
		
		// Form resources are defined via attributes that start with resource-<unique-key>-<resource-property>
		
		Map<String, String> formResourceNamesToAttributePrefixes = new LinkedHashMap<>();
		for (String attribute : htmlFormAttributes.keySet()) {
			if (attribute.startsWith(FORM_RESOURCE_PREFIX + FORM_RESOURCE_DELIMITER)) {
				String[] parts = attribute.split(FORM_RESOURCE_DELIMITER, 3);
				if (parts.length < 3) {
					throw new RuntimeException("Could not parse form resource attribute: " + attribute);
				}
				if (parts[2].equals(FORM_RESOURCE_NAME)) {
					String prefix = parts[0] + FORM_RESOURCE_DELIMITER + parts[1] + FORM_RESOURCE_DELIMITER;
					formResourceNamesToAttributePrefixes.put(htmlFormAttributes.get(attribute), prefix);
				}
			}
		}
		
		Map<String, FormResource> resourcesByName = new HashMap<>();
		if (!isNewForm) {
			for (FormResource formResource : formService.getFormResourcesForForm(form)) {
				if (formResourceNamesToAttributePrefixes.containsKey(formResource.getName())) {
					resourcesByName.put(formResource.getName(), formResource);
				} else {
					hasChanges = true;
				}
			}
		}
		
		for (String resourceName : formResourceNamesToAttributePrefixes.keySet()) {
			String prefix = formResourceNamesToAttributePrefixes.get(resourceName);
			htmlFormAttributes.remove(prefix + FORM_RESOURCE_NAME);
			String resourceValue = htmlFormAttributes.remove(prefix + FORM_RESOURCE_VALUE);
			String resourceDataTypeClass = htmlFormAttributes.remove(prefix + FORM_RESOURCE_DATA_TYPE_CLASS);
			String resourceDataTypeConfig = htmlFormAttributes.remove(prefix + FORM_RESOURCE_DATA_TYPE_CONFIG);
			String resourceHandlerClass = htmlFormAttributes.remove(prefix + FORM_RESOURCE_HANDLER_CLASS);
			String resourceHandlerConfig = htmlFormAttributes.remove(prefix + FORM_RESOURCE_HANDLER_CONFIG);
			
			if (StringUtils.isBlank(resourceDataTypeClass)) {
				resourceDataTypeClass = FreeTextDatatype.class.getName();
			}
			
			FormResource resource = resourcesByName.get(resourceName);
			if (resource == null) {
				resource = new FormResource();
				resource.setName(resourceName);
				resource.setForm(form);
				resource.setValueReferenceInternal(resourceValue);
				resource.setDatatypeClassname(resourceDataTypeClass);
				resource.setDatatypeConfig(resourceDataTypeConfig);
				resource.setPreferredHandlerClassname(resourceHandlerClass);
				resource.setHandlerConfig(resourceHandlerConfig);
				resourcesByName.put(resourceName, resource);
				hasChanges = true;
			} else {
				if (!resourceValue.equals(resource.getValueReference())) {
					resource.setValueReferenceInternal(resourceValue);
					hasChanges = true;
				}
				if (!resourceValue.equals(resource.getValueReference())) {
					resource.setValueReferenceInternal(resourceValue);
					hasChanges = true;
				}
				if (!OpenmrsUtil.nullSafeEquals(resource.getDatatypeClassname(), resourceDataTypeClass)) {
					resource.setDatatypeClassname(resourceDataTypeClass);
					hasChanges = true;
				}
				if (!OpenmrsUtil.nullSafeEquals(resource.getDatatypeConfig(), resourceDataTypeConfig)) {
					resource.setDatatypeConfig(resourceDataTypeConfig);
					hasChanges = true;
				}
				if (!OpenmrsUtil.nullSafeEquals(resource.getPreferredHandlerClassname(), resourceHandlerClass)) {
					resource.setPreferredHandlerClassname(resourceHandlerClass);
					hasChanges = true;
				}
				if (!OpenmrsUtil.nullSafeEquals(resource.getHandlerConfig(), resourceHandlerConfig)) {
					resource.setHandlerConfig(resourceHandlerConfig);
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
