package org.openmrs.module.htmlformentry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.RelationshipType;
import org.openmrs.Role;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.compatibility.RegimenSuggestionCompatibility;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.module.htmlformentry.substitution.HtmlFormSubstitutionUtils;

/**
 * HtmlFormExporter intended to be used by the Metadata sharing module. The clone includes a
 * list of OpenmrsObjects references in the form xml so that the Metadata sharing module knows which
 * other Openmrs objects it needs to package up with the form.
 */
public class HtmlFormExporter {
	
	private static Log log = LogFactory.getLog(HtmlFormExporter.class);
	
	private final HtmlForm form;
	
	private Boolean includeLocations;
	
	private Boolean includePersons;
	
	private Boolean includeRoles;
	
	private Boolean includePatientIdentifierTypes;
	
	private HtmlForm formToExport;
	
	public HtmlFormExporter(HtmlForm form) {
		this.form = form;
	}
	
	private HtmlForm copyOf(HtmlForm form) {
		HtmlForm copy = new HtmlForm();
		copy.setChangedBy(form.getChangedBy());
		copy.setCreator(form.getCreator());
		copy.setDateChanged(form.getDateChanged());
		copy.setDateCreated(form.getDateCreated());
		copy.setDateRetired(form.getDateRetired());
		copy.setForm(form.getForm());
		copy.setId(form.getId());
		copy.setRetired(form.getRetired());
		copy.setRetiredBy(form.getRetiredBy());
		copy.setRetireReason(form.getRetireReason());
		copy.setUuid(form.getUuid());
		copy.setXmlData(form.getXmlData());
		return copy;
	}

	private MetadataMappingResolver getMetadataMappingResolver(){
		return Context.getRegisteredComponent("metadataMappingResolver", MetadataMappingResolver.class);
	}
	
	@SuppressWarnings("unchecked")
	private void calculateDependencies() {
		Set<OpenmrsObject> dependencies = new HashSet<OpenmrsObject>();
		
		Set<Class<?>> classesNotToExport = getClassesNotToExport();

		MetadataMappingResolver metadataMappingResolver = getMetadataMappingResolver();

		// we to resolve any macros or repeat/renders first, but we *don't* want these changes to
		// be applied to the form we are exporting so we copy the xml into a new string first
		// (calculate Uuid dependencies should operate properly even with out this, but will be do this just to be safe)
		
		String xml = new String(formToExport.getXmlData());
		HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();
		
		try {
			xml = generator.applyMacros(xml);
			xml = generator.applyRepeats(xml);
		}
		catch (Exception e) {
			throw new APIException("Unable to process macros and templates when processing form to make it shareable", e);
		}
		
		// now we need to loop through the attributes to find what dependencies we need to look for
		// fetch the tag handlers so we can gain access to the attribute descriptors
		Map<String, TagHandler> tagHandlers = Context.getService(HtmlFormEntryService.class).getHandlers();
		
		// loop through all the attribute descriptors for the registered handlers
		for (String tagName : tagHandlers.keySet()) {
			log.debug("Handling dependencies for tag " + tagName);

			if (tagHandlers.get(tagName).getAttributeDescriptors() != null) {
				for (AttributeDescriptor attributeDescriptor : tagHandlers.get(tagName).getAttributeDescriptors()) {
					if (attributeDescriptor.getClazz() != null && !classesNotToExport.contains(attributeDescriptor.getClazz())) {

						// build the attribute string we are searching for
						// pattern matches <tagName .* attribute="[anything]"; group(1) is set to [anything]
						// to break down the regex in detail, ?: simply means that we don't want include this grouping in the groups that we backreference;
						// the grouping itself is an "or", that matches either "\\s" (a single whitespace character) or
						// "\\s[^>]*\\s" (a single whitespace character plus 0 to n characters of any type but a >, followed by another single whitespace character)
						String pattern = "<" + tagName + "(?:\\s|\\s[^>]*\\s)" + attributeDescriptor.getName() + "=\"(.*?)\"";
						log.debug("dependency substitution pattern: " + pattern);
						
						// now search through and find all matches
						Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(xml);
						while (matcher.find()) {
							// split the matched result group into the various ids
							String[] ids = matcher.group(1).split(",");
							
							for (String id : ids) {
								// if this id matches a uuid pattern, try to fetch the object by uuid
								if (HtmlFormEntryUtil.isValidUuidFormat(id) && OpenmrsObject.class.isAssignableFrom(attributeDescriptor.getClazz())) {
									OpenmrsObject object = Context.getService(HtmlFormEntryService.class).getItemByUuid(
									    (Class<? extends OpenmrsObject>) attributeDescriptor.getClazz(), id);
									if (object != null) {
										//special handling of Form -- if passed a Form, see if it can be passed along as  HtmlForm
										if (Form.class.equals(attributeDescriptor.getClazz())) {
											Form form = (Form) object;
											HtmlForm htmlForm = Context.getService(HtmlFormEntryService.class).getHtmlFormByForm(form);
											if (htmlForm != null){
												dependencies.add(htmlForm);
												continue;
											}
										}
										dependencies.add(object);
										continue;
									}
								}

								//if openmrs metadata, try to get it by metadataMapping service
								if(OpenmrsMetadata.class.isAssignableFrom(attributeDescriptor.getClazz())){
									OpenmrsObject object = metadataMappingResolver.getMetadataItem((Class<? extends OpenmrsMetadata>) attributeDescriptor.getClazz(), id);
									if(object != null){
										dependencies.add(object);
										continue;
									}
								}

								// if we haven't found anything by uuid, try by name
								if (OpenmrsMetadata.class.isAssignableFrom(attributeDescriptor.getClazz())) {
									OpenmrsObject object = Context.getService(HtmlFormEntryService.class).getItemByName(
									    (Class<? extends OpenmrsMetadata>) attributeDescriptor.getClazz(), id);
									if (object != null) {
										dependencies.add(object);
										continue;
									}
								}
								// finally, handle any special cases
								// if it's a concept, we also need to handle concepts referenced by map
								if (Concept.class.equals(attributeDescriptor.getClazz())) {
									Concept concept = HtmlFormEntryUtil.getConcept(id);
									if (concept != null) {
										dependencies.add(concept);
										continue;
									}
								}
								// need to handle the special case where a program "name" is considered the "name" of the underlying concept
								if (Program.class.equals(attributeDescriptor.getClazz())) {
									Program program = HtmlFormEntryUtil.getProgram(id);
									if (program != null) {
										dependencies.add(program);
										continue;
									}
								}
								// need to handle the special case where a program workflow is specified by a concept map pointing to it's underlying concept
								// note that we shouldn't have to handle program workflow states because they should always be picked up when sharing the overriding program and/or program workflow
								if (ProgramWorkflow.class.equals(attributeDescriptor.getClazz())) {
									ProgramWorkflow workflow = HtmlFormEntryUtil.getWorkflow(id);
									if (workflow != null) {
										dependencies.add(workflow);
									}
								}
								// need to special case of the name of a role
								if (Role.class.equals(attributeDescriptor.getClazz())) {
									Role role = Context.getUserService().getRole(id);
									if (role != null) {
										dependencies.add(role);
										continue;
									}
								}
								//RelationshipType from the relationship tag, in case of lookup by name (which may or may not be implemented yet...)
								if (RelationshipType.class.equals(attributeDescriptor.getClazz())) {
									RelationshipType relationshipType = Context.getPersonService().getRelationshipTypeByName(id);
									if (relationshipType != null) {
										dependencies.add(relationshipType);
										continue;
									}
								}
								
								RegimenSuggestionCompatibility regimen = Context.getRegisteredComponent("htmlformentry.RegimenSuggestionCompatibility", RegimenSuggestionCompatibility.class);
								regimen.AddDrugDependencies(id, attributeDescriptor, dependencies);
							}
						}
					}
				}
			}
		}
		formToExport.setDependencies(dependencies);
	}
	
