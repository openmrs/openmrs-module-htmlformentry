package org.openmrs.module.htmlformentry.impl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.openmrs.Cohort;
import org.openmrs.Form;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.htmlformentry.*;
import org.openmrs.module.htmlformentry.db.HtmlFormEntryDAO;
import org.openmrs.module.htmlformentry.element.PersonStub;
import org.openmrs.module.htmlformentry.handler.TagHandler;

/**
 * Standard implementation of the HtmlFormEntryService
 */
public class HtmlFormEntryServiceImpl extends BaseOpenmrsService implements HtmlFormEntryService {
	
    protected final Log log = LogFactory.getLog(getClass());
    
    private HtmlFormEntryDAO dao;
    private static Map<String, TagHandler> handlers = new LinkedHashMap<String, TagHandler>();
    private String basicFormXmlTemplate;

	/*
	 * Optimization to minimize database hits for the needs-name-and-description-migration check.
	 * Once all forms have been migrated, we no longer need to hit the database on further checks
	 * because there is no way to add more un-migrated forms. (In theory someone could add some 
	 * directly to the database, so we use an instance variable here that will be reset whenever
	 * the system is restarted or the module is reloaded.
	 */
    private boolean nameAndDescriptionMigrationDone = false;
    
    @Override
    public void addHandler(String tagName, TagHandler handler) {
        handlers.put(tagName, handler);
    }
    
    @Override
    public TagHandler getHandlerByTagName(String tagName){
        return handlers.get(tagName);
    }
    
    @Override
    public Map<String, TagHandler> getHandlers(){
        return handlers;
    }
    
    /**
     * Sets the tag handlers 
     * 
     * @param handlersToSet
     */
    public void setHandlers(Map<String, TagHandler> handlersToSet) {
        handlers.putAll(handlersToSet);
    }
    
    /**
     * Sets the DAO
     * 
     * @param dao
     */
    public void setDao(HtmlFormEntryDAO dao) {
        this.dao = dao;
    }
    
    /**
     * @return the basicFormXmlTemplate
     */
    public String getBasicFormXmlTemplate() {
    	return basicFormXmlTemplate;
    }
	
    /**
     * @param basicFormXmlTemplate the basicFormXmlTemplate to set
     */
    public void setBasicFormXmlTemplate(String basicFormXmlTemplate) {
    	this.basicFormXmlTemplate = basicFormXmlTemplate;
    }
    
	@Override
    public HtmlForm getHtmlForm(Integer id) {
        return dao.getHtmlForm(id);
    }
	
	@Override
    public HtmlForm getHtmlFormByUuid(String uuid)  {
        return dao.getHtmlFormByUuid(uuid);
	}
    
    @Override
    public HtmlForm saveHtmlForm(HtmlForm htmlForm) {
        if (htmlForm.getCreator() == null)
            htmlForm.setCreator(Context.getAuthenticatedUser());
        if (htmlForm.getDateCreated() == null)
            htmlForm.setDateCreated(new Date());
        if (htmlForm.getId() != null) {
            htmlForm.setChangedBy(Context.getAuthenticatedUser());
            htmlForm.setDateChanged(new Date());
        }
        Context.getFormService().saveForm(htmlForm.getForm());
        return dao.saveHtmlForm(htmlForm);
    }
    
    @Override
    public void purgeHtmlForm(HtmlForm htmlForm) {
        dao.deleteHtmlForm(htmlForm);
    }

    @Override
    public List<HtmlForm> getAllHtmlForms() {
        return dao.getAllHtmlForms();
    }

    @Override
    public HtmlForm getHtmlFormByForm(Form form) {
        return dao.getHtmlFormByForm(form);
    }
    
	@Override
    public boolean needsNameAndDescriptionMigration() {
		if (nameAndDescriptionMigrationDone) {
			return false;
		} else {
			boolean needsMigration = dao.needsNameAndDescriptionMigration();
			if (!needsMigration)
				nameAndDescriptionMigrationDone = true;
			return needsMigration;
		}
    }

	/**
	 * @see HtmlFormEntryService#getStartingFormXml()
	 */
	@Override
    public String getStartingFormXml(HtmlForm form) {
		VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, 
        		"org.apache.velocity.runtime.log.CommonsLogLogChute");
        velocityEngine.setProperty(CommonsLogLogChute.LOGCHUTE_COMMONS_LOG_NAME,
        		"htmlformentry_velocity");
        try {
            velocityEngine.init();
        }
        catch (Exception e) {
            log.error("Error initializing Velocity engine", e);
        }

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("htmlForm", form);
        velocityContext.put("identifierTypes", Context.getPatientService().getAllPatientIdentifierTypes(false));
        velocityContext.put("personAttributeTypes", Context.getPersonService().getAllPersonAttributeTypes(false));
        
