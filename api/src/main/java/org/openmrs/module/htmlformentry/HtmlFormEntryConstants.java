package org.openmrs.module.htmlformentry;

/** Constants used by the HTML Form Entry module */

public class HtmlFormEntryConstants {
	
	/** Constant used by {@see HtmlFormEntryUtil#documentToString(Document)} */
	public final static String CONSTANT_XML = "xml";
	
	/** Constant used by {@see HtmlFormEntryUtil#documentToString(Document)} */
	public final static String CONSTANT_YES = "yes";
	
	/** Constant used by {@see HtmlFormEntryUtil#documentToString(Document)} */
	public final static String ERROR_TRANSFORMER_1 = "TransformerFactory.newTransformer error:";
	
	/** Constant used by {@see HtmlFormEntryUtil#documentToString(Document)} */
	public final static String ERROR_TRANSFORMER_2 = "Transformer.transform error:";
	
	public final static String[] ENCOUNTER_TAGS = { "encounterDate", "encounterLocation", "encounterProvider" };
	
	public final static String[] PATIENT_TAGS = { "patient" };
	
	public final static String SYSTEM_DEFAULT = "SystemDefault";
	
	public final static String GP_DATE_FORMAT = "htmlformentry.dateFormat";
	
	public final static String GP_TIME_FORMAT = "htmlformentry.timeFormat";
	
	public final static String GP_YEARS_RANGE = "htmlformentry.datePickerYearsRange";
	
	public final static String GP_SHOW_DATE_FORMAT = "htmlformentry.showDateFormat";
	
	public final static String GP_CLASSES_NOT_TO_EXPORT_WITH_MDS = "htmlformentry.classesNotToExportWithMetadataSharing";
	
	public static final String GP_UNKNOWN_CONCEPT = "concept.unknown";
	
	public static final String GP_RESTRICT_ENCOUNTER_LOCATION_TO_CURRENT_VISIT_LOCATION = "htmlformentry.restrictEncounterLocationToCurrentVisitLocation";
	
	public static final String GP_TIME_WIDGET_HIDE_SECONDS_DEFAULT = "htmlformentry.timeWidgetHideSecondsDefault";
	
	public final static String GP_TIMEZONE_CONVERSIONS = "timezone.conversions";
	
	public static final String COMPLEX_UUID = "8d4a6242-c2cc-11de-8d13-0010c6dffd0f";
	
	public static final String ANSWER_LOCATION_TAGS = "answerLocationTags";
	
	public static final String FORM_NAMESPACE = "HtmlFormEntry";
	
	public static final String DATETIME_FALLBACK_FORMAT = "dd-MM-yyyy, HH:mm:ss";
	
	/**
	 * The name of the user property that saves the client timezone TODO this UP should be removed once
	 * the timezone.conversions UP is provided by Core.
	 */
	public static final String UP_CLIENT_TIMEZONE = "clientTimezone";
	
	//The name of the global property that saves Datetime Format.
	public static final String GP_FORMATTER_DATETIME = "uiframework.formatter.dateAndTimeFormat";
	
	public static final String HTML_FORM_TAG = "htmlform";
	
	public static final String FORM_UUID_ATTRIBUTE = "formUuid";
	
	public static final String FORM_NAME_ATTRIBUTE = "formName";
	
	public static final String FORM_DESCRIPTION_ATTRIBUTE = "formDescription";
	
	public static final String FORM_VERSION_ATTRIBUTE = "formVersion";
	
	public static final String FORM_PUBLISHED_ATTRIBUTE = "formPublished";
	
	public static final String FORM_RETIRED_ATTRIBUTE = "formRetired";
	
	public static final String FORM_ENCOUNTER_TYPE_ATTRIBUTE = "formEncounterType";
	
	public static final String HTML_FORM_UUID_ATTRIBUTE = "htmlformUuid";
	
	public static final String FORM_RESOURCE_PREFIX = "resource";
	
	public static final String FORM_RESOURCE_DELIMITER = "-";
	
	public static final String FORM_RESOURCE_NAME = "name";
	
	public static final String FORM_RESOURCE_VALUE = "value";
	
	public static final String FORM_RESOURCE_DATA_TYPE_CLASS = "datatype-class";
	
	public static final String FORM_RESOURCE_DATA_TYPE_CONFIG = "datatype-config";
	
	public static final String FORM_RESOURCE_HANDLER_CLASS = "handler-class";
	
	public static final String FORM_RESOURCE_HANDLER_CONFIG = "handler-config";
}
