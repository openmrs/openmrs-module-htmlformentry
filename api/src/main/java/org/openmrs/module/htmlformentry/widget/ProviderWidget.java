/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.util.ProviderByPersonNameComparator;
import org.springframework.util.StringUtils;

/**
 * Widget that lets you choose a {@link Provider} from a dropdown
 */
public class ProviderWidget implements Widget {
	
	private Provider initialValue;
	
	private List<Provider> providers;
	
	public ProviderWidget(List<Provider> providers) {
		this.providers = providers;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
	 */
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (Provider) initialValue;
	}
	
	public void setProviders(List<Provider> providers) {
		this.providers = providers;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
			if (initialValue != null)
				return WidgetFactory.displayValue(initialValue.getName());
			else
				return "";
		}
		
		List<Provider> retiredProviders = new ArrayList<>();
		List<Provider> nonRetiredProviders = new ArrayList<>();
		List<Provider> sortedProviders = new ArrayList<>();
		
		for(Provider provider:providers) {
			if(provider.getRetired() == true) {
			   retiredProviders.add(provider);
			}
			else {
			   nonRetiredProviders.add(provider);
			}
		}
		Collections.sort(nonRetiredProviders, new ProviderByPersonNameComparator());
		Collections.sort(retiredProviders, new ProviderByPersonNameComparator());
		
		sortedProviders.addAll(nonRetiredProviders);
		sortedProviders.addAll(retiredProviders);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<select name=\"" + context.getFieldName(this) + "\">");
		sb.append("\n<option value=\"\">");
		sb.append(Context.getMessageSourceService().getMessage("htmlformentry.chooseAProvider"));
		sb.append("</option>");
		
		for (Provider provider : sortedProviders) {
			if (provider.getRetired()) {
				sb.append("\n<option ");
				if (initialValue != null && initialValue.equals(provider))
					sb.append("selected=\"true\" ");
				sb.append("style=\"font-style: italic\" ");
				sb.append("value=\"" + provider.getId() + "\">")
				        .append(provider.getPerson() != null
				                ? HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(provider.getPerson().getPersonName())
				                : provider.getName())
				        .append("</option>");
			} else {
				
				sb.append("\n<option ");
				if (initialValue != null && initialValue.equals(provider))
					sb.append("selected=\"true\" ");
				sb.append("value=\"" + provider.getId() + "\">")
				        .append(provider.getPerson() != null
				                ? HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(provider.getPerson().getPersonName())
				                : provider.getName())
				        .append("</option>");
			}
		}
		sb.append("</select>");
		return sb.toString();
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      HttpServletRequest)
	 */
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String val = request.getParameter(context.getFieldName(this));
		if (StringUtils.hasText(val)) {
			return Context.getProviderService().getProvider(Integer.valueOf(val));
		}
		return null;
	}

}
