package org.openmrs.module.htmlformentry.widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Person;
import org.openmrs.PersonName;
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
       Collections.sort(personList, new PersonByNameComparator());
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
	private class PersonByNameComparator implements Comparator<Person> {
		public int compare(Person person1, Person person2) {

			PersonName name1 = person1.getPersonName();
			PersonName name2 = person2.getPersonName();
			
			int ret = OpenmrsUtil.compareWithNullAsGreatest(name1.getFamilyName(), name2.getFamilyName());
			
			if (ret == 0) {
				ret = OpenmrsUtil.compareWithNullAsGreatest(name1.getFamilyName2(), name2.getFamilyName2());
			}
			
			if (ret == 0) {
				ret = OpenmrsUtil.compareWithNullAsGreatest(name1.getGivenName(), name2.getGivenName());
			}
			
			if (ret == 0) {
				ret = OpenmrsUtil.compareWithNullAsGreatest(name1.getMiddleName(), name2.getMiddleName());
			}
			
			if (ret == 0) {
				ret = OpenmrsUtil.compareWithNullAsGreatest(name1.getFamilyNamePrefix(), name2.getFamilyNamePrefix());
			}
			
			if (ret == 0) {
				ret = OpenmrsUtil.compareWithNullAsGreatest(name1.getFamilyNameSuffix(), name2.getFamilyNameSuffix());
			}
		
			return ret;
		}
	}
}
