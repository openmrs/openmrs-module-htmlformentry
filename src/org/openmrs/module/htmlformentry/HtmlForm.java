package org.openmrs.module.htmlformentry;

import java.util.Date;

import org.openmrs.Form;
import org.openmrs.User;

/**
 * The basic HTML Form data object
 */

public class HtmlForm {
	
	/** Unique identifying id */
    private Integer id;
    
    /** The Form object this HTML Form is associated with */
    private Form form;
    
    /** The name of the HTML Form */
    private String name;
    
    /** User that created the HTML Form */
    private User creator;
    
    /** Date the HTML Form was created */
    private Date dateCreated;
    
    /** User that last changed the HTML Form */
    private User changedBy;
    
    /** Date the HTML Form was last changed */
    private Date dateChanged;
    
    /** True/false whether or not the HTML Form has been retired */
    private Boolean retired = false;
    
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

    /** Gets the name of this HTML Form */
    public String getName() {
        return name;
    }

    /** Sets the name of this HTML Form */
    public void setName(String name) {
        this.name = name;
    }

    /** Gets the actual XML content of the form */
    public String getXmlData() {
        return xmlData;
    }

    /** Sets the actual XML content of the form */
    public void setXmlData(String xmlData) {
        this.xmlData = xmlData;
    }
    
    /** Gets the User that created the HTML Form */
    public User getCreator() {
        return creator;
    }

    /** Sets the User that created the HTML Form */
    public void setCreator(User creator) {
        this.creator = creator;
    }

    /** Gets the Date the HTML Form was created */
    public Date getDateCreated() {
        return dateCreated;
    }

    /** Sets the Date the HTML Form was created */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /** Gets the User that last changed the HTML Form */
    public User getChangedBy() {
        return changedBy;
    }

    /** Sets the User that last changed the HTML Form */
    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    /** Gets the Date the HTML Form was last changed */
    public Date getDateChanged() {
        return dateChanged;
    }

    /** Sets the Date the HTML Form was last changed */
    public void setDateChanged(Date dateChanged) {
        this.dateChanged = dateChanged;
    }

    /** Gets whether or not the HTML Form has been retired */
    public Boolean getRetired() {
        return retired;
    }

    /** Sets whether or not the HTML Form has been retired */
    public void setRetired(Boolean retired) {
        this.retired = retired;
    }
       
}
