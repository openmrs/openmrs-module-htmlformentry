package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

/**
 * A widget that allows for the selection of a Person.  Implemented uses a pop-up to display person 
 * search.
 */
public class PersonSearchWidget implements Widget {
	
	private Person person;
	
	private String searchAttribute = null;
	private String attributeValue = null;
	private String searchProgram = null;
	
	public PersonSearchWidget() { }

	@Override
    public void setInitialValue(Object initialValue) {
	    person = (Person) initialValue;
    }
	
	
	@Override
    public String generateHtml(FormEntryContext context) {
		
		if (context.getMode() == Mode.VIEW) {
            if (person != null)
                return WidgetFactory.displayValue(person.getPersonName().toString());
            else
                return "";
        }
     
        StringBuilder sb = new StringBuilder();
        
        sb.append("<script type='text/javascript'> \n");
        sb.append("var $j = jQuery.noConflict(); ");
        sb.append("$j(document).ready(function() { \n");
        sb.append("	$j('#displayPopup_");
        sb.append(context.getFieldName(this));
        sb.append("').dialog({ \n");
        sb.append("			title: 'dynamic', \n");
        sb.append("			autoOpen: false, \n");
        sb.append("			draggable: false, \n");
        sb.append("			resizable: false, \n");
        sb.append("			width: '95%', \n");
        sb.append("			modal: true, \n");
        sb.append("			open: function(a, b) { $j('#displayPopupLoading_");
        sb.append(context.getFieldName(this));
        sb.append("').show(); } \n");
        sb.append("	}); \n");
        sb.append("}); \n");

        sb.append("function loadUrlIntoPopup_");
        sb.append(context.getFieldName(this));
        sb.append("(title, urlToLoad) { \n");
        sb.append("	$j('#displayPopupIframe_");
    	sb.append(context.getFieldName(this));
    	sb.append("').attr('src', urlToLoad); \n");
        sb.append("	$j('#displayPopup_");
    	sb.append(context.getFieldName(this));
    	sb.append("') \n");
        sb.append("		.dialog('option', 'title', title) \n");
        sb.append("		.dialog('option', 'height', $j(window).height() - 50) \n"); 
        sb.append("		.dialog('open'); \n");
        sb.append("} \n");
        
        sb.append("function showPopup_");
        sb.append(context.getFieldName(this));
        sb.append("() { \n");
    	sb.append("	loadUrlIntoPopup_");
    	sb.append(context.getFieldName(this));
    	sb.append("('");
    	sb.append(Context.getMessageSourceService().getMessage("htmlformentry.personSearchPopup"));
    	sb.append("', (openmrsContextPath.startsWith('/') ? '' : '/') + openmrsContextPath + '/module/htmlformentry/personSearch.form?inPopup=true&prefix=");
    	sb.append(context.getFieldName(this));
    	if(searchAttribute != null)
    	{
    		sb.append("&searchAttribute=");
    		sb.append(searchAttribute);
    	}
    	if(attributeValue != null)
    	{
    		sb.append("&attributeValue=");
    		sb.append(attributeValue);
    	}
    	if(searchProgram != null)
    	{
    		sb.append("&searchProgram=");
    		sb.append(searchProgram);
    	}
    	sb.append("'); \n");	
    	sb.append(" $j('#displayPopupLoading_");
    	sb.append(context.getFieldName(this));
    	sb.append("').hide(); \n");
    	sb.append("} \n");
    	
    	sb.append("function setValue(id, name, prefix) { \n");
    	sb.append("var idField = \"#\" + prefix; \n");
    	sb.append("var nameField = \"#\" + prefix + 'name'; \n");
    	sb.append("var popup = \"#displayPopup_\" + prefix; \n");
    	sb.append(" $j(idField).val(id); \n");
    	sb.append(" $j(nameField).val(name); \n");
    	sb.append("$j(popup).dialog('close'); \n");
    	sb.append(" } \n");
    	
        sb.append("</script> \n");
    	
    	sb.append("<div id='displayPopup_");
    	sb.append(context.getFieldName(this));
    	sb.append("'> \n");
    	sb.append("<div id='displayPopupLoading_");
    	sb.append(context.getFieldName(this));
    	sb.append("'>Loading</div> \n");
    	sb.append("<iframe id='displayPopupIframe_");
    	sb.append(context.getFieldName(this));
    	sb.append("' width='100%' height='100%' marginWidth='0' marginHeight='0' frameBorder='0' scrolling='auto'></iframe> \n");
    	sb.append("</div>");
    	
    	sb.append("<input type='button' value='");
    	sb.append(Context.getMessageSourceService().getMessage("htmlformentry.personSearchPopup"));
    	sb.append("' onClick='showPopup_");
    	sb.append(context.getFieldName(this));
    	sb.append("()'> \n");
    	
    	sb.append("<input name='");
    	sb.append(context.getFieldName(this));
    	sb.append("name' id='");
    	sb.append(context.getFieldName(this));
    	sb.append("name' readonly='true' >");
    	sb.append("<input name='");
    	sb.append(context.getFieldName(this));
    	sb.append("' id='");
    	sb.append(context.getFieldName(this));
    	sb.append("' type='hidden'>");
    	
    	
        return sb.toString();
    }

	@Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
		
		String val = request.getParameter(context.getFieldName(this));
        if (StringUtils.hasText(val))
        {
        	person = (Person)HtmlFormEntryUtil.convertToType(val, Person.class);
            return person;
        }
        return null;
    }

	
    /**
     * @return the searchAttribute
     */
    public String getSearchAttribute() {
    	return searchAttribute;
    }

	
    /**
     * @param searchAttribute the searchAttribute to set
     */
    public void setSearchAttribute(String searchAttribute) {
    	this.searchAttribute = searchAttribute;
    }

	
    /**
     * @return the attributeValue
     */
    public String getAttributeValue() {
    	return attributeValue;
    }

	
    /**
     * @param attributeValue the attributeValue to set
     */
    public void setAttributeValue(String attributeValue) {
    	this.attributeValue = attributeValue;
    }

	
    /**
     * @return the searchProgram
     */
    public String getSearchProgram() {
    	return searchProgram;
    }

	
    /**
     * @param searchProgram the searchProgram to set
     */
    public void setSearchProgram(String searchProgram) {
    	this.searchProgram = searchProgram;
    }
}
