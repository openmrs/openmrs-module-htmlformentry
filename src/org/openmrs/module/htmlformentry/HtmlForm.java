package org.openmrs.module.htmlformentry;

import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Form;

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
    
    public HtmlForm() { }

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

    /** Gets the actual XML content of the form */
    public String getXmlData() {
        return xmlData;
    }

    /** Sets the actual XML content of the form */
    public void setXmlData(String xmlData) {
        this.xmlData = xmlData;
    }       
    
    /** Allows HtmlForm to be shared via Metadata Sharing Module **/
    protected HtmlForm writeReplace() {
    	// default: includeLocations = true, includeProviders = false
    	return new ShareableHtmlForm(this, true, false);
    }
}
