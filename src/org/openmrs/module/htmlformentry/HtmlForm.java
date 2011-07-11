package org.openmrs.module.htmlformentry;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Form;
import org.openmrs.OpenmrsObject;

/**
 * The basic HTML Form data object
 */

public class HtmlForm extends BaseOpenmrsMetadata {
	
	/** Unique identifying id */
	private Integer id;
	
	/** The Form object this HTML Form is associated with */
	private Form form;
	
	/** Actual XML content of the form */
	private String xmlData;
	
	/** Deprecated fields: this class now inherits its name and description from its form */
	private String deprecatedName;
	
	private String deprecatedDescription;
	
	/** Allows HtmlForm to be shared via Metadata Sharing Module **/
	private Collection<OpenmrsObject> dependencies;
	
	public HtmlForm() {
	}
	
	/** Gets the unique identifying id for this HTML Form */
	public Integer getId() {
		return id;
	}
	
	/** Sets the unique identifying id for this HTML Form */
	public void setId(Integer id) {
		this.id = id;
	}
	
	/** Gets the Form object this HTML Form is associated with */
	public Form getForm() {
		return form;
	}
	
	/** Sets the Form object this HTML Form is associated with */
	public void setForm(Form form) {
		this.form = form;
	}
	
	/**
	 * Gets the name (inherited from form)
	 */
	@Override
	public String getName() {
		return form != null ? form.getName() : null;
	}
	
	/**
	 * Not supported (set the name on the form instead)
	 */
	@Override
	public void setName(String name) {
		//throw new UnsupportedOperationException("Not supported. Set the name on form instead"); 
	}
	
	/**
	 * Gets the description (inherited from form)
	 */
	@Override
	public String getDescription() {
		return form != null ? form.getDescription() : null;
	}
	
	/**
	 * Not supported (set the description on the form instead)
	 */
	@Override
	public void setDescription(String description) {
		//throw new UnsupportedOperationException("Not supported. Set the description on form instead"); 
	}
	
	/** Gets the actual XML content of the form */
	public String getXmlData() {
		return xmlData;
	}
	
	/** Sets the actual XML content of the form */
	public void setXmlData(String xmlData) {
		this.xmlData = xmlData;
	}
	
	/**
	 * @return the deprecatedName
	 */
	public String getDeprecatedName() {
		return deprecatedName;
	}
	
	/**
	 * @param deprecatedName the deprecatedName to set
	 */
	public void setDeprecatedName(String deprecatedName) {
		this.deprecatedName = deprecatedName;
	}
	
	/**
	 * @return the deprecatedDescription
	 */
	public String getDeprecatedDescription() {
		return deprecatedDescription;
	}
	
	/**
	 * @param deprecatedDescription the deprecatedDescription to set
	 */
	public void setDeprecatedDescription(String deprecatedDescription) {
		this.deprecatedDescription = deprecatedDescription;
	}
	
	/**
	 * @return the dependencies
	 */
	public Collection<OpenmrsObject> getDependencies() {
		return dependencies;
	}
	
	/**
	 * @param dependencies the dependencies to set
	 */
	public void setDependencies(Collection<OpenmrsObject> dependencies) {
		this.dependencies = dependencies;
	}
	
	/** Allows HtmlForm to be shared via Metadata Sharing Module **/
	protected HtmlForm writeReplace() {
		HtmlFormExporter exporter = new HtmlFormExporter(this);
		
		// default: includeLocations = true, includePersons = false, includeRoles = true, includePatientIdentifierTypes = true
		return exporter.export(true, false, true, true);
	}
	
	/**
	 * Allows HtmlForm to be shared via Metadata Sharing Module
	 * 
	 * @param incomingToExisting
	 * @should throw exception if incoming uuid has not 36 characters
	 * @should should replace uuids
	 */
	protected void onSave(Map<OpenmrsObject, OpenmrsObject> incomingToExisting) {
		for (Entry<OpenmrsObject, OpenmrsObject> entry : incomingToExisting.entrySet()) {
			if (!entry.getKey().getUuid().equals(entry.getValue().getUuid())) {
				if (entry.getKey().getUuid().length() != 36) {
					throw new IllegalArgumentException("UUID has an supported format " + "[" + entry.getKey().getUuid()
					        + "]");
				}
				xmlData = xmlData.replace(entry.getKey().getUuid(), entry.getValue().getUuid());
			}
		}
	}
}
