package org.openmrs.module.htmlformentry.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Provider;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.ProviderStub;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ProviderSearchController {

    @RequestMapping("/module/htmlformentry19/providers")
    @ResponseBody
    public Object getProviders(@RequestParam(value="searchParam",required = false) String searchParam,
                               @RequestParam(value="matchMode",required=false)MatchMode matchMode)
            throws Exception {
        ProviderService ps = Context.getProviderService();

        List<Provider> providerList = null;
        List<ProviderStub> ret = null;
        if(searchParam == null) {
            providerList = ps.getAllProviders();
            ret = HtmlFormEntryUtil.getProviderStubs(providerList);
        }
        else {
            providerList = ps.getProviders(searchParam, null, null, null);
            ret = HtmlFormEntryUtil.getProviderStubs(providerList,searchParam,matchMode);
        }

        return ret;
    }
}
