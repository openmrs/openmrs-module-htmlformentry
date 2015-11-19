package org.openmrs.module.htmlformentry.web.controller;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.HtmlFormSection;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.http.HttpSession;

/**
 * Returns a simple JSON representation of an Encounter, organized by the schema associated with the form
 */
@Controller
public class HtmlFormEncounterController {

    @Autowired
    private LocationService locationService;

    // ISO standard date formats
    public static final SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSSZ");

    // TODO does not handle DrugOrder and Regimen schema objects--currently only works for obs and obsgroups
    // TODO schema display still works?
    @RequestMapping(value = "/module/htmlformentry/encounter.json", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode encounterSchemaAsJson(@RequestParam(value="encounter") Encounter encounter,
                                          HttpSession httpSession) throws Exception {

        // TODO error handling-- no form?
        ObjectMapper jackson = new ObjectMapper();
        HtmlForm form = Context.getService(HtmlFormEntryService.class).getHtmlFormByForm(encounter.getForm());
        HtmlFormSchema schema = generateSchema(form.getXmlData(), httpSession, encounter);
        return buildSchemaAsJsonNode(schema, jackson);
    }

    // is public to make it easier to test
    public JsonNode buildSchemaAsJsonNode(HtmlFormSchema schema, ObjectMapper jackson) {
        ObjectNode schemaAsJson = jackson.createObjectNode();
        schemaAsJson.put("sections", addSections(schema.getSections(), jackson));
        schemaAsJson.put("fields", addFields(schema.getFields(), jackson));
        return schemaAsJson;
    }

    private JsonNode addSections(List<HtmlFormSection> sections,  ObjectMapper jackson) {

        ArrayNode node = jackson.createArrayNode();

        for (HtmlFormSection section : sections) {

            ObjectNode sectionNode = jackson.createObjectNode();
            sectionNode.put("name", section.getName());

            if (section.getSections() != null && section.getSections().size() > 0) {
                sectionNode.put("sections", (addSections(section.getSections(), jackson)));
            }

            if (section.getFields() != null && section.getFields().size() > 0) {
                sectionNode.put("fields", (addFields(section.getFields(), jackson)));
            }

            node.add(sectionNode);
        }

        return node;
    }

    private JsonNode addFields(List<HtmlFormField> fields, ObjectMapper jackson) {

        ArrayNode node = jackson.createArrayNode();

        for (HtmlFormField field : fields) {

            // only handling Obs and ObsGroup fields at this point
            if (field instanceof ObsField) {
                ObjectNode fieldNode = jackson.createObjectNode();
                fieldNode.put("name", (getName((ObsField) field)));
                fieldNode.put("datatype", (getDatatype((ObsField) field)));
                fieldNode.put("value", (getValue((ObsField) field)));
                node.add(fieldNode);
            }
            else if (field instanceof ObsGroup) {
                ObjectNode fieldNode = jackson.createObjectNode();
                fieldNode.put("name", (getName((ObsGroup) field)));
                if (((ObsGroup) field).getChildren() != null && ((ObsGroup) field).getChildren().size() > 0) {
                    fieldNode.put("fields", addFields(((ObsGroup) field).getChildren(), jackson));
                }
                node.add(fieldNode);
            }
        }
        return node;
    }

    private String getName(ObsField field) {
        String name = field.getName();
        if (!StringUtils.hasText(name)) {
            if (field.getQuestion() != null && field.getQuestion().getName() != null) {
                name = field.getQuestion().getName().getName();
            }
        }
        return name;
    }

    private String getName(ObsGroup obsGroup) {
        String name = obsGroup.getLabel();
        if (!StringUtils.hasText(name)) {
            if (obsGroup.getConcept() != null && obsGroup.getConcept().getName() != null) {
                name = obsGroup.getConcept().getName().getName();
            }
        }
        return name;
    }


    private String getDatatype(ObsField field) {
        return field.getQuestion() != null ? field.getQuestion().getDatatype().getName().toLowerCase() : "";
    }

    private String getValue(ObsField field) {


        String value = "";
        if (field.getExistingObs() != null) {
            ConceptDatatype datatype = field.getQuestion().getDatatype();
            if (datatype.isDateTime()) {
                value = field.getExistingObs().getValueDate() != null ?
                        datetimeFormat.format(field.getExistingObs().getValueDate()) : "";
            } else if (datatype.isDate()) {
                value = field.getExistingObs().getValueDate() != null ?
                        dateFormat.format(field.getExistingObs().getValueDate()) : "";
            } else if (datatype.isTime()) {
                value = field.getExistingObs().getValueDate() != null ?
                        timeFormat.format(field.getExistingObs().getValueDate()) : "";
            } else if (datatype.isNumeric()) {
                value = field.getExistingObs().getValueNumeric() != null ?
                        field.getExistingObs().getValueNumeric().toString() : "";
            } else if (datatype.isBoolean()) {
                value = field.getExistingObs().getValueBoolean() != null ?
                        field.getExistingObs().getValueBoolean().toString() : "";
            } else if (datatype.isText()) {
                // handle the special-case location obs
                if ("org.openmrs.Location".equals(field.getExistingObs().getComment())) {
                    value = locationService.getLocation(new Integer(field.getExistingObs().getValueText())).getName();
                }
                else {
                    value = field.getExistingObs().getValueText();
                }
            } else if (datatype.isCoded()) {
                value = field.getExistingObs().getValueCodedName() != null ?
                        field.getExistingObs().getValueCodedName().getName() :
                        field.getExistingObs().getValueCoded() != null ?
                                field.getExistingObs().getValueCoded().getName().getName() : "";
            }
        }
        return value;
    }

    private HtmlFormSchema generateSchema(String xml, HttpSession httpSession, Encounter encounter) throws Exception {
        HtmlForm fakeForm = new HtmlForm();
        fakeForm.setXmlData(xml);
        FormEntrySession fes = new FormEntrySession(encounter.getPatient(), encounter, FormEntryContext.Mode.VIEW, fakeForm, httpSession);
        fes.getHtmlToDisplay();
        return fes.getContext().getSchema();
    }

    // used for mocking
    public void setLocationService(LocationService locationService) {
        this.locationService = locationService;
    }
}
