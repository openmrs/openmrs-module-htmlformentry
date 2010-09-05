package org.openmrs.module.htmlformentry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;

/**
 * A clone of an HtmlForm, intended to be used by the Metadata sharing module.
 * The clone includes a list of OpenmrsObjects references in the form xml
 * so that the Metadata sharing module knows which other Openmrs objects it needs
 * to package up with the form.
 */
public class HtmlFormClone extends HtmlForm {

	private static Log log = LogFactory.getLog(HtmlFormClone.class);
	
	private Collection<OpenmrsObject> dependencies;
	
	public HtmlFormClone(HtmlForm form) {
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
		
		// replace any Ids with Uuids
		HtmlFormEntryUtil.replaceIdsWithUuids(this);
		
		// strip out any local attributes we don't want to pass one
		this.setXmlData(stripLocalAttributesFromXml(this.getXmlData()));
		
		// make sure all dependent OpenmrsObjects are loaded and explicitly referenced
		this.setDependencies(retrieveDependenciesFromXml(this.getXmlData()));
	}
	
	public void setDependencies(Collection<OpenmrsObject> dependencies) {
	    this.dependencies = dependencies;
    }

	public Collection<OpenmrsObject> getDependencies() {
	    return this.dependencies;
    }
	
	private String stripLocalAttributesFromXml(String xml) {
		
		// TODO: implement this
		
		return xml;
	}
	
	private Collection<OpenmrsObject> retrieveDependenciesFromXml(String xml) {
	
		Set<OpenmrsObject> objectsToExport = new HashSet<OpenmrsObject>();
		
		Pattern uuid = Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+");
		Matcher matcher = uuid.matcher(xml);
		
		while(matcher.find()) {
			
			if(Context.getConceptService().getConceptByUuid(matcher.group()) != null) {
				objectsToExport.add(Context.getConceptService().getConceptByUuid(matcher.group()));
			} 
			else if(Context.getLocationService().getLocationByUuid(matcher.group()) != null) {
				objectsToExport.add(Context.getConceptService().getConceptByUuid(matcher.group()));
			}
			else if(Context.getProgramWorkflowService().getProgramByUuid(matcher.group()) != null) {
				objectsToExport.add(Context.getConceptService().getConceptByUuid(matcher.group()));
			}
			else {
				log.warn("Unable to load OpenMrs object with uuid = " + matcher.group());
			}
		}
		
		return objectsToExport;
	}
	
}
