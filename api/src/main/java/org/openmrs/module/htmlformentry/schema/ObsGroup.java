package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

/**
 * Represents an ObsGroup in the HTML Form Schema
 */
public class ObsGroup implements HtmlFormField {

	private Concept concept;
	private List<HtmlFormField> children = new ArrayList<HtmlFormField>();
    
    public ObsGroup(Concept concept) {
    	this.concept = concept;
    }

	/**
	 * Gets the parent concept associated with this ObsGroup
	 * 
	 * @return the concept
	 */
	public Concept getConcept() {
		return concept;
	}

	/**
	 * Sets the parent concept associated with this ObsGroup
	 * 
	 * @param concept the concept to set
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}

    public List<HtmlFormField> getChildren() {
        return children;
    }
    
    public void setChildren(List<HtmlFormField> children) {
        for (HtmlFormField hff : children){
            if (hff instanceof ObsField == false && hff instanceof ObsGroup == false)
                throw new RuntimeException("You can only add an ObsField or an ObsGroup to an ObsGroup");
        }
        this.children = children;
    }
    
    public void addChild(HtmlFormField hff){
        if (hff instanceof ObsField || hff instanceof ObsGroup)
            this.children.add(hff);
        else 
            throw new RuntimeException("You can only add an ObsField or an ObsGroup to an ObsGroup");
    }
    
	

}
