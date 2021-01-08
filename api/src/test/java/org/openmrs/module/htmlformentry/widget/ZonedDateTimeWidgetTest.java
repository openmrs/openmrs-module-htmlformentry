package org.openmrs.module.htmlformentry.widget;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.FormEntryContext;

public class ZonedDateTimeWidgetTest extends BaseHtmlFormEntryTest {
	
	@Test
	public void generateHtml_should() throws Exception {
		Date date = new SimpleDateFormat("yyyy/mm/dd HH:mm:ss").parse("2021/01/30 18:09:35");
		
		ZonedDateTimeWidget widget = new ZonedDateTimeWidget();
		widget.setHidden(false);
		widget.setInitialValue(date);
		
		FormEntryContext formEntryContext = mock(FormEntryContext.class);
		when(formEntryContext.getFieldName(widget)).thenReturn("w1");
		when(formEntryContext.getMode()).thenReturn(FormEntryContext.Mode.VIEW);
		
		String html = widget.generateHtml(formEntryContext);
		
		System.out.println(html);
	}
	
}
