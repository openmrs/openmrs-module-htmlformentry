package org.openmrs.module.htmlformentry.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Provider;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.ProviderStub;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ProviderSearchController {
	
	@RequestMapping("/module/htmlformentry/providers")
	@ResponseBody
	public Object getProviders(@RequestParam(value = "searchParam", required = false) String searchParam,
	        @RequestParam(value = "matchMode", required = false) MatchMode matchMode,
	        @RequestParam(value = "providerRoles", required = false) String providerRoles) throws Exception {
		
		List<String> providerRoleIds = new ArrayList<String>();
		if (StringUtils.isNotBlank(providerRoles)) {
			for (String roleId : providerRoles.split(",")) {
				providerRoleIds.add(roleId);
			}
		}
		
		List<Provider> providerList = HtmlFormEntryUtil.getProviders(providerRoleIds, true);
		
		List<ProviderStub> stubs;
		if (searchParam == null) {
			stubs = HtmlFormEntryUtil.getProviderStubs(providerList);
		} else {
			stubs = HtmlFormEntryUtil.getProviderStubs(providerList, searchParam, matchMode);
		}
		
		Collections.sort(stubs, new BeanComparator("name"));
		return stubs;
	}
}
