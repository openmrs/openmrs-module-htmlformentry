package org.openmrs.module.htmlformentry;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Program;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.PatientEditor;
import org.openmrs.propertyeditor.PersonEditor;
import org.openmrs.propertyeditor.UserEditor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** 
 * HTML Form Entry utility methods
 */
public class HtmlFormEntryUtil {
	
	private static Log log = LogFactory.getLog(HtmlFormEntryUtil.class);
    
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
            try {
                DateFormat df = Context.getDateFormat();
                df.setLenient(false);
                return df.parse(val);
            } catch (ParseException e) {
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
        } else if (dt.isText()) {
            if (value instanceof Location) {
                obs.setValueText(((Location) value).getLocationId().toString());
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
        } else if (ConceptDatatype.DATE.equals(dt.getHl7Abbreviation()) ||
        		ConceptDatatype.TIME.equals(dt.getHl7Abbreviation()) ||
        		ConceptDatatype.DATETIME.equals(dt.getHl7Abbreviation())) {
            Date date = (Date) value;
            obs.setValueDatetime(date);
        } else if ("ZZ".equals(dt.getHl7Abbreviation())) {
            // don't set a value
        } else {
            throw new IllegalArgumentException("concept datatype not yet implemented: " + dt.getName() + " with Hl7 Abbreviation: " + dt.getHl7Abbreviation());
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
        
        // jmiranda - setting the transformer factory in order to assure that 
        // the XML is rendered correctly (the resolver was picking up 
        // net.sf.saxon.TransformerFactoryImpl as the transform factory class.
        // Setting the transform factory ensures that we instantiate the 
        // Sun transform factory which has been show
        System.setProperty("javax.xml.transform.TransformerFactory",  
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        
        TransformerFactory transfac = TransformerFactory.newInstance();
        
        try {
            trans = transfac.newTransformer();
        } catch (TransformerException te) {
            System.out.println(HtmlFormEntryConstants.ERROR_TRANSFORMER_1 + te); 
        }
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, HtmlFormEntryConstants.CONSTANT_YES);
        trans.setOutputProperty(OutputKeys.INDENT, HtmlFormEntryConstants.CONSTANT_YES);
        trans.setOutputProperty(OutputKeys.METHOD, HtmlFormEntryConstants.CONSTANT_XML);
        
        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(document);
        try {
            trans.transform(source, result);            
        } catch (TransformerException te) {
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
     * Returns all the attributes associated with a Node
     * 
     * @param node the Node to retrieve attributes from
     * @return a Map containing all the attributes of the Node
     */
    public static Map<String, String> getNodeAttributes(Node node) {
    	Map<String, String> ret = new CaseInsensitiveMap();
        NamedNodeMap atts = node.getAttributes();
        for (int i=0; i<atts.getLength(); i++) {
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
     * @param defaultVal a default value to return if the attribute is not specified for the selected Node
     * @return
     */
    public static String getNodeAttribute(Node node, String attributeName, String defaultVal) {
    	String ret = getNodeAttributes(node).get(attributeName);
    	return (ret == null ? defaultVal : ret);
    }

    /** Creates a non-persistent "Fake" Person (used when previewing or validating an HTML Form)
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
        	if (type.getFormat().equals("java.lang.String")) {
        		demo.addAttribute(new PersonAttribute(type, "Test "+type.getName()+" Attribute"));
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
     * Combines a Date object that contains only a date component (day, month, year) with a Date object that contains
     * only a time component (hour, minute, second) into a single Date object
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
	 * Get the concept by id,
	 * the id can either be 
	 * 		1)an integer id like 5090 
	 * 	 or 2)mapping type id like "XYZ:HT"
	 * 	 or 3)uuid like "a3e12268-74bf-11df-9768-17cfc9833272"
	 * @param Id
	 * @return the concept if exist, else null
	 * @should find a concept by its conceptId 
     * @should find a concept by its mapping 
     * @should find a concept by its uuid
     * @should return null otherwise
	 */
	public static Concept getConcept(String id){
		Concept cpt = null;
		
		if(id != null){
			
			try{//handle integer: id
				int conceptId = Integer.parseInt(id);
				cpt  = Context.getConceptService().getConcept(conceptId);
				return cpt;
			}catch (Exception ex){
				//do nothing 
			}
			
			//handle  mapping id: xyz:ht
			int index = id.indexOf(":");
			if(index != -1){
				String mappingCode = id.substring(0,index).trim();
				String conceptCode = id.substring(index+1,id.length()).trim();	
				cpt = Context.getConceptService().getConceptByMapping(conceptCode,mappingCode);
				return cpt;
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if the id matches a uuid pattern
			if(Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(id).matches()){
				cpt = Context.getConceptService().getConceptByUuid(id);
				return cpt;
			}
		}
		
		return cpt;
	}
	
	/**
	 * Gets a concept by id, mapping, or uuid. (See #getConcept(String) for precise details.)
	 * If no concept is found, throws an IllegalArgumentException with the given message. 
	 * @param id
	 * @param errorMessageIfNotFound
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static Concept getConcept(String id, String errorMessageIfNotFound) throws IllegalArgumentException {
		Concept c = null;
		try {
			c = getConcept(id);
		} catch (Exception ex) {
			throw new IllegalArgumentException(errorMessageIfNotFound, ex);
		}
		if (c == null)
			throw new IllegalArgumentException(errorMessageIfNotFound);
		return c;
	}
	
	/***
	 * Get the location by: 
	 * 		1)an integer id like 5090 
	 *   or 2) uuid like "a3e12268-74bf-11df-9768-17cfc9833272"
	 *   or 3) location name like "Boston"
	 * @param Id
	 * @return the location if exist, else null
	 * @should find a location by its locationId
	 * @should find a location by name
     * @should find a location by its uuid
     * @should return null otherwise
	 */
	public static Location getLocation(String id){
		Location location = null;
		
		if(id != null){
			
			try{//handle integer: id
				int locationId = Integer.parseInt(id);
				location = Context.getLocationService().getLocation(locationId);
				return location;
			}catch (Exception ex){
				//do nothing 
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272" if id matches a uuid pattern
			if(Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(id).matches()){
				location  = Context.getLocationService().getLocationByUuid(id);
			}
			else {
				// if it's neither a uuid or id, try location name
				location = Context.getLocationService().getLocation(id);
			}
			
		}
		return location;
	}
	
	/***
	 * Get the program by: 
	 * 		1)an integer id like 5090 
	 *   or 2) uuid like "a3e12268-74bf-11df-9768-17cfc9833272"
	 *   or 3) name of *associated concept* (not name of program), like "MDR-TB Program"
	 *   	
	 * @param Id
	 * @return the program if exist, else null
	 * @should find a program by its id
	 * @should find a program by name of associated concept
     * @should find a program by its uuid
     * @should return null otherwise
	 */
	public static Program getProgram(String id){
		Program program = null;
		
		if(id != null){
			
			try{//handle integer: id
				int programId = Integer.parseInt(id);
				program = Context.getProgramWorkflowService().getProgram(programId);
				return program;
			}catch (Exception ex){
				//do nothing 
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid pattern
			if(Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(id).matches()){
				program  = Context.getProgramWorkflowService().getProgramByUuid(id);
			}
			else {
				// if it's neither a uuid or id, try program name
				// (note that this API method actually checks based on name of the associated concept, not the name of the program itself)
				program = Context.getProgramWorkflowService().getProgramByName(id);			
			}
			
		}
		return program;
	}
	
	/***
	 * Get the person by: 
	 * 		1)an integer id like 5090 
	 *   or 2) uuid like "a3e12268-74bf-11df-9768-17cfc9833272"
	 *   or 3) a username like "mgoodrich"
	 * @param Id
	 * @return the person if exist, else null
	 * @should find a person by its id
     * @should find a person by its uuid
     * @should find a person by username of corresponding user
     * @should return null otherwise
	 */
	public static Person getPerson(String id){
		Person person = null;
		
		if(id != null){
			
			try{//handle integer: id
				int personId = Integer.parseInt(id);
				person = Context.getPersonService().getPerson(personId);
				return person;
			}catch (Exception ex){
				//do nothing 
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid pattern
			if(Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(id).matches()){
				person  = Context.getPersonService().getPersonByUuid(id);
			}
			// handle username
			else {
				User personByUsername = Context.getUserService().getUserByUsername(id);
				
				if (personByUsername != null) {
					person = personByUsername.getPerson();
				} 
			}
		}
		return person;
	}
	
	/***
	 * Get the patient identifier type by: 
	 * 		1)an integer id like 5090 
	 *   or 2) uuid like "a3e12268-74bf-11df-9768-17cfc9833272"
	 *   or 3) a name like "Temporary Identifier"
	 * @param Id
	 * @return the identifier type if exist, else null
	 * @should find an identifier type by its id
     * @should find an identifier type by its uuid
     * @should find an identifier type by its name
     * @should return null otherwise
	 */
	public static PatientIdentifierType getPatientIdentifierType(String id){
		PatientIdentifierType identifierType = null;
		
		if(id != null){
			
			try{//handle integer: id
				int identifierTypeId = Integer.parseInt(id);
				identifierType = Context.getPatientService().getPatientIdentifierType(identifierTypeId);
				return identifierType;
			}catch (Exception ex){
				//do nothing 
			}
			
			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid pattern
			if(Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(id).matches()){
				identifierType  = Context.getPatientService().getPatientIdentifierTypeByUuid(id);
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
	 * Replaces all the ids in a form with uuids
	 * 
	 * This method operates using the AttributeDescriptors property of TagHandlers; if you have have a new attribute
	 * that you want to configure for id-to-uuid substitution, add a descriptor for that attribute to the appropriate tag handler and
	 * set the class property of that descriptor to the appropriate class
	 * 
	 * Can currently handle ids for the following classes: Concept, Program, Person, PatientIdentifierType, Location, and Drug
	 * 
	 * @return
	 * @return
	 */
	public static void replaceIdsWithUuids(HtmlForm form) {
		// get the tag handlers so we can gain access to the attribute descriptors
		Map<String, TagHandler> tagHandlers = Context.getService(HtmlFormEntryService.class).getHandlers();
		
		// loop through all the attribute descriptors for all the tags that have been registered
		for (String tagName : tagHandlers.keySet()) {
			log.debug("Handling id-to-uuid substitutions for tag " + tagName);
			
			if (tagHandlers.get(tagName).getAttributeDescriptors() != null) {
				for (AttributeDescriptor attributeDescriptor : tagHandlers.get(tagName).getAttributeDescriptors()) {
					// we only need to deal with descriptors that have an associated class
					if (attributeDescriptor.getClazz() != null) {
						// build the attribute string we are searching for
						// match any time that attribute name falls within the specified tag --- [^>]* means any character except a >
						String pattern = "<" + tagName + "[^>]*" + attributeDescriptor.getName();
						log.debug("id-to-uuid substitution pattern: " + pattern);
						form.setXmlData(replaceIdsWithUuidsHelper(form.getXmlData(), pattern, attributeDescriptor.getClazz()));
					}
				}
			}
		}
	}
	
	private static String replaceIdsWithUuidsHelper(String formXmlData, String attribute, Class<? extends OpenmrsObject> clazz) {
		return replaceIdsWithUuidsHelper(formXmlData, attribute, clazz, true);
	}
	
	private static String replaceIdsWithUuidsHelper(String formXmlData, String attribute, Class<? extends OpenmrsObject> clazz, Boolean includeQuotes) {
		Pattern substitutionPattern;
		
		if (includeQuotes) {
			// pattern to find the specified attribute and pull out its values; regex matches any characters within quotes after an equals, i.e. ="a2-32" would match a232
			// we use () to break the match into three groups: 1) the characters up to the including the first quotes; 2) the characters in the quotes; and 3) then the trailing quote
			// (put a space before the attribute name so we don't get border= instead of order=)
			substitutionPattern = Pattern.compile("(\\s" + attribute + "=\")(.*?)(\")", Pattern.CASE_INSENSITIVE);
		}	
		else {
			// the same pattern as above, but without the quotes (to handle the macro assignments),
			// and with a blank space at the end (which we need to account for when we do the replace)	
			substitutionPattern = Pattern.compile("(\\s" + attribute + "=)(.*?)(\\s)", Pattern.CASE_INSENSITIVE);
		}
			
		Matcher matcher = substitutionPattern.matcher(formXmlData);
		
		// lists to keep track of any "repeat" keys and macros we are going to have to substitute out as well
		Set<String> repeatKeysToReplace = new HashSet<String>();
		Set<String> macrosToReplace = new HashSet<String>();
		
		StringBuffer buffer = new StringBuffer();
		
		while (matcher.find()) {	
			// split the group into the various ids
			String[] ids = matcher.group(2).split(",");
			
			StringBuffer idBuffer = new StringBuffer();
			// now loop through each id
			for (String id : ids) {
				// see if this is an id (i.e., is made up of one or more digits), as opposed to a mapping id or a uuid, or a key used in a repeat template)
				if (id.matches("^\\d+$")) {
					// try to find the OpenmrsObject referenced by this id, and convert the id to a uuid
					OpenmrsObject object = Context.getService(HtmlFormEntryService.class).getItemById(clazz, Integer.valueOf(id));
					idBuffer.append(object.getUuid() + ",");
				} else {
					// otherwise, leave the id only
					idBuffer.append(id + ",");
					
					// also, if this id is a repeat key (i.e., something in curly braces) we need to keep track of it so that we can perform key substitutions
					// pattern matches one or more characters of any type within curly braces
					Matcher repeatKeyMatcher = Pattern.compile("\\{(.+)\\}").matcher(id);
					if(repeatKeyMatcher.find()) {
						repeatKeysToReplace.add(repeatKeyMatcher.group(1));
					}
					
					// also, if this id is a macro reference (i.e, something that starts with a $) we need to keep track of it so that we can perform macro substitution
					Matcher macroMatcher = Pattern.compile("\\$(.+)").matcher(id);
					if (macroMatcher.find()) {
						macrosToReplace.add(macroMatcher.group(1));
					}
				}
			}
			
			// trim off the trailing comma
			idBuffer.deleteCharAt(idBuffer.length() - 1);
			
			// now do the replacement
			
			// create the replacement string from the matched sequence, substituting out group(2) with the updated ids
			String replacementString = matcher.group(1) + idBuffer.toString() + matcher.group(3);

			// we need to escape any $ characters in the buffer or we run into errors with the appendReplacement method since 
			// the $ has a special meaning to that method
			replacementString = replacementString.replace("$", "\\$");
				
			// now append the replacement string to the buffer
			matcher.appendReplacement(buffer, replacementString);
		}
		
		// append the rest of htmlform
		matcher.appendTail(buffer);
		
		formXmlData = buffer.toString();
		
		// now handle any repeat keys we have discovered during this substitution
		for (String key : repeatKeysToReplace) {
			formXmlData = replaceIdsWithUuidsHelper(formXmlData, key, clazz);
		}
		
		// and now handle any macros we have discovered during this substitution
		for (String key : macrosToReplace) {
			formXmlData = replaceIdsWithUuidsHelper(formXmlData, key, clazz, false);
		}
		
		return formXmlData;
	}

	public static List<PatientIdentifierType> getPatientIdentifierTypes(){
		return Context.getPatientService().getAllPatientIdentifierTypes();
	}

}
