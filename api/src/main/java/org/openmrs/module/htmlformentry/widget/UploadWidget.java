package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.obs.ComplexData;

/**
 * A widget that implements a upload button to upload images/files, like
 * {@code <input type="file"/>}, or as a {@code <obs conceptId="1234">}. The attribute conceptId
 * mentioned in the obs tag has to be the id of a concept with datatype 'complex'.
 * 
 * @author Jibesh
 */
public class UploadWidget implements Widget {
	
	private Obs initialValue;
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (Obs) initialValue;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			if (initialValue != null) {
				return WidgetFactory.displayComplexValue(initialValue);
			} else {
				return WidgetFactory.displayEmptyValue("");
			}
		}
		
		String id = context.getFieldName(this);
		StringBuilder sb = new StringBuilder();
		sb.append("<input type=\"file\" class=\"uploadWidget\" id=\"" + id + "\" name=\"" + id + "\"");
		
		if (context.getMode() == FormEntryContext.Mode.EDIT) {
			if (initialValue != null) {
				sb.append("/>");
				String complexValueHtml = WidgetFactory.displayComplexValue(initialValue);
				return "<p>" + complexValueHtml + "</p>" + sb.toString() + "<input type=\"checkbox\" name=\"" + id
				        + "_delete\" id=\"" + id + "_delete\" />"
				        + Context.getMessageSourceService().getMessage("htmlformentry.form.complex.delete") + "<br/>";
			} else {
				sb.append("/>");
				return WidgetFactory.displayEmptyValue("") + sb.toString();
			}
			
		}
		sb.append("/>");
		return sb.toString();
	}
	
	/**
	 * @param context
	 * @param request
	 * @return Complex Data associated with the Obs
	 */
	@Override
	public ComplexData getValue(FormEntryContext context, HttpServletRequest request) {
		try {
			return (ComplexData) HtmlFormEntryUtil
			        .getParameterAsType(request, context.getFieldName(this), ComplexData.class);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Illegal value");
		}
	}
	
	/**
	 * @param context
	 * @param request
	 * @return boolean to indicate if obs is to be deleted
	 */
	public boolean shouldDelete(FormEntryContext context, HttpServletRequest request) {
		String deleteStr = request.getParameter(context.getFieldName(this) + "_delete");
		if ("on".equals(deleteStr)) {
			return true;
		} else {
			return false;
		}
	}
}
