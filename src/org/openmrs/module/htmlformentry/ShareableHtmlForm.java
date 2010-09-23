package org.openmrs.module.htmlformentry;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;

/**
 * A clone of an HtmlForm, intended to be used by the Metadata sharing module. The clone includes a
 * list of OpenmrsObjects references in the form xml so that the Metadata sharing module knows which
 * other Openmrs objects it needs to package up with the form.
 */
public class ShareableHtmlForm extends HtmlForm {
	
	private static Log log = LogFactory.getLog(ShareableHtmlForm.class);
	
	private Collection<OpenmrsObject> dependencies;
	
	private Boolean includeMappedConcepts;
	
	private Boolean includeLocations;
	
	private Boolean includeProviders;
	
	public ShareableHtmlForm(HtmlForm form, Boolean includeMappedConcepts, Boolean includeLocations, Boolean includeProviders) {
		// first, make a clone of the form
		// (do we need to worry about pass-by-reference?)
		this.setChangedBy(form.getChangedBy());
		this.setCreator(form.getCreator());
		this.setDateChanged(form.getDateChanged());
		this.setDateCreated(form.getDateCreated());
		this.setDateRetired(form.getDateRetired());
		this.setDescription(form.getDescription());
		this.setForm(form.getForm());
		this.setId(form.getId());
		this.setName(form.getName());
		this.setRetired(form.getRetired());
		this.setRetiredBy(form.getRetiredBy());
		this.setRetireReason(form.getRetireReason());
		this.setUuid(form.getUuid());
		this.setXmlData(form.getXmlData());
		
		// set the parameters
		this.includeMappedConcepts = includeMappedConcepts;
		this.includeLocations = includeLocations;
		this.includeProviders = includeProviders;
		
		// strip out any local attributes we don't want to pass one
		// default is to include locations, but not providers
		stripLocalAttributesFromXml();
		
		// replace any Ids with Uuids
		HtmlFormEntryUtil.replaceIdsWithUuids(this);
		
		// make sure all dependent OpenmrsObjects are loaded and explicitly referenced
		calculateDependencies();
		// TODO: update the calculate dependencies method to handle locations and persons specified by name...
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
	
	public void calculateDependencies() {
		
		this.dependencies = new HashSet<OpenmrsObject>();
		
		calculateUuidDependencies();
		
		if (this.includeMappedConcepts) {
			calculateMappedConceptDependencies();
		}
		
		if (this.includeLocations) {
			calculateLocationDependencies();
		}
		
		if (this.includeProviders) {
			calculateProviderDependencies();
		}
	}
	
	private void calculateUuidDependencies() {
		// pattern to match a uuid, i.e., five blocks of alphanumerics separated by hyphens
		Pattern uuid = Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+");
		Matcher matcher = uuid.matcher(this.getXmlData());
		
		while (matcher.find()) {
			
			if (Context.getConceptService().getConceptByUuid(matcher.group()) != null) {
				this.dependencies.add(Context.getConceptService().getConceptByUuid(matcher.group()));
			} else if (Context.getLocationService().getLocationByUuid(matcher.group()) != null) {
				this.dependencies.add(Context.getLocationService().getLocationByUuid(matcher.group()));
			} else if (Context.getProgramWorkflowService().getProgramByUuid(matcher.group()) != null) {
				this.dependencies.add(Context.getProgramWorkflowService().getProgramByUuid(matcher.group()));
			} else if (Context.getPersonService().getPersonByUuid(matcher.group()) != null) {
				this.dependencies.add(Context.getPersonService().getPersonByUuid(matcher.group()));
			} else {
				// there is a chance that the "uuid" pattern could match a non-uuid; one reason I'm
				// choosing *not* to throw an exception or log an error here is to handle that case
				log.warn("Unable to load OpenMrs object with uuid = " + matcher.group());
			}
		}
	}
	
	private void calculateMappedConceptDependencies() {
		// pattern matches conceptId="[anything]"; group(1) is set to [anything]
		calculateMappedConceptDependenciesHelper(Pattern.compile("conceptId=\"(.*?)\""));
		// pattern matches conceptIds="[anything]"; group(1) is set to [anything]
		calculateMappedConceptDependenciesHelper(Pattern.compile("conceptIds=\"(.*?)\""));
		// pattern matches groupingConceptId="[anything]"; group(1) is set to [anything]
		calculateMappedConceptDependenciesHelper(Pattern.compile("groupingConceptId=\"(.*?)\""));
		// pattern matches answerConceptId="[anything]"; group(1) is set to [anything]
		calculateMappedConceptDependenciesHelper(Pattern.compile("answerConceptId=\"(.*?)\""));
		// pattern matches answerConceptIds="[anything]"; group(1) is set to [anything]
		calculateMappedConceptDependenciesHelper(Pattern.compile("answerConceptIds=\"(.*?)\""));
	}
	
	private void calculateMappedConceptDependenciesHelper(Pattern pattern) {
		
		Matcher matcher = pattern.matcher(this.getXmlData());
		
		while (matcher.find()) {
			
			// split the group into the various ids
			String[] ids = matcher.group(1).split(",");
			
			// check each id to see if it is a mapping, and, if so, fetch the appropriate concept
			for (String id : ids) {
				int index = id.indexOf(":");
				if (index != -1) {
					String mappingCode = id.substring(0, index).trim();
					String conceptCode = id.substring(index + 1, id.length()).trim();
					Concept concept = Context.getConceptService().getConceptByMapping(conceptCode, mappingCode);
					
					if (concept != null) {
						this.dependencies.add(concept);
					}
				}
			}
		}
	}
	
	private void calculateLocationDependencies() {
		// pattern matches <encounterLocation [anything but greater-than] default="[anything]"; group(1) is set to [anything]
		calculateLocationDependenciesHelper(Pattern.compile("<encounterLocation[^>]* default=\"(.*?)\""));
		// pattern matches <encounterLocation [anything but greater-than] order="[anything]"; group(1) is set to [anything]
		calculateLocationDependenciesHelper(Pattern.compile("<encounterLocation[^>]* order=\"(.*?)\""));
	}
	
	private void calculateLocationDependenciesHelper(Pattern pattern) {
		
		Matcher matcher = pattern.matcher(this.getXmlData());
		
		while (matcher.find()) {
			
			// split the group into the various ids
			String[] ids = matcher.group(1).split(",");
			
			for (String id : ids) {
				
				Location location = Context.getLocationService().getLocation(id);
				
				if (location != null) {
					this.dependencies.add(location);
				}
			}
		}
		
	}
	
	private void calculateProviderDependencies() {
		
		// TODO: method not implemented (or currently used)
		// if we do start allowing providers to exported with forms, we need to decide how we want to handle this
		// should we export *all* persons who have roles assigned in the role="" parameter of encounterProvider?
		
		// TODO: this code has not been tested
		// pattern matches <encounterProvider [anything but greater-than] default="[anything]"; group(1) is set to "[anything]"
		/*	Matcher matcher = Pattern.compile("<encounterProvider[^>]* default=\"(.*?)\"").matcher(this.getXmlData);
		
		while (matcher.find()) {
			User user = Context.getUserService().getUserByUsername(matcher.group(1));
			
			if (user != null) {
				this.dependencies.add(user.getPerson());
			}
		} */

	}
	
	public void stripLocalAttributesFromXml() {
		if (!this.includeProviders) {
			// pattern matches <encounterProvider [anything but greater-than] default="[anything]"; group(1) is set to default="[anything]"
			stripLocalAttributesFromXmlHelper(Pattern.compile("<encounterProvider[^>]*( default=\".*?\")"));
			// pattern matches <encounterProvider [anything but greater-than] role="[anything]"; group(1) is set to role="[anything]"
			stripLocalAttributesFromXmlHelper(Pattern.compile("<encounterProvider[^>]*( role=\".*?\")"));
		}
		
		if (!this.includeLocations) {
			// pattern matches <encounterLocation [anything but greater-than] default="[anything]"; group(1) is set to default="[anything]"
			stripLocalAttributesFromXmlHelper(Pattern.compile("<encounterLocation[^>]*( default=\".*?\")"));
			// pattern matches <encounterLocation [anything but greater-than] order="[anything]"; group(1) is set to order="[anything]"
			stripLocalAttributesFromXmlHelper(Pattern.compile("<encounterLocation[^>]*( order=\".*?\")"));
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
