package org.openmrs.module.htmlformentry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.Role;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.module.htmlformentry.handler.TagHandler;

/**
 * A clone of an HtmlForm, intended to be used by the Metadata sharing module. The clone includes a
 * list of OpenmrsObjects references in the form xml so that the Metadata sharing module knows which
 * other Openmrs objects it needs to package up with the form.
 */
public class ShareableHtmlForm extends HtmlForm {
	
	private static Log log = LogFactory.getLog(ShareableHtmlForm.class);
	
	private Collection<OpenmrsObject> dependencies;
	
	private Boolean includeLocations;
	
	private Boolean includePersons;
	
	private Boolean includeRoles;
	
	private Boolean includePatientIdentifierTypes;
	
	public ShareableHtmlForm(HtmlForm form, Boolean includeLocations, Boolean includePersons, Boolean includeRoles, Boolean includePatientIdentifierTypes) {
		// first, make a clone of the form
		// (do we need to worry about pass-by-reference?)
		this.setChangedBy(form.getChangedBy());
		this.setCreator(form.getCreator());
		this.setDateChanged(form.getDateChanged());
		this.setDateCreated(form.getDateCreated());
		this.setDateRetired(form.getDateRetired());
		this.setForm(form.getForm());
		this.setId(form.getId());
		this.setRetired(form.getRetired());
		this.setRetiredBy(form.getRetiredBy());
		this.setRetireReason(form.getRetireReason());
		this.setUuid(form.getUuid());
		this.setXmlData(form.getXmlData());
		
		// set the parameters
		this.includeLocations = includeLocations;
		this.includePersons = includePersons;
		this.includeRoles = includeRoles;
		this.includePatientIdentifierTypes = includePatientIdentifierTypes;
		
		// first, strip out any local attributes we don't want to pass on
		// default (set in HtmlForm.java) is to include locations, roles, and patient identitfier types, but not persons
		stripLocalAttributesFromXml();
		
		// within the form, replace any Ids with Uuids
		HtmlFormEntryUtil.replaceIdsWithUuids(this);
		
		// make sure all dependent OpenmrsObjects are loaded and explicitly referenced
		calculateDependencies();
		// TODO: update the calculate dependencies method to handle persons specified by name...
	}
	
	/** Allows HtmlForm to be shared via Metadata Sharing Module **/
	protected HtmlForm saveReplace() {
		HtmlForm form = new HtmlForm();
		form.setChangedBy(getChangedBy());
		form.setCreator(getCreator());
		form.setDateChanged(getDateChanged());
		form.setDateCreated(getDateCreated());
		form.setDateRetired(getDateRetired());
		form.setDescription(getDescription());
		form.setForm(getForm());
		form.setId(getId());
		form.setName(getName());
		form.setRetired(getRetired());
		form.setRetiredBy(getRetiredBy());
		form.setRetireReason(getRetireReason());
		form.setUuid(getUuid());
		form.setXmlData(getXmlData());
		return form;
	}
	
	public void setDependencies(Collection<OpenmrsObject> dependencies) {
		this.dependencies = dependencies;
	}
	
	public Collection<OpenmrsObject> getDependencies() {
		return this.dependencies;
	}

