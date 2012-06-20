package org.openmrs.module.htmlformentry.widget;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;


public class DynamicAutocompleteWidget  implements Widget {

	private List <Concept> initialValue;
	private String allowedConceptIds;
	private String allowedConceptClassNames;
	private String src;
	private static String defaultSrc = "conceptSearch.form";
	private int count=0;


	public DynamicAutocompleteWidget(List<Concept> conceptList,
			List<ConceptClass> allowedconceptclasses, String src) {
		this.src = src;
		initialValue=new Vector<Concept>();
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
	
	public DynamicAutocompleteWidget(List<Concept> conceptList,
	              			List<ConceptClass> allowedconceptclasses) {
	              		this(conceptList, allowedconceptclasses, defaultSrc);
	              	}
	@Override
    public String generateHtml(FormEntryContext context) {
		// hardcoded for concept search

		StringBuilder sb = new StringBuilder();
		if (context.getMode().equals(Mode.VIEW)) {
			String toPrint = "";
			if (!initialValue.isEmpty()) {
				for (int i = 0; i < initialValue.size(); i++) {
				toPrint =toPrint+ initialValue.get(i).getDisplayString()+";";
				 }
				return WidgetFactory.displayValue(toPrint);
			} else {
				toPrint = "_______________";
				return WidgetFactory.displayEmptyValue(toPrint);
			}
		} else {
			sb.append("<input name=\"" + context.getFieldName(this) + "_hid"
					+ "\" id=\"" + context.getFieldName(this) + "_hid" + "\""
					+ " type=\"hidden\" class=\"autoCompleteHidden\" ");
			if (!initialValue.isEmpty()) {
				sb.append(" value=\"" + initialValue.size() + "\"");
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

			
			sb.append("/>");
			
			sb.append("<input id=\""
					+ context.getFieldName(this) +"_button"+ "\" type=\"button\" class=\"addConceptButton\" value=\"Add\" />");
			  sb.insert(0, "<div id=\""+ context.getFieldName(this) +"_div"+ "\">");
			  if (!initialValue.isEmpty()){
				  for (int i = 0; i < initialValue.size(); i++) {
		               String spanid=""+ context.getFieldName(this)+"span_"+count;
					sb.append("<span id=\""+spanid+"\"></br>"+initialValue.get(i).getDisplayString()+"<input id=\""+spanid+"_hid"+ "\"  class=\"autoCompleteHidden\" type=\"hidden\" name=\""+spanid+"_hid"+"\" value=\""+initialValue.get(i).getConceptId()+"\"> <input id=\""+spanid+"_button"+ "\" type=\"button\" value=\"remove\" onClick=\"$j(\'#'+spanid+'\').remove();refresh()\"></span>");
				  }
				}
			  sb.append("</div>");
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
		this.initialValue.add((Concept) initialValue);
	}
}

