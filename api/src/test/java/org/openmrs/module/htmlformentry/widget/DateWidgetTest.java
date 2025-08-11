package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

public class DateWidgetTest extends BaseModuleContextSensitiveTest {
	
	private AdministrationService administrationService;
	
	@BeforeEach
	public void setUp() throws Exception {
		administrationService = Context.getAdministrationService();
		administrationService.setGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS, "false");
		
	}
	
	@Test
	public void testHidden() throws Exception {
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2014-10-01");
		
		DateWidget widget = new DateWidget();
		widget.setHidden(true);
		widget.setInitialValue(date);
		
		FormEntryContext formEntryContext = mock(FormEntryContext.class);
		when(formEntryContext.getFieldName(widget)).thenReturn("w1");
		
		String html = widget.generateHtml(formEntryContext);
		assertThat(html, is("<input type=\"hidden\" name=\"w1\" id=\"w1\" value=\"2014-10-01\" />"));
	}
	
	@Test
	public void testNonHiddenByDefault() throws Exception {
		DateWidget widget = new DateWidget();
		assertThat(widget.isHidden(), is(false));
	}
	
	@Test
	public void testSetOnChangeFunction() throws Exception {
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2014-10-01");
		
		DateWidget widget = new DateWidget();
		widget.setHidden(true);
		widget.setInitialValue(date);
		widget.setDateFormat("yyyy-MM-dd");
		widget.setOnChangeFunction("changeFunc");
		
		FormEntryContext formEntryContext = mock(FormEntryContext.class);
		when(formEntryContext.getFieldName(widget)).thenReturn("w1");
		
		String html = widget.generateHtml(formEntryContext);
		assertThat(html,
		    is("<input type=\"hidden\" name=\"w1\" id=\"w1\" onChange=\"changeFunc\"  value=\"2014-10-01\" />"));
	}
}
