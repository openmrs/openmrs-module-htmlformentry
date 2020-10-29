/*
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class NumberFieldWidgetTest {
	
	FormEntryContext context;
	
	MessageSourceService messageSourceService;
	
	NumberFieldWidget doubleWidget;
	
	NumberFieldWidget integerWidget;
	
	@Before
	public void setUp() throws Exception {
		context = mock(FormEntryContext.class);
		when(context.isAutomaticClientSideValidation()).thenReturn(true);
		when(context.isClientSideValidationHints()).thenReturn(true);
		
		messageSourceService = mock(MessageSourceService.class);
		mockStatic(Context.class);
		when(Context.getMessageSourceService()).thenReturn(messageSourceService);
		
		doubleWidget = new NumberFieldWidget(null, null, true);
		integerWidget = new NumberFieldWidget(null, null, false);
		when(context.getFieldName(doubleWidget)).thenReturn("doubleWidget");
		when(context.getFieldName(integerWidget)).thenReturn("integerWidget");
	}
	
	@Test
	public void testNoAttributesWhenNotNeeded() {
		NumberFieldWidget widget = new NumberFieldWidget(null, null, true);
		String html = widget.generateHtml(context);
		assertThat(getAttribute(html, "class"), is("number"));
		assertThat(getAttribute(html, "min"), nullValue());
		assertThat(getAttribute(html, "max"), nullValue());
	}
	
	@Test
	public void testRangeAttributes() {
		NumberFieldWidget widget = new NumberFieldWidget(1d, 100d, false);
		String html = widget.generateHtml(context);
		assertThat(getAttribute(html, "class"), is("integer numeric-range"));
		assertThat(getAttribute(html, "min"), is("1.0"));
		assertThat(getAttribute(html, "max"), is("100.0"));
	}
	
	@Test
	public void testDisplaysWholeNumbersWithoutDecimal() throws Exception {
		when(context.getMode()).thenReturn(FormEntryContext.Mode.VIEW);
		
		NumberFieldWidget widget = new NumberFieldWidget(null, null, true);
		widget.setInitialValue(100d);
		
		assertThat(widget.generateHtml(context), is("<span class=\"value\">100</span>"));
	}
	
	@Test
	public void testShowsWholeNumberWithoutDecimalInEditMode() throws Exception {
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		
		NumberFieldWidget widget = new NumberFieldWidget(null, null, true);
		widget.setInitialValue(100d);
		
		String html = widget.generateHtml(context);
		assertThat(getAttribute(html, "value"), is("100"));
	}
	
	@Test
	public void testReturnDoubleIfFloatingPointTrue() throws Exception {
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("doubleWidget", "1.2");
		Number val = doubleWidget.getValue(context, request);
		assertThat(val.getClass(), is(Double.class));
		assertThat(val, is(1.2d));
	}
	
	@Test
	public void testThrowsExceptionIfInvalidFloatingPointConfigured() throws Exception {
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("doubleWidget", "one");
		Exception foundException = null;
		try {
			doubleWidget.getValue(context, request);
		}
		catch (Exception e) {
			assertThat(e.getClass(), is(IllegalArgumentException.class));
			foundException = e;
		}
		assertThat(foundException, notNullValue());
	}
	
	@Test
	public void testReturnIntegerIfFloatingPointFalse() throws Exception {
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("integerWidget", "12");
		Number val = integerWidget.getValue(context, request);
		assertThat(val.getClass(), is(Integer.class));
		assertThat(val, is(12));
	}
	
	@Test
	public void testThrowsExceptionIfInvalidIntegerConfigured() throws Exception {
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("integerWidget", "3.2");
		Exception foundException = null;
		try {
			integerWidget.getValue(context, request);
		}
		catch (Exception e) {
			assertThat(e.getClass(), is(IllegalArgumentException.class));
			foundException = e;
		}
		assertThat(foundException, notNullValue());
	}
	
	/**
	 * Hacky, incomplete, but good enough for these tests
	 */
	private String getAttribute(String html, String attr) {
		int index = html.indexOf(attr + "=\"");
		if (index < 0) {
			return null;
		}
		int startIndex = html.indexOf("\"", index) + 1;
		int endingQuote = html.indexOf("\"", startIndex);
		return html.substring(startIndex, endingQuote);
	}
	
}
