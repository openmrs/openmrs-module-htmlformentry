package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.widget.AutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateTimeWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.LocationWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsUtil;

/**
 * Holds the widgets used to represent a specific Observation, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for the Observation.
 */
public class ObsSubmissionElement implements HtmlGeneratorElement,
		FormSubmissionControllerAction {

	private Concept concept;
	private String valueLabel;
	private Widget valueWidget;
	private String dateLabel;
	private DateWidget dateWidget;
	private String accessionNumberLabel;
	private TextFieldWidget accessionNumberWidget;
	private ErrorWidget errorWidget;
	private boolean allowFutureDates = false;
	private Concept answerConcept;
	private List<Concept> conceptAnswers = new ArrayList<Concept>();
	private List<Number> numericAnswers = new ArrayList<Number>();
	private List<String> textAnswers = new ArrayList<String>();
	private List<String> answerLabels = new ArrayList<String>();
	private String answerLabel;
    private Obs existingObs; // in edit mode, this allows submission to check whether the obs has been modified or not
    private boolean required;

	public ObsSubmissionElement(FormEntryContext context,
			Map<String, String> parameters) {
		try {
			String conceptId = parameters.get("conceptId");
			concept = HtmlFormEntryUtil.getConcept(conceptId);
			if (concept == null)
				throw new NullPointerException();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Cannot find concept in "
					+ parameters);
		}
		if ("true".equals(parameters.get("allowFutureDates")))
			allowFutureDates = true;
        if ("true".equals(parameters.get("required"))) {
        	required = true;
        }
		prepareWidgets(context, parameters);
	}

	private void prepareWidgets(FormEntryContext context,
			Map<String, String> parameters) {
		String userLocaleStr = Context.getLocale().toString();
		try {
			answerConcept = HtmlFormEntryUtil.getConcept(parameters.get("answerConceptId"));
		} catch (Exception ex) {
		}
		if (context.getCurrentObsGroupConcepts() != null
				&& context.getCurrentObsGroupConcepts().size() > 0) {
			existingObs = context
					.getObsFromCurrentGroup(concept, answerConcept);
		} else {
			if (concept.getDatatype().isBoolean()
					&& "checkbox".equals(parameters.get("style"))) {
				// since a checkbox has one value we need to look for an exact
				// match for that value
				if ("false".equals(parameters.get("value"))) {
					existingObs = context.removeExistingObs(concept, false);
				} else {
					// if not 'false' we treat as 'true'
					existingObs = context.removeExistingObs(concept, true);
				}

			} else {
				existingObs = context.removeExistingObs(concept, answerConcept);
			}
		}

		errorWidget = new ErrorWidget();
		context.registerWidget(errorWidget);

		if (parameters.containsKey("labelNameTag")) {
			if (parameters.get("labelNameTag").equals("default"))
				valueLabel = concept.getBestName(Context.getLocale()).getName();
			else
				throw new IllegalArgumentException(
						"Name tags other than 'default' not yet implemented");
		} else if (parameters.containsKey("labelText")) {
			valueLabel = parameters.get("labelText");
		} else if (parameters.containsKey("labelCode")) {
			valueLabel = context.getTranslator().translate(userLocaleStr,
					parameters.get("labelCode"));
		} else {
			valueLabel = "";
		}
		if (parameters.get("answerLabels") != null) {
			answerLabels = Arrays.asList(parameters.get("answerLabels").split(
					","));
		}
		if (parameters.get("answerCodes") != null) {
			String[] split = parameters.get("answerCodes").split(",");
			for (String s : split) {
				answerLabels.add(context.getTranslator().translate(
						userLocaleStr, s));
			}
		}
		if (concept.getDatatype().isNumeric()) {
			if (parameters.get("answers") != null) {
				try {
					for (StringTokenizer st = new StringTokenizer(parameters
							.get("answers"), ", "); st.hasMoreTokens();) {
						Number answer = Double.valueOf(st.nextToken());
						numericAnswers.add(answer);
					}
				} catch (Exception ex) {
					throw new RuntimeException(
							"Error in answer list for concept "
									+ concept.getConceptId() + " ("
									+ ex.toString() + "): " + conceptAnswers);
				}
			}
			ConceptNumeric cn = Context.getConceptService().getConceptNumeric(
					concept.getConceptId());
			if (numericAnswers.size() == 0) {
				valueWidget = new NumberFieldWidget(cn);
			} else {
				if ("radio".equals(parameters.get("style"))) {
					valueWidget = new RadioButtonsWidget();
				} else { // dropdown
					valueWidget = new DropdownWidget();
					((DropdownWidget) valueWidget).addOption(new Option());
				}
				// need to make sure we have the initialValue too
				Number lookFor = existingObs == null ? null : existingObs
						.getValueNumeric();
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
					((SingleOptionWidget) valueWidget).addOption(new Option(
							label, n.toString(), false));
				}
				// if lookFor is still non-null, we need to add it directly as
				// an option:
				if (lookFor != null)
					((SingleOptionWidget) valueWidget).addOption(new Option(
							lookFor.toString(), lookFor.toString(), true));
			}
			if (existingObs != null) {
				valueWidget.setInitialValue(existingObs.getValueNumeric());
			}
		} else if (concept.getDatatype().isText()) {
			if (parameters.get("answers") != null) {
				try {
					for (StringTokenizer st = new StringTokenizer(parameters
							.get("answers"), ","); st.hasMoreTokens();) {
						textAnswers.add(st.nextToken());
					}
				} catch (Exception ex) {
					throw new RuntimeException(
							"Error in answer list for concept "
									+ concept.getConceptId() + " ("
									+ ex.toString() + "): " + conceptAnswers);
				}
			}
			if ("location".equals(parameters.get("style"))) {
				valueWidget = new LocationWidget();
			} else {
				if (textAnswers.size() == 0) {
					Integer rows = null;
					Integer cols = null;
					try {
						rows = Integer.valueOf(parameters.get("rows"));
					} catch (Exception ex) {
					}
					try {
						cols = Integer.valueOf(parameters.get("cols"));
					} catch (Exception ex) {
					}
					if (rows != null || cols != null
							|| "textarea".equals(parameters.get("style"))) {
						valueWidget = new TextFieldWidget(rows, cols);
					} else {
						Integer size = null;
						try {
							size = Integer.valueOf(parameters.get("size"));
						} catch (Exception ex) {
						}
						valueWidget = new TextFieldWidget(size);
					}
				} else {
					if ("radio".equals(parameters.get("style"))) {
						valueWidget = new RadioButtonsWidget();
					} else { // dropdown
						valueWidget = new DropdownWidget();
						((DropdownWidget) valueWidget).addOption(new Option());
					}
					// need to make sure we have the initialValue too
					String lookFor = existingObs == null ? null : existingObs
							.getValueText();
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
						((SingleOptionWidget) valueWidget)
								.addOption(new Option(label, s, false));
					}
					// if lookFor is still non-null, we need to add it directly
					// as an option:
					if (lookFor != null)
						((SingleOptionWidget) valueWidget)
								.addOption(new Option(lookFor, lookFor, true));
				}
			}
			if (existingObs != null) {
				Object value;
				if ("location".equals(parameters.get("style"))) {
					value = Context.getLocationService().getLocation(
							Integer.valueOf(existingObs.getValueText()));
				} else {
					value = existingObs.getValueText();
				}
				valueWidget.setInitialValue(value);
			}
		} else if (concept.getDatatype().isCoded()) {
			if (parameters.get("answerConceptIds") != null) {
				try {
					for (StringTokenizer st = new StringTokenizer(parameters
							.get("answerConceptIds"), ", "); st.hasMoreTokens();) {
						Concept c = HtmlFormEntryUtil.getConcept(st.nextToken());
						if (c == null)
							throw new RuntimeException("Cannot find concept "
									+ st.nextToken());
						conceptAnswers.add(c);
					}
				} catch (Exception ex) {
					throw new RuntimeException(
							"Error in answer list for concept "
									+ concept.getConceptId() + " ("
									+ ex.toString() + "): " + conceptAnswers);
				}
			} else if (parameters.get("answerClasses") != null) {
				try {
					for (StringTokenizer st = new StringTokenizer(parameters
							.get("answerClasses"), ","); st.hasMoreTokens();) {
						String className = st.nextToken().trim();
						ConceptClass cc = Context.getConceptService()
								.getConceptClassByName(className);
						if (cc == null) {
							throw new RuntimeException(
									"Cannot find concept class " + className);
						}
						conceptAnswers.addAll(Context.getConceptService()
								.getConceptsByClass(cc));
					}
					Collections.sort(conceptAnswers, conceptNameComparator);
				} catch (Exception ex) {
					throw new RuntimeException(
							"Error in answer class list for concept "
									+ concept.getConceptId() + " ("
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
						answerLabel = context.getTranslator().translate(
								userLocaleStr, answerCode);
					} else {
						answerLabel = answerConcept.getBestName(
								Context.getLocale()).getName();
					}
				}
				valueWidget = new CheckboxWidget(answerLabel, answerConcept
						.getConceptId().toString());
				if (existingObs != null) {
					valueWidget.setInitialValue(existingObs.getValueCoded());
				}
			} else if ("true".equals(parameters.get("multiple"))) {
				// if this is a select-multi, we need a group of checkboxes
				throw new RuntimeException(
						"Multi-select coded questions are not yet implemented");
			} else {
				// If no conceptAnswers are specified, 
				if (conceptAnswers == null || conceptAnswers.isEmpty()) {
					// if style = autocomplete
					if("autocomplete".equals(parameters.get("style"))){
						throw new RuntimeException(
						"style \"autocomplete\" has to work with either \"answerClasses\" or \"answerConceptIds\" attribute");
					}
					// else use all available conceptAnswers
					conceptAnswers = new ArrayList<Concept>();
					for (ConceptAnswer ca : concept.getAnswers(false)) {
						conceptAnswers.add(ca.getAnswerConcept());
					}
					Collections.sort(conceptAnswers, conceptNameComparator);
				}
				
				if ("autocomplete".equals(parameters.get("style"))){
					List<ConceptClass> cptClasses = new ArrayList<ConceptClass>();
					if (parameters.get("answerClasses") != null) {
						for (StringTokenizer st = new StringTokenizer(parameters
								.get("answerClasses"), ","); st.hasMoreTokens();) {
							String className = st.nextToken().trim();
							ConceptClass cc = Context.getConceptService()
									.getConceptClassByName(className);
							cptClasses.add(cc);
						}
					}
					valueWidget = new AutocompleteWidget(conceptAnswers, cptClasses);
				} else {
					// Show Radio Buttons if specified, otherwise default to Drop
					// Down 
					boolean isRadio = "radio".equals(parameters.get("style"));
					if (isRadio) {
						valueWidget = new RadioButtonsWidget();
					}
					else {
						valueWidget = new DropdownWidget();
						((DropdownWidget) valueWidget).addOption(new Option());
					}
					for (int i = 0; i < conceptAnswers.size(); ++i) {
						Concept c = conceptAnswers.get(i);
						String label = null;
						if (answerLabels != null && i < answerLabels.size()) {
							label = answerLabels.get(i);
						} else {
							label = c.getBestName(Context.getLocale()).getName();
						}
						((SingleOptionWidget) valueWidget).addOption(new Option(
							label, c.getConceptId().toString(), false));
					}
				}
				if (existingObs != null) {
					valueWidget.setInitialValue(existingObs.getValueCoded());
				}
			}
		} else if (concept.getDatatype().isBoolean()) {
			String noStr = parameters.get("noLabel");
			if (StringUtils.isEmpty(noStr)) {
				noStr = context.getTranslator().translate(userLocaleStr,
						"general.no");
			}
			String yesStr = parameters.get("yesLabel");
			if (StringUtils.isEmpty(yesStr)) {
				yesStr = context.getTranslator().translate(userLocaleStr,
						"general.yes");
			}
			if ("checkbox".equals(parameters.get("style"))) {
				valueWidget = new CheckboxWidget(valueLabel, parameters
						.get("value") != null ? parameters.get("value")
						: "true");
				valueLabel = "";
				if (existingObs != null) {
					valueWidget
							.setInitialValue(existingObs.getValueAsBoolean());
				}
			} else if ("no_yes".equals(parameters.get("style"))) {
				valueWidget = new RadioButtonsWidget();
				((RadioButtonsWidget) valueWidget).addOption(new Option(noStr,
						"false", false));
				((RadioButtonsWidget) valueWidget).addOption(new Option(yesStr,
						"true", false));
				if (existingObs != null) {
					valueWidget
							.setInitialValue(existingObs.getValueAsBoolean());
				}
			} else if ("yes_no".equals(parameters.get("style"))) {
				valueWidget = new RadioButtonsWidget();
				((RadioButtonsWidget) valueWidget).addOption(new Option(yesStr,
						"true", false));
				((RadioButtonsWidget) valueWidget).addOption(new Option(noStr,
						"false", false));
				if (existingObs != null) {
					valueWidget
							.setInitialValue(existingObs.getValueAsBoolean());
				}
			} else if ("no_yes_dropdown".equals(parameters.get("style"))) {
				valueWidget = new DropdownWidget();
				((DropdownWidget) valueWidget).addOption(new Option());
				((DropdownWidget) valueWidget).addOption(new Option(noStr,
						"false", false));
				((DropdownWidget) valueWidget).addOption(new Option(yesStr,
						"true", false));
				if (existingObs != null) {
					valueWidget
							.setInitialValue(existingObs.getValueAsBoolean());
				}
			} else if ("yes_no_dropdown".equals(parameters.get("style"))) {
				valueWidget = new DropdownWidget();
				((DropdownWidget) valueWidget).addOption(new Option());
				((DropdownWidget) valueWidget).addOption(new Option(yesStr,
						"true", false));
				((DropdownWidget) valueWidget).addOption(new Option(noStr,
						"false", false));
				if (existingObs != null) {
					valueWidget
							.setInitialValue(existingObs.getValueAsBoolean());
				}
			} else {
				throw new RuntimeException("Boolean with style = "
						+ parameters.get("style")
						+ " not yet implemented (concept = "
						+ concept.getConceptId() + ")");
			}
		
		// TODO: in 1.7-compatible version of the module, we can replace the H17 checks
		// used below with the new isDate, isTime, and isDatetime
		
		// if it's a Date type
		} else if (ConceptDatatype.DATE.equals(concept.getDatatype()
				.getHl7Abbreviation())) {
			valueWidget = new DateWidget();
			if (existingObs != null)
				valueWidget.setInitialValue(existingObs.getValueDatetime());
		}
		// if it's a Time type
		else if (ConceptDatatype.TIME.equals(concept.getDatatype()
				.getHl7Abbreviation())) {
			valueWidget = new TimeWidget();
			if (existingObs != null)
				valueWidget.setInitialValue(existingObs.getValueDatetime());
		}
		// if it's a Date Time type
		else if (ConceptDatatype.DATETIME.equals(concept.getDatatype()
				.getHl7Abbreviation())) {
			DateWidget dateWidget = new DateWidget();
			TimeWidget timeWidget = new TimeWidget();
				
			valueWidget = new DateTimeWidget(dateWidget,timeWidget);
			if (existingObs != null)
				valueWidget.setInitialValue(existingObs.getValueDatetime());
				
			context.registerWidget(dateWidget);
			context.registerWidget(timeWidget);
		} else {
			throw new RuntimeException("Cannot handle datatype: "
					+ concept.getDatatype().getName() + " (for concept "
					+ concept.getConceptId() + ")");
		}
		context.registerWidget(valueWidget);
		context.registerErrorWidget(valueWidget, errorWidget);

		// if a date is requested, do that too
		if ("true".equals(parameters.get("showDate"))
				|| parameters.containsKey("dateLabel")) {
			if (parameters.containsKey("dateLabel"))
				dateLabel = parameters.get("dateLabel");
			dateWidget = new DateWidget();
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, errorWidget);
			if (existingObs != null) {
				dateWidget.setInitialValue(existingObs.getObsDatetime());
			}
		}

		// if an accessionNumber is requested, do that too
		if ("true".equals(parameters.get("showAccessionNumber"))
				|| parameters.containsKey("accessionNumberLabel")) {
			if (parameters.containsKey("accessionNumberLabel"))
				accessionNumberLabel = parameters.get("accessionNumberLabel");
			accessionNumberWidget = new TextFieldWidget();
			context.registerWidget(accessionNumberWidget);
			context.registerErrorWidget(accessionNumberWidget, errorWidget);
			if (existingObs != null) {
				accessionNumberWidget.setInitialValue(existingObs
						.getAccessionNumber());
			}
		}

		ObsField field = new ObsField();
		field.setName(valueLabel);
		field.setQuestion(concept);
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
		if (context.getActiveObsGroup() != null) {
			context.getActiveObsGroup().getChildren().add(field);
		} else {
			context.getSchema().addField(field);
		}
	}

	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		ret.append(valueLabel);
		if (!"".equals(valueLabel))
			ret.append(" ");
		ret.append(valueWidget.generateHtml(context));
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
        
        // if value is required
        if (required) {
        	ret.append("<span class='required'>*</span>");
        }
        
		if (context.getMode() != Mode.VIEW) {
			ret.append(" ");
			ret.append(errorWidget.generateHtml(context));
		}
		return ret.toString();
	}

	public Collection<FormSubmissionError> validateSubmission(
			FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		Object value = null;
		Object date = null;

		try {
			value = valueWidget.getValue(context, submission);
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(valueWidget, ex.getMessage()));
		}

		try {
			if (dateWidget != null)
				date = dateWidget.getValue(context, submission);
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(dateWidget, ex.getMessage()));
		}

		if (value == null && date != null)
			ret.add(new FormSubmissionError(valueWidget, Context
					.getMessageSourceService().getMessage(
							"htmlformentry.error.dateWithoutValue")));

		if (date != null && OpenmrsUtil.compare((Date) date, new Date()) > 0)
			ret.add(new FormSubmissionError(dateWidget, Context
					.getMessageSourceService().getMessage(
							"htmlformentry.error.cannotBeInFuture")));



		if (value instanceof Date && !allowFutureDates
				&& OpenmrsUtil.compare((Date) value, new Date()) > 0) {
			ret.add(new FormSubmissionError(valueWidget, Context
					.getMessageSourceService().getMessage(
							"htmlformentry.error.cannotBeInFuture")));
		}

		if (required) {
        	if (value == null) {
        		ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.required")));
        	} else if (value instanceof String) {
        		String valueStr = (String) value;
        		if (StringUtils.isEmpty(valueStr)) {
        			ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.required")));
        		}
        	}
        }
		
		return ret;
	}

	public void handleSubmission(FormEntrySession session,
			HttpServletRequest submission) {
		Object value = valueWidget.getValue(session.getContext(), submission);
		Date obsDatetime = null;
		String accessionNumberValue = null;
		if (dateWidget != null)
			obsDatetime = (Date) dateWidget.getValue(session.getContext(),
					submission);
		if (accessionNumberWidget != null)
			accessionNumberValue = (String) accessionNumberWidget.getValue(
					session.getContext(), submission);
		if (existingObs != null && session.getContext().getMode() == Mode.EDIT) {
			// call this regardless of whether the new value is null -- the
			// modifyObs method is smart
			session.getSubmissionActions().modifyObs(existingObs, concept,
					value, obsDatetime, accessionNumberValue);
		} else {
			if (value != null && !"".equals(value)) {
				session.getSubmissionActions().createObs(concept, value,
						obsDatetime, accessionNumberValue);
			}
		}
	}

	private Comparator<Concept> conceptNameComparator = new Comparator<Concept>() {
		public int compare(Concept c1, Concept c2) {
			String n1 = c1.getBestName(Context.getLocale()).getName();
			String n2 = c2.getBestName(Context.getLocale()).getName();
			return n1.compareTo(n2);
		}
	};

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
}
