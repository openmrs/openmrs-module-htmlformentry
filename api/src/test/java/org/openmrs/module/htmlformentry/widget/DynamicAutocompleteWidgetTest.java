package org.openmrs.module.htmlformentry.widget;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptName;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.TestUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;



public class DynamicAutocompleteWidgetTest {

    private FormEntryContext context;

    private HttpServletRequest request;

    @Before
    public void setup() {
        request = mock(HttpServletRequest.class);
        context = mock(FormEntryContext.class);
        when(context.getFieldName(argThat(any(Widget.class)))).thenReturn("w2");
    }


    @Test
    public void generateHtml_shouldRenderProperHtmlWithConceptList() {

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);

        DynamicAutocompleteWidget dynamicAutocompleteWidget = new DynamicAutocompleteWidget(generateConceptList(), null);
        String html = dynamicAutocompleteWidget.generateHtml(context);

        TestUtil.assertFuzzyContains("<div id=\"w2_div\" class=\"dynamicAutocomplete\">" +
            "<input name=\"w2_hid\" id=\"w2_hid\" type=\"hidden\" class=\"autoCompleteHidden\" />" +
            "<input type=\"text\"  id=\"w2\" name=\"w2\" onfocus=\"setupAutocomplete\\(this, 'conceptSearch.form','1,2,3','null'\\); \"class=\"autoCompleteText\" onBlur=\"onBlurAutocomplete\\(this\\)\"/>" +
            "<input id=\"w2_button\" type=\"button\" class=\"addConceptButton\" value=\"Add\" /></div>",
                html);

    }

    @Test
    public void generateHtml_shouldRenderProperHtmlWithConceptClasses() {

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);

        DynamicAutocompleteWidget dynamicAutocompleteWidget = new DynamicAutocompleteWidget(null, generateConceptClassList());
        String html = dynamicAutocompleteWidget.generateHtml(context);

        TestUtil.assertFuzzyContains("<div id=\"w2_div\" class=\"dynamicAutocomplete\">" +
                "<input name=\"w2_hid\" id=\"w2_hid\" type=\"hidden\" class=\"autoCompleteHidden\" />" +
                "<input type=\"text\"  id=\"w2\" name=\"w2\" onfocus=\"setupAutocomplete\\(this, 'conceptSearch.form','null','someClass,anotherClass'\\); \"class=\"autoCompleteText\" onBlur=\"onBlurAutocomplete\\(this\\)\"/>" +
                "<input id=\"w2_button\" type=\"button\" class=\"addConceptButton\" value=\"Add\" /></div>",
                html);

    }

    @Test
    public void generateHtml_shouldRenderSingleConceptProperlyInViewMode() {

        when(context.getMode()).thenReturn(FormEntryContext.Mode.VIEW);

        DynamicAutocompleteWidget dynamicAutocompleteWidget = new DynamicAutocompleteWidget(generateConceptList(), null);

        Concept initialValue = mock(Concept.class);
        when(initialValue.getDisplayString()).thenReturn("concept1");
        dynamicAutocompleteWidget.setInitialValue(initialValue);

        String html = dynamicAutocompleteWidget.generateHtml(context);

        TestUtil.assertFuzzyContains("concept1", html);
    }

    @Test
    public void generateHtml_shouldRenderMultipleConceptProperlyInViewMode() {

        when(context.getMode()).thenReturn(FormEntryContext.Mode.VIEW);

        DynamicAutocompleteWidget dynamicAutocompleteWidget = new DynamicAutocompleteWidget(generateConceptList(), null);

        Concept initialValue1 = mock(Concept.class);
        when(initialValue1.getDisplayString()).thenReturn("concept1");
        dynamicAutocompleteWidget.addInitialValue(initialValue1);

        Concept initialValue2 = mock(Concept.class);
        when(initialValue2.getDisplayString()).thenReturn("concept2");
        dynamicAutocompleteWidget.addInitialValue(initialValue2);

        String html = dynamicAutocompleteWidget.generateHtml(context);

        TestUtil.assertFuzzyContains("concept1;concept2;", html);
    }


    @Test
    public void getValue_shouldReturnProperValues() {

        when(request.getParameter("w2_hid")).thenReturn("3");
        when(request.getParameter("w2span_0_hid")).thenReturn("1001");
        when(request.getParameter("w2span_1_hid")).thenReturn("1002");
        when(request.getParameter("w2span_2_hid")).thenReturn("1003");

        DynamicAutocompleteWidget dynamicAutocompleteWidget = new DynamicAutocompleteWidget(generateConceptList(), null);
        List values = (List) dynamicAutocompleteWidget.getValue(context, request);
        assertThat(values.size(), is(3));

        Set<String> results = new HashSet<String>();

        for (Object val : values) {
            results.add((String) val);
        }

        assertTrue(results.contains("1001"));
        assertTrue(results.contains("1002"));
        assertTrue(results.contains("1003"));
    }

    @Test
    public void getValue_shouldReturnEmptyListIfNoValuesInRequest() {
        DynamicAutocompleteWidget dynamicAutocompleteWidget = new DynamicAutocompleteWidget(generateConceptList(), null);
        List values = (List) dynamicAutocompleteWidget.getValue(context, request);
        assertThat(values.size(), is(0));
    }


    private List<Concept> generateConceptList() {

        List<Concept>  conceptList = new ArrayList<Concept>();

        Concept concept1 = new Concept();
        concept1.setConceptId(1);
        ConceptName concept1Name = new ConceptName();
        concept1Name.setName("concept1");
        concept1.addName(concept1Name);
        conceptList.add(concept1);

        Concept concept2 = new Concept();
        concept2.setConceptId(2);
        ConceptName concept2Name = new ConceptName();
        concept1Name.setName("concept2");
        concept1.addName(concept2Name);
        conceptList.add(concept2);

        Concept concept3 = new Concept();
        concept3.setConceptId(3);
        ConceptName concept3Name = new ConceptName();
        concept1Name.setName("concept3");
        concept1.addName(concept3Name);
        conceptList.add(concept3);

        return conceptList;
    }

    private List<ConceptClass> generateConceptClassList() {

        List<ConceptClass> conceptClassList = new ArrayList<ConceptClass>();

        ConceptClass conceptClass1 = new ConceptClass();
        conceptClass1.setName("someClass");
        conceptClassList.add(conceptClass1);

        ConceptClass conceptClass2 = new ConceptClass();
        conceptClass2.setName("anotherClass");
        conceptClassList.add(conceptClass2);

        return conceptClassList;
    }
}
