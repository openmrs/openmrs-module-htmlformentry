package org.openmrs.module.htmlformentry.widget;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptName;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.TestUtil;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConceptSeachAutocompleteWidgetTest {


    private FormEntryContext context;

    @Before
    public void setup() {
        context = mock(FormEntryContext.class);
        when(context.getFieldName(argThat(any(Widget.class)))).thenReturn("w2");
    }


    @Test
    public void generateHtml_shouldRenderProperHtmlWithConceptList() {

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);

        ConceptSearchAutocompleteWidget conceptSearchAutocompleteWidget = new ConceptSearchAutocompleteWidget(generateConceptList(), null);
        String html = conceptSearchAutocompleteWidget.generateHtml(context);

        TestUtil.assertFuzzyContains("<input type=\"text\" id=\"w2\" name=\"w2\" onfocus=\"setupAutocomplete\\(this, 'conceptSearch.form','1,2,3','null'\\); \"class=\"autoCompleteText\" onchange=\"setValWhenAutocompleteFieldBlanked\\(this\\)\" onblur=\"onBlurAutocomplete\\(this\\)\"/>" +
                        "<input name=\"w2_hid\" id=\"w2_hid\" type=\"hidden\" class=\"autoCompleteHidden\" />",
                html);
    }

    @Test
    public void generateHtml_shouldRenderProperHtmlWithConceptClassList() {

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);

        ConceptSearchAutocompleteWidget conceptSearchAutocompleteWidget = new ConceptSearchAutocompleteWidget(null, generateConceptClassList());
        String html = conceptSearchAutocompleteWidget.generateHtml(context);

        TestUtil.assertFuzzyContains("<input type=\"text\" id=\"w2\" name=\"w2\" onfocus=\"setupAutocomplete\\(this, 'conceptSearch.form','null','someClass,anotherClass'\\); \"class=\"autoCompleteText\" onchange=\"setValWhenAutocompleteFieldBlanked\\(this\\)\" onblur=\"onBlurAutocomplete\\(this\\)\"/>" +
                        "<input name=\"w2_hid\" id=\"w2_hid\" type=\"hidden\" class=\"autoCompleteHidden\" />",
                html);
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
