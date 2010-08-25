package org.openmrs.module.htmlformentry.widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;

/**
 * A widget that allows for the selection of a Person.  Implemented using a drop-down selection list.
 */
public class PersonWidget implements Widget {
	
	private Person person;
	private List<Person> options;
	
	public PersonWidget() { }

	public void setInitialValue(Object initialValue) {
	    person = (Person) initialValue;
    }
	
	/**
	 * Sets the Persons to use as options for this widget
	 * 
	 * @param options
	 */
	public void setOptions(List<Person> options) {
        this.options = options;
    }
	
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
            if (person != null)
                return WidgetFactory.displayValue(person.getPersonName().toString());
            else
                return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"" + context.getFieldName(this) + "\">");
        // TODO translate
        sb.append("\n<option value=\"\">");
        sb.append(Context.getMessageSourceService().getMessage("general.choose") + "...");
        sb.append("</option>");
        List<Person> personList;
        if (options != null) {
            personList = options;
        } else {
            personList = Context.getPersonService().getPeople("", true);
        }
        Collections.sort(personList, new PersonComparator());
        for (Person p : personList) {
            sb.append("\n<option");
            if (person != null && person.equals(p))
                sb.append(" selected=\"true\"");
            sb.append(" value=\"" + p.getPersonId() + "\">").append(p.getPersonName()).append("</option>");
        }
        sb.append("</select>");
        return sb.toString();
    }

	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String val = request.getParameter(context.getFieldName(this));
        if (StringUtils.hasText(val))
            return HtmlFormEntryUtil.convertToType(val, Person.class);
        return null;
    }

	/**
	 * A simple person comparator for sorting providers by name
	 */
	private class PersonComparator implements Comparator<Person> {
		public int compare(Person person1, Person person2) {
			// compare family names
			int comparison = OpenmrsUtil.compareWithNullAsGreatest(person1.getFamilyName(), person2.getFamilyName());
			
			if(comparison != 0) {
				return comparison;
			} else {
				// if family names are equal, compare given name
				return OpenmrsUtil.compareWithNullAsGreatest(person1.getGivenName(), person2.getGivenName());
			}
		}
	}
}
