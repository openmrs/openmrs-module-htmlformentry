package org.openmrs.module.htmlformentry.schema;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;


/**
 * This class is responsible for updating the core OpenMRS Form Schema,
 * when saving an HTML Form
 * @author jportela
 *
 */
public class FormSchemaUpdater {
	
	private HtmlForm htmlForm;
	
    protected final Log log = LogFactory.getLog(getClass());
	
	public FormSchemaUpdater(HtmlForm form) {
		htmlForm = form;
	}
	
	/** Updates the Core OpenMRS Schema
	 * To be called on HtmlFormEntryServiceImpl.saveHtmlForm
	 */
	public void updateSchema() {
		
		//Retrieves the FormEntrySession
		FormEntrySession session = null;
		try {
			session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlForm);
		} catch (Exception e) {	//TODO: Better exception handling
			e.printStackTrace();
		}
        		
        HtmlFormSchema schema = session.getContext().getSchema();	//contains the html form schema
        List <HtmlFormSection> sections = schema.getSections();		//gets a list with all sections of the Html Form
        
        Form form = htmlForm.getForm();		//gets the core OpenMRS Form
        
        //iterates through the sections
        handleSections(sections);
	}
	
	public void handleSections(List<HtmlFormSection> sections) {
		for (int i = 0; i < sections.size(); i++) {
        	
        	//this should add the sections to their place in the form schema
        	//keep in mind that <obs> tags should go into an OBS section, 
        	//and all the other "special" sections (PATIENT, ENCOUNTER)
        	
        	HtmlFormSection section = sections.get(i);
        	handleSection(section);
        }
	}
	
	public void handleSection(HtmlFormSection section) {
		List<HtmlFormField> fields = section.getFields();
		List<HtmlFormSection> sections = section.getSections();
		
		log.info("HtmlFormSection " + section.getName() + " | " + fields.size() + " fields | " + sections.size() + " sections.");
		
		handleSections(sections);
		
		for (int i = 0; i < fields.size(); i++) {
			handleField(fields.get(i));
		}
	}
	
	public void handleField(HtmlFormField field) {
		if (field instanceof ObsField) {
    		ObsField obsField = (ObsField) field;
    		log.info("ObsField - " + obsField.getQuestion().getName() + " - ConceptID: " + obsField.getQuestion().getId());
    	}
	}

}
