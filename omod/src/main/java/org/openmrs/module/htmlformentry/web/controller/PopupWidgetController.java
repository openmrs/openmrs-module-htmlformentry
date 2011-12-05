package org.openmrs.module.htmlformentry.web.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.web.dwr.PersonListItem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class PopupWidgetController {
                      
	@RequestMapping("/module/htmlformentry/personSearch")
	public void patientSearch(ModelMap model) throws Exception {
		
	}
	
	@SuppressWarnings("unchecked")
    @RequestMapping("/module/htmlformentry/personResultTable")
	public void personSearch(ModelMap model, @RequestParam(value="pSearch",required=false) String searchPhrase, 
	                                 @RequestParam(value="pAttribute",required=false) String searchAttribute,
	                                 @RequestParam(value="pAttributeValue",required=false) String attributeValue,
	                                 @RequestParam(value="pProgram",required=false) String searchProgram) throws Exception {
		
		List<Integer> personId = new ArrayList<Integer>();
		List<Object> personList = new ArrayList<Object>();
		
		//first we are going to search by name for patients
		PatientService patientService = Context.getPatientService();
		for (Patient p : patientService.getPatients(searchPhrase, null, null, false)) {
			personList.add(PersonListItem.createBestMatch(p));
			personId.add(p.getId());
		}
		
		// now we check for persons (that won't be brought back by the patient search)
		PersonService ps = Context.getPersonService();
		for (Person p : ps.getPeople(searchPhrase, null)) {
			
			if(!personId.contains(p.getId()))
			{
				personList.add(PersonListItem.createBestMatch(p));
			}
		}
		
		// also search on patient identifier if the query contains a number
		if (searchPhrase.matches(".*\\d+.*")) {
			patientService = Context.getPatientService();
			for (Patient p : patientService.getPatients(null, searchPhrase, null, false)) {
				if(!personId.contains(p.getId()))
				{
					personList.add(PersonListItem.createBestMatch(p));
				}
			}
		}
		
		Cohort cohort = null;
		if(searchAttribute != null)
		{
			String[] attributes = searchAttribute.split(",");
			String[] attrValues = null;
			if(attributeValue != null)
			{
				attrValues = searchAttribute.split(",");
			}
			
			for(int i = 0; i < attributes.length; i++)
			{
				String attr = attributes[i];
				String val = null;
				if(attrValues != null && attrValues[i].trim().length() > 0)
				{
					val = attrValues[i];
				}
				if(attr != null && attr.trim().length() > 0)
				{
					List<Integer> ids = Context.getService(HtmlFormEntryService.class).getPersonIdsHavingAttributes(attr, val);
					Set<Integer> setOfIds = new HashSet<Integer>();
					setOfIds.addAll(ids);
					
					Cohort pp = new Cohort();
					pp.setMemberIds(setOfIds);
				
					if(cohort != null)
					{
						cohort = Cohort.intersect(cohort, pp);
					}
					else
					{
						cohort = pp;
					}
				}
			}
		}
		
		if(searchProgram != null)
		{
			String[] programs = searchProgram.split(",");
			
			for(int i = 0; i < programs.length; i++)
			{
				String prog = programs[i];
				if(prog != null && prog.trim().length() > 0)
				{
					Program personProgram = Context.getProgramWorkflowService().getProgramByUuid(prog);
					if(personProgram == null)
					{
						personProgram = Context.getProgramWorkflowService().getProgram(Integer.parseInt(prog));
					}
					if(personProgram != null)
					{
						Cohort pp = Context.getPatientSetService().getPatientsInProgram(personProgram, null, null);
						if(cohort != null)
						{
							cohort = Cohort.intersect(cohort, pp);
						}
						else
						{
							cohort = pp;
						}
					}
				}
			}
		}
		//not iterate through the person list and filter based on the cohort
		if(cohort != null)
		{
			List<Object> filteredList = new ArrayList<Object>();
			for(Object o: personList)
			{
				PersonListItem pli = (PersonListItem)o;
				
				if(cohort.getMemberIds().contains(pli.getPersonId()))
				{
					filteredList.add(o);
				}
			}
			personList = filteredList;
		}	
		
		model.put("people", personList);
		
	}
	
	
	
	
}
