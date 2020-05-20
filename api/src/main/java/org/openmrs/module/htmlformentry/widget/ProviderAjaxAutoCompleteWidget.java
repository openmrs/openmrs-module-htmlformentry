package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.element.ProviderStub;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.springframework.util.StringUtils;

public class ProviderAjaxAutoCompleteWidget implements Widget {
	
	private List<String> providerRoles;
	
	private MatchMode matchMode;
	
	private Provider initialValue;
	
	public MatchMode getMatchMode() {
		return matchMode;
	}
	
	public void setMatchMode(MatchMode matchMode) {
		if (matchMode == null) {
			this.matchMode = MatchMode.ANYWHERE;
		} else {
			this.matchMode = matchMode;
		}
	}
	
	public ProviderAjaxAutoCompleteWidget(MatchMode matchMode, List<String> providerRoles) {
		setMatchMode(matchMode);
		this.providerRoles = providerRoles;
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (Provider) initialValue;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		
		StringBuilder sb = new StringBuilder();
		if (context.getMode().equals(FormEntryContext.Mode.VIEW)) {
			String toPrint = "";
			if (initialValue != null) {
				toPrint = initialValue.getName();
				return WidgetFactory.displayValue(toPrint);
			} else {
				return WidgetFactory.displayEmptyValue("___________");
			}
		} else {
			String placeholder = Context.getMessageSourceService().getMessage("htmlformentry.providerPlaceHolder");
			sb.append("<input type=\"text\"  id=\"" + context.getFieldName(this) + "\"" + " name=\""
			        + context.getFieldName(this) + "\" " + " onfocus=\"setupProviderAutocomplete(this);\""
			        + " class=\"autoCompleteText\"" + " onchange=\"setValWhenAutocompleteFieldBlanked(this)\""
			        + " onblur=\"onBlurAutocomplete(this)\"" + " placeholder = \"" + placeholder + "\"");
			
			if (initialValue != null) {
				sb.append(" value=\"" + (new ProviderStub(initialValue).getDisplayValue()) + "\"");
			}
			sb.append("/>\n");
			
			//Add hidden field for provider match mode
			sb.append("<input type='hidden' id='").append(context.getFieldName(this) + "_matchMode_hid'");
			sb.append(" value='").append(matchMode).append("' />\n");
			
			//Add hidden field for provider roles
			String csvRoles = StringUtils.collectionToCommaDelimitedString(providerRoles);
			String escapedCsvRoles = StringEscapeUtils.escapeJavaScript(csvRoles);
			sb.append("<input type='hidden' id='").append(context.getFieldName(this) + "_providerRoles_hid'");
			sb.append(" value='").append(escapedCsvRoles).append("' />\n");
			
			sb.append("<input name=\"" + context.getFieldName(this) + "_hid" + "\" id=\"");
			sb.append(context.getFieldName(this) + "_hid" + "\"");
			sb.append(" type=\"hidden\" class=\"autoCompleteHidden\" ");
			if (initialValue != null) {
				sb.append(" value=\"" + initialValue.getProviderId() + "\"");
			}
			sb.append("/> \n");
		}
		
		return sb.toString();
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String value = request.getParameter(context.getFieldName(this) + "_hid");
		Provider provider = null;
		if (StringUtils.hasText(value)) {
			provider = Context.getProviderService().getProvider(Integer.valueOf(value));
		}
		return provider;
	}
}
