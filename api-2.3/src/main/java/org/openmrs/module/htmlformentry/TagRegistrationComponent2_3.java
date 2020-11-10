package org.openmrs.module.htmlformentry;

import org.openmrs.module.htmlformentry.handler.ConditionTagHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class TagRegistrationComponent2_3 implements ApplicationContextAware {
	
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		HtmlFormEntryService svc = (HtmlFormEntryService) context.getBean("htmlFormEntryServiceImpl");
		svc.addHandler("condition", new ConditionTagHandler());
	}
}