	@SuppressWarnings("unchecked")
    public void calculateDependencies() {
		
		this.dependencies = new HashSet<OpenmrsObject>();
		
		// we to resolve any macros or repeat/renders first, but we *don't* want these changes to 
		// be applied to the form we are exporting so we copy the xml into a new string first
		// (calculate Uuid dependencies should operate properly even with out this, but will be do this just to be safe)
		
		String xml = new String(this.getXmlData()); 
		HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator(); 
		
		try {
			xml = generator.applyMacros(xml);
			xml = generator.applyTemplates(xml);
		}
		catch (Exception e) {
			throw new APIException ("Unable to process macros and templates when processing form to make it shareable", e);
		}
		
		// now we need to loop through the attributes to find what other (non-uuid) dependencies we need to look for
		// fetch the tag handlers so we can gain access to the attribute descriptors
		Map<String, TagHandler> tagHandlers = Context.getService(HtmlFormEntryService.class).getHandlers();
		
		// loop through all the attribute descriptors for the registered handlers
		for (String tagName : tagHandlers.keySet()) {
			log.debug("Handling dependencies for tag " + tagName);
			
			if (tagHandlers.get(tagName).getAttributeDescriptors() != null) {
				for (AttributeDescriptor attributeDescriptor : tagHandlers.get(tagName).getAttributeDescriptors()) {
					if (attributeDescriptor.getClazz() != null) {
						// build the attribute string we are searching for
						// pattern matches <tagName .* attribute="[anything]"; group(1) is set to [anything]
						String pattern = "<" + tagName + "[^>]*" + attributeDescriptor.getName() + "=\"(.*?)\"";
						log.debug("dependency substitution pattern: " + pattern);
						
						// now search through and find all matches
						Matcher matcher = Pattern.compile(pattern).matcher(xml);
						while (matcher.find()) {
							// split the matched result group into the various ids
							String[] ids = matcher.group(1).split(",");
							
							for (String id : ids) {
								// if this id matches a uuid pattern, try to fetch the object by uuid
								if(Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(id).matches()){
									OpenmrsObject object = Context.getService(HtmlFormEntryService.class).getItemByUuid(attributeDescriptor.getClazz(), id);
									if (object != null) {
										this.dependencies.add(object);
										continue;
									}
								}
								
								// if we haven't found anything by uuid, try by name
								if (OpenmrsMetadata.class.isAssignableFrom(attributeDescriptor.getClazz())) { 
									OpenmrsObject object = Context.getService(HtmlFormEntryService.class).getItemByName((Class<? extends OpenmrsMetadata>) attributeDescriptor.getClazz(), id);
									if (object != null) {
										this.dependencies.add(object);
										continue;
									}
								}								
				
								// finally, handle any special cases
								// if it's a concept, we also need to handle concepts referenced by map
								if (Concept.class.equals(attributeDescriptor.getClazz())) {
									Concept concept = HtmlFormEntryUtil.getConcept(id);
									if (concept != null) {
										this.dependencies.add(concept);
										continue;
									}
								}
								// need to handle the special case where a program "name" is considered the "name" of the underlying concept
								if (Program.class.equals(attributeDescriptor.getClazz())) {
									Program program = HtmlFormEntryUtil.getProgram(id);
									if (program != null) {
										this.dependencies.add(program);
										continue;
									}
								}
								// need to special case of the name of a role
								if (Role.class.equals(attributeDescriptor.getClazz())) {
									Role role = Context.getUserService().getRole(id);
									if (role != null) {
										this.dependencies.add(role);
										continue;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void stripLocalAttributesFromXml() {
		// get the tag handlers so we can gain access to the attribute descriptors
		Map<String, TagHandler> tagHandlers = Context.getService(HtmlFormEntryService.class).getHandlers();
		
		// loop through all the attribute descriptors for this
		for (String tagName : tagHandlers.keySet()) {
			log.debug("Handling dependencies for tag " + tagName);
			
			if (tagHandlers.get(tagName).getAttributeDescriptors() != null) {
				for (AttributeDescriptor attributeDescriptor : tagHandlers.get(tagName).getAttributeDescriptors()) {
					if (attributeDescriptor.getClazz() != null) {
						// build the attribute string we are searching for
						// pattern matches <tagName .* attribute="[anything]"; group(1) is set to attribute="[anything]"
						String stripPattern = "<" + tagName + "[^>]*( " + attributeDescriptor.getName() + "=\".*?\")";
						log.debug("stripping substitution pattern: " + stripPattern);
						
						if (!this.includeLocations && attributeDescriptor.getClazz().equals(Location.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern));
						}
						else if (!this.includePersons && attributeDescriptor.getClazz().equals(Person.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern));
						}
						else if (!this.includeRoles && attributeDescriptor.getClazz().equals(Role.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern));
						}
						else if (!this.includePatientIdentifierTypes && attributeDescriptor.getClazz().equals(PatientIdentifierType.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern));
						}
					}
				}
			}
		}
	}
	
	private void stripLocalAttributesFromXmlHelper(Pattern pattern) {
		
		Matcher matcher = pattern.matcher(this.getXmlData());
		StringBuffer buffer = new StringBuffer();
		
		// search through the xml data for the Pattern specified in the pattern parameter, and remove group(1)
		while (matcher.find()) {
			matcher.appendReplacement(buffer, matcher.group().substring(0, matcher.start(1) - matcher.start()));
		}
		
		matcher.appendTail(buffer);
		
		this.setXmlData(buffer.toString());
	}
}
