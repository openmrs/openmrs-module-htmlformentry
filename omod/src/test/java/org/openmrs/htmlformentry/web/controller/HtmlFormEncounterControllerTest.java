package org.openmrs.htmlformentry.web.controller;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.api.LocationService;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.HtmlFormSection;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.openmrs.module.htmlformentry.web.controller.HtmlFormEncounterController;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlFormEncounterControllerTest {

    public static final SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSSZ");

    @Test
    public void shouldBuildSchemaWithSections() {

        HtmlFormSchema schema = new HtmlFormSchema();
        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");
        schema.getSections().add(section1);
        HtmlFormSection section2 = new HtmlFormSection();
        section2.setName("Section 2");
        schema.getSections().add(section2);

        JsonNode schemaAsJson = new HtmlFormEncounterController().buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));
        assertThat(schemaAsJson.get("sections").get(1).get("name").getValueAsText(), is("Section 2"));

    }

    @Test
    public void shouldBuildSchemaWithSectionAndSeparateField() {

        HtmlFormSchema schema = new HtmlFormSchema();

        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");
        schema.getSections().add(section1);

        ObsField field1 = new ObsField();
        field1.setName("Field 1");
        schema.getFields().add(field1);

        JsonNode schemaAsJson = new HtmlFormEncounterController().buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));
        assertThat(schemaAsJson.get("fields").get(0).get("name").getValueAsText(), is("Field 1"));

    }

    @Test
    public void shouldBuildSchemaWithSectionsNestedSections() {

        HtmlFormSchema schema = new HtmlFormSchema();

        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");

        HtmlFormSection section1a = new HtmlFormSection();
        section1a.setName("Section 1a");
        section1.addChildSection(section1a);

        HtmlFormSection section1b = new HtmlFormSection();
        section1b.setName("Section 1b");
        section1.addChildSection(section1b);

        schema.getSections().add(section1);

        HtmlFormSection section2 = new HtmlFormSection();
        section2.setName("Section 2");
        schema.getSections().add(section2);

        JsonNode schemaAsJson = new HtmlFormEncounterController().buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("sections").get(0).get("name").getValueAsText(), is("Section 1a"));
        assertThat(schemaAsJson.get("sections").get(0).get("sections").get(1).get("name").getValueAsText(), is("Section 1b"));
        assertThat(schemaAsJson.get("sections").get(1).get("name").getValueAsText(), is("Section 2"));

    }

    @Test
    public void shouldBuildSchemaWithSectionsAndObsFields() {

        HtmlFormSchema schema = new HtmlFormSchema();
        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");
        schema.getSections().add(section1);


        ObsField field1 = new ObsField();
        field1.setName("Field 1");
        ConceptName conceptName1 = new ConceptName();
        conceptName1.setName("Should Not Be Overridden by field name");
        Concept concept1 = mock(Concept.class);
        when(concept1.getName()).thenReturn(conceptName1);
        ConceptDatatype datatype1 = mock(ConceptDatatype.class);
        when(datatype1.getName()).thenReturn("Numeric");
        when(concept1.getDatatype()).thenReturn(datatype1);
        field1.setQuestion(concept1);
        section1.addField(field1);

        ObsField field2 = new ObsField();
        ConceptName conceptName2 = new ConceptName();
        conceptName2.setName("Field 2");
        Concept concept2 = mock(Concept.class);
        when(concept2.getName()).thenReturn(conceptName2);
        ConceptDatatype datatype2 = mock(ConceptDatatype.class);
        when(datatype2.getName()).thenReturn("Datetime");
        when(concept2.getDatatype()).thenReturn(datatype2);
        field2.setQuestion(concept2);
        section1.addField(field2);

        HtmlFormSection section2 = new HtmlFormSection();
        section2.setName("Section 2");
        schema.getSections().add(section2);

        JsonNode schemaAsJson = new HtmlFormEncounterController().buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("name").getValueAsText(), is("Field 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("datatype").getValueAsText(), is("numeric"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(1).get("name").getValueAsText(), is("Field 2"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(1).get("datatype").getValueAsText(), is("datetime"));
        assertThat(schemaAsJson.get("sections").get(1).get("name").getValueAsText(), is("Section 2"));

    }

    @Test
    public void shouldBuildSchemaWithSectionsAndObsGroup() {

        HtmlFormSchema schema = new HtmlFormSchema();
        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");
        schema.getSections().add(section1);

        ConceptName groupConceptName = new ConceptName();
        groupConceptName.setName("ObsGroup");
        Concept groupConcept = mock(Concept.class);
        when(groupConcept.getName()).thenReturn(groupConceptName);
        ObsGroup obsGroup = new ObsGroup(groupConcept);
        section1.getFields().add(obsGroup);

        ObsField field1 = new ObsField();
        field1.setName("Field 1");
        ConceptName conceptName1 = new ConceptName();
        conceptName1.setName("Should Not Be Overridden by field name");
        Concept concept1 = mock(Concept.class);
        when(concept1.getName()).thenReturn(conceptName1);
        ConceptDatatype datatype1 = mock(ConceptDatatype.class);
        when(datatype1.getName()).thenReturn("Numeric");
        when(concept1.getDatatype()).thenReturn(datatype1);
        field1.setQuestion(concept1);
        obsGroup.addChild(field1);

        ObsField field2 = new ObsField();
        ConceptName conceptName2 = new ConceptName();
        conceptName2.setName("Field 2");
        Concept concept2 = mock(Concept.class);
        when(concept2.getName()).thenReturn(conceptName2);
        ConceptDatatype datatype2 = mock(ConceptDatatype.class);
        when(datatype2.getName()).thenReturn("Datetime");
        when(concept2.getDatatype()).thenReturn(datatype2);
        field2.setQuestion(concept2);
        obsGroup.addChild(field2);

        JsonNode schemaAsJson = new HtmlFormEncounterController().buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("name").getValueAsText(), is("ObsGroup"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(0).get("name").getValueAsText(), is("Field 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(0).get("datatype").getValueAsText(), is("numeric"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(1).get("name").getValueAsText(), is("Field 2"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(1).get("datatype").getValueAsText(), is("datetime"));

    }


    @Test
    public void shouldBuildSchemaWithSectionsAndNestedObsGroup() {

        HtmlFormSchema schema = new HtmlFormSchema();
        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");
        schema.getSections().add(section1);

        ConceptName groupConceptName = new ConceptName();
        groupConceptName.setName("ObsGroup");
        Concept groupConcept = mock(Concept.class);
        when(groupConcept.getName()).thenReturn(groupConceptName);
        ObsGroup obsGroup = new ObsGroup(groupConcept);
        section1.getFields().add(obsGroup);

        ConceptName nestedGroupName = new ConceptName();
        nestedGroupName.setName("Nested ObsGroup");
        Concept nestedGroupConcept = mock(Concept.class);
        when(nestedGroupConcept.getName()).thenReturn(nestedGroupName);
        ObsGroup nestedGroup = new ObsGroup(nestedGroupConcept);
        obsGroup.addChild(nestedGroup);

        ObsField field1 = new ObsField();
        field1.setName("Field 1");
        ConceptName conceptName1 = new ConceptName();
        conceptName1.setName("Should Not Be Overridden by field name");
        Concept concept1 = mock(Concept.class);
        when(concept1.getName()).thenReturn(conceptName1);
        ConceptDatatype datatype1 = mock(ConceptDatatype.class);
        when(datatype1.getName()).thenReturn("Numeric");
        when(concept1.getDatatype()).thenReturn(datatype1);
        field1.setQuestion(concept1);
        nestedGroup.addChild(field1);

        JsonNode schemaAsJson = new HtmlFormEncounterController().buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("name").getValueAsText(), is("ObsGroup"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(0).get("name").getValueAsText(), is("Nested ObsGroup"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(0).get("fields").get(0).get("name").getValueAsText(), is("Field 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("fields").get(0).get("fields").get(0).get("datatype").getValueAsText(), is("numeric"));

    }

    @Test
    public void shouldBuildSchemaWithSectionsAndObsFieldsWithValues() {

        HtmlFormSchema schema = new HtmlFormSchema();
        HtmlFormSection section1 = new HtmlFormSection();
        section1.setName("Section 1");
        schema.getSections().add(section1);

        ObsField field1 = new ObsField();
        field1.setName("Field 1");
        ConceptName conceptName1 = new ConceptName();
        conceptName1.setName("Should Not Be Overridden by field name");
        Concept concept1 = mock(Concept.class);
        when(concept1.getName()).thenReturn(conceptName1);
        ConceptDatatype datatype1 = mock(ConceptDatatype.class);
        when(datatype1.getName()).thenReturn("Numeric");
        when(datatype1.isNumeric()).thenReturn(true);
        when(concept1.getDatatype()).thenReturn(datatype1);
        field1.setQuestion(concept1);
        Obs obs1 = new Obs();
        obs1.setValueNumeric(new Double(123));
        field1.setExistingObs(obs1);
        section1.addField(field1);

        ObsField field2 = new ObsField();
        ConceptName conceptName2 = new ConceptName();
        conceptName2.setName("Field 2");
        Concept concept2 = mock(Concept.class);
        when(concept2.getName()).thenReturn(conceptName2);
        ConceptDatatype datatype2 = mock(ConceptDatatype.class);
        when(datatype2.getName()).thenReturn("Datetime");
        when(datatype2.isDateTime()).thenReturn(true);
        when(concept2.getDatatype()).thenReturn(datatype2);
        field2.setQuestion(concept2);
        Obs obs2 = new Obs();
        Date obsDateValue = new Date();
        obs2.setValueDate(obsDateValue);
        field2.setExistingObs(obs2);
        section1.addField(field2);

        ObsField field3 = new ObsField();
        field3.setName("Field 3");
        Concept concept3 = mock(Concept.class);
        ConceptDatatype datatype3 = mock(ConceptDatatype.class);
        when(datatype3.getName()).thenReturn("Time");
        when(datatype3.isTime()).thenReturn(true);
        when(concept3.getDatatype()).thenReturn(datatype3);
        field3.setQuestion(concept3);
        Obs obs3 = new Obs();
        obs3.setValueDate(obsDateValue);
        field3.setExistingObs(obs3);
        section1.addField(field3);

        ObsField field4 = new ObsField();
        field4.setName("Field 4");
        Concept concept4 = mock(Concept.class);
        ConceptDatatype datatype4 = mock(ConceptDatatype.class);
        when(datatype4.getName()).thenReturn("Date");
        when(datatype4.isDate()).thenReturn(true);
        when(concept4.getDatatype()).thenReturn(datatype4);
        field4.setQuestion(concept4);
        Obs obs4 = new Obs();
        obs4.setValueDate(obsDateValue);
        field4.setExistingObs(obs4);
        section1.addField(field4);

        ObsField field5 = new ObsField();
        field5.setName("Field 5");
        Concept concept5 = mock(Concept.class);
        ConceptDatatype datatype5 = mock(ConceptDatatype.class);
        when(datatype5.getName()).thenReturn("boolean");
        when(datatype5.isBoolean()).thenReturn(true);
        when(concept5.getDatatype()).thenReturn(datatype5);
        field5.setQuestion(concept5);
        Obs obs5 = mock(Obs.class);
        when(obs5.getValueBoolean()).thenReturn(false);
        field5.setExistingObs(obs5);
        section1.addField(field5);

        ObsField field6 = new ObsField();
        field6.setName("Field 6");
        Concept concept6 = mock(Concept.class);
        ConceptDatatype datatype6 = mock(ConceptDatatype.class);
        when(datatype6.getName()).thenReturn("text");
        when(datatype6.isText()).thenReturn(true);
        when(concept6.getDatatype()).thenReturn(datatype6);
        field6.setQuestion(concept6);
        Obs obs6 = new Obs();
        obs6.setValueText("test test");
        field6.setExistingObs(obs6);
        section1.addField(field6);

        ObsField field7 = new ObsField();
        field7.setName("Field 7");
        Concept concept7 = mock(Concept.class);
        ConceptDatatype datatype7 = mock(ConceptDatatype.class);
        when(datatype7.getName()).thenReturn("coded");
        when(datatype7.isCoded()).thenReturn(true);
        when(concept7.getDatatype()).thenReturn(datatype7);
        field7.setQuestion(concept7);
        Obs obs7 = mock(Obs.class);
        ConceptName conceptName7 = new ConceptName();
        conceptName7.setName("TB");
        when(obs7.getValueCodedName()).thenReturn(conceptName7);
        field7.setExistingObs(obs7);
        section1.addField(field7);
        HtmlFormSection section2 = new HtmlFormSection();
        section2.setName("Section 2");
        schema.getSections().add(section2);

        ObsField field8 = new ObsField();
        field8.setName("Field 8");
        Concept concept8 = mock(Concept.class);
        ConceptDatatype datatype8 = mock(ConceptDatatype.class);
        when(datatype8.getName()).thenReturn("text");
        when(datatype8.isText()).thenReturn(true);
        when(concept8.getDatatype()).thenReturn(datatype8);
        field8.setQuestion(concept8);
        Obs obs8 = new Obs();
        obs8.setValueText("34");
        obs8.setComment("org.openmrs.Location");
        field8.setExistingObs(obs8);
        section1.addField(field8);

        Location location = new Location();
        location.setName("Boston");
        LocationService locationService = mock(LocationService.class);
        when(locationService.getLocation(34)).thenReturn(location);

        HtmlFormEncounterController controller = new HtmlFormEncounterController();
        controller.setLocationService(locationService);

        JsonNode schemaAsJson = controller.buildSchemaAsJsonNode(schema, new ObjectMapper());
        assertThat(schemaAsJson.get("sections").get(0).get("name").getValueAsText(), is("Section 1"));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("name").getValueAsText(), is("Field 1"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("datatype").getValueAsText(), is("numeric"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(0).get("value").getValueAsText(), is("123.0"));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(1).get("name").getValueAsText(), is("Field 2"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(1).get("datatype").getValueAsText(), is("datetime"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(1).get("value").getValueAsText(), is(datetimeFormat.format(obsDateValue)));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(2).get("name").getValueAsText(), is("Field 3"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(2).get("datatype").getValueAsText(), is("time"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(2).get("value").getValueAsText(), is(timeFormat.format(obsDateValue)));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(3).get("name").getValueAsText(), is("Field 4"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(3).get("datatype").getValueAsText(), is("date"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(3).get("value").getValueAsText(), is(dateFormat.format(obsDateValue)));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(4).get("name").getValueAsText(), is("Field 5"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(4).get("datatype").getValueAsText(), is("boolean"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(4).get("value").getValueAsText(), is("false"));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(5).get("name").getValueAsText(), is("Field 6"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(5).get("datatype").getValueAsText(), is("text"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(5).get("value").getValueAsText(), is("test test"));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(6).get("name").getValueAsText(), is("Field 7"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(6).get("datatype").getValueAsText(), is("coded"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(6).get("value").getValueAsText(), is("TB"));

        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(7).get("name").getValueAsText(), is("Field 8"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(7).get("datatype").getValueAsText(), is("text"));
        assertThat(schemaAsJson.get("sections").get(0).get("fields").get(7).get("value").getValueAsText(), is("Boston"));


        assertThat(schemaAsJson.get("sections").get(1).get("name").getValueAsText(), is("Section 2"));

    }

}
