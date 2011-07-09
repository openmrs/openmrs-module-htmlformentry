package org.openmrs.module.htmlformentry.schema;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Field;
import org.openmrs.FieldType;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
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
	private FormField obsSection = null;
	private FormField patientSection = null;
	private FormField encounterSection = null;
	private Form form = null;
	private HashMap<Integer, LinkedList<FormField>> formSchemaFields = null;
	private HashMap<String, FormField> formSchemaSections = null;
	private FormService formService = null;
	private FieldType sectionType = null;
	private FieldType conceptType = null;
	private FieldType setOfConceptsType = null;
	
    protected final Log log = LogFactory.getLog(getClass());
	
	public FormSchemaUpdater(HtmlForm htmlForm) {
		this.htmlForm = htmlForm;
		form = htmlForm.getForm();
		formService = Context.getFormService();		
	}
	
	/** 
	 * Updates the Core OpenMRS Schema
	 * To be called on HtmlFormEntryServiceImpl.saveHtmlForm
	 * @should add new form fields if they don't exist in the schema
	 * @should create an entire form schema from the html code
	 * @should not remove any created form fields
	 */
	public void updateSchema() {
		
		//Retrieves the FormEntrySession
		FormEntrySession session = null;
		try {
			session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlForm);
		} catch (Exception e) {	//TODO: Better exception handling
			e.printStackTrace();
		}
		
		sortFormFieldsByParentId(form.getFormFields());

        HtmlFormSchema schema = session.getContext().getSchema();	//contains the html form schema
        List <HtmlFormSection> sections = schema.getSections();		//gets a list with all sections of the Html Form
        
		findOrCreateDefaultFieldTypes();
        findOrCreateDefaultSections();
        
        addExtraFields();
        
        //iterates through the sections
        //the first section stores all sections... weird...
        if (!sections.isEmpty())
        {
        	handleSections(sections.get(0).getSections(), null);
        	handleFields(sections.get(0).getFields(), obsSection);
        }
	}
	
	/**
	 * Retrieves the Default sections from the Data Model
	 * If they cannot be found, it will automatically create
	 * new sections on the Data Model
	 */
	private void findOrCreateDefaultSections()
	{
		if (!findDefaultSections())
			createDefaultSections();
	}
	
	/**
	 * Retrieves the OBS, PATIENT and ENCOUNTER sections from the
	 * Data Model
	 * @return true if all sections were found
	 */
	private boolean findDefaultSections() {
		
		obsSection = formSchemaSections.get("OBS");
		patientSection = formSchemaSections.get("PATIENT");
		encounterSection = formSchemaSections.get("ENCOUNTER");
			
		if (!(obsSection == null || patientSection == null || encounterSection == null))
			return true;
		else
			return false;
	}
	
	/**
	 * Adds PATIENT and ENCOUNTER fields to the form schema
	 */
	private void addExtraFields() {
		findOrCreateFormFieldByName("PATIENT.PATIENT_ID", conceptType, patientSection, 0);
		findOrCreateFormFieldByName("ENCOUNTER.ENCOUNTER_DATETIME", conceptType, encounterSection, 0);
		findOrCreateFormFieldByName("ENCOUNTER.LOCATION_ID", conceptType, encounterSection, 0);
		findOrCreateFormFieldByName("ENCOUNTER.PROVIDER_ID", conceptType, encounterSection, 0);
	}
	
	/**
	 * Populates the "Section" and "Concept" field type from the DB. If it doesn't exist
	 * it will be created.
	 */
	
	private void findOrCreateDefaultFieldTypes() {
		List<FieldType> types = formService.getAllFieldTypes();
		
		//retrieves the Section field type
		for (FieldType type : types) {
			if (type.getName().equals("Section")) {
				sectionType = type;
			}
			else if (type.getName().equals("Concept")) {
				conceptType = type;
			}
			else if (type.getName().equals("Set of Concepts")) {
				setOfConceptsType = type;
			}
			if (!(sectionType == null || conceptType == null || setOfConceptsType == null))
				break;
		}
		
		//if it could not be found, create a new one
		if (sectionType == null) {
			sectionType = createFieldType("Section", true);
		}
		if (conceptType == null) {
			conceptType = createFieldType("Concept", false);
		}
		if (setOfConceptsType == null) {
			setOfConceptsType = createFieldType("Set of Concepts", true);
		}
				
	}
	
	/**
	 * Creates a new Field Type
	 * @param name of the field type
	 * @param isSet Is it a Set?
	 * @return the new Field Type
	 */
	private FieldType createFieldType(String name, boolean isSet) {
		
		FieldType fieldType = new FieldType();
		fieldType.setName(name);
		fieldType.setDescription("Generated by HtmlFormEntry module");
		fieldType.setIsSet(isSet);
		return formService.saveFieldType(fieldType);				
	}

	/**
	 * Creates a new Default Section 
	 * @param name name of the section
	 * @return the formfield for that section
	 */
	private FormField createDefaultSection(String name) {
		
		Field sectionField = null;
		
		List<Field> fields = formService.getAllFields();
		
		for (Field field : fields) {
			if (field.getName().equals(name)) {
				sectionField = field;
				break;
			}
		}
		
		if (sectionField == null) {
			sectionField = new Field();
			sectionField.setName(name);
			sectionField.setDescription("Generated by HtmlFormEntry module");
			sectionField.setFieldType(sectionType);
		}
		
		FormField formField = new FormField();
		formField.setField(sectionField);
		formField.setForm(form);
		formField.setName(name);
		formField.setDescription("Generated by HtmlFormEntry module");
		return formService.saveFormField(formField);
				
	}
	
	/**
	 * Creates the Default Sections in the Data Model (OBS,
	 * PATIENT, ENCOUNTER), if they were not found
	 */
	private void createDefaultSections() { 
		 		
		if (obsSection == null) {
			obsSection = createDefaultSection("OBS");
		}
		if (patientSection == null) {
			patientSection = createDefaultSection("PATIENT");
		}
		if (encounterSection == null) {
			encounterSection = createDefaultSection("ENCOUNTER");
		}
		
	}
	
	/**
	 * Iterates through the HtmlFormSections on the Html Form Schema
	 * and updates the DB accordingly
	 * @param sections Html Form Schema sections
	 */
	public void handleSections(List<HtmlFormSection> sections, FormField parent) {
		for (int i = 0; i < sections.size(); i++) {
        	
			HtmlFormSection section = sections.get(i);
        	//this should add the sections to their place in the form schema
        	//keep in mind that <obs> tags should go into an OBS section, 
        	//and all the other "special" sections (PATIENT, ENCOUNTER)
        	
        	handleSection(section, parent, i);
        }
	}

	/**
	 * Finds the form field, by its concept and it's parent form field
	 * @param conceptId ID of the Concept
	 * @param parentId ID of the parent Form Field
	 * @return the form field
	 */
	
	private FormField findFormField(Integer conceptId, Integer parentId) {
		
		LinkedList<FormField> fields = formSchemaFields.get(parentId);
		
		if (fields == null) return null;
		
		for (FormField field : fields) {
			if (field.getField().getConcept().getConceptId().equals(conceptId)) {
				fields.remove(field);		//removes the field, already found
				return field;
			}
		}
		
		return null;
	}

	/**
	 * Finds a Section Form Field
	 * @param name
	 * @return form field with the section name
	 */
	private FormField findSectionFormField(String name) {
		
		return formSchemaSections.get(name);
	}
	
	/**
	 * Creates a Section Field
	 * @param name of the section
	 * @return field with the name
	 */
	private Field createSectionField(String name) {
		Field field = new Field();
		field.setName(name);
		field.setDescription("Generated by Html Form Entry Module");
		field.setFieldType(sectionType); 
		return field;
	}
	
	/**
	 * Finds or creates a field with the given concept
	 * @param concept of this field
	 * @param type of the field
	 * @return field with the given concept
	 */
	
	private Field findOrCreateField(Concept concept, FieldType type) {
		
		List<Field> fields = formService.getFieldsByConcept(concept);
		
		if (fields.isEmpty()) {		//couldn't find the field
			Field field = new Field();
			field.setConcept(concept);
			field.setName("" + concept.getName());
			field.setDescription("Generated by Html Form Entry Module");
			field.setFieldType(type); 
			return field;
		}
		else
			return fields.get(0);	//returns the first field found
	}

	/**
	 * Finds a field by its name or creates a new one with that name
	 * @param name of the field
	 * @param type of the field
	 * @return the field with the given name
	 */
	private Field findOrCreateFieldByName(String name, FieldType type) {
		
		List<Field> fields = formService.getAllFields();
		
		for (Field field : fields) {
			if (name.equals("" + field.getName())) {
				return field;
			}
		}
		
		//didn't found....
		
		Field field = new Field();
		field.setName(name);
		field.setDescription("Generated by Html Form Entry Module");
		field.setFieldType(type); 
		return field;
	}
	
	/**
	 * Creates a form field
	 * @param concept of the form field
	 * @param name of the form field
	 * @param type of the form field
	 * @param parent of the form field
	 * @param index - used for sorting (sort weight)
	 * @return the newly created Form Field on the DB
	 */
	private FormField createFormField(Concept concept, String name, FieldType type, FormField parent, int index) {

		Field field = findOrCreateField(concept, type);
		
		FormField formField = new FormField();
		formField.setForm(form);
		formField.setField(field);
		formField.setParent(parent);
		formField.setSortWeight((float)index);
		formField.setName("" + name);
		formField.setDescription("Generated by Html Form Entry Module");
		return formService.saveFormField(formField);
	}
	
	/**
	 * Creates a Form Field with the given name
	 * @param name of the form field
	 * @param type of the form field
	 * @param parent of the form field
	 * @param index - used for sorting (sort weight)
	 * @return the created form field
	 */
	
	private FormField createFormFieldByName(String name, FieldType type, FormField parent, int index) {

		Field field = findOrCreateFieldByName(name, type);
		
		FormField formField = new FormField();
		formField.setForm(form);
		formField.setField(field);
		formField.setParent(parent);
		formField.setSortWeight((float)index);
		formField.setName("" + name);
		formField.setDescription("Generated by Html Form Entry Module");
		return formService.saveFormField(formField);
	}
	
	/**
	 * Creates a Form Field for a Section
	 * @param name of the section
	 * @param parent of the section
	 * @param index - used for sorting (sort weight)
	 * @return the section form field created
	 */
	private FormField createSectionFormField(String name, FormField parent, int index) {

		Field field = createSectionField(name);
		
		FormField formField = new FormField();
		formField.setForm(form);
		formField.setField(field);
		formField.setParent(parent);
		formField.setSortWeight((float)index);
		formField.setName("" + name);
		formField.setDescription("Generated by Html Form Entry Module");
		return formService.saveFormField(formField);
	}

	/**
	 * Finds the Section form field with the specified name and parent, or else it creates it
	 * @param name of the section
	 * @param parent of the section
	 * @param index - used for sorting (sort weight)
	 * @return the form field for the section
	 */
	
	private FormField findOrCreateSectionFormField(String name, FormField parent, int index) {
		
		FormField field = findSectionFormField(name);
		
		if (field == null)
			field = createSectionFormField(name, parent, index);
		
		return field;
	}
	
	/**
	 * Finds a form field with the specified concept and parent, or else it creates it
	 * @param concept
	 * @param type
	 * @param parent
	 * @param index - used for sorting (sort weight)
	 * @return the form field
	 */
	private FormField findOrCreateFormField(Concept concept, FieldType type, FormField parent, int index) {
		FormField field = null;
		
		if (concept != null && parent != null)
			field = findFormField(concept.getConceptId(), parent.getId());
		
		if (field == null)
			field = createFormField(concept, "" + concept.getName(), type, parent, index);
		
		return field; 
	}
	
	/**
	 * Finds a form field with the specified name and parent, or else it creates it
	 * @param name
	 * @param type
	 * @param parent
	 * @param index - used for sorting (sort weight)
	 * @return the form field
	 */
	private FormField findOrCreateFormFieldByName(String name, FieldType type, FormField parent, int index) {
		FormField field = null;
		
		field = findFormFieldByName(name);
		
		if (field == null)
			field = createFormFieldByName("" + name, type, parent, index);
		
		return field; 
	}
	
	/**
	 * Finds a form field with the specified name.
	 * @param name
	 * @return the form field. Returns null if it doesn't exist
	 */
	private FormField findFormFieldByName(String name) {
		FormField field = formSchemaSections.get(name);
		return field;
	}
	

	/**
	 * Updates fields and sections from a section, to the form schema
	 * @param section to be handled
	 * @param parent of the section
	 * @param index - used for sorting (sort weight) 
	 */
	public void handleSection(HtmlFormSection section, FormField parent, int index) {
		List<HtmlFormField> fields = section.getFields();
		List<HtmlFormSection> sections = section.getSections();
		
		log.info("HtmlFormSection " + section.getName() + " | " + fields.size() + " fields | " + sections.size() + " sections.");
		
		parent = (parent == null) ? obsSection : parent;
		
		FormField formField = findOrCreateSectionFormField("" + section.getName(), parent, index);	

		handleFields(fields, formField);
		
		if (!sections.isEmpty())
			handleSections(sections, formField);
	}
	
	/**
	 * Handles all fields from a field list
	 * @param fields to be handled
	 * @param parent of the fields
	 */
	public void handleFields(List<HtmlFormField> fields, FormField parent) {
		for (int i = 0; i < fields.size(); i++) {
			HtmlFormField field = fields.get(i);
			handleField(field, parent, i);
		}
	}
	
	/**
	 * Updates an Html Form Field in the Database
	 * @param htmlFormField to update
	 * @param parent of the field
	 * @param index - used for sorting (sort weight) 
	 */
	public void handleField(HtmlFormField htmlFormField, FormField parent, int index) {
		if (htmlFormField instanceof ObsField) {
    		ObsField obsField = (ObsField) htmlFormField;
    		log.info("ObsField - " + obsField.getQuestion().getName() + " - ConceptID: " + obsField.getQuestion().getId());
    		
    		findOrCreateFormField(obsField.getQuestion(), conceptType, parent, index);

		}

		else if (htmlFormField instanceof ObsGroup) {
    		ObsGroup obsGroup = (ObsGroup) htmlFormField;
    		
    		log.info("ObsField - " + obsGroup.getConcept().getName() + " - ConceptID: " + obsGroup.getConcept().getId());
    		
    		FormField formField = findOrCreateFormField(obsGroup.getConcept(), setOfConceptsType, parent, index);
    		
    		List<HtmlFormField> fields = obsGroup.getChildren();
    		
    		for (int i = 0; i < fields.size(); i++) {
    			HtmlFormField groupField = fields.get(i);
    			handleField(groupField, formField, i);
    		}
			
		}
	}
	
	/**
	 * Populates maps with the form fields of a parent (formSchemaFields)
	 * and the sections of the schema (formSchemaSections)
	 * FIXME: I think this could work with just formSchemaFields
	 * @param formField
	 */
	private void sortFormFieldsByParentId(Set<FormField> formFields) {
		formSchemaFields = new HashMap<Integer, LinkedList<FormField>>();
		formSchemaSections = new HashMap<String, FormField>();
		
		if (formFields == null) return;
		
		for (FormField field : formFields) {
			
			if (field.getParent() == null) //top level elements: OBS, PATIENT, ENCOUNTER
			{
				String name = field.getField().getName();
				formSchemaSections.put("" + name, field);
			}
			else if (field.getField().getConcept() == null) {	//Section...
				String name = field.getField().getName();
				formSchemaSections.put("" + name, field);
			}
			else {
				Integer parentId = field.getParent().getId();	

				LinkedList<FormField> fields = null;
				
				if (formSchemaFields.containsKey(parentId)) {
					fields = formSchemaFields.get(parentId);
				}
				else {
					fields = new LinkedList<FormField>();
					formSchemaFields.put(parentId, fields);
				}
				fields.add(field);
			}
		}
	}
	
	

}
