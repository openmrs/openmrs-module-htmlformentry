package org.openmrs.module.htmlformentry.element;

import org.openmrs.module.htmlformentry.FormEntryContext;

/**
 * An element in a form that is capable of generating HTML. This would typically contain widgets
 * and delegate most of its HTML-generation to them.  
 */
public interface HtmlGeneratorElement {

    public String generateHtml(FormEntryContext context);
    
}
