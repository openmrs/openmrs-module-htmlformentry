package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.PersonSearchWidget;
import org.openmrs.module.htmlformentry.widget.PersonStubWidget;
import org.openmrs.module.htmlformentry.widget.RelationshipWidget;

/**
 * Holds the widgets used to represent a relationship, and serves as both the HtmlGeneratorElement
 * and the FormSubmissionControllerAction for the relationship.
 */
public class RelationshipSubmissionElement implements HtmlGeneratorElement,
        FormSubmissionControllerAction {

    protected final Log log = LogFactory.getLog(RelationshipSubmissionElement.class);

    private String id;
    private static String FIELD_TYPE = "type";
    private static String FIELD_WHO_AM_I = "whoAmI";
    private static String FIELD_REQUIRED = "required";
    private static String FIELD_REPLACE_CURRENT = "changeExistingRelationship";
    private static String FIELD_ATTRIBUTE = "requireAttributes";
    private static String FIELD_PROGRAMS = "programIds";
    private static String FIELD_DISPLAY = "display";
    private static String FIELD_LABEL_TEXT = "labelText";

    private static String DISPLAY_SEARCH = "search";
    private static String DISPLAY_DROPDOWN = "dropDown";

    private PersonSearchWidget personWidget = null;
    private PersonStubWidget personStubWidget = null;
    private ErrorWidget personErrorWidget;
    private RelationshipWidget relationshipWidget;

    private String personAttributes;
    private String personPrograms;
    private String display;
    private String labelText = null;


    private List<RelationshipType> relationshipsToCreate = new ArrayList<RelationshipType>();
    private List<String> roleInRelationship = new ArrayList<String>();
    private List<Boolean> replaceExisting = new ArrayList<Boolean>();

    private boolean required = false;

    public RelationshipSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
        String relationshipsToBeCreated = parameters.get(FIELD_TYPE);
        String whoAmI = parameters.get(FIELD_WHO_AM_I);
        String replaceCurrent = parameters.get(FIELD_REPLACE_CURRENT);

        if (relationshipsToBeCreated == null || whoAmI == null || replaceCurrent == null){
            throw new RuntimeException("You must include type, whoAmI and changeExistingRelationship" +
                    " fields in a relationship tag, all three fields are required");
        }

        relationshipWidget = new RelationshipWidget();

        if (relationshipsToBeCreated != null) {
            String[] relationships = relationshipsToBeCreated.split(",");

            for (String rel : relationships) {
                RelationshipType r = Context.getPersonService().getRelationshipTypeByUuid(rel);
                if (r == null) {
                    try {
                        r = Context.getPersonService().getRelationshipType(Integer.valueOf(rel));
                    } catch (Exception ex) {
                    }
                }
                if (r == null)
                    throw new IllegalArgumentException("Relationship tag type parameter item " + rel + " is invalid.");
                relationshipsToCreate.add(r);
            }
            relationshipWidget.setRelationshipsToCreate(relationshipsToCreate);
        }

        if (whoAmI != null) {
            String[] who = whoAmI.split(",");
            for (String side : who) {
                roleInRelationship.add(side);
            }

            relationshipWidget.setRoleInRelationship(roleInRelationship);
        }

        if (replaceCurrent != null) {
            String[] replace = replaceCurrent.split(",");
            for (String rep : replace) {
                if (rep != null && rep.toLowerCase().equals("true")) {
                    replaceExisting.add(true);
                } else {
                    replaceExisting.add(false);
                }
            }
        }

        String requiredStr = parameters.get(FIELD_REQUIRED);
        if (requiredStr != null && requiredStr.toLowerCase().equals("true")) {
            required = true;
        }

        personAttributes = parameters.get(FIELD_ATTRIBUTE);
        personPrograms = parameters.get(FIELD_PROGRAMS);

        display = parameters.get(FIELD_DISPLAY);
        //set default for display to search box
        if (display == null || display.trim().length() == 0) {
            display = DISPLAY_SEARCH;
        }

        if (DISPLAY_SEARCH.equals(display)) {
            personWidget = new PersonSearchWidget();
            personWidget.setSearchProgram(personPrograms);
            context.registerWidget(personWidget);
        }
        if (DISPLAY_DROPDOWN.equals(display)) {
            personStubWidget = new PersonStubWidget();
            context.registerWidget(personStubWidget);
        }
        personErrorWidget = new ErrorWidget();

        labelText = parameters.get(FIELD_LABEL_TEXT);

        context.registerWidget(relationshipWidget);
        context.registerErrorWidget(personWidget, personErrorWidget);

        // set the id, if it has been specified
        if (parameters.get("id") != null) {
            id = (String) parameters.get("id");
        }
    }


    /**
     * @should return HTML snippet
     * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
     */
    @Override
    public String generateHtml(FormEntryContext context) {

        StringBuilder ret = new StringBuilder();

        // if an id has been specified, wrap the whole relationship element in a span tag so that we access property values via javascript
        if (id != null) {
            ret.append("<span id='" + id + "'>");
        }

        if (labelText != null && labelText.length() > 0) {
            ret.append(labelText);
            ret.append("<br />");
        }


        //this check is needed or else the code falls over when generating preview for editing form
        //TODO: probably need a new mode for previewing to better deal with this sort of stuff.
        if (context.getExistingPatient() != null && context.getExistingPatient().getId() != null) {
            List<String> searchAttributes = new ArrayList<String>();
            List<String> attributeValues = new ArrayList<String>();
            //now deal with the attribute stuff as we may have parameter based stuff and may need to retrieve from the patient
            if (personAttributes != null) {
                String[] allAttributes = personAttributes.split(",");
                for (String attr : allAttributes) {
                    String[] split = attr.split(":");
                    searchAttributes.add(split[0]);

                    if (split.length > 1) {
                        //need to find out the value from the current patient
                        if (split[1].indexOf("${currentPatientAttribute(") == 0) {
                            // we want to pull out whatever is within the quotes
                            Pattern withinQuotes = Pattern.compile("[\"\'](.*)[\"\']");
                            Matcher matcher = withinQuotes.matcher(split[1]);
                            matcher.find();
                            String patientAttr = matcher.group(1);

                            PersonAttribute pa = context.getExistingPatient().getAttribute(patientAttr);
                            if (pa != null) {
                                attributeValues.add(pa.getValue());
                            } else {
                                attributeValues.add(null);
                            }
                        }
                        // add the value to the attribute list
                        else {
                            attributeValues.add(split[1]);
                        }
                    } else {
                        //adding a null so that all attributes have a value
                        attributeValues.add(null);

                    }
                }
                if (personWidget != null) {
                    StringBuilder sAttributes = new StringBuilder();
                    StringBuilder sValues = new StringBuilder();
                    for (String s : searchAttributes) {
                        if (sAttributes.length() > 0) {
                            sAttributes.append(",");
                        }
                        sAttributes.append(s);
                    }
                    for (String s : attributeValues) {
                        if (sValues.length() > 0) {
                            sValues.append(",");
                        }
                        if (s == null) {
                            //adding a bogus value as all attributes should
                            sValues.append("null");
                        } else {
                            sValues.append(s);
                        }
                    }

                    personWidget.setSearchAttribute(sAttributes.toString());
                    personWidget.setAttributeValue(sValues.toString());
                }

            }
            if (personStubWidget != null && context.getMode() != Mode.VIEW) {
                List<String> progIds = new ArrayList<String>();
                if (personPrograms != null) {
                    String[] programIds = personPrograms.split(",");

                    for (String progId : programIds) {
                        progIds.add(progId);
                    }
                }

                List<Person> personsToExclude = new ArrayList<Person>();
                // exclude the exisiting patient from any results
                personsToExclude.add(context.getExistingPatient());
                personStubWidget.setOptions(Context.getService(HtmlFormEntryService.class).getPeopleAsPersonStubs(searchAttributes, attributeValues, progIds, personsToExclude));
            }

            if (relationshipWidget != null) {
                if (id != null)
                    relationshipWidget.setParentId(id);
                ret.append(relationshipWidget.generateHtml(context));

            }

        }

        if (context.getMode() != Mode.VIEW) {

            ret.append("<br /><strong>");
            ret.append(Context.getMessageSourceService().getMessage("htmlformentry.newRelationshipsLabel"));
            ret.append("</strong>");

            if (personWidget != null) {
                ret.append(personWidget.generateHtml(context) + " ");
                context.registerPropertyAccessorInfo(id + "." + "newRelationship" + ".value", context.getFieldNameIfRegistered(personWidget), null, "newRelationshipFieldGetterFunction", null);

            }
            if (personStubWidget != null) {
                ret.append(personStubWidget.generateHtml(context) + " ");
            }
            ret.append(personErrorWidget.generateHtml(context));
        }
        // close out the span if we have an id tag
        if (id != null) {
            ret.append("</span>");
        }
        return ret.toString();
    }

    /**
     * handleSubmission saves a drug order if in ENTER or EDIT-mode
     *
     * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession, javax.servlet.http.HttpServletRequest)
     */
    @Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        Person relatedPerson = null;

        if (personWidget != null && personWidget.getValue(session.getContext(), submission) != null) {
            relatedPerson = ((Person) personWidget.getValue(session.getContext(), submission));
        }
        if (personStubWidget != null && personStubWidget.getValue(session.getContext(), submission) != null) {
            relatedPerson = ((Person) personStubWidget.getValue(session.getContext(), submission));
        }

        if (relatedPerson != null) {
            List<Relationship> existingRelationships = Context.getPersonService().getRelationshipsByPerson(session.getSubmissionActions().getCurrentPerson());
            //want to check to see if relationship already exists
            for (int i = 0; i < relationshipsToCreate.size(); i++) {
                RelationshipType r = relationshipsToCreate.get(i);
                String side = roleInRelationship.get(i);
                boolean replace = replaceExisting.get(i);
                if (r != null) {
                    //firstCheckToSeeIfWeNeedToCreate
                    boolean create = true;

                    for (Relationship er : existingRelationships) {

                        if (er.getRelationshipType().getId().equals(r.getId())) {
                            //now check the person is correct
                            if (side.equals("A")) {
                                //Relationship already exists
                                if (er.getPersonB().equals(relatedPerson)) {
                                    create = false;
                                }
                                //now check if we should be replacing the existing relationship
                                else if (er.getPersonA().equals(session.getSubmissionActions().getCurrentPerson()) && replace) {
                                    session.getSubmissionActions().getRelationshipsToVoid().add(er);
                                }
                            }
                            if (side.equals("B")) {
                                //Relationship already exists
                                if (er.getPersonA().equals(relatedPerson)) {
                                    create = false;
                                }
                                //now check if we should be replacing the existing relationship
                                else if (er.getPersonB().equals(session.getSubmissionActions().getCurrentPerson()) && replace) {
                                    session.getSubmissionActions().getRelationshipsToVoid().add(er);
                                }
                            }
                        }
                    }
                    if (create) {
                        Relationship rel = new Relationship();
                        if (side.equals("A")) {
                            rel.setPersonA(session.getSubmissionActions().getCurrentPerson());
                            rel.setPersonB(relatedPerson);
                        }
                        if (side.equals("B")) {
                            rel.setPersonB(session.getSubmissionActions().getCurrentPerson());
                            rel.setPersonA(relatedPerson);
                        }
                        rel.setRelationshipType(r);

                        Context.getPersonService().saveRelationship(rel);
                        //session.getSubmissionActions().getRelationshipsToCreate().add(rel);
                    }
                }
            }

        }
    }

    /**
     * @should return validation errors if value is not filled in and required
     * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {

        List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();

        try {
            if (personWidget != null) {
                if (required && !relationshipWidget.isAllRelationshipsFullfilled() && (personWidget.getValue(context, submission) == null || personWidget.getValue(context, submission).toString().trim().length() == 0)) {
                    throw new Exception("htmlformentry.error.required");
                }
            }
            if (personStubWidget != null) {
                if (required && !relationshipWidget.isAllRelationshipsFullfilled() && (personStubWidget.getValue(context, submission) == null || personStubWidget.getValue(context, submission).toString().trim().length() == 0)) {
                    throw new Exception("htmlformentry.error.required");
                }
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context
                    .getFieldName(personErrorWidget), Context
                    .getMessageSourceService().getMessage(ex.getMessage())));
        }

        return ret;
    }

}
