package org.openmrs.module.htmlformentry.widget;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.List;

/**
 * A double parameter autocomplete widget to do the <obs><obs/> auto completion
 */

public class ConceptSearchAutocompleteWidget implements Widget {

	private Concept initialValue;
	private String allowedConceptIds;
	private String allowedConceptClassNames;
	private String src;
	private static String defaultSrc = "conceptSearch.form";

	public ConceptSearchAutocompleteWidget(List<Concept> conceptList,
                                           List<ConceptClass> allowedconceptclasses, String src) {
		this.src = src;
		
		//only 1 of them is used to specify the filter
		if (allowedconceptclasses == null || allowedconceptclasses.size() == 0) {
			StringBuilder sb = new StringBuilder();
			for (Iterator<Concept> it = conceptList.iterator(); it.hasNext();) {
				sb.append(it.next().getConceptId());
				if (it.hasNext())
					sb.append(",");
			}
			this.allowedConceptIds = sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			for (Iterator<ConceptClass> it = allowedconceptclasses.iterator(); it
					.hasNext();) {
				sb.append(it.next().getName());
				if (it.hasNext())
					sb.append(",");
			}
			this.allowedConceptClassNames = sb.toString();
		}
	}

	public ConceptSearchAutocompleteWidget(List<Concept> conceptList,
                                           List<ConceptClass> allowedconceptclasses) {
		this(conceptList, allowedconceptclasses, defaultSrc);
	}


	@Override
    public String generateHtml(FormEntryContext context) {
		// hardcoded for concept search

		StringBuilder sb = new StringBuilder();
		if (context.getMode().equals(Mode.VIEW)) {
			String toPrint = "";
			if (initialValue != null) {
				toPrint = initialValue.getDisplayString();
				return WidgetFactory.displayValue(toPrint);
			} else {
				return WidgetFactory.displayDefaultEmptyValue();
			}
		} else {
			sb.append("<input type=\"text\"  id=\""
					+ context.getFieldName(this) + "\"" + " name=\""
					+ context.getFieldName(this) + "\" "
					+ " onfocus=\"setupAutocomplete(this, '" + this.src + "','"
					+ this.allowedConceptIds + "','"
					+ this.allowedConceptClassNames + "');\""
					+ "class=\"autoCompleteText\""
                    + "onchange=\"setValWhenAutocompleteFieldBlanked(this)\""
					+ " onblur=\"onBlurAutocomplete(this)\"");

			if (initialValue != null)
				sb.append(" value=\"" + initialValue.getDisplayString() + "\"");
			sb.append("/>");

            sb.append("<input name=\"" + context.getFieldName(this) + "_hid"
                    + "\" id=\"" + context.getFieldName(this) + "_hid" + "\""
                    + " type=\"hidden\" class=\"autoCompleteHidden\" ");
            if (initialValue != null) {
                sb.append(" value=\"" + initialValue.getConceptId() + "\"");
            }
            sb.append("/>");
        }
		return sb.toString();
	}


	@Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
		return request.getParameter(context.getFieldName(this) + "_hid");
	}


	@Override
    public void setInitialValue(Object initialValue) {
		// TODO Auto-generated method stub
		this.initialValue = (Concept) initialValue;
	}
}
