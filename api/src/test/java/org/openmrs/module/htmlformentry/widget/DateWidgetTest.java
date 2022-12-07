package org.openmrs.module.htmlformentry.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class })
@PowerMockIgnore("javax.management.*")
public class DateWidgetTest {
	
	@Mock
	private AdministrationService administrationService;
	
	@Before
	public void setUp() throws Exception {
		
		// Prepare static mocks
		PowerMockito.mockStatic(Context.class);
		administrationService.setGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS, "false");
		PowerMockito.when(Context.getAdministrationService()).thenReturn(administrationService);
		
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
