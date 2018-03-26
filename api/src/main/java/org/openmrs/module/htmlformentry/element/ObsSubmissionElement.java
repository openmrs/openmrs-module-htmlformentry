package org.openmrs.module.htmlformentry.element;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.Drug;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.comparator.OptionComparator;
import org.openmrs.module.htmlformentry.compatibility.ConceptCompatibility;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.DateTimeWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.DynamicAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.PersonStubWidget;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.RemoteJsonAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.module.htmlformentry.widget.ToggleWidget;
import org.openmrs.module.htmlformentry.widget.UploadWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.obs.ComplexData;
import org.openmrs.util.LocaleUtility;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.RoleConstants;
import org.openmrs.web.WebConstants;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Holds the widgets used to represent a specific Observation, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for the Observation.
 */
public class ObsSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

    private Locale locale = Context.getLocale();

	private String id;

    private String clazz;

	private Concept concept;
	
	private String valueLabel;
	
	private Widget valueWidget;
	
	private String defaultValue;
	
	private boolean showUnits = false;

    private String unitsCode;

    private String unitsCssClass = "units";
	
	private String dateLabel;
	
	private DateWidget dateWidget;
	
	private String accessionNumberLabel;
	
	private TextFieldWidget accessionNumberWidget;

	private String commentFieldLabel;

	private TextFieldWidget commentFieldWidget;

	private ErrorWidget errorWidget;
	
	private boolean allowFutureDates = false;
	
	private Concept answerConcept;
	
	private List<Concept> conceptAnswers = new ArrayList<Concept>();
	
	private List<Number> numericAnswers = new ArrayList<Number>();
	
	private List<String> textAnswers = new ArrayList<String>();
	
	private List<String> answerLabels = new ArrayList<String>();
	
	private String answerLabel;
	
	private Obs existingObs; // in edit mode, this allows submission to check whether the obs has been modified or not
	
	private List<Obs> existingObsList;  // used by the dynamic autocomplete which allows the selection of multiple answers
	
	private boolean required;
	
	//these are for conceptSelects:
	private List<Concept> concepts = null; //possible concepts
	
	private List<String> conceptLabels = null; //the text to show for possible concepts
	
	private String answerSeparator = null;

    // these are for location and provider options

    private List<Option> locationOptions = new ArrayList<Option>();

    private Map<Object, String> whenValueThenDisplaySection = new LinkedHashMap<Object, String>();

    private Map<Object, String> whenValueThenJavascript = new LinkedHashMap<Object, String>();

    private Map<Object, String> whenValueElseJavascript = new LinkedHashMap<Object, String>();

    private Boolean isLocationObs; // determines whether the valueText for this obs should be a location_id;

	public ObsSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
        if (parameters.get("locale") != null) {
            this.locale = LocaleUtility.fromSpecification(parameters.get("locale"));
        }
		String conceptId = parameters.get("conceptId");
		String conceptIds = parameters.get("conceptIds");
		defaultValue = parameters.get("defaultValue");
		if (parameters.get("answerSeparator") != null) {
			answerSeparator = parameters.get("answerSeparator");
        }

		if (conceptId != null && conceptIds != null)
			throw new RuntimeException("You can't use conceptId and conceptIds in the same tag!");
		else if (conceptId == null && conceptIds == null)
			throw new RuntimeException("You must include either conceptId or conceptIds in an obs tag");

		if (conceptId != null) {
			concept = HtmlFormEntryUtil.getConcept(conceptId);
			if (concept == null)
				throw new IllegalArgumentException("Cannot find concept for value " + conceptId
				        + " in conceptId attribute value. Parameters: " + parameters);
		} else {
			concepts = new ArrayList<Concept>();
			for (StringTokenizer st = new StringTokenizer(conceptIds, ","); st.hasMoreTokens();) {
				String s = st.nextToken().trim();
				Concept concept = HtmlFormEntryUtil.getConcept(s);
				if (concept == null)
					throw new IllegalArgumentException("Cannot find concept for value " + s
					        + " in conceptIds attribute value. Parameters: " + parameters);
				concepts.add(concept);
			}
			if (concepts.size() == 0)
				throw new IllegalArgumentException(
				        "You must provide some valid conceptIds for the conceptIds attribute. Parameters: " + parameters);
		}

		// test to make sure the answerConceptId, if it exists, is valid
		String answerConceptId = parameters.get("answerConceptId");
		if (StringUtils.isNotBlank(answerConceptId)) {
			if (HtmlFormEntryUtil.getConcept(answerConceptId) == null)
				throw new IllegalArgumentException("Cannot find concept for value " + answerConceptId
				        + " in answerConceptId attribute value. Parameters: " + parameters);
		}

		// test to make sure the answerConceptIds, if they exist, are valid
		String answerConceptIds = parameters.get("answerConceptIds");
		if (StringUtils.isNotBlank(answerConceptIds)) {
			for (StringTokenizer st = new StringTokenizer(answerConceptIds, ","); st.hasMoreTokens();) {
				String s = st.nextToken().trim();
				Concept concept = HtmlFormEntryUtil.getConcept(s);
				if (concept == null)
					throw new IllegalArgumentException("Cannot find concept for value " + s
					        + " in answerConceptIds attribute value. Parameters: " + parameters);
			}
		}

		if ("true".equals(parameters.get("allowFutureDates")))
			allowFutureDates = true;
		if ("true".equals(parameters.get("required"))) {
			required = true;
		}
		if (parameters.get("id") != null) {
			id = parameters.get("id");
        }
        if (parameters.get("class") !=null) {
            clazz = parameters.get("class");
        }

        isLocationObs = "location".equals(parameters.get("style"));

		prepareWidgets(context, parameters);
	}


    private Widget buildDropdownWidget(Integer size){
        Widget dropdownWidget = new DropdownWidget(size);
        if(size==1 || !required){
            // show an empty option when size =1, even if required =true
            ((DropdownWidget) dropdownWidget).addOption(new Option());
        }
        return dropdownWidget;
    }
	private void prepareWidgets(FormEntryContext context, Map<String, String> parameters) {
		String userLocaleStr = locale.toString();
		try {
			if (answerConcept == null)
				answerConcept = HtmlFormEntryUtil.getConcept(parameters.get("answerConceptId"));
		}
		catch (Exception ex) {}
        Integer size = 1;
        try {
            size = Integer.valueOf(parameters.get("size"));
        }catch (Exception ex) {}
		if (context.getCurrentObsGroupConcepts() != null && context.getCurrentObsGroupConcepts().size() > 0) {
			existingObs = context.getObsFromCurrentGroup(concept, answerConcept);
		} else if (concept != null) {
			if (concept.getDatatype().isBoolean() && "checkbox".equals(parameters.get("style"))) {
				// since a checkbox has one value we need to look for an exact
				// match for that value
				if ("false".equals(parameters.get("value"))) {
					existingObs = context.removeExistingObs(concept, false);
				} else {
					// if not 'false' we treat as 'true'
					existingObs = context.removeExistingObs(concept, true);
				}
            // if we use 'checkbox' with numeric values, first find existing obs for each answer
			}else if (concept.getDatatype().isNumeric() && "checkbox".equals(parameters.get("style"))){
                String numericAns = parameters.get("answer");
                existingObs = context.removeExistingObs(concept, numericAns);
                //for dynamicAutocomplete if selectMulti is true
			} else if ("autocomplete".equals(parameters.get("style")) && "true".equals(parameters.get("selectMulti"))) {
				existingObsList = context.removeExistingObs(concept);
			} else {
				existingObs = context.removeExistingObs(concept, answerConcept);
			}
		} else {
			existingObs = context.removeExistingObs(concepts, answerConcept);
		}
		
		errorWidget = new ErrorWidget();
		context.registerWidget(errorWidget);
		
		if (parameters.containsKey("labelNameTag")) {
			if (parameters.get("labelNameTag").equals("default"))
				if (concepts != null)
					valueLabel = answerConcept.getName(locale, false).getName();
				else
					valueLabel = concept.getName(locale, false).getName();
			else
				throw new IllegalArgumentException("Name tags other than 'default' not yet implemented");
		} else if (parameters.containsKey("labelText")) {
			valueLabel = parameters.get("labelText");
		} else if (parameters.containsKey("labelCode")) {
			valueLabel = context.getTranslator().translate(userLocaleStr, parameters.get("labelCode"));
		} else {
			if (concepts != null)
				valueLabel = answerConcept.getName(locale, false).getName();
			else
				valueLabel = "";
		}
		if (parameters.get("answerLabels") != null) {
			answerLabels = Arrays.asList(parameters.get("answerLabels").split(","));
		}
		if (parameters.get("answerCodes") != null) {
			String[] split = parameters.get("answerCodes").split(",");
			for (String s : split) {
				answerLabels.add(context.getTranslator().translate(userLocaleStr, s));
			}
		}
		if (concepts != null) {
			conceptLabels = new ArrayList<String>();
			if (parameters.get("conceptLabels") != null)
				conceptLabels = Arrays.asList(parameters.get("conceptLabels").split(","));
			if (conceptLabels.size() != 0 && (conceptLabels.size() != concepts.size()))
				throw new IllegalArgumentException(
				        "If you want to use the conceptLabels attribute, you must to provide the same number of conceptLabels as there are conceptIds.  Parameters: "
				                + parameters);
			if ("radio".equals(parameters.get("style"))) {
				valueWidget = new RadioButtonsWidget();
				if (answerSeparator != null) {
					((RadioButtonsWidget) valueWidget).setAnswerSeparator(answerSeparator);
                }
			} else { // dropdown
				valueWidget =buildDropdownWidget(size);
			}
			for (int i = 0; i < concepts.size(); ++i) {
				Concept c = concepts.get(i);
				String label = null;
				if (conceptLabels != null && i < conceptLabels.size()) {
					label = conceptLabels.get(i);
				} else {
					label = c.getName(locale, false).getName();
				}
				((SingleOptionWidget) valueWidget).addOption(new Option(label, c.getConceptId().toString(), false));
			}
			if (existingObs != null) {
				valueWidget.setInitialValue(existingObs.getConcept());
			} else if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
				Concept initialValue = HtmlFormEntryUtil.getConcept(defaultValue);
				if (initialValue == null) {
					throw new IllegalArgumentException("Invalid default value. Cannot find concept: " + defaultValue);
				}
				valueWidget.setInitialValue(initialValue);
			}
			answerLabel = getValueLabel();
		} else {

			// Obs of datatypes date, time, and datetime support the attributes
			// defaultDatetime. Make sure this date format string matches the
			// format documented at
			// https://wiki.openmrs.org/display/docs/HTML+Form+Entry+Module+HTML+Reference#HTMLFormEntryModuleHTMLReference-%3Cobs%3E
			// See <obs> section, attributes defaultDatetime and
			// defaultObsDatetime
			String defaultDatetimeFormat = "yyyy-MM-dd-HH-mm";

			if (concept.getDatatype().isNumeric()) {

                ConceptNumeric cn;
                if (concept instanceof ConceptNumeric) {
                    cn = (ConceptNumeric) concept;
                } else {
                    cn = Context.getConceptService().getConceptNumeric(concept.getConceptId());
                }

                ConceptCompatibility conceptCompatibility = Context.getRegisteredComponent("htmlformentry.ConceptCompatibility", ConceptCompatibility.class);
                boolean isPrecise = cn != null ? conceptCompatibility.isAllowDecimal(cn) : true;

				if (parameters.get("answers") != null) {
					try {
						for (StringTokenizer st = new StringTokenizer(parameters.get("answers"), ", "); st.hasMoreTokens();) {
							Number answer = Double.valueOf(st.nextToken());
							numericAnswers.add(answer);
						}
					}
					catch (Exception ex) {
						throw new RuntimeException("Error in answer list for concept " + concept.getConceptId() + " ("
						        + ex.toString() + "): " + conceptAnswers);
					}
				}

				// added to avoid creating this widget when a checkbox is needed
                if (numericAnswers.size() == 0) {
                    if (!"checkbox".equals(parameters.get("style"))) {
                        valueWidget = new NumberFieldWidget(cn, parameters.get("size"));
                    } else {
                        // render CheckboxWidgets for <obs> tag with numeric datatype;
                        // i.e. <obs conceptId="1234" answer="8" answerLabel="Eight" style="checkbox"/>
                        if (parameters.get("answer") != null) {
                            try {
                                Number number = Double.valueOf(parameters.get("answer"));
                                numericAnswers.add(number);
                                answerLabel = parameters.get("answerLabel");
                                if (number != null) {
                                    valueWidget = createCheckboxWidget(answerLabel, isPrecise ? number.toString() : Integer.valueOf(number.intValue()).toString(), parameters.get("toggle"));
                                }

                            } catch (Exception ex) {
                                throw new RuntimeException("Error in answer for concept " + concept.getConceptId() + " ("
                                        + ex.toString() + "): ");
                            }
                        }
                    }
                } else {
					if ("radio".equals(parameters.get("style"))) {
						valueWidget = new RadioButtonsWidget();
						if (answerSeparator != null) {
							((RadioButtonsWidget) valueWidget).setAnswerSeparator(answerSeparator);
                        }
					} else { // dropdown
                        valueWidget = buildDropdownWidget(size);
					}
					// need to make sure we have the initialValue too
					Number lookFor = existingObs == null ? null : existingObs.getValueNumeric();
					for (int i = 0; i < numericAnswers.size(); ++i) {
						Number n = numericAnswers.get(i);
						if (lookFor != null && lookFor.equals(n))
							lookFor = null;
						String label = null;
						if (answerLabels != null && i < answerLabels.size()) {
							label = answerLabels.get(i);
						} else {
							label = n.toString();
						}
						((SingleOptionWidget) valueWidget).addOption(new Option(label, isPrecise ?  n.toString() : Integer.valueOf(n.intValue()).toString(), false));
					}
					// if lookFor is still non-null, we need to add it directly as
					// an option:
					if (lookFor != null)
						((SingleOptionWidget) valueWidget)
						        .addOption(new Option(lookFor.toString(), isPrecise ?  lookFor.toString() : Integer.valueOf(lookFor.intValue()).toString(), true));
				}

				if (valueWidget != null) {
					Number initialValue = null;

					if (existingObs != null && existingObs.getValueNumeric() != null) {
						// for non-precise numeric obs, initial value should be rendered as an integer
						initialValue = isPrecise ? ((Number) existingObs.getValueNumeric()) : existingObs.getValueNumeric().intValue();
					} else if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
						try {
							initialValue = isPrecise ? ((Number) Double.valueOf(defaultValue)) : Integer.valueOf(defaultValue);
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("Default value " + defaultValue + " is not a valid " + (isPrecise ? "double" : "integer"), e);
						}
					}
					valueWidget.setInitialValue(initialValue);
				}

			} else if (concept.isComplex()) {
				valueWidget = new UploadWidget();
				String lookFor = existingObs == null ? null : existingObs.getValueComplex();
				Obs initialValue = null;
				if (lookFor != null) {
					initialValue = existingObs;
				}
				valueWidget.setInitialValue(initialValue);
			} else if (concept.getDatatype().isText()) {

				String initialValue = null;
				if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
					initialValue = defaultValue;
				}
				if (existingObs != null) {
					initialValue = existingObs.getValueText();
				}

				if (parameters.get("answers") != null) {
					try {
						for (StringTokenizer st = new StringTokenizer(parameters.get("answers"), ","); st.hasMoreTokens();) {
							textAnswers.add(st.nextToken());
						}
					}
					catch (Exception ex) {
						throw new RuntimeException("Error in answer list for concept " + concept.getConceptId() + " ("
						        + ex.toString() + "): " + conceptAnswers);
					}
				}

                // configure the special obs type that allows selection of a location (the location_id PK is stored as the valueText)
				if (isLocationObs) {

                    valueWidget = new DropdownWidget();
                    // if "answerLocationTags" attribute is present try to get locations by tags
                    List<Location> locationList = HtmlFormEntryUtil.getLocationsByTags(HtmlFormEntryConstants.ANSWER_LOCATION_TAGS, parameters);
                    if ((locationList == null) ||
                            (locationList != null && locationList.size()<1)){
                        // if no locations by tags are found then get all locations
                        locationList = Context.getLocationService().getAllLocations();
                    }

                    for (Location location : locationList) {
                            String label = HtmlFormEntryUtil.format(location);
                            Option option = new Option(label, location.getId().toString(), location.getId().toString().equals(initialValue));
                            locationOptions.add(option);
                       }
                    Collections.sort(locationOptions, new OptionComparator());

                    // if initialValueIsSet=false, no initial/default location, hence this shows the 'select input' field as first option
                    boolean initialValueIsSet = !(initialValue == null);
                    ((DropdownWidget)valueWidget).addOption(new Option(Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation"),"",!initialValueIsSet));
                    if (!locationOptions.isEmpty()) {
                        for(Option option: locationOptions)
                            ((DropdownWidget)valueWidget).addOption(option);
                    }

				} else if ("person".equals(parameters.get("style"))) {
					
					List<PersonStub> options = new ArrayList<PersonStub>();
                    List<Option> personOptions = new ArrayList<Option>();
					
					// If specific persons are specified, display only those persons in order
					String personsParam = (String) parameters.get("persons");
					if (personsParam != null) {
						for (String s : personsParam.split(",")) {
							Person p = HtmlFormEntryUtil.getPerson(s);
							if (p == null) {
								throw new RuntimeException("Cannot find Person: " + s);
							}
							options.add(new PersonStub(p));
						}
					}
					
					// Only if specific person ids are not passed in do we get by user Role
					if (options.isEmpty()) {
						
						List<PersonStub> users = new ArrayList<PersonStub>();
						
						// If the "role" attribute is passed in, limit to users with this role
						if (parameters.get("role") != null) {
							Role role = Context.getUserService().getRole((String) parameters.get("role"));
							if (role == null) {
								throw new RuntimeException("Cannot find role: " + parameters.get("role"));
							} else {
								users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(role.getRole());
							}
						}

						// Otherwise, limit to users with the default OpenMRS PROVIDER role, 
						else {
							String defaultRole = RoleConstants.PROVIDER;
							Role role = Context.getUserService().getRole(defaultRole);
							if (role != null) {
								users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(role.getRole());
							}
							// If this role isn't used, default to all Users
							if (users.isEmpty()) {
								users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(null);
							}
						}
						options.addAll(users);
						//    					sortOptions = true;
					}

					valueWidget = new PersonStubWidget(options);

				} else {
					if (textAnswers.size() == 0) {
						Integer rows = null;
						Integer cols = null;

						try {
							rows = Integer.valueOf(parameters.get("rows"));
						}
						catch (Exception ex) {}
						try {
							cols = Integer.valueOf(parameters.get("cols"));
						}
						catch (Exception ex) {}

						if (rows != null || cols != null || "textarea".equals(parameters.get("style"))) {
							valueWidget = new TextFieldWidget(rows, cols);
						} else {
							Integer textFieldSize = null;
							try {
                                textFieldSize = Integer.valueOf(parameters.get("size"));
							}
							catch (Exception ex) {}
							valueWidget = new TextFieldWidget(textFieldSize);
						}
                        ((TextFieldWidget) valueWidget).setPlaceholder(parameters.get("placeholder"));

						try {
							Integer maxlength = Integer.valueOf(parameters.get("maxlength"));
							((TextFieldWidget) valueWidget).setTextFieldMaxLength(maxlength);
						}
						catch (Exception ex) {}
                    }
					else {
						if ("radio".equals(parameters.get("style"))) {
							valueWidget = new RadioButtonsWidget();
							if (answerSeparator != null) {
								((RadioButtonsWidget) valueWidget).setAnswerSeparator(answerSeparator);
                            }
						} else { // dropdown
							valueWidget =buildDropdownWidget(size);
						}
						// need to make sure we have the initialValue too
						String lookFor = existingObs == null ? null : existingObs.getValueText();
						for (int i = 0; i < textAnswers.size(); ++i) {
							String s = textAnswers.get(i);
							if (lookFor != null && lookFor.equals(s))
								lookFor = null;
							String label = null;
							if (answerLabels != null && i < answerLabels.size()) {
								label = answerLabels.get(i);
							} else {
								label = s;
							}
							((SingleOptionWidget) valueWidget).addOption(new Option(label, s, false));
						}
						// if lookFor is still non-null, we need to add it directly
						// as an option:
						if (lookFor != null)
							((SingleOptionWidget) valueWidget).addOption(new Option(lookFor, lookFor, true));
					}
				}

				if (initialValue != null) {
					if (isLocationObs) {
						Location l = HtmlFormEntryUtil.getLocation(initialValue, context);
						if (l == null) {
							throw new RuntimeException("Cannot find Location: " + initialValue);
						}
						valueWidget.setInitialValue(l);
					} else if ("person".equals(parameters.get("style"))) {
						Person p = HtmlFormEntryUtil.getPerson(initialValue);
						if (p == null) {
							throw new RuntimeException("Cannot find Person: " + initialValue);
						}
						valueWidget.setInitialValue(new PersonStub(p));
					} else {
						valueWidget.setInitialValue(initialValue);
					}
				}
			} else if (concept.getDatatype().isCoded()) {
				if (parameters.get("answerConceptIds") != null) {
					try {
						for (StringTokenizer st = new StringTokenizer(parameters.get("answerConceptIds"), ","); st
						        .hasMoreTokens();) {
							Concept c = HtmlFormEntryUtil.getConcept(st.nextToken());
							if (c == null)
								throw new RuntimeException("Cannot find concept " + st.nextToken());
							conceptAnswers.add(c);
						}
					}
					catch (Exception ex) {
						throw new RuntimeException("Error in answer list for concept " + concept.getConceptId() + " ("
						        + ex.toString() + "): " + conceptAnswers);
					}
				}
                else if (parameters.get("answerConceptSetIds") != null) {
                    String answerConceptSetIds = parameters.get("answerConceptSetIds");
                    try {
						for (StringTokenizer st = new StringTokenizer(answerConceptSetIds, ","); st.hasMoreTokens();) {
							Concept answerConceptSet = HtmlFormEntryUtil.getConcept(st.nextToken());
							conceptAnswers.addAll(Context.getConceptService().getConceptsByConceptSet(answerConceptSet));
						}
                    }
                    catch (Exception ex) {
                        throw new RuntimeException("Error loading answer concepts from answerConceptSet " + answerConceptSetIds, ex);
                    }
                }
                else if (parameters.get("answerClasses") != null && !"autocomplete".equals(parameters.get("style"))) {
					try {
						for (StringTokenizer st = new StringTokenizer(parameters.get("answerClasses"), ","); st
						        .hasMoreTokens();) {
							String className = st.nextToken().trim();
							ConceptClass cc = Context.getConceptService().getConceptClassByName(className);
							if (cc == null) {
								throw new RuntimeException("Cannot find concept class " + className);
							}
							conceptAnswers.addAll(Context.getConceptService().getConceptsByClass(cc));
						}
						Collections.sort(conceptAnswers, conceptNameComparator);
					}
					catch (Exception ex) {
						throw new RuntimeException("Error in answer class list for concept " + concept.getConceptId() + " ("
						        + ex.toString() + "): " + conceptAnswers);
					}
				}
				
				if (answerConcept != null) {
					// if there's also an answer concept specified, this is a single
					// checkbox
					answerLabel = parameters.get("answerLabel");
					if (answerLabel == null) {
						String answerCode = parameters.get("answerCode");
						if (answerCode != null) {
							answerLabel = context.getTranslator().translate(userLocaleStr, answerCode);
						} else {
							answerLabel = answerConcept.getName(locale, false).getName();
						}
					}
					valueWidget = createCheckboxWidget(answerLabel, answerConcept.getConceptId().toString(), parameters.get("toggle"));
					if (existingObsList != null && !existingObsList.isEmpty()) {
						for (int i = 0; i < existingObsList.size(); i++) {
							((DynamicAutocompleteWidget)valueWidget).addInitialValue(existingObsList.get(i).getValueCoded());
						}
					} else if (existingObs != null) {
						valueWidget.setInitialValue(existingObs.getValueCoded());
					} else if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
						Concept initialValue = HtmlFormEntryUtil.getConcept(defaultValue);
						if (initialValue == null) {
							throw new IllegalArgumentException("Invalid default value. Cannot find concept: " + defaultValue);
						}
						if (!answerConcept.equals(initialValue)) {
							throw new IllegalArgumentException("Invalid default value: " + defaultValue
							        + ". The only allowed answer is: " + answerConcept.getId());
						}
						valueWidget.setInitialValue(initialValue);
					}
				} else if ("true".equals(parameters.get("multiple"))) {
					// if this is a select-multi, we need a group of checkboxes
					throw new RuntimeException("Multi-select coded questions are not yet implemented");
				} else {
					// allow selecting one of multiple possible coded values
					
					// if no answers are specified explicitly (by conceptAnswers or conceptClasses), get them from concept.answers.
					if (!parameters.containsKey("answerConceptIds") && !parameters.containsKey("answerClasses")
							&& !parameters.containsKey("answerDrugs") && !parameters.containsKey("answerConceptSetIds")) {
						conceptAnswers = new ArrayList<Concept>();
						for (ConceptAnswer ca : concept.getAnswers(false)) {
							conceptAnswers.add(ca.getAnswerConcept());
						}
						Collections.sort(conceptAnswers, conceptNameComparator);
					}
					
					if ("autocomplete".equals(parameters.get("style"))) {
						List<ConceptClass> cptClasses = new ArrayList<ConceptClass>();
						if (parameters.get("answerClasses") != null) {
							for (StringTokenizer st = new StringTokenizer(parameters.get("answerClasses"), ","); st
							        .hasMoreTokens();) {
								String className = st.nextToken().trim();
								ConceptClass cc = Context.getConceptService().getConceptClassByName(className);
								cptClasses.add(cc);
							}
						}
						if ((conceptAnswers == null || conceptAnswers.isEmpty())
						        && (cptClasses == null || cptClasses.isEmpty())) {
							throw new RuntimeException(
							        "style \"autocomplete\" but there are no possible answers. Looked for answerConcepts and answerClasses attributes, and answers for concept "
							                + concept.getConceptId());
						}
						if ("true".equals(parameters.get("selectMulti"))) {
							valueWidget = new DynamicAutocompleteWidget(conceptAnswers, cptClasses);
                        }
                        else {
						    valueWidget = new ConceptSearchAutocompleteWidget(conceptAnswers, cptClasses);
                        }
                    } else if (parameters.get("answerDrugs") != null) {
                        // we support searching through all drugs via AJAX
                        RemoteJsonAutocompleteWidget widget = new RemoteJsonAutocompleteWidget("/" + WebConstants.WEBAPP_NAME + "/module/htmlformentry/drugSearch.form");
                        widget.setValueTemplate("Drug:{{id}}");
                        if (parameters.get("displayTemplate") != null) {
                            widget.setDisplayTemplate(parameters.get("displayTemplate"));
                        } else {
                            widget.setDisplayTemplate("{{name}}");
                        }
                        if (existingObs != null && existingObs.getValueDrug() != null) {
                            widget.setInitialValue(new Option(existingObs.getValueDrug().getName(), existingObs.getValueDrug().getDrugId().toString(), true));
                        }
                        valueWidget = widget;

                    } else {
			            // Show Radio Buttons if specified, otherwise default to Drop
						// Down 
						boolean isRadio = "radio".equals(parameters.get("style"));
						if (isRadio) {
							valueWidget = new RadioButtonsWidget();
							if (answerSeparator != null) {
								((RadioButtonsWidget) valueWidget).setAnswerSeparator(answerSeparator);
                            }
						} else {
							valueWidget = buildDropdownWidget(size);
						}
						for (int i = 0; i < conceptAnswers.size(); ++i) {
							Concept c = conceptAnswers.get(i);
							String label = null;
							if (answerLabels != null && i < answerLabels.size()) {
								label = answerLabels.get(i);
							} else {
								label = c.getName(locale, false).getName();
							}
							((SingleOptionWidget) valueWidget).addOption(new Option(label, c.getConceptId().toString(),
							        false));
						}
					}
					if (existingObsList != null && !existingObsList.isEmpty()) {
						for (int i = 0; i < existingObsList.size(); i++) {
							((DynamicAutocompleteWidget)valueWidget).addInitialValue(existingObsList.get(i).getValueCoded());
						}
					}
					if (existingObs != null) {
                        if (existingObs.getValueDrug() != null) {
                            valueWidget.setInitialValue(existingObs.getValueDrug());
                        } else {
                            valueWidget.setInitialValue(existingObs.getValueCoded());
                        }
                    } else if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
						Concept initialValue = HtmlFormEntryUtil.getConcept(defaultValue);
						if (initialValue == null) {
							throw new IllegalArgumentException("Invalid default value. Cannot find concept: " + defaultValue);
						}
						
						if (!conceptAnswers.contains(initialValue)) {
							String allowedIds = "";
							for (Concept conceptAnswer : conceptAnswers) {
								allowedIds += conceptAnswer.getId() + ", ";
							}
							allowedIds = allowedIds.substring(0, allowedIds.length() - 2);
							throw new IllegalArgumentException("Invalid default value: " + defaultValue
							        + ". The only allowed answers are: " + allowedIds);
						}
						valueWidget.setInitialValue(initialValue);
					}
				}
			} else if (concept.getDatatype().isBoolean()) {
				String noStr = parameters.get("noLabel");
				if (StringUtils.isEmpty(noStr)) {
					noStr = context.getTranslator().translate(userLocaleStr, "general.no");
				}
				String yesStr = parameters.get("yesLabel");
				if (StringUtils.isEmpty(yesStr)) {
					yesStr = context.getTranslator().translate(userLocaleStr, "general.yes");
				}
				
				if ("checkbox".equals(parameters.get("style"))) {
					valueWidget = createCheckboxWidget(valueLabel, parameters.get("value") != null ? parameters.get("value") : "true", parameters.get("toggle"));
					valueLabel = "";
				} else if ("no_yes".equals(parameters.get("style"))) {
					valueWidget = new RadioButtonsWidget();
					((RadioButtonsWidget) valueWidget).addOption(new Option(noStr, "false", false));
					((RadioButtonsWidget) valueWidget).addOption(new Option(yesStr, "true", false));
				} else if ("yes_no".equals(parameters.get("style"))) {
					valueWidget = new RadioButtonsWidget();
					((RadioButtonsWidget) valueWidget).addOption(new Option(yesStr, "true", false));
					((RadioButtonsWidget) valueWidget).addOption(new Option(noStr, "false", false));
				} else if ("no_yes_dropdown".equals(parameters.get("style"))) {
					valueWidget = new DropdownWidget();
					((DropdownWidget) valueWidget).addOption(new Option());
					((DropdownWidget) valueWidget).addOption(new Option(noStr, "false", false));
					((DropdownWidget) valueWidget).addOption(new Option(yesStr, "true", false));
				} else if ("yes_no_dropdown".equals(parameters.get("style"))) {
					valueWidget = new DropdownWidget();
					((DropdownWidget) valueWidget).addOption(new Option());
					((DropdownWidget) valueWidget).addOption(new Option(yesStr, "true", false));
					((DropdownWidget) valueWidget).addOption(new Option(noStr, "false", false));
				} else {
					throw new RuntimeException("Boolean with style = " + parameters.get("style")
					        + " not yet implemented (concept = " + concept.getConceptId() + ")");
				}
				
				if (existingObs != null) {
					valueWidget.setInitialValue(existingObs.getValueAsBoolean());
				} else if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
					defaultValue = defaultValue.trim();
					
					//Check the default value. Do not use Boolean.valueOf as it only tests for 'true'.
					Boolean initialValue = null;
					if (defaultValue.equalsIgnoreCase(Boolean.TRUE.toString())) {
						initialValue = true;
					} else if (defaultValue.equalsIgnoreCase(Boolean.FALSE.toString())) {
						initialValue = false;
					} else if (defaultValue.isEmpty()) {
						initialValue = null;
					} else {
						throw new IllegalArgumentException("Invalid default value " + defaultValue
						        + ". Must be 'true', 'false' or ''.");
					}
					valueWidget.setInitialValue(initialValue);
				}
				
				// TODO: in 1.7-compatible version of the module, we can replace the H17 checks
				// used below with the new isDate, isTime, and isDatetime
				
			} else {
				DateWidget dateWidget = null;
				TimeWidget timeWidget = null;
				boolean disableTime = "false".equalsIgnoreCase(parameters.get("allowTime"));
				boolean hideSeconds = "true".equalsIgnoreCase(parameters.get("hideSeconds"));
				
				if (ConceptDatatype.DATE.equals(concept.getDatatype().getHl7Abbreviation()) || (ConceptDatatype.DATETIME.equals(concept.getDatatype().getHl7Abbreviation()) && disableTime)) {
					valueWidget = new DateWidget();
				} else if (ConceptDatatype.TIME.equals(concept.getDatatype().getHl7Abbreviation())) {
					valueWidget = new TimeWidget();
					if(hideSeconds){
						((TimeWidget) valueWidget).setHideSeconds(true);
					}
				} else if (ConceptDatatype.DATETIME.equals(concept.getDatatype().getHl7Abbreviation())) {
					dateWidget = new DateWidget();
					timeWidget = new TimeWidget();
					if(hideSeconds){
						timeWidget.setHideSeconds(true);
					}
					valueWidget = new DateTimeWidget(dateWidget, timeWidget);
				} else {
					throw new RuntimeException("Cannot handle datatype: " + concept.getDatatype().getName()
					        + " (for concept " + concept.getConceptId() + ")");
				}
				
				if (defaultValue != null && parameters.get("defaultDatetime") != null) {
					throw new IllegalArgumentException("Cannot set defaultDatetime and defaultValue at the same time.");
				} else if (defaultValue == null) {
					defaultValue = parameters.get("defaultDatetime");
				}
				
				if (existingObs != null) {
					valueWidget.setInitialValue(existingObs.getValueDatetime());
				} else if (defaultValue != null && Mode.ENTER.equals(context.getMode())) {
					valueWidget.setInitialValue(HtmlFormEntryUtil
					        .translateDatetimeParam(defaultValue, defaultDatetimeFormat));
				}
				
				if (dateWidget != null) {
					context.registerWidget(dateWidget);
				}
				if (timeWidget != null) {
					context.registerWidget(timeWidget);
				}
			}
		}
		context.registerWidget(valueWidget);
		context.registerErrorWidget(valueWidget, errorWidget);
		
		if (parameters.get("showUnits") != null) {
            if ("true".equalsIgnoreCase(parameters.get("showUnits"))) {
                showUnits = true;
            } else if (!"false".equalsIgnoreCase(parameters.get("showUnits"))) {
                showUnits = true;
                unitsCode = parameters.get("showUnits");
            }
		}

        if (parameters.get("unitsCssClass") != null) {
            unitsCssClass = parameters.get("unitsCssClass");
        }

        // if a date is requested, do that too
		if ("true".equals(parameters.get("showDate")) || parameters.containsKey("dateLabel")) {
			if (parameters.containsKey("dateLabel")){
                dateLabel = parameters.get("dateLabel");
            }
			dateWidget = new DateWidget();
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, errorWidget);
			if (existingObs != null) {
				dateWidget.setInitialValue(existingObs.getObsDatetime());
			} else if (parameters.get("defaultObsDatetime") != null) {
				// Make sure this format continues to match
				// the <obs> attribute defaultObsDatetime documentation at
				// https://wiki.openmrs.org/display/docs/HTML+Form+Entry+Module+HTML+Reference#HTMLFormEntryModuleHTMLReference-%3Cobs%3E
				String supportedDateFormat = "yyyy-MM-dd-HH-mm";
				dateWidget.setInitialValue(HtmlFormEntryUtil.translateDatetimeParam(parameters.get("defaultObsDatetime"),
				    supportedDateFormat));
			}
		}
		
		// if an accessionNumber is requested, do that too
		if ("true".equals(parameters.get("showAccessionNumber")) || parameters.containsKey("accessionNumberLabel")) {
			if (parameters.containsKey("accessionNumberLabel")) {
               accessionNumberLabel = parameters.get("accessionNumberLabel");
            }
			accessionNumberWidget = new TextFieldWidget();
			context.registerWidget(accessionNumberWidget);
			context.registerErrorWidget(accessionNumberWidget, errorWidget);
			if (existingObs != null) {
				accessionNumberWidget.setInitialValue(existingObs.getAccessionNumber());
			}
		}
		
		// if a comment is requested, do that too
		if ("true".equals(parameters.get("showCommentField"))  || parameters.containsKey("commentFieldLabel") || parameters.containsKey("commentFieldCode")) {
			if(parameters.containsKey("commentFieldLabel")){
                commentFieldLabel = parameters.get("commentFieldLabel");
            }
			else if (parameters.containsKey("commentFieldCode")) {
				commentFieldLabel = context.getTranslator().translate(userLocaleStr, parameters.get("commentFieldCode"));
			}
            commentFieldWidget = new TextFieldWidget();
			context.registerWidget(commentFieldWidget);
			context.registerErrorWidget(commentFieldWidget, errorWidget);
			if (existingObs != null) {
				commentFieldWidget.setInitialValue(existingObs.getComment());
			}
		}

		ObsField field = new ObsField();
		field.setName(valueLabel);
		if (concept != null) {
			field.setQuestion(concept);
		} else if (concepts != null && concepts.size() > 0) { //for concept selects
			for (int i = 0; i < concepts.size(); i++) {
				ObsFieldAnswer ans = new ObsFieldAnswer();
				ans.setConcept(concepts.get(i));
				if (i < conceptLabels.size()) {
					ans.setDisplayName(conceptLabels.get(i));
				}
				field.getQuestions().add(ans);
			}
		}
		if (answerConcept != null) {
			ObsFieldAnswer ans = new ObsFieldAnswer();
			ans.setDisplayName(getAnswerLabel());
			ans.setConcept(answerConcept);
			field.setAnswers(Arrays.asList(ans));
		} else if (conceptAnswers != null) {
			for (int i = 0; i < conceptAnswers.size(); i++) {
				ObsFieldAnswer ans = new ObsFieldAnswer();
				ans.setConcept(conceptAnswers.get(i));
				if (i < answerLabels.size()) {
					ans.setDisplayName(answerLabels.get(i));
				}
				field.getAnswers().add(ans);
			}
		}

       field.setExistingObs(existingObs);

        // add the field to active obsgroup if there is one, other to the active section
		if (concept != null && context.getActiveObsGroup() != null) {
			context.addFieldToActiveObsGroup(field);
		} else {
			context.addFieldToActiveSection(field);
		}
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		if (id != null || clazz != null) {
			ret.append("<span " + (id != null ?  "id=\"" + id + "\" " : "") +
                    "class=\"obs-field"+ (clazz !=null ? " " + clazz : "") + "\">");
			context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(valueWidget),
			    getFieldFunction(valueWidget), getGetterFunction(valueWidget), getSetterFunction(valueWidget));
			context.registerPropertyAccessorInfo(id + ".date", context.getFieldNameIfRegistered(dateWidget),
			    "dateFieldGetterFunction", null, "dateSetterFunction");
			context.registerPropertyAccessorInfo(id + ".error", context.getFieldNameIfRegistered(errorWidget), null, null,
			    null);
			context.registerPropertyAccessorInfo(id + ".accessionNumber",
			    context.getFieldNameIfRegistered(accessionNumberWidget), null, null, null);
			if (commentFieldWidget != null) {
				context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(commentFieldWidget),
				    null, null, null);
			}
		}
		ret.append(valueLabel);
		if (!"".equals(valueLabel))
			ret.append(" ");
		ret.append(valueWidget.generateHtml(context));
		if (showUnits) {
			if (concept == null || !concept.getDatatype().isNumeric()) {
				throw new IllegalArgumentException("Can only show units when the concept is numeric");
			}
			String units;
			if (concept instanceof ConceptNumeric) {
				units = ((ConceptNumeric) concept).getUnits();
			} else {
				ConceptNumeric asConceptNumeric = Context.getConceptService().getConceptNumeric(concept.getConceptId());
				if (asConceptNumeric == null) {
					throw new IllegalStateException("Concept " + concept + " (" + concept.getName().getName() + ") has datatype = Numeric, but no row in concept_numeric");
				}
				units = asConceptNumeric.getUnits();
			}
			ret.append("<span class=\"" + unitsCssClass + "\">");
            if (unitsCode != null) {
                ret.append(context.getTranslator().translate(locale.toString(), unitsCode));
            } else if (units != null) {
                ret.append(units);
            }
            ret.append("</span>");
		}
		if (dateWidget != null) {
			ret.append(" ");
			if (dateLabel != null) {
				ret.append(dateLabel);
			}
			ret.append(dateWidget.generateHtml(context));
		}
		if (accessionNumberWidget != null) {
			ret.append(" ");
			if (accessionNumberLabel != null) {
				ret.append("<br/>" + accessionNumberLabel);
			}
			ret.append(accessionNumberWidget.generateHtml(context));
		}
		if (commentFieldWidget != null) {
			ret.append(" ");
            if(commentFieldLabel != null){
               ret.append(commentFieldLabel);
            }else {
               ret.append(Context.getMessageSourceService().getMessage("htmlformentry.comment")+":");
            }
			ret.append(" ");
			ret.append(commentFieldWidget.generateHtml(context));
		}

		if (context.getMode() != Mode.VIEW) {
            // if value is required
            if (required) {
                ret.append("<span class='required'>*</span>");
            }
            ret.append(" ");
			ret.append(errorWidget.generateHtml(context));
		}
		if (id != null || clazz != null) {
			ret.append("</span>");
        }
		return ret.toString();
	}
	
	/**
	 * TODO implement for all non-standard widgets
	 * 
	 * @param widget
	 * @return
	 */
	private String getSetterFunction(Widget widget) {
		if (widget == null)
			return null;
		if (widget instanceof CheckboxWidget)
			return "checkboxSetterFunction";
		if (widget instanceof DateWidget)
			return "dateSetterFunction";
		return null;
	}
	
	/**
	 * TODO implement for all non-standard widgets
	 * 
	 * @param widget
	 * @return
	 */
	private String getGetterFunction(Widget widget) {
		if (widget == null)
			return null;
		if (widget instanceof CheckboxWidget)
			return "checkboxGetterFunction";
		return null;
	}
	
	/**
	 * TODO implement for all non-standard widgets TODO figure out how to return multiple elements,
	 * e.g. for date+time widget
	 * 
	 * @param widget
	 * @return
	 */
	private String getFieldFunction(Widget widget) {
		if (widget == null)
			return null;
		if (widget instanceof DateWidget)
			return "dateFieldGetterFunction";
		return null;
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		Object value = null;
		Object date = null;
		
		try {
			value = valueWidget.getValue(context, submission);
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(valueWidget, ex.getMessage()));
		}
		
		try {
			if (dateWidget != null)
				date = dateWidget.getValue(context, submission);
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(dateWidget, ex.getMessage()));
		}
		
		if (value == null && date != null)
			ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage(
			    "htmlformentry.error.dateWithoutValue")));
		
		if (date != null && OpenmrsUtil.compare((Date) date, new Date()) > 0)
			ret.add(new FormSubmissionError(dateWidget, Context.getMessageSourceService().getMessage(
			    "htmlformentry.error.cannotBeInFuture")));
		
		if (value instanceof Date && !allowFutureDates && OpenmrsUtil.compare((Date) value, new Date()) > 0) {
			ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage(
			    "htmlformentry.error.cannotBeInFuture")));
		}
		
		if (required) {
			if (value == null) {
				ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage(
				    "htmlformentry.error.required")));
			} else if (value instanceof String) {
				String valueStr = (String) value;
				if (StringUtils.isEmpty(valueStr)) {
					ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage(
					    "htmlformentry.error.required")));
				}
			}
		}
		
		return ret;
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		Object value = valueWidget.getValue(session.getContext(), submission);
        if (value instanceof String && ((String) value).startsWith("Drug:")) {
            String drugId = ((String) value).substring("Drug:".length());
            value = HtmlFormEntryUtil.convertToType(drugId, Drug.class);
        }
		if (concepts != null) {
			try {
				if (value instanceof Concept)
					concept = (Concept) value;
				else
					concept = (Concept) HtmlFormEntryUtil.convertToType(value.toString(), Concept.class);
			}
			catch (Exception ex) {
				throw new RuntimeException("Unable to convert response to a concept!");
			}
		}
		Date obsDatetime = null;
		String accessionNumberValue = null;
		if (dateWidget != null)
			obsDatetime = (Date) dateWidget.getValue(session.getContext(), submission);
		if (accessionNumberWidget != null)
			accessionNumberValue = (String) accessionNumberWidget.getValue(session.getContext(), submission);

		String comment = null;
		if (commentFieldWidget != null)
			comment = commentFieldWidget.getValue(session.getContext(), submission);

        // note that for the remote use case where showCommentField=true and style=location won't work, as "org.openmrs.Location"
        // will override any user-entered comment
        if (isLocationObs) {
            comment = "org.openmrs.Location";
        }

		if (existingObsList != null && session.getContext().getMode() == Mode.EDIT) {

		    // trying to break this up a little bit by factoring specific functionality for specific widgets into separate methods at least
            handleDynamicAutocompleteSubmissionInEditMode(session, submission, value, obsDatetime, accessionNumberValue);

	    } else if (existingObs != null && session.getContext().getMode() == Mode.EDIT) {
	    	// For complex obs: if value is to be retained, get the value from existingObs
			if (valueWidget instanceof UploadWidget && value == null) {
				if (!((UploadWidget) valueWidget).shouldDelete(session.getContext(), submission)) {
					value = new ComplexData(existingObs.getValueComplex(), null);
				}
			}
	    				
			// call this regardless of whether the new value is null -- the
			// modifyObs method is smart
			if (concepts != null)
				session.getSubmissionActions().modifyObs(existingObs, concept, answerConcept, obsDatetime,
				    accessionNumberValue, comment);
			else
				session.getSubmissionActions().modifyObs(existingObs, concept, value, obsDatetime, accessionNumberValue, comment);
		} else {
			if (concepts != null && value != null && !"".equals(value) && concept != null) {
				session.getSubmissionActions().createObs(concept, answerConcept, obsDatetime, accessionNumberValue, comment);
			} else if (value != null && !"".equals(value)) {
				if (valueWidget instanceof DynamicAutocompleteWidget) {

                    // trying to break this up a little bit by factoring specific functionality for specific widgets into separate methods at least
				    handleDynamicAutocompleteSubmissionInEnterMode(session, submission, value, obsDatetime, accessionNumberValue);

                } else {
				    session.getSubmissionActions().createObs(concept, value, obsDatetime, accessionNumberValue, comment);
			    }
            }
		}
	}

    private void handleDynamicAutocompleteSubmissionInEnterMode(FormEntrySession session, HttpServletRequest submission, Object value, Date obsDatetime, String accessionNumberValue) {

        List values = (List) value;

        // create an obs for each value
        for (Object val : values) {
            int conceptId = Integer.valueOf((String) val);
            ((DynamicAutocompleteWidget)valueWidget).addInitialValue(Context.getConceptService().getConcept(conceptId));
            session.getSubmissionActions().createObs(concept, conceptId, obsDatetime, accessionNumberValue);
        }

    }

    private void handleDynamicAutocompleteSubmissionInEditMode(FormEntrySession session, HttpServletRequest submission, Object value, Date obsDatetime, String accessionNumberValue) {

        List values = (List) value;

        List<Concept> newConceptList = new Vector<Concept>();
        List<Concept> existingConceptList = ((DynamicAutocompleteWidget) valueWidget).getInitialValueList();

        // get the list of concepts entered on the form
        for (Object val : values) {
            if (StringUtils.isNotBlank((String) val)) {
                int conceptId = Integer.valueOf((String) val);
                newConceptList.add(Context.getConceptService().getConcept(conceptId));
            }
        }

        // figure out what obs we need to create and what ones we need to remove
        for (Concept c : existingConceptList) {
            if (newConceptList.contains(c))
                newConceptList.remove(c);
            else {
                for (Obs o : existingObsList) {
                    if (o.getValueCoded().equals(c))
                        session.getSubmissionActions().modifyObs(o, concept, null, obsDatetime, accessionNumberValue);
                }
            }
        }
        if (!newConceptList.isEmpty()) {
            for (Concept c : newConceptList) {
                session.getSubmissionActions().createObs(concept, c, obsDatetime, accessionNumberValue);
            }
        }
    }

	private Comparator<Concept> conceptNameComparator = new Comparator<Concept>() {
		
		@Override
		public int compare(Concept c1, Concept c2) {
			String n1 = c1.getName(locale, false).getName();
			String n2 = c2.getName(locale, false).getName();
			return n1.compareTo(n2);
		}
	};

	private CheckboxWidget createCheckboxWidget(String label, String value, String toggleParameter) {
		if (toggleParameter != null) {
			ToggleWidget toggleWidget = new ToggleWidget(toggleParameter);
			return new CheckboxWidget(label, value, toggleWidget.getTargetId(), toggleWidget.isToggleDim());
		} else {
			return new CheckboxWidget(label, value, null);
		}
	}

	/**
	 * Returns the concept associated with this Observation
	 */
	public Concept getConcept() {
		return concept;
	}
	
	/**
	 * Returns the concept associated with the answer to this Observation
	 */
	public Concept getAnswerConcept() {
		return answerConcept;
	}
	
	/**
	 * Returns the concepts that are potential answers to this Observation
	 */
	public List<Concept> getConceptAnswers() {
		return conceptAnswers;
	}
	
	/**
	 * Returns the Numbers that are potential answers for this Observation
	 */
	public List<Number> getNumericAnswers() {
		return numericAnswers;
	}
	
	/**
	 * Returns the potential text answers for this Observation
	 */
	public List<String> getTextAnswers() {
		return textAnswers;
	}
	
	/**
	 * Returns the labels to use for the answers to this Observation
	 */
	public List<String> getAnswerLabels() {
		return answerLabels;
	}
	
	/**
	 * Returns the label to use for the answer to this Observation
	 */
	public String getAnswerLabel() {
		return answerLabel;
	}
	
	public String getValueLabel() {
		return valueLabel;
	}
	
	public Obs getExistingObs() {
		return existingObs;
	}

    public void whenValueThenDisplaySection(Object value, String thenSection) {
        whenValueThenDisplaySection.put(value, thenSection);
    }

    public Map<Object, String> getWhenValueThenDisplaySection() {
        return whenValueThenDisplaySection;
    }

    public void whenValueThenJavaScript(Object value, String thenJavaScript) {
        whenValueThenJavascript.put(value, thenJavaScript);
    }

    public Map<Object, String> getWhenValueThenJavascript() {
        return whenValueThenJavascript;
    }

    public void whenValueElseJavaScript(Object value, String elseJavaScript) {
        whenValueElseJavascript.put(value, elseJavaScript);
    }

    public Map<Object, String> getWhenValueElseJavascript() {
        return whenValueElseJavascript;
    }

    public boolean hasWhenValueThen() {
        return whenValueThenDisplaySection.size() > 0
                || whenValueThenJavascript.size() > 0
                || whenValueElseJavascript.size() > 0;
    }

    public String getId() {
        return id;
    }

}
