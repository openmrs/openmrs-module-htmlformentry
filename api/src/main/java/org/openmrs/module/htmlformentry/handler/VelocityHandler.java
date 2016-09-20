package org.openmrs.module.htmlformentry.handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.springframework.util.StringUtils;

/**
 * Handles the {@code <lookup>} tag
 */
public class VelocityHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) {
        String expression = parameters.get("expression");
        String className = parameters.get("class");
        String complexExpression = parameters.get("complexExpression");
        if (expression != null && complexExpression != null)
            throw new RuntimeException("Cannot specify both expression and complexExpression");
        if (expression == null && complexExpression == null)
            throw new RuntimeException("Must specify expression or complexExpression for velocity");
        String value;
        if (expression != null)
            value = session.evaluateVelocityExpression("$!{" + expression + "}");
        else
            value = session.evaluateVelocityExpression(complexExpression);
        
        // Enable date formatting 
    	DateFormat fromFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    	DateFormat fromFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    	Date d = null;
    	try {
        	d = fromFormat.parse(value);
    	}
        catch (Exception e) {
        	try {
        		d = fromFormat2.parse(value);
        	}
            catch (Exception e2) {
            	// Do nothing
            }
        }
        if (d != null) {
        	String newFormat = parameters.get("format");
        	DateFormat toFormat = new SimpleDateFormat(newFormat == null ? "dd/MMM/yyyy" : newFormat, Context.getLocale());
        	value = toFormat.format(d);
        }
        
        // Enable translation via existing message sources
        String codePrefix = parameters.get("codePrefix");
        if (codePrefix != null) {
        	String lookupCode = codePrefix + value;
        	String translatedValue = session.getContext().getTranslator().translate(Context.getLocale().toString(), lookupCode);
        	if (StringUtils.hasText(translatedValue) && !translatedValue.equals(lookupCode)) {
        		value = translatedValue;
        	}
        }
        
        if (className != null) {
            return "<span class=\"" + className + "\">" + value + "</span>";
        } else {
            return value;
        }
    }

}