        StringWriter writer = new StringWriter();
        try {
            velocityEngine.evaluate(velocityContext, writer, "Basic HTML Form", getBasicFormXmlTemplate());
            String result = writer.toString();
            return result;
        } catch (Exception ex) {
            log.error("Exception evaluating velocity expression", ex);
            return "<htmlform>Velocity Error! " + ex.getMessage() + "</htmlform>"; 
        }
    }
	
    @Override
	public List<PersonStub> getUsersAsPersonStubs(String roleName){
	    return  dao.getUsersAsPersonStubs(roleName);
	}

    @Override
    public OpenmrsObject getItemByUuid(Class<? extends OpenmrsObject> type, String uuid) {
    	return dao.getItemByUuid(type, uuid);
    }
	
    @Override
    public OpenmrsObject getItemById(Class<? extends OpenmrsObject> type, Integer id) {
	    return dao.getItemById(type, id);
    }

    @Override
    public OpenmrsObject getItemByName(Class<? extends OpenmrsMetadata> type, String name) {
	    return dao.getItemByName(type, name);
    }
    
    @Override
    public List<Integer> getPersonIdsHavingAttributes(String attribute, String attributeValue) {
    		    
    	return dao.getPersonIdHavingAttributes(attribute, attributeValue);
    }
	 	
	@Override
    public List<PersonStub> getPeopleAsPersonStubs(List<String> attributes, List<String> attributeValues, List<String> programIds, List<Person> personsToExclude){
		List<PersonStub> stubs = new ArrayList<PersonStub>();
		
		Set<Integer> attributeMatches = null;
		Set<Integer> programMatches = null;
		
		if(attributes != null)
		{
			for(int i = 0; i < attributes.size(); i++)
			{
				String attr = attributes.get(i);
				String val = null;
				if(attributeValues != null && attributeValues.size() > i)
				{
					val = attributeValues.get(i);
				}
				
				Set<Integer> setOfIds = new HashSet<Integer>();
				setOfIds.addAll(getPersonIdsHavingAttributes(attr, val));
					
				if(attributeMatches != null)
				{
					attributeMatches.retainAll(setOfIds);
				}
				else
				{
					attributeMatches = setOfIds;
				}
			}
		}
		
		if(programIds != null)
		{
			for(String prog: programIds)
		{
				if(prog != null && prog.trim().length() > 0)
				{
					Program personProgram = HtmlFormEntryUtil.getProgram(prog);
					
					if(personProgram != null)
					{
						Cohort pp = Context.getPatientSetService().getPatientsInProgram(personProgram, null, null);
						if(programMatches != null)
						{
							programMatches.retainAll(pp.getMemberIds());
						}
						else 
						{
							programMatches = pp.getMemberIds();
						}
					}
				}
			}
		}
		
		Set<Integer> results;
		
		// if no attributes specified, just use to the program matches
		if (attributes == null || attributes.isEmpty()) {
			results = programMatches;
		}
		// if no programs specified, just use the attribute matches
		else if (programIds == null || programIds.isEmpty()) {
			results = attributeMatches;
		}
		// otherwise, intersect the results
		else {
			results = programMatches;
			if (results != null) {
				results.retainAll(attributeMatches);
			}
		}
		
		
		if(results != null)
		{
			//now iterate through the results, returning person stubs
			for(Integer id : results)
			{
				Person person = Context.getPersonService().getPerson(id);
				if(person != null && !personsToExclude.contains(person))
				{
					PersonStub pStub = new PersonStub();
					pStub.setGivenName(person.getGivenName());
					pStub.setFamilyName(person.getFamilyName());
					pStub.setMiddleName(person.getMiddleName());
				pStub.setId(id);
					stubs.add(pStub);
				}
			}
		}
		return stubs;
	}
	
	@Override
	public void applyActions(FormEntrySession session) throws BadFormDesignException {
		//Wrapped in a transactional service method such that actions in it 
		//either pass or fail together. See TRUNK-3572
		session.applyActions();
	}

	@Override
	public void reprocessArchivedForm(String argument,boolean isPath) throws Exception {
        SerializableFormObject formObject;
		if(isPath) {
             formObject = SerializableFormObject.deserializeXml(argument);
        } else {
             formObject = SerializableFormObject.deserializeXml(argument,false);
        }
		formObject.handleSubmission();

		//Save data to database
		HtmlFormEntryUtil.getService().applyActions(formObject.getSession());
	}

    @Override
    public void reprocessArchivedForm(String path) throws Exception {
        reprocessArchivedForm(path,true);
    }
}