	/**
     * @return results of parsing the {@link HtmlFormEntryConstants#GP_CLASSES_NOT_TO_EXPORT_WITH_MDS} global property
     */
    private Set<Class<?>> getClassesNotToExport() {
    	Set<Class<?>> ret = new HashSet<Class<?>>();
    	String gp = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_CLASSES_NOT_TO_EXPORT_WITH_MDS);
    	if (StringUtils.isNotBlank(gp)) {
    		for (StringTokenizer st = new StringTokenizer(gp, ", "); st.hasMoreTokens(); ) {
    			String className = st.nextToken();
    			try {
    				ret.add(Context.loadClass(className));
    			} catch (ClassNotFoundException ex) {
    				// pass
    			}
    		}
    	}
    	return ret;
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
						// see above method for more detail
						String stripPattern = "<" + tagName + "(?:\\s|\\s[^>]*\\s)(" + attributeDescriptor.getName() + "=\".*?\")";
						log.debug("stripping substitution pattern: " + stripPattern);
						
						if (!this.includeLocations && attributeDescriptor.getClazz().equals(Location.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern, Pattern.CASE_INSENSITIVE));
						} else if (!this.includePersons && attributeDescriptor.getClazz().equals(Person.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern, Pattern.CASE_INSENSITIVE));
						} else if (!this.includeRoles && attributeDescriptor.getClazz().equals(Role.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern, Pattern.CASE_INSENSITIVE));
						} else if (!this.includePatientIdentifierTypes
						        && attributeDescriptor.getClazz().equals(PatientIdentifierType.class)) {
							stripLocalAttributesFromXmlHelper(Pattern.compile(stripPattern, Pattern.CASE_INSENSITIVE));
						}
					}
				}
			}
		}
	}
	
	private void stripLocalAttributesFromXmlHelper(Pattern pattern) {
		
		Matcher matcher = pattern.matcher(formToExport.getXmlData());
		StringBuffer buffer = new StringBuffer();
		
		// search through the xml data for the Pattern specified in the pattern parameter, and remove group(1)
		while (matcher.find()) {
			matcher.appendReplacement(buffer, matcher.group().substring(0, matcher.start(1) - matcher.start()));
		}
		
		matcher.appendTail(buffer);
		
		formToExport.setXmlData(buffer.toString());
	}
	
	public HtmlForm export(Boolean includeLocations, Boolean includePersons, Boolean includeRoles,
	                       Boolean includePatientIdentifierTypes) {
		this.includeLocations = includeLocations;
		this.includePersons = includePersons;
		this.includeRoles = includeRoles;
		this.includePatientIdentifierTypes = includePatientIdentifierTypes;
		
		formToExport = copyOf(form);
		
		// first, strip out any local attributes we don't want to pass on
		// default (set in HtmlForm.java) is to include locations, roles, and patient identifier types, but not persons
		stripLocalAttributesFromXml();
		
		// within the form, replace any Ids with Uuids
		HtmlFormSubstitutionUtils.replaceIdsWithUuids(formToExport);
		// replace any programs referenced by name with uuids (since programs referenced by name are really referenced by the underlying concept which can cause issues during metadata sharing)
		HtmlFormSubstitutionUtils.replaceProgramNamesWithUuids(formToExport);
		
		// make sure all dependent OpenmrsObjects are loaded and explicitly referenced
		calculateDependencies();
		// TODO: update the calculate dependencies method to handle persons specified by name...
		
		return formToExport;
	}
}
