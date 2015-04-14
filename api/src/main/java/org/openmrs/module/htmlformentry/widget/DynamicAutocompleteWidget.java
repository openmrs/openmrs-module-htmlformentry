package org.openmrs.module.htmlformentry.widget;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A dynamic autocomplete widget, a new dynamic version of ConceptSearchAutocompleteWidget
 */

public class DynamicAutocompleteWidget implements Widget {

	private List<Concept> initialValues;

	private String allowedConceptIds;

	private String allowedConceptClassNames;

	private String src;

	private static String defaultSrc = "conceptSearch.form";

	public DynamicAutocompleteWidget(List<Concept> conceptList, List<ConceptClass> allowedconceptclasses, String src) {
		this.src = src;
		initialValues = new Vector<Concept>();
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
			for (Iterator<ConceptClass> it = allowedconceptclasses.iterator(); it.hasNext();) {
				sb.append(it.next().getName());
				if (it.hasNext())
					sb.append(",");
			}
			this.allowedConceptClassNames = sb.toString();
		}
	}

	public DynamicAutocompleteWidget(List<Concept> conceptList, List<ConceptClass> allowedconceptclasses) {
		this(conceptList, allowedconceptclasses, defaultSrc);
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		// hardcoded for concept search

		StringBuilder sb = new StringBuilder();
		if (context.getMode().equals(Mode.VIEW)) {
			String toPrint = "";
			if (!initialValues.isEmpty()) {
				for (int i = 0; i < initialValues.size(); i++) {
					toPrint = toPrint + initialValues.get(i).getDisplayString() + ";";
				}
				return WidgetFactory.displayValue(toPrint);
			} else {
				return WidgetFactory.displayDefaultEmptyValue();
			}
		} else {
			sb.append("<input name=\"" + context.getFieldName(this) + "_hid" + "\" id=\"" + context.getFieldName(this)
			        + "_hid" + "\"" + " type=\"hidden\" class=\"autoCompleteHidden\" ");
			if (!initialValues.isEmpty()) {
				sb.append(" value=\"" + initialValues.size() + "\"");
			}
			sb.append("/>");

			sb.append("<input type=\"text\"  id=\"" + context.getFieldName(this) + "\"" + " name=\""
			        + context.getFieldName(this) + "\" " + " onfocus=\"setupAutocomplete(this, '" + this.src + "','"
			        + this.allowedConceptIds + "','" + this.allowedConceptClassNames + "');\""
			        + "class=\"autoCompleteText\"" + " onBlur=\"onBlurAutocomplete(this)\"");

			sb.append("/>");
			sb.append("<input id=\"" + context.getFieldName(this) + "_button"
			        + "\" type=\"button\" class=\"addConceptButton\" value=\"Add\" />");
			sb.insert(0, "<div id=\"" + context.getFieldName(this) + "_div" + "\" class=\"dynamicAutocomplete\">");
			if (!initialValues.isEmpty()) {
				for (int i = 0; i < initialValues.size(); i++) {
					String spanid = "" + context.getFieldName(this) + "span_" + i;
					sb.append("<span id=\"" + spanid + "\"></br>" + initialValues.get(i).getDisplayString() + "<input id=\""
					        + spanid + "_hid" + "\"  class=\"autoCompleteHidden\" type=\"hidden\" name=\"" + spanid + "_hid"
					        + "\" value=\"" + initialValues.get(i).getConceptId() + "\"> <input id=\"" + spanid + "_button"
					        + "\" type=\"button\" value=\"Remove\" onClick=\"$j('#" + spanid
					        + "').remove();openmrs.htmlformentry.refresh(this.id)\"></span>");
				}
			}
			sb.append("</div>");
		}
		return sb.toString();
	}

	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {

        List values = new ArrayList<Object>();
        String widgetName = context.getFieldName(this);
        String entryCount = request.getParameter(widgetName + "_hid");

        if (StringUtils.isNotBlank(entryCount)) {
            int count = Integer.parseInt(entryCount);

            for (int i = 0; i < count; i++) {
                values.add(request.getParameter(widgetName + "span_" + i + "_hid"));
            }
        }

        return values;
	}

	@Override
    public void setInitialValue(Object initialValue) {
        this.initialValues = new Vector<Concept>();
        initialValues.add((Concept) initialValue);
	}

	public void addInitialValue(Object initialValue) {
		this.initialValues.add((Concept) initialValue);
	}

	public List<Concept> getInitialValueList() {
		return initialValues;
	}
}
