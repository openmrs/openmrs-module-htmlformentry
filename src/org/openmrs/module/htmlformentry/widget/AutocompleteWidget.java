package org.openmrs.module.htmlformentry.widget;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

public class AutocompleteWidget implements Widget {

	private Concept initialValue;
	private String allowedConceptIds;
	private String allowedConceptClassNames;
	private String src;
	private static String defaultSrc = "conceptSearch.form";

	public AutocompleteWidget(List<Concept> conceptList,
			List<ConceptClass> allowedconceptclasses, String src) {
		this.src = src;
		
		//only 1 of them is used to specify the filter
		if (allowedconceptclasses.size() == 0) {
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

	public AutocompleteWidget(List<Concept> conceptList,
			List<ConceptClass> allowedconceptclasses) {
		this(conceptList, allowedconceptclasses, defaultSrc);
	}


	public String generateHtml(FormEntryContext context) {
		// hardcoded for concept search

		StringBuilder sb = new StringBuilder();
		if (context.getMode().equals(Mode.VIEW)) {
			String toPrint = "";
			if (initialValue != null) {
				toPrint = initialValue.getDisplayString();
				return WidgetFactory.displayValue(toPrint);
			} else {
				toPrint = "_______________";
				return WidgetFactory.displayEmptyValue(toPrint);
			}
		} else {
			sb.append("<input name=\"" + context.getFieldName(this) + "_hid"
					+ "\" id=\"" + context.getFieldName(this) + "_hid" + "\""
					+ " type=\"hidden\" class=\"autoCompleteHidden\" ");
			if (initialValue != null) {
				sb.append(" value=\"" + initialValue.getConceptId() + "\"");
			}
			sb.append("/>");

			sb.append("<input type=\"text\"  id=\""
					+ context.getFieldName(this) + "\"" + " name=\""
					+ context.getFieldName(this) + "\" "
					+ " onfocus=\"setupAutocomplete(this, '" + this.src + "','"
					+ this.allowedConceptIds + "','"
					+ this.allowedConceptClassNames + "');\""
					+ "class=\"autoCompleteText\""
					+ " onBlur=\"onBlurAutocomplete(this)\"");

			if (initialValue != null)
				sb.append(" value=\"" + initialValue.getDisplayString() + "\"");
			sb.append("/>");

		}
		return sb.toString();
	}


	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		return request.getParameter(context.getFieldName(this) + "_hid");
	}


	public void setInitialValue(Object initialValue) {
		// TODO Auto-generated method stub
		this.initialValue = (Concept) initialValue;
	}
}
