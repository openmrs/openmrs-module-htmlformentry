package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.action.ObsGroupAction;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.openmrs.obs.ComplexData;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.EncounterTypeEditor;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.PatientEditor;
import org.openmrs.propertyeditor.PersonEditor;
import org.openmrs.propertyeditor.UserEditor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HTML Form Entry utility methods
 */
public class HtmlFormEntryUtil {
	
	public static Log log = LogFactory.getLog(HtmlFormEntryUtil.class);
	
	/**
	 * Returns the HTML Form Entry service from the Context
	 * 
	 * @return HTML Form Entry service
	 */
	public static HtmlFormEntryService getService() {
		return Context.getService(HtmlFormEntryService.class);
	}
	
	/**
	 * Fetches a http request parameter from an http request and returns it as a specific type
	 * 
	 * @param request an http servlet request
	 * @param name the name of the parameter to retrieve
	 * @param clazz the type to convert the parameter to
	 * @return parameter, converted to appropriate type
	 */
	public static Object getParameterAsType(HttpServletRequest request, String name, Class<?> clazz) {
		String val = request.getParameter(name);
		return convertToType(val, clazz);
	}
	
	/**
	 * Converts a string to specified type
	 * 
	 * @param val the string to convert
	 * @param clazz the type to convert to
	 * @return an instance of the specified type, with it's value set to val
	 */
	public static Object convertToType(String val, Class<?> clazz) {
		if (val == null)
			return null;
		if ("".equals(val) && !String.class.equals(clazz))
			return null;
		if (Location.class.isAssignableFrom(clazz)) {
			LocationEditor ed = new LocationEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (User.class.isAssignableFrom(clazz)) {
			UserEditor ed = new UserEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (Date.class.isAssignableFrom(clazz)) {
			// all HTML Form Entry dates should be submitted as yyyy-mm-dd
			try {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				df.setLenient(false);
				return df.parse(val);
			}
			catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		} else if (Double.class.isAssignableFrom(clazz)) {
			return Double.valueOf(val);
		} else if (Integer.class.isAssignableFrom(clazz)) {
			return Integer.valueOf(val);
		} else if (Concept.class.isAssignableFrom(clazz)) {
			ConceptEditor ed = new ConceptEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (Patient.class.isAssignableFrom(clazz)) {
			PatientEditor ed = new PatientEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (Person.class.isAssignableFrom(clazz)) {
			PersonEditor ed = new PersonEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else if (EncounterType.class.isAssignableFrom(clazz)) {
			EncounterTypeEditor ed = new EncounterTypeEditor();
			ed.setAsText(val);
			return ed.getValue();
		} else {
			return val;
		}
	}
	
	/**
	 * Creaets an OpenMRS Obs instance
	 * 
	 * @param formField the form field that specifies the concept associated with the Obs
	 * @param value value associated with the Obs
	 * @param datetime date/time associated with the Obs (may be null)
	 * @param accessionNumber accession number associatd with the Obs (may be null)
	 * @return the created Obs instance
	 */
	
	public static Obs createObs(FormField formField, Object value, Date datetime, String accessionNumber) {
		Concept concept = formField.getField().getConcept();
		if (concept == null)
			throw new FormEntryException("Can't create an Obs for a formField that doesn't represent a Concept");
		return createObs(concept, value, datetime, accessionNumber);
	}
	
	/**
	 * Creates an OpenMRS Obs instance
	 * 
	 * @param concept concept associated with the Obs
	 * @param value value associated with the Obs
	 * @param datetime date/time associated with the Obs (may be null)
	 * @param accessionNumber accession number associatd with the Obs (may be null)
	 * @return the created Obs instance
	 */
	public static Obs createObs(Concept concept, Object value, Date datetime, String accessionNumber) {
		Obs obs = new Obs();
		obs.setConcept(concept);
		ConceptDatatype dt = obs.getConcept().getDatatype();
		if (dt.isNumeric()) {
			obs.setValueNumeric(Double.parseDouble(value.toString()));
		} else if (HtmlFormEntryConstants.COMPLEX_UUID.equals(dt.getUuid())) {
			obs.setComplexData((ComplexData) value);
			obs.setValueComplex(obs.getComplexData().getTitle());
		} else if (dt.isText()) {
			if (value instanceof Location) {
				Location location = (Location) value;
				obs.setValueText(location.getId().toString() + " - " + location.getName());
			} else if (value instanceof Person) {
				Person person = (Person) value;
				obs.setValueText(person.getId().toString() + " - " + person.getPersonName().toString());
			} else {
				obs.setValueText(value.toString());
			}
		} else if (dt.isCoded()) {
			if (value instanceof Concept)
				obs.setValueCoded((Concept) value);
			else
				obs.setValueCoded((Concept) convertToType(value.toString(), Concept.class));
		} else if (dt.isBoolean()) {
			boolean booleanValue = value != null && !Boolean.FALSE.equals(value) && !"false".equals(value);
			obs.setValueNumeric(booleanValue ? 1.0 : 0.0);
		} else if (ConceptDatatype.DATE.equals(dt.getHl7Abbreviation())
		        || ConceptDatatype.TIME.equals(dt.getHl7Abbreviation())
		        || ConceptDatatype.DATETIME.equals(dt.getHl7Abbreviation())) {
			Date date = (Date) value;
			obs.setValueDatetime(date);
		} else if ("ZZ".equals(dt.getHl7Abbreviation())) {
			// don't set a value
		} else {
			throw new IllegalArgumentException("concept datatype not yet implemented: " + dt.getName()
			        + " with Hl7 Abbreviation: " + dt.getHl7Abbreviation());
		}
		if (datetime != null)
			obs.setObsDatetime(datetime);
		if (accessionNumber != null)
			obs.setAccessionNumber(accessionNumber);
		return obs;
	}
	
	/**
	 * Converts an xml string to a Document object
	 * 
	 * @param xml the xml string to convert
	 * @return the resulting Document object
	 * @throws Exception
	 */
	public static Document stringToDocument(String xml) throws Exception {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(new InputSource(new StringReader(xml)));
			return document;
		}
		catch (Exception e) {
			log.error("Error converting String to Document:\n" + xml);
			throw e;
		}
	}
	
	/**
	 * Converts a Document object to an xml string
	 * 
	 * @param document the Document instance to convert
	 * @return the resulting xml string
	 * @throws Exception
	 */
	public static String documentToString(Document document) throws Exception {
		//set up a transformer
		Transformer trans = null;
		TransformerFactory transfac = TransformerFactory.newInstance();

		try {
			trans = transfac.newTransformer();
		}
		catch (TransformerException te) {
			System.out.println(HtmlFormEntryConstants.ERROR_TRANSFORMER_1 + te);
		}
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, HtmlFormEntryConstants.CONSTANT_YES);
		trans.setOutputProperty(OutputKeys.INDENT, HtmlFormEntryConstants.CONSTANT_YES);
		trans.setOutputProperty(OutputKeys.METHOD, HtmlFormEntryConstants.CONSTANT_XML);
        trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		
		//create string from xml tree
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(document);
		try {
			trans.transform(source, result);
		}
		catch (TransformerException te) {
			System.out.println(HtmlFormEntryConstants.ERROR_TRANSFORMER_2 + te);
		}
		String xmlString = sw.toString();
		
		return xmlString;
	}
	
	/**
	 * Retrieves a child Node by name
	 * 
	 * @param content the parent Node
	 * @param name the name of the child Node
	 * @return the child Node with the specified name
	 */
	public static Node findChild(Node content, String name) {
		if (content == null)
			return null;
		NodeList children = content.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node node = children.item(i);
			if (name.equals(node.getNodeName()))
				return node;
		}
		return null;
	}

    /**
     * Finds the first descendant of this node with the given tag name
     * @param node
     * @param tagName
     * @return
     */
    public static Node findDescendant(Node node, String tagName) {
        if (node == null)
            return null;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (tagName.equals(node.getNodeName()))
                return node;
            Node matchingDescendant = findDescendant(child, tagName);
            if (matchingDescendant != null) {
                return matchingDescendant;
            }
        }
        return null;
    }

    /**
	 * Returns all the attributes associated with a Node
	 * 
	 * @param node the Node to retrieve attributes from
	 * @return a Map containing all the attributes of the Node
	 */
	public static Map<String, String> getNodeAttributes(Node node) {
		Map<String, String> ret = new HashMap<String, String>();
		NamedNodeMap atts = node.getAttributes();
		for (int i = 0; i < atts.getLength(); i++) {
			Node attribute = atts.item(i);
			ret.put(attribute.getNodeName(), attribute.getNodeValue());
		}
		return ret;
	}
	
	/**
	 * Returns a specific attribute of a Node
	 * 
	 * @param node the Node to retrieve the attribute from
	 * @param attributeName the name of the attribute to return
	 * @param defaultVal a default value to return if the attribute is not specified for the
	 *            selected Node
	 * @return
	 */
	public static String getNodeAttribute(Node node, String attributeName, String defaultVal) {
		String ret = getNodeAttributes(node).get(attributeName);
		return (ret == null ? defaultVal : ret);
	}

    /**
     * @param node
     * @return the contents of node as a String
     */
    public static String getNodeContentsAsString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException ex) {
            throw new RuntimeException("Error transforming node", ex);
        }
        return sw.toString();
    }

    /**
	 * Creates a non-persistent "Fake" Person (used when previewing or validating an HTML Form)
	 * 
	 * @return the "fake" person
	 */
	public static Patient getFakePerson() {
		Patient demo = new Patient(12345);
		demo.addName(new PersonName("Demo", "The", "Person"));
		Location l = Context.getLocationService().getAllLocations().iterator().next();
		for (PatientIdentifierType pit : Context.getPatientService().getAllPatientIdentifierTypes()) {
			if (StringUtils.isEmpty(pit.getValidator())) {
				demo.addIdentifier(new PatientIdentifier("Testing" + pit.getName() + "123", pit, l));
			}
		}
		demo.setGender("F");
		demo.setUuid("testing-html-form-entry");
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -31);
			demo.setBirthdate(cal.getTime());
		}
		
		for (PersonAttributeType type : Context.getPersonService().getAllPersonAttributeTypes()) {
			if (type.getFormat() != null && type.getFormat().equals("java.lang.String")) {
				demo.addAttribute(new PersonAttribute(type, "Test " + type.getName() + " Attribute"));
			}
		}
		PersonAddress addr = new PersonAddress();
		addr.setCityVillage("Rwinkwavu");
		addr.setCountyDistrict("Kayonza District");
		addr.setStateProvince("Eastern Province");
		addr.setCountry("Rwanda");
		demo.addAddress(addr);
		return demo;
	}
	
	/**
	 * Combines a Date object that contains only a date component (day, month, year) with a Date
	 * object that contains only a time component (hour, minute, second) into a single Date object
	 * 
	 * @param date the Date object that contains date information
	 * @param time the Date object that contains time information
	 * @return a Date object with the combined date/time
	 */
	public static Date combineDateAndTime(Date date, Date time) {
		if (date == null)
			return null;
		Calendar cal = Calendar.getInstance();
		
		cal.setTime(date);
		if (time != null) {
			Calendar temp = Calendar.getInstance();
			temp.setTime(time);
			cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
			cal.set(Calendar.SECOND, temp.get(Calendar.SECOND));
			cal.set(Calendar.MILLISECOND, temp.get(Calendar.MILLISECOND));
		}
		return cal.getTime();
	}
	
	/***
	 * Get the concept by id, the id can either be 1)an integer id like 5090 or 2)mapping type id
	 * like "XYZ:HT" or 3)uuid like "a3e12268-74bf-11df-9768-17cfc9833272"
	 * 
	 * @param id
	 * @return the concept if exist, else null
	 * @should find a concept by its conceptId
	 * @should find a concept by its mapping
	 * @should find a concept by its uuid
	 * @should return null otherwise
	 * @should find a concept by its mapping with a space in between
	 */
	public static Concept getConcept(String id) {
		Concept cpt = null;
		
		if (id != null) {
			
			// see if this is a parseable int; if so, try looking up concept by id
			try { //handle integer: id
				int conceptId = Integer.parseInt(id);
				cpt = Context.getConceptService().getConcept(conceptId);
				
				if (cpt != null) {
					return cpt;
				}
			}
			catch (Exception ex) {
				//do nothing 
			}
			
			// handle  mapping id: xyz:ht
			int index = id.indexOf(":");
			if (index != -1) {
				String mappingCode = id.substring(0, index).trim();
				String conceptCode = id.substring(index + 1, id.length()).trim();
				cpt = Context.getConceptService().getConceptByMapping(conceptCode, mappingCode);
				
				if (cpt != null) {
					return cpt;
				}
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if the id matches a uuid format
			if (isValidUuidFormat(id)) {
				cpt = Context.getConceptService().getConceptByUuid(id);
			}
		}
		
		return cpt;
	}
	
	/**
	 * Gets a concept by id, mapping, or uuid. (See #getConcept(String) for precise details.) If no
	 * concept is found, throws an IllegalArgumentException with the given message.
	 * 
	 * @param id
	 * @param errorMessageIfNotFound
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static Concept getConcept(String id, String errorMessageIfNotFound) throws IllegalArgumentException {
		Concept c = null;
		try {
			c = getConcept(id);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(errorMessageIfNotFound, ex);
		}
		if (c == null)
			throw new IllegalArgumentException(errorMessageIfNotFound);
		return c;
	}
	
	/**
     * This method doesn't support "SessionAttribute:", but is otherwise like the similarly-named method.
	 * @see #getLocation(String, FormEntryContext)
	 */
	public static Location getLocation(String id) {
        return getLocation(id, null);
    }

    /**
     * Get the location by:
     * <ol>
     *     <li>an integer id like 5090</li>
     *     <li>a uuid like "a3e12268-74bf-11df-9768-17cfc9833272"</li>
     *     <li>a name like "Boston"</li>
     *     <li>an id/name pair like "501 - Boston" (this format is used when saving a location on a obs as a value text)</li>
     *     <li>"GlobalProperty:property.name"</li>
     *     <li>"UserProperty:propertyName"</li>
     *     <li>"SessionAttribute:attributeName"</li>
     * </ol>
     *
     *
     * @param id
     * @param context
     * @return the location if exist, else null
     * @should find a location by its locationId
     * @should find a location by name
     * @should find a location by its uuid
     * @should find a location by global property
     * @should find a location by user property
     * @should find a location by session attribute
     * @should not fail if trying to find a location by session attribute and we have no session
     * @should return null otherwise
     */
	public static Location getLocation(String id, FormEntryContext context) {

		Location location = null;
		
		if (id != null) {
			
			// handle GlobalProperty:property.name
			if (id.startsWith("GlobalProperty:")) {
				String gpName = id.substring("GlobalProperty:".length());
				String gpValue = Context.getAdministrationService().getGlobalProperty(gpName);
				if (StringUtils.isNotEmpty(gpValue)) {
					return getLocation(gpValue, context);
				}
			}
			
			// handle UserProperty:propName
			if (id.startsWith("UserProperty:")) {
				String upName = id.substring("UserProperty:".length());
				String upValue = Context.getAuthenticatedUser().getUserProperty(upName);
				if (StringUtils.isNotEmpty(upValue)) {
					return getLocation(upValue, context);
				}
			}

            // handle SessionAttribute:attributeName
            if (id.startsWith("SessionAttribute:")) {
                if (context.getHttpSession() == null) {
                    // if we don't have access to a session, e.g. when validating a form, we can't do anything
                    return null;
                }
                String saName = id.substring("SessionAttribute:".length());
                Object saValue = context.getHttpSession().getAttribute(saName);
                if (saValue == null) {
                    return null;
                } else if (saValue instanceof Location) {
                    return (Location) saValue;
                } else if (saValue instanceof String) {
                    return getLocation((String) saValue, context);
                } else {
                    return getLocation(saValue.toString(), context);
                }
            }

            // see if this is parseable int; if so, try looking up by id
			try { //handle integer: id
				int locationId = Integer.parseInt(id);
				location = Context.getLocationService().getLocation(locationId);
				
				if (location != null) {
					return location;
				}
			}
			catch (Exception ex) {
				//do nothing 
			}
			
			// handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272" if id matches a uuid format
			if (isValidUuidFormat(id)) {
				location = Context.getLocationService().getLocationByUuid(id);
				
				if (location != null) {
					return location;
				}
			}
			
			// if it's neither a uuid or id, try location name
			location = Context.getLocationService().getLocation(id);
			
			if (location != null) {
				return location;
			}
			
			// try the "101 - Cange" case
			if (id.contains(" ")) {
				String[] values = id.split(" ");
				try {
					int locationId = Integer.parseInt(values[0]);
					location = Context.getLocationService().getLocation(locationId);
					
					if (location != null) {
						return location;
					}
				}
				catch (Exception ex) {
					//do nothing 
				}
			}
		}
		
		// no match found, so return null
		return null;
	}
	
	/***
	 * Get the program by: 1)an integer id like 5090 or 2) uuid like
	 * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) name of *associated concept* (not name of
	 * program), like "MDR-TB Program"
	 * 
	 * @param id
	 * @return the program if exist, else null
	 * @should find a program by its id
	 * @should find a program by name of associated concept
	 * @should find a program by its uuid
	 * @should return null otherwise
	 */
	public static Program getProgram(String id) {
		
		Program program = null;
		
		if (id != null) {

			// see if this is parseable int; if so, try looking up by id
			try {//handle integer: id
				int programId = Integer.parseInt(id);
				program = Context.getProgramWorkflowService().getProgram(programId);
				
				if (program != null) {
					return program;
				}
			}
			catch (Exception ex) {
				//do nothing 
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid format
			if (isValidUuidFormat(id)) {
				program = Context.getProgramWorkflowService().getProgramByUuid(id);
				
				if (program != null) {
					return program;
				}
			} else {
				// if it's neither a uuid or id, try program name
				// (note that this API method actually checks based on name of the associated concept, not the name of the program itself)
				program = Context.getProgramWorkflowService().getProgramByName(id);
			}
			
		}
		return program;
	}
	
	/***
	 * Get the person by: 1)an integer id like 5090 or 2) uuid like
	 * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) a username like "mgoodrich" or 4) an id/name
	 * pair like "5090 - Bob Jones" (this format is used when saving a person on a obs as a value
	 * text)
	 * 
	 * @param id
	 * @return the person if exist, else null
	 * @should find a person by its id
	 * @should find a person by its uuid
	 * @should find a person by username of corresponding user
	 * @should return null otherwise
	 */
	public static Person getPerson(String id) {
		
		Person person = null;
		
		if (id != null) {
			
			// see if this is parseable int; if so, try looking up by id
			try { //handle integer: id
				int personId = Integer.parseInt(id);
				person = Context.getPersonService().getPerson(personId);
				
				if (person != null) {
					return person;
				}
			}
			catch (Exception ex) {
				//do nothing 
			}
			
			// handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid format
			if (isValidUuidFormat(id)) {
				person = Context.getPersonService().getPersonByUuid(id);
				
				if (person != null) {
					return person;
				}
			}
			
			// handle username
			User personByUsername = Context.getUserService().getUserByUsername(id);
			if (personByUsername != null) {
				return personByUsername.getPerson();
			}
			
			// try the "5090 - Bob Jones" case
			if (id.contains(" ")) {
				String[] values = id.split(" ");
				try {
					int personId = Integer.parseInt(values[0]);
					person = Context.getPersonService().getPerson(personId);
					
					if (person != null) {
						return person;
					}
				}
				catch (Exception ex) {
					//do nothing 
				}
			}
		}
		
		// no match found, so return null
		return null;
	}
	
	/***
	 * Get the patient identifier type by: 1)an integer id like 5090 or 2) uuid like
	 * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) a name like "Temporary Identifier"
	 * 
	 * @param id
	 * @return the identifier type if exist, else null
	 * @should find an identifier type by its id
	 * @should find an identifier type by its uuid
	 * @should find an identifier type by its name
	 * @should return null otherwise
	 */
	public static PatientIdentifierType getPatientIdentifierType(String id) {
		PatientIdentifierType identifierType = null;
		
		if (id != null) {
			
			// see if this is parseable int; if so, try looking up by id
			try { //handle integer: id
				int identifierTypeId = Integer.parseInt(id);
				identifierType = Context.getPatientService().getPatientIdentifierType(identifierTypeId);
				
				if (identifierType != null) {
					return identifierType;
				}
			}
			catch (Exception ex) {
				//do nothing 
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid format
			if (isValidUuidFormat(id)) {
				identifierType = Context.getPatientService().getPatientIdentifierTypeByUuid(id);
				
				if (identifierType != null) {
					return identifierType;
				}
			}
			// handle name
			else {
				// if it's neither a uuid or id, try identifier type name
				identifierType = Context.getPatientService().getPatientIdentifierTypeByName(id);
			}
		}
		return identifierType;
	}
	
	/**
	 * Looks up a {@link ProgramWorkflow} by id, uuid or by concept map of the underlying concept
	 */
	@SuppressWarnings("deprecation")
	public static ProgramWorkflow getWorkflow(String identifier) {
		ProgramWorkflow workflow = null;
		
		if (identifier != null) {
			// first try to fetch by id
			try {
				Integer id = Integer.valueOf(identifier);
				workflow = Context.getProgramWorkflowService().getWorkflow(id);
				
				if (workflow != null) {
					return workflow;
				}
			}
			catch (NumberFormatException e) {}
			
			// if not, try to fetch by uuid
			if (isValidUuidFormat(identifier)) {
				workflow = Context.getProgramWorkflowService().getWorkflowByUuid(identifier);
				
				if (workflow != null) {
					return workflow;
				}
			}
			
			// finally, try to fetch by concept map
			// handle  mapping id: xyz:ht
			int index = identifier.indexOf(":");
			if (index != -1) {
				Concept concept = getConcept(identifier);
				
				// iterate through workflows until we see if we find a match
				if (concept != null) {
					for (Program program : Context.getProgramWorkflowService().getAllPrograms(false)) {
						for (ProgramWorkflow w : program.getAllWorkflows()) {
							if (w.getConcept().equals(concept)) {
								return w;
							}
						}
					}
				}
			}
		}
		
		return workflow;
	}
	
	/**
	 * Looks up a {@link ProgramWorkflowState} from the specified program by programWorkflowStateId,
	 * uuid, or by a concept map to the the underlying concept (Note that if there are multiple
	 * states associated with the same concept in the program, this method will return an arbitrary
	 * one if fetched by concept mapping)
	 * 
	 * @param identifier the programWorkflowStateId, uuid or the concept name to match against
	 * @param program
	 * @return
	 * @should return the state with the matching id
	 * @should return the state with the matching uuid
	 * @should return the state associated with a concept that matches the passed concept map
	 */
	public static ProgramWorkflowState getState(String identifier, Program program) {
		if (identifier == null) {
			return null;
		}
		
		// first try to fetch by id or uuid
		ProgramWorkflowState state = getState(identifier);
		
		if (state != null) {
			return state;
		}
		// if we didn't find a match, see if this is a concept mapping
		else {
			int index = identifier.indexOf(":");
			if (index != -1) {
				Concept concept = getConcept(identifier);
				if (concept != null) {
					for (ProgramWorkflow workflow : program.getAllWorkflows()) {
						for (ProgramWorkflowState s : workflow.getStates(false)) {
							if (s.getConcept().equals(concept)) {
								return s;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Looks up a {@link ProgramWorkflowState} from the specified workflow by
	 * programWorkflowStateId, uuid, or by a concept map to the the underlying concept (Note that if
	 * there are multiple states associated with the same concept in the workflow, this method will
	 * return an arbitrary one if fetched by concept mapping)
	 * 
	 * @param identifier the programWorkflowStateId, uuid or the concept name to match against
	 * @param workflow
	 * @return
	 * @should return the state with the matching id
	 * @should return the state with the matching uuid
	 * @should return the state associated with a concept that matches the passed concept map
	 */
	public static ProgramWorkflowState getState(String identifier, ProgramWorkflow workflow) {
		if (identifier == null) {
			return null;
		}
		
		// first try to fetch by id or uuid
		ProgramWorkflowState state = getState(identifier);
		
		if (state != null && state.getProgramWorkflow().equals(workflow)) {
			return state;
		}
		
		// if we didn't find a match, see if this is a concept mapping
		else {
			int index = identifier.indexOf(":");
			if (index != -1) {
				Concept concept = getConcept(identifier);
				if (concept != null) {
					for (ProgramWorkflowState s : workflow.getStates(false)) {
						if (s.getConcept().equals(concept)) {
							return s;
						}
					}
				}
			}
		}
		return null;
	}

    /**
     * Fetches a location tag by name or id
     * (Will add support for uuid once we stop supporting OpenMRS 1.6, which doesn't a uuid on location tag)
     *
     * @param identifier
     * @return
     */
    public static LocationTag getLocationTag(String identifier) {

        LocationTag tag = null;

        if (identifier != null) {
            // first try to fetch by id
            try {
                Integer id = Integer.valueOf(identifier);
                tag = Context.getLocationService().getLocationTag(id);

                if (tag != null) {
                    return tag;
                }
            }
            catch (NumberFormatException e) {}

            // if not, try to fetch by name
            tag = Context.getLocationService().getLocationTagByName(identifier);

            if (tag != null) {
                return tag;
            }

           // TODO add ability to fetch by uuid once we are no longer worried about being compatible with OpenMRS 1.6 (since getLocationTagByUuid is not available in 1.6)

        }

        return null;
    }

	/**
	 * Looks up a {@link ProgramWorkflowState} from the specified workflow by
	 * programWorkflowStateId, or uuid
	 * 
	 * @param identifier the programWorkflowStateId or uuid to match against
	 * @param
	 * @return
	 * @should return the state with the matching id
	 * @should return the state with the matching uuid
	 */
	@SuppressWarnings("deprecation")
	public static ProgramWorkflowState getState(String identifier) {
		ProgramWorkflowState state = null;
		
		if (identifier != null) {
			try {
				Integer id = Integer.valueOf(identifier);
				state = Context.getProgramWorkflowService().getState(id);
				
				if (state != null) {
					return state;
				}
			}
			catch (NumberFormatException e) {}
			
			if (isValidUuidFormat(identifier)) {
				state = Context.getProgramWorkflowService().getStateByUuid(identifier);
				
				if (state != null) {
					return state;
				}
			}
		}
		return null;
	}

	/***
	 * Determines if the passed string is in valid uuid format By OpenMRS standards, a uuid must be
	 * 36 characters in length and not contain whitespace, but we do not enforce that a uuid be in
	 * the "canonical" form, with alphanumerics seperated by dashes, since the MVP dictionary does
	 * not use this format (We also are being slightly lenient and accepting uuids that are 37 or 38
	 * characters in length, since the uuid data field is 38 characters long)
	 */
	public static boolean isValidUuidFormat(String uuid) {
		if (uuid.length() < 36 || uuid.length() > 38 || uuid.contains(" ")) {
			return false;
		}
		
		return true;
	}
	
	public static List<PatientIdentifierType> getPatientIdentifierTypes() {
		return Context.getPatientService().getAllPatientIdentifierTypes();
	}
	
	/**
	 * Utility method to void an encounter. If the voidEncounterByHtmlFormSchema global property has
	 * been set to true, OR if the Html Form Flowsheet module has been started AND the
	 * voidEncounterByHtmlFormSchema global property has not been explicitly set to false, void the
	 * encounter via the special voidEncounterByHtmlFormSchema algorithm. Otherwise simply void the
	 * encounter normally. This test is done because when using the Html Form Flowsheet module we
	 * only want to void observations associated with the current form, not the entire encounter
	 */
	public static void voidEncounter(Encounter e, HtmlForm htmlform, String voidReason) throws Exception {
		if (voidReason == null) {
			voidReason = "htmlformentry";
		}
		
		if (HtmlFormEntryGlobalProperties.VOID_ENCOUNTER_BY_HTML_FORM_SCHEMA() != null) {
			
			if (HtmlFormEntryGlobalProperties.VOID_ENCOUNTER_BY_HTML_FORM_SCHEMA() == true) {
				voidEncounterByHtmlFormSchema(e, htmlform, voidReason);
			} else {
				Context.getEncounterService().voidEncounter(e, voidReason);
			}
			
		} else if (HtmlFormEntryGlobalProperties.HTML_FORM_FLOWSHEET_STARTED() != null
		        && HtmlFormEntryGlobalProperties.HTML_FORM_FLOWSHEET_STARTED() == true) {
			voidEncounterByHtmlFormSchema(e, htmlform, voidReason);
		} else {
			Context.getEncounterService().voidEncounter(e, voidReason);
		}
	}
	
	/**
	 * Utility method that sets all matched obs and orders to voided, and voids encounter if all obs
	 * and orders in encounter are voided. Does not call save, just updates the voided fields on all
	 * objects in encounter Uses a 'dummy' FormEntrySession to use htmlformentry schema matching
	 * mechanism, and then examines the leftover Obs, Orders from the FormEntrySession constructor
	 * 
	 * @param session
	 */
	public static void voidEncounterByHtmlFormSchema(Encounter e, HtmlForm htmlform, String voidReason) throws Exception {
		if (e != null && htmlform != null) {
			if (voidReason == null)
				voidReason = "htmlformentry";
			boolean shouldVoidEncounter = true;
			Map<Obs, Obs> replacementObs = new HashMap<Obs, Obs>();//new, then source
			Map<Order, Order> replacementOrders = new HashMap<Order, Order>();//new, then source
			Encounter eTmp = returnEncounterCopy(e, replacementObs, replacementOrders);
			FormEntrySession session = new FormEntrySession(eTmp.getPatient(), eTmp, Mode.VIEW, htmlform, null); // session gets a null HttpSession
            session.getHtmlToDisplay();
			List<FormSubmissionControllerAction> actions = session.getSubmissionController().getActions();
			Set<Obs> matchedObs = new HashSet<Obs>();
			Set<Order> matchedOrders = new HashSet<Order>();
			for (FormSubmissionControllerAction lfca : actions) {
				if (lfca instanceof ObsSubmissionElement) {
					ObsSubmissionElement ose = (ObsSubmissionElement) lfca;
					if (ose.getExistingObs() != null) {
						matchedObs.add(ose.getExistingObs());
					}
				}
				if (lfca instanceof ObsGroupAction) {
					ObsGroupAction oga = (ObsGroupAction) lfca;
					if (oga.getExistingGroup() != null) {
						matchedObs.add(oga.getExistingGroup());
					}
				}
				if (lfca instanceof DrugOrderSubmissionElement) {
					DrugOrderSubmissionElement dse = (DrugOrderSubmissionElement) lfca;
					if (dse.getExistingOrder() != null) {
						matchedOrders.add(dse.getExistingOrder());
					}
				}
			}
			
			for (Obs o : e.getAllObs(false)) { //ignore voided obs
				boolean matched = false;
				for (Obs oMatched : matchedObs) {
					if (replacementObs.get(oMatched) != null && replacementObs.get(oMatched).equals(o)) {
						o.setVoided(true);
						o.setVoidedBy(Context.getAuthenticatedUser());
						o.setVoidReason(voidReason);
						o.setDateVoided(new Date());
						matched = true;
						break;
					}
				}
				if (!matched)
					shouldVoidEncounter = false;
			}
			
			for (Order o : e.getOrders()) {
				if (!o.isVoided()) { //ignore voided orders
					boolean matched = false;
					for (Order oMatched : matchedOrders) {
						//Order.equals only checks Id value
						if (replacementOrders.get(oMatched) != null && replacementOrders.get(oMatched).equals(o)
						        && !o.isVoided()) {
							o.setVoided(true);
							o.setVoidedBy(Context.getAuthenticatedUser());
							o.setVoidReason(voidReason);
							o.setDateVoided(new Date());
							matched = true;
							break;
						}
					}
					if (!matched)
						shouldVoidEncounter = false;
				}
			}
			
			if (shouldVoidEncounter) {
				e.setVoided(true);
				e.setVoidedBy(Context.getAuthenticatedUser());
				e.setVoidReason(voidReason);
				e.setDateVoided(new Date());
			}
			eTmp = null;
		}
	}
	
	/**
	 * Method that returns a copy of an Encounter. Includes copies of all Obs tree structures and
	 * Orders.
	 * 
	 * @param source
	 * @param replacementObs
	 * @param replacementOrders
	 * @return
	 * @throws Exception
	 */
	private static Encounter returnEncounterCopy(Encounter source, Map<Obs, Obs> replacementObs,
	                                             Map<Order, Order> replacementOrders) throws Exception {
		if (source != null) {
			Encounter encNew = (Encounter) returnCopy(source);
			
			//note: we can do this because we're not going to manipulate anything about these obs or orders, and this copy won't be persisted...
			
			Set<Obs> newObs = new HashSet<Obs>();
			for (Obs o : source.getAllObs(true)) {
				Obs oNew = returnObsCopy(o, replacementObs);
				newObs.add(oNew);
			}
			encNew.setObs(newObs);
			
			Set<Order> newOrders = new HashSet<Order>();
			for (Order o : source.getOrders()) {
				Order oNew = (Order) returnOrderCopy(o, replacementOrders);
				newOrders.add(oNew);
			}
			encNew.setOrders(newOrders);
			return encNew;
		}
		return null;
	}
	
	/**
	 * Returns a copy of an Obs. Recurses through GroupMembers to return copies of those also, so
	 * the whole Obs tree is a copy.
	 * 
	 * @param obsToCopy
	 * @param replacements
	 * @return
	 * @throws Exception
	 */
	private static Obs returnObsCopy(Obs obsToCopy, Map<Obs, Obs> replacements) throws Exception {
		Obs newObs = (Obs) returnCopy(obsToCopy);
		
		if (obsToCopy.isObsGrouping()) {
			newObs.setGroupMembers(null);
			for (Obs oinner : obsToCopy.getGroupMembers()) {
				Obs oinnerNew = returnObsCopy(oinner, replacements);
				newObs.addGroupMember(oinnerNew);
			}
		}
		
		replacements.put(newObs, obsToCopy);
		return newObs;
	}
	
	/**
	 * Utility to return a copy of an Order. Uses reflection so that this code will support any
	 * subclassing of Order, such as DrugOrder
	 * 
	 * @param source
	 * @param replacementOrders
	 * @return A copy of an Order
	 * @throws Exception
	 */
	private static Object returnOrderCopy(Order source, Map<Order, Order> replacementOrders) throws Exception {
		Object ret = returnCopy(source);
		replacementOrders.put((Order) ret, (Order) source);
		return ret;
	}
	
	/**
	 * Utility to return a copy of an Object. Copies all properties that are referencese by getters
	 * and setters and *are not* collection
	 * 
	 * @param source
	 * @param replacements
	 * @return A copy of an object
	 * @throws Exception
	 */
	private static Object returnCopy(Object source) throws Exception {
		Class<? extends Object> clazz = source.getClass();
		Object ret = clazz.newInstance();
		Set<String> fieldNames = new HashSet<String>();
		List<Field> fields = new ArrayList<Field>();
		addSuperclassFields(fields, clazz);
		for (Field f : fields) {
			fieldNames.add(f.getName());
		}
		for (String root : fieldNames) {
			for (Method getter : clazz.getMethods()) {
				if (getter.getName().toUpperCase().equals("GET" + root.toUpperCase())
				        && getter.getParameterTypes().length == 0) {
					Method setter = getSetter(clazz, getter, "SET" + root.toUpperCase());
					//NOTE: Collection properties are not copied
					if (setter != null && methodsSupportSameArgs(getter, setter)
					        && !(getter.getReturnType().isInstance(Collection.class))) {
						Object o = getter.invoke(source, Collections.EMPTY_LIST.toArray());
						if (o != null) {
							setter.invoke(ret, o);
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * The Encounter.setProvider() contains the different overloaded methods and this filters the
	 * correct setter from those
	 * 
	 * @param clazz
	 * @param getter
	 * @param methodname
	 * @return
	 */
	private static Method getSetter(Class<? extends Object> clazz, Method getter, String methodname) {
		
		List<Method> setterMethods = getMethodCaseInsensitive(clazz, methodname);
		if (setterMethods != null && !setterMethods.isEmpty()) {
			if (setterMethods.size() == 1) {
				return setterMethods.get(0);
			} else if (setterMethods.size() > 1) {
				for (Method m : setterMethods) {
					Class<?>[] parameters = m.getParameterTypes();
					for (Class<?> parameter : parameters) {
						if (getter.getReturnType().equals(parameter)) {
							return m;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Performs a case insensitive search on a class for a method by name.
	 * 
	 * @param clazz
	 * @param methodName
	 * @return the found Method
	 */
	private static List<Method> getMethodCaseInsensitive(Class<? extends Object> clazz, String methodName) {
		
		List<Method> methodList = new ArrayList<Method>();
		for (Method m : clazz.getMethods()) {
			if (m.getName().toUpperCase().equals(methodName.toUpperCase())) {
				methodList.add(m);
				
			}
		}
		return methodList;
	}
	
	/**
	 * compares getter return types to setter parameter types
	 * 
	 * @param getter
	 * @param setter
	 * @return true if getter return types are the same as setter parameter types. Else false.
	 */
	private static boolean methodsSupportSameArgs(Method getter, Method setter) {
		if (getter != null && setter != null && setter.getParameterTypes() != null && setter.getParameterTypes().length == 1
		        && getter.getReturnType() != null && getter.getReturnType().equals(setter.getParameterTypes()[0]))
			return true;
		return false;
	}
	
	/**
	 * recurses through all superclasses of a class and adds the fields from that superclass
	 * 
	 * @param fields
	 * @param clazz
	 */
	private static void addSuperclassFields(List<Field> fields, Class<? extends Object> clazz) {
		fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
		if (clazz.getSuperclass() != null) {
			addSuperclassFields(fields, clazz.getSuperclass());
		}
	}
	
	/**
	 * Translates a String into a Date.
	 * 
	 * @param value use "now" for the current timestamp, "today" for the current date with a
	 *            timestamp of 00:00, or a date string that can be parsed by SimpleDateFormat with
	 *            the format parameter.
	 * @param format the pattern SimpleDateTime will use to parse the value, if other than "now" or
	 *            "today".
	 * @return Date on success; null for an invalid value
	 * @throws IllegalArgumentException if a date string cannot be parsed with the format string you
	 *             provided
	 * @see java.text.SimpleDateFormat
	 * @should return a Date object with current date and time for "now"
	 * @shold return a Date with current date, but time of 00:00:00:00, for "today"
	 * @should return a Date object matching the value param if a format is specified
	 * @should return null for null value
	 * @should return null if format is null and value not in [ null, "now", "today" ]
	 * @should fail if date parsing fails
	 */
	public static Date translateDatetimeParam(String value, String format) {
		if (value == null)
			return null;
		if ("now".equals(value)) {
			return new Date();
		} else if ("today".equals(value)) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} else if (format != null) { // the value is a timestamp; parse it with
			                         // the format pattern
			try {
				SimpleDateFormat df = new SimpleDateFormat(format);
				return df.parse(value);
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
		return null;
	}

    /**
     * Given a list of patient programs and a program, returns the first patient program
     * that matches the specified program
     *
     * @param patientPrograms
     * @param program
     * @return
     */
    public static PatientProgram getPatientProgramByProgram(List<PatientProgram> patientPrograms, Program program) {

        for (PatientProgram patientProgram : patientPrograms) {
            if (patientProgram.getProgram().equals(program)) {
                return patientProgram;
            }
        }

        return null;
    }

    /**
     * Given a patient and a program workflow, returns the first patient program that contains
     * a state in the specified workflow
     *
     * @param patient
     * @param workflow
     * @return
     */
	public static PatientProgram getPatientProgramByWorkflow(Patient patient, ProgramWorkflow workflow) {
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient,
		    workflow.getProgram(), null, null, null, null, false);
		
		PatientProgram patientProgram = null;
		
		for (PatientProgram eachPatientProgram : patientPrograms) {
			boolean foundState = false;
			for (PatientState patientState : eachPatientProgram.getStates()) {
				if (patientState.getState().getProgramWorkflow().equals(workflow)) {
					foundState = true;
				}
			}
			
			if (foundState) {
				if (patientProgram != null) {
					throw new IllegalStateException("Does not support multiple programs");
				} else {
					patientProgram = eachPatientProgram;
				}
			}
		}
		
		return patientProgram;
	}

    /**
     * If the specified patient is enrolled in the specified program on the specified date,
     * return the associated patient program, otherwise return null
     *
     * @param patient
     * @param program
     * @param date
     * @return
     */
    public static PatientProgram getPatientProgramByProgramOnDate(Patient patient, Program program, Date date) {

        List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient, program, null, date, date, null, false);

        if (patientPrograms.size() > 1) {
            throw new APIException("Simultaneous program enrollments in same program not supported");
        }

        if (patientPrograms.size() == 1) {
            return patientPrograms.get(0);
        }
        else {
            return null;
        }

    }

	/**
	 * Checks whether the encounter has a provider specified (including ugly reflection code for
	 * 1.9+)
	 * 
	 * @param e
	 * @return whether e has one or more providers specified
	 */
	@SuppressWarnings("rawtypes")
	public static boolean hasProvider(Encounter e) {
		try {
			Method method = e.getClass().getMethod("getProvidersByRoles");
			// this is a Map<EncounterRole, Set<Provider>>
			Map providersByRoles = (Map) method.invoke(e);
			return providersByRoles != null && providersByRoles.size() > 0;
		}
		catch (Exception ex) {
			return e.getProvider() != null;
		}
	}
	
	/**
	 * Checks if the user is enrolled in a program at the specified date
	 * 
	 * @param patient the patient that should be enrolled in the program
	 * @param program the program the patient be enrolled in
	 * @param date the date at which to check
	 * @return
	 * @should return true if the patient is enrolled in the program at the specified date
	 * @should return false if the patient is not enrolled in the program
	 * @should return false if the program was completed
	 * @should return false if the date is before the existing patient program enrollment date
	 */
	public static boolean isEnrolledInProgramOnDate(Patient patient, Program program, Date date) {
		if (patient == null)
			throw new IllegalArgumentException("patient should not be null");
		if (program == null)
			throw new IllegalArgumentException("program should not be null");
		if (date == null)
			throw new IllegalArgumentException("date should not be null");
		
		if (patient.getPatientId() == null)
			return false;
		
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient, program,
		    null, date, date, null, false);
		
		return (patientPrograms.size() > 0);
		
	}
	
	/**
	 * Checks to see if the patient has a program enrollment in the specified program after the
	 * given date If multiple patient programs, returns the earliest enrollment If no enrollments,
	 * returns null
	 */
	public static PatientProgram getClosestFutureProgramEnrollment(Patient patient, Program program, Date date) {
		if (patient == null)
			throw new IllegalArgumentException("patient should not be null");
		if (program == null)
			throw new IllegalArgumentException("program should not be null");
		if (date == null)
			throw new IllegalArgumentException("date should not be null");
		
		if (patient.getPatientId() == null)
			return null;
		
		PatientProgram closestProgram = null;
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient, program,
		    date, null, null, null, false);
		
		for (PatientProgram pp : patientPrograms) {
			if ((closestProgram == null || pp.getDateEnrolled().before(closestProgram.getDateEnrolled()))
			        && pp.getDateEnrolled().after(date)) {
				closestProgram = pp;
			}
			
		}
		
		return closestProgram;
	}
	
	/**
	 * Given a Date object, returns a Date object for the same date but with the time component
	 * (hours, minutes, seconds & milliseconds) removed
	 */
	public static Date clearTimeComponent(Date date) {
		// Get Calendar object set to the date and time of the given Date object  
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		// Set time fields to zero  
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		return cal.getTime();
	}
	
	/**
	 * @param s the string to conver to camelcase
	 * @return should return the passed in string in the camelcase format
	 */
	public static String toCamelCase(String s) {
		StringBuffer sb = new StringBuffer();
		String[] words = s.replaceAll("[^A-Za-z]", " ").replaceAll("\\s+", " ").trim().split(" ");
		
		for (int i = 0; i < words.length; i++) {
			if (i == 0)
				words[i] = words[i].toLowerCase();
			else
				words[i] = String.valueOf(words[i].charAt(0)).toUpperCase() + words[i].substring(1);
			
			sb.append(words[i]);
		}
		return sb.toString();
	}
	
	/***
	 * Get the encountger type by: 1)an integer id like 1 or 2) uuid like
	 * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) encounter type name like "AdultInitial".
	 * 
	 * @param Id
	 * @return the encounter type if exist, else null
	 * @should find a encounter type by its encounterTypeId
	 * @should find a encounter type by name
	 * @should find a encounter type by its uuid
	 * @should return null otherwise
	 */
	public static EncounterType getEncounterType(String id) {
		
		EncounterType encounterType = null;
		
		if (StringUtils.isNotBlank(id)) {
			id = id.trim();
			// see if this is parseable int; if so, try looking up by id
			try {
				int encounterTypeId = Integer.parseInt(id);
				encounterType = Context.getEncounterService().getEncounterType(encounterTypeId);
				
				if (encounterType != null)
					return encounterType;
			}
			catch (Exception ex) {
				//do nothing
			}
			
			// handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272" if id matches a uuid format
			if (isValidUuidFormat(id)) {
				encounterType = Context.getEncounterService().getEncounterTypeByUuid(id);
				
				if (encounterType != null)
					return encounterType;
			}
			
			// if it's neither a uuid or id, try encounter type name
			encounterType = Context.getEncounterService().getEncounterType(id);
			
			if (encounterType != null)
				return encounterType;
		}
		
		// no match found
		return null;
	}

	/**
	 * Removes any Obs that are empty or which have only empty children
	 */
	public static void removeEmptyObs(Collection<Obs> obsList) {
		if (obsList != null) {
			Set<Obs> obsToRemove = new HashSet<Obs>();
			for (Obs o : obsList) {
				removeEmptyObs(o.getGroupMembers());
				boolean valueEmpty = StringUtils.isEmpty(o.getValueAsString(Context.getLocale()));
				boolean membersEmpty = o.getGroupMembers() == null || o.getGroupMembers().isEmpty();
				if (valueEmpty && membersEmpty) {
					obsToRemove.add(o);
				}
			}
			for (Obs o : obsToRemove) {
				if (o.getObsGroup() != null) {
					o.getObsGroup().removeGroupMember(o);
					o.setObsGroup(null);
				}
				if (o.getEncounter() != null) {
					o.getEncounter().removeObs(o);
					o.setEncounter(null);
				}
				obsList.remove(o);
			}
		}
	}


    /**
     * Formats a piece of Metadata for Display
     *
     * @param md
     * @param locale
     * @return
     */
    public static String format(OpenmrsMetadata md) {
        return format(md, Context.getLocale());
    }

    /**
     * Formats a piece of Metadata for Display
     *
     * TODO This was copied from UiFramework--we probably should come up with a way to inject a formatter directly into HFE
     *
     * @param md
     * @param locale
     * @return
     */
    public static String format(OpenmrsMetadata md, Locale locale) {
        String override = getLocalization(locale, md.getClass().getSimpleName(), md.getUuid());
        return override != null ? override : md.getName();
    }

    /**
     * See if there is a custom name for this message in the messages.properties files
     *
     * TODO This was copied from UiFramework--we probably should come up with a way to inject a formatter directly into HFE
     *
     * @param md
     * @param locale
     * @return
     */
    private static String getLocalization(Locale locale, String shortClassName, String uuid) {

        // in case this is a hibernate proxy, strip off anything after an underscore
        // ie: EncounterType_$$_javassist_26 needs to be converted to EncounterType
        int underscoreIndex = shortClassName.indexOf("_$");
        if (underscoreIndex > 0) {
            shortClassName = shortClassName.substring(0, underscoreIndex);
        }

        String code = "ui.i18n." + shortClassName + ".name." + uuid;
        String localization = Context.getService(MessageSourceService.class).getMessage(code, null, locale);
        if (localization == null || localization.equals(code)) {
            return null;
        } else {
            return localization;
        }
    }
}
