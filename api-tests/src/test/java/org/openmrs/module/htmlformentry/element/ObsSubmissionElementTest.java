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

package org.openmrs.module.htmlformentry.element;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.Translator;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.util.LocaleUtility;

/**
 *
 */
public class ObsSubmissionElementTest {
	
	private FormEntryContext context;
	
	private Map<String, String> params;
	
	private MockedStatic<Context> mockedContext;
	
	private MockedStatic<HtmlFormEntryUtil> mockedHtmlFormEntryUtil;
	
	@Before
	public void setUp() {
		mockedContext = mockStatic(Context.class);
		mockedHtmlFormEntryUtil = mockStatic(HtmlFormEntryUtil.class);
		HtmlFormSchema schema = new HtmlFormSchema();
		Translator translator = new Translator() {
			
			@Override
			public String translate(String localeStr, String key) {
				return key;
			}
		};
		
		params = new HashMap<>();
		
		context = mock(FormEntryContext.class);
		when(context.getSchema()).thenReturn(schema);
		when(context.getTranslator()).thenReturn(translator);
	}
	
	@After
	public void tearDown() {
		mockedContext.close();
		mockedHtmlFormEntryUtil.close();
	}
	
	@Test
	public void testShowUnitsUsingTrue() {
		ConceptDatatype numeric = new ConceptDatatype();
		numeric.setUuid(ConceptDatatype.NUMERIC_UUID);
		
		ConceptNumeric weight = new ConceptNumeric();
		String units = "kg";
		weight.setUnits(units);
		weight.setDatatype(numeric);
		
		when(HtmlFormEntryUtil.getConcept(anyString())).thenReturn(weight);
		
		params.put("showUnits", "true");
		params.put("conceptId", "5089");
		params.put("locale", "ht");
		ObsSubmissionElement element = new ObsSubmissionElement<>(context, params);
		String html = element.generateHtml(context);
		
		TestUtil.assertContains("<span class=\"units\">" + units + "</span>", html);
	}
	
	@Test
	@Ignore
	public void testShowUnitsUsingCode() {
		ConceptDatatype numeric = new ConceptDatatype();
		numeric.setUuid(ConceptDatatype.NUMERIC_UUID);
		
		ConceptNumeric weight = new ConceptNumeric();
		weight.setUnits("kg");
		weight.setDatatype(numeric);
		
		when(HtmlFormEntryUtil.getConcept(anyString())).thenReturn(weight);
		
		String unitsCode = "units.kg";
		params.put("showUnits", unitsCode);
		params.put("conceptId", "5089");
		ObsSubmissionElement element = new ObsSubmissionElement(context, params);
		String html = element.generateHtml(context);
		
		TestUtil.assertContains("<span class=\"units\">" + unitsCode + "</span>", html);
	}
	
	@Test
	public void testForcingLocale() throws Exception {
		ConceptDatatype numeric = new ConceptDatatype();
		numeric.setUuid(ConceptDatatype.NUMERIC_UUID);
		
		ConceptName primaryName = new ConceptName("Weight", Locale.ENGLISH);
		primaryName.setLocalePreferred(true);
		primaryName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		
		ConceptNumeric weight = new ConceptNumeric();
		String units = "kg";
		weight.setUnits(units);
		weight.setDatatype(numeric);
		weight.addName(primaryName);
		weight.addName(new ConceptName("Pwa", LocaleUtility.fromSpecification("ht")));
		
		when(HtmlFormEntryUtil.getConcept(anyString())).thenReturn(weight);
		
		params.put("conceptId", "5089");
		params.put("locale", "ht");
		params.put("labelNameTag", "default");
		
		ObsSubmissionElement element = new ObsSubmissionElement(context, params);
		String html = element.generateHtml(context);
		assertTrue(html.startsWith("Pwa"));
	}
	
	@Test
	public void testForcingLocaleOnCheckboxLabel() throws Exception {
		ConceptDatatype coded = new ConceptDatatype();
		coded.setUuid(ConceptDatatype.CODED_UUID);
		
		Concept option = new Concept(456);
		option.addName(new ConceptName("English", Locale.ENGLISH));
		option.addName(new ConceptName("Kreyol", LocaleUtility.fromSpecification("ht")));
		
		Concept options = new Concept(123);
		options.setDatatype(coded);
		options.addAnswer(new ConceptAnswer(option));
		
		when(HtmlFormEntryUtil.getConcept("123")).thenReturn(options);
		when(HtmlFormEntryUtil.getConcept("456")).thenReturn(option);
		
		params.put("conceptId", "123");
		params.put("answerConceptId", "456");
		params.put("locale", "ht");
		
		ObsSubmissionElement element = new ObsSubmissionElement(context, params);
		String html = element.generateHtml(context);
		assertTrue(html.matches(".*<label.*>Kreyol</label>.*"));
	}
	
	@Test
	public void testForcingLocaleOnRadioLabels() throws Exception {
		ConceptDatatype coded = new ConceptDatatype();
		coded.setUuid(ConceptDatatype.CODED_UUID);
		
		Concept option1 = new Concept(1);
		option1.addName(new ConceptName("English 1", Locale.ENGLISH));
		option1.addName(new ConceptName("Kreyol 1", LocaleUtility.fromSpecification("ht")));
		
		Concept option2 = new Concept(2);
		option2.addName(new ConceptName("English 2", Locale.ENGLISH));
		option2.addName(new ConceptName("Kreyol 2", LocaleUtility.fromSpecification("ht")));
		
		Concept options = new Concept(123);
		options.setDatatype(coded);
		options.addAnswer(new ConceptAnswer(option1));
		options.addAnswer(new ConceptAnswer(option2));
		
		when(HtmlFormEntryUtil.getConcept("123")).thenReturn(options);
		when(HtmlFormEntryUtil.getConcept("1")).thenReturn(option1);
		when(HtmlFormEntryUtil.getConcept("2")).thenReturn(option2);
		
		params.put("conceptId", "123");
		params.put("answerConceptIds", "1,2");
		params.put("style", "radio");
		params.put("locale", "ht");
		
		ObsSubmissionElement element = new ObsSubmissionElement(context, params);
		String html = element.generateHtml(context);
		System.out.println(html);
		assertTrue(html.matches(".*<label.*>Kreyol 1</label>.*<label.*>Kreyol 2</label>.*"));
	}
	
	@Test(expected = RuntimeException.class)
	public void testNoConceptId_shouldThrowException() {
		new ObsSubmissionElement(context, params);
	}
	
	@Test(expected = RuntimeException.class)
	public void testDuplicateConceptIds_shouldThrowException() {
		params.put("conceptId", "1");
		params.put("conceptIds", "2");
		new ObsSubmissionElement(context, params);
	}
	
	@Test(expected = RuntimeException.class)
	public void testInvalidConceptIds_shouldThrowException() {
		params.put("conceptIds", "adas,asda");
		new ObsSubmissionElement(context, params);
	}
	
	@Test(expected = RuntimeException.class)
	public void testEmptyConceptIds_shouldThrowException() {
		params.put("conceptIds", "");
		new ObsSubmissionElement(context, params);
	}
	
	@Test(expected = RuntimeException.class)
	public void testInvalidAnswerConceptIds_shouldThrowException() {
		params.put("answerConceptIds", "adas,asda");
		new ObsSubmissionElement(context, params);
	}
}
