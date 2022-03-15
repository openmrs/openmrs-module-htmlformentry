package org.openmrs.module.htmlformentry;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.handler.ConditionTagHandler;
import org.openmrs.module.htmlformentry.handler.ObsTagHandler2_4;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@OpenmrsProfile(openmrsPlatformVersion = "2.4.* - 2.*")
public class TagRegistrationComponent2_4 implements ApplicationContextAware {
	
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		HtmlFormEntryService svc = (HtmlFormEntryService) context.getBean("htmlFormEntryServiceImpl");
		svc.addHandler("condition", new ConditionTagHandler());
		svc.addHandler("obs", new ObsTagHandler2_4());
	}
}
