package org.openmrs.module.htmlformentry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.FormField;
import org.openmrs.FormRecordable;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
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
import org.openmrs.Provider;
import org.openmrs.ServiceOrder;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.action.ObsGroupAction;
import org.openmrs.module.htmlformentry.compatibility.EncounterCompatibility;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.openmrs.module.htmlformentry.element.OrderSubmissionElement;
import org.openmrs.module.htmlformentry.element.ProviderStub;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.openmrs.module.htmlformentry.util.Predicate;
import org.openmrs.module.htmlformentry.util.ProviderTransformer;
import org.openmrs.module.providermanagement.ProviderRole;
import org.openmrs.module.providermanagement.api.ProviderManagementService;
import org.openmrs.obs.ComplexData;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.DrugEditor;
import org.openmrs.propertyeditor.EncounterTypeEditor;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.PatientEditor;
import org.openmrs.propertyeditor.PersonEditor;
import org.openmrs.propertyeditor.UserEditor;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HTML Form Entry utility methods
 */
public class HtmlFormEntryUtil {
	
	public static Log log = LogFactory.getLog(HtmlFormEntryUtil.class);
	
	private static MetadataMappingResolver getMetadaMappingResolver() {
		return Context.getRegisteredComponent("metadataMappingResolver", MetadataMappingResolver.class);
	}
	
	/**
	 * Returns the HTML Form Entry service from the Context
	 *
	 * @return HTML Form Entry service
	 */
	public static HtmlFormEntryService getService() {
		return Context.getService(HtmlFormEntryService.class);
	}
	
	private static <T extends OpenmrsMetadata> T getMetadataByMapping(Class<T> type, String identifier) {
		MetadataMappingResolver metadataMappingResolver = getMetadaMappingResolver();
		if (metadataMappingResolver != null) {
			return metadataMappingResolver.getMetadataItem(type, identifier);
		}
		return null;
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
		if (ComplexData.class.isAssignableFrom(clazz) && request instanceof MultipartHttpServletRequest) {
			return convertToComplexData(request, name);
		} else {
			String val = request.getParameter(name);
			return convertToType(val, clazz);
		}
	}
	
	public static ComplexData convertToComplexData(HttpServletRequest request, String name) {
		MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;
		MultipartFile file = mRequest.getFile(name);
		if (file != null && file.getSize() > 0) {
			try {
				byte[] bytes = IOUtils.toByteArray(file.getInputStream());
				ByteArrayInputStream is = new ByteArrayInputStream(bytes);
				return new ComplexData(file.getOriginalFilename(), is);
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			return null;
		}
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
		} else if (Drug.class.isAssignableFrom(clazz)) {
			DrugEditor ed = new DrugEditor();
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
	 * @param accessionNumber accession number associated with the Obs (may be null)
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
			if (value instanceof Drug) {
				Drug drugValue = (Drug) value;
				// TODO: Not sure why this should be necessary, but unit tests fail without it (MS, 9/22/2020)
				Concept conceptValue = HibernateUtil.getRealObjectFromProxy(drugValue.getConcept());
				obs.setValueDrug(drugValue);
				obs.setValueCoded(conceptValue);
			} else if (value instanceof ConceptName) {
				obs.setValueCodedName((ConceptName) value);
				obs.setValueCoded(obs.getValueCodedName().getConcept());
			} else if (value instanceof Concept) {
				obs.setValueCoded((Concept) value);
			} else {
				obs.setValueCoded((Concept) convertToType(value.toString(), Concept.class));
			}
		} else if (dt.isBoolean()) {
			if (value != null) {
				try {
					obs.setValueAsString(value.toString());
				}
				catch (ParseException e) {
					throw new IllegalArgumentException("Unable to convert " + value + " to a Boolean Obs value", e);
				}
			}
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
			
			// Disable XXE: security measure to prevent DOS, arbitrary-file-read, and possibly RCE
			dbf.setExpandEntityReferences(false);
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			
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
	 *
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
	 * @param defaultVal a default value to return if the attribute is not specified for the selected
	 *            Node
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
		}
		catch (TransformerException ex) {
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
	
	public static HtmlFormSchema getHtmlFormSchema(HtmlForm htmlForm, Mode mode) {
		try {
			Patient patient = HtmlFormEntryUtil.getFakePerson();
			FormEntrySession fes = new FormEntrySession(patient, null, mode, htmlForm, null);
			fes.getHtmlToDisplay();
			return fes.getContext().getSchema();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to load html form schema for htmlform: " + htmlForm, e);
		}
	}
	
	/**
	 * Combines a Date object that contains only a date component (day, month, year) with a Date object
	 * that contains only a time component (hour, minute, second) into a single Date object
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
	
	public static Date startOfDay(Date d) {
		if (d != null) {
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			return c.getTime();
		}
		return null;
	}
	
	/**
	 * Convenience method to translate a given code
	 */
	public static String translate(String message) {
		return Context.getMessageSourceService().getMessage(message);
	}
	
	/**
	 * If an encounter has any non-voided providers, return the first listed Otherwise, if the current
	 * user has any provider accounts, return the first listed Otherwise, return null
	 */
	public static Provider getOrdererFromEncounter(Encounter e) {
		if (e != null) {
			Set<EncounterProvider> encounterProviders = e.getEncounterProviders();
			for (EncounterProvider encounterProvider : encounterProviders) {
				if (BooleanUtils.isNotTrue(encounterProvider.getVoided())) {
					return encounterProvider.getProvider();
				}
			}
		}
		User u = Context.getAuthenticatedUser();
		Collection<Provider> l = Context.getProviderService().getProvidersByPerson(u.getPerson(), false);
		if (!l.isEmpty()) {
			return l.iterator().next();
		}
		return null;
	}
	
	public static <T extends OpenmrsObject> T getOpenmrsObject(String lookup, Class<T> type) {
		if (type == Concept.class) {
			return (T) getConcept(lookup);
		}
		if (type == Drug.class) {
			return (T) getDrug(lookup);
		}
		if (type == OrderType.class) {
			return (T) getOrderType(lookup);
		}
		if (type == OrderFrequency.class) {
			return (T) getOrderFrequency(lookup);
		}
		if (type == CareSetting.class) {
			return (T) getCareSetting(lookup);
		}
		if (type == Location.class) {
			return (T) getLocation(lookup);
		}
		if (type == Program.class) {
			return (T) getProgram(lookup);
		}
		if (type == PatientIdentifierType.class) {
			return (T) getPatientIdentifierType(lookup);
		}
		if (type == ProgramWorkflow.class) {
			return (T) getWorkflow(lookup);
		}
		if (type == LocationTag.class) {
			return (T) getLocationTag(lookup);
		}
		if (type == ProviderRole.class) {
			return (T) getProviderRole(lookup);
		}
		if (type == Provider.class) {
			return (T) getProvider(lookup);
		}
		throw new IllegalArgumentException("Not able to lookup OpenMRS object of type: " + type);
	};
	
	/**
	 * Find drug by uuid, name, or id
	 */
	public static Drug getDrug(String uuidOrIdOrName) {
		Drug drug = null;
		if (StringUtils.isNotBlank(uuidOrIdOrName)) {
			uuidOrIdOrName = uuidOrIdOrName.trim();
			try {
				drug = Context.getConceptService().getDrugByUuid(uuidOrIdOrName);
				if (drug == null) {
					drug = Context.getConceptService().getDrug(uuidOrIdOrName);
				}
			}
			catch (Exception e) {
				log.error("Failed to find drug: ", e);
			}
		}
		return drug;
	}
	
	/**
	 * Find order type by uuid, name, or id
	 */
	public static OrderType getOrderType(String uuidOrIdOrName) {
		OrderType ret = null;
		if (StringUtils.isNotBlank(uuidOrIdOrName)) {
			uuidOrIdOrName = uuidOrIdOrName.trim();
			ret = Context.getOrderService().getOrderTypeByUuid(uuidOrIdOrName);
			if (ret == null) {
				try {
					ret = Context.getOrderService().getOrderType(Integer.parseInt(uuidOrIdOrName));
				}
				catch (Exception e) {}
			}
			if (ret == null) {
				ret = Context.getOrderService().getOrderTypeByName(uuidOrIdOrName);
			}
		}
		return ret;
	}
	
	/**
	 * @return true if a given Order Type represents a Drug Order
	 */
	public static boolean isADrugOrderType(OrderType orderType) {
		try {
			return DrugOrder.class.isAssignableFrom(orderType.getJavaClass());
		}
		catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @return true if a given Order Type represents a Service Order
	 */
	public static boolean isAServiceOrderType(OrderType orderType) {
		try {
			return ServiceOrder.class.isAssignableFrom(orderType.getJavaClass());
		}
		catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @return all of the Order Types in the system that represent Drug Orders
	 */
	public static List<OrderType> getDrugOrderTypes() {
		List<OrderType> ret = new ArrayList<>();
		for (OrderType orderType : Context.getOrderService().getOrderTypes(false)) {
			if (isADrugOrderType(orderType)) {
				ret.add(orderType);
			}
		}
		return ret;
	}
	
	/**
	 * @return all orders for a patient, ordered by date, accounting for previous orders
	 */
	public static List<Order> getOrdersForPatient(Patient patient, Set<Concept> concepts) {
		List<Order> ret = new ArrayList<>();
		List<Order> orders = Context.getOrderService().getAllOrdersByPatient(patient);
		for (Order order : orders) {
			order = HibernateUtil.getRealObjectFromProxy(order);
			if (BooleanUtils.isNotTrue(order.getVoided())) {
				if (concepts.contains(order.getConcept())) {
					ret.add(order);
				}
			}
		}
		sortOrders(ret);
		return ret;
	}
	
	/**
	 * Sorts the given orders in place. This first determines if a given order is a revision of an
	 * order, if so it is later Otherwise, it compares effectiveStartDate Otherwise, it compares
	 * effectiveStopDate, where a null stop date is later than a non-null one
	 */
	public static void sortOrders(List<Order> orders) {
		if (orders != null && orders.size() > 1) {
			Collections.sort(orders, (d1, d2) -> {
				// Get all of the previous orders for d1.  If any are d2, then d1 is later
				for (Order d1Prev = d1.getPreviousOrder(); d1Prev != null; d1Prev = d1Prev.getPreviousOrder()) {
					if (d1Prev.equals(d2)) {
						return 1;
					}
				}
				// Get all of the previous orders for d2.  If any are d1, then d2 is later
				for (Order d2Prev = d2.getPreviousOrder(); d2Prev != null; d2Prev = d2Prev.getPreviousOrder()) {
					if (d2Prev.equals(d1)) {
						return -1;
					}
				}
				// If neither is a revision of the other, then compare based on effective start date
				int dateCompare = d1.getEffectiveStartDate().compareTo(d2.getEffectiveStartDate());
				if (dateCompare != 0) {
					return dateCompare;
				}
				// If they are still the same, then order based on end date
				int ret = OpenmrsUtil.compareWithNullAsLatest(d1.getEffectiveStopDate(), d2.getEffectiveStopDate());
				if (ret == 0) {
					// Finally, order based on orderId
					ret = d1.getOrderId().compareTo(d2.getOrderId());
				}
				return ret;
			});
		}
	}
	
	/**
	 * Find order frequency by uuid or id, or concept
	 */
	public static OrderFrequency getOrderFrequency(String lookup) {
		OrderFrequency orderFrequency = null;
		if (StringUtils.isNotBlank(lookup)) {
			lookup = lookup.trim();
			orderFrequency = Context.getOrderService().getOrderFrequencyByUuid(lookup);
			if (orderFrequency == null) {
				try {
					orderFrequency = Context.getOrderService().getOrderFrequency(Integer.parseInt(lookup));
				}
				catch (Exception e) {}
			}
			if (orderFrequency == null) {
				Concept c = getConcept(lookup);
				if (c != null) {
					orderFrequency = Context.getOrderService().getOrderFrequencyByConcept(c);
				}
			}
		}
		return orderFrequency;
	}
	
	/**
	 * Find order frequency by uuid or id, or concept
	 */
	public static Class<? extends DosingInstructions> getDosingType(String lookup) {
		if (StringUtils.isNotBlank(lookup)) {
			try {
				return (Class<? extends DosingInstructions>) Context.loadClass(lookup);
			}
			catch (Exception e) {
				log.warn("Unable to load dosing type with name: " + lookup);
			}
		}
		return null;
	}
	
	/**
	 * Find exact care setting by uuid, name, or id. If not found, but lookup corresponds to a care
	 * setting type, return first matching one found
	 */
	public static CareSetting getCareSetting(String lookup) {
		CareSetting ret = null;
		if (StringUtils.isNotBlank(lookup)) {
			lookup = lookup.trim();
			ret = Context.getOrderService().getCareSettingByUuid(lookup);
			if (ret == null) {
				ret = Context.getOrderService().getCareSettingByName(lookup);
			}
			if (ret == null) {
				try {
					ret = Context.getOrderService().getCareSetting(Integer.parseInt(lookup));
				}
				catch (Exception e) {
					
				}
			}
			if (ret == null) {
				try {
					CareSetting.CareSettingType type = CareSetting.CareSettingType.valueOf(lookup);
					if (type != null) {
						for (CareSetting cs : Context.getOrderService().getCareSettings(false)) {
							if (cs.getCareSettingType().equals(type)) {
								return cs;
							}
						}
					}
				}
				catch (Exception e) {}
			}
		}
		return ret;
	}
	
	/**
	 * Get the concept by id where the id can either be: 1) an integer id like 5090 2) a mapping type id
	 * like "XYZ:HT" 3) a uuid like "a3e12268-74bf-11df-9768-17cfc9833272" 4) the fully qualified name
	 * of a Java constant that contains one of above
	 *
	 * @param id the concept identifier
	 * @return the concept if exist, else null <strong>Should</strong> find a concept by its conceptId
	 *         <strong>Should</strong> find a concept by its mapping <strong>Should</strong> find a
	 *         concept by its uuid <strong>Should</strong> find a concept by static constant
	 *         <strong>Should</strong> return null otherwise <strong>Should</strong> find a concept by
	 *         its mapping with a space in between
	 */
	public static Concept getConcept(String id) {
		
		Concept cpt = null;
		
		if (id != null) {
			
			id = id.trim();
			
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
				cpt = Context.getService(HtmlFormEntryService.class).getConceptByMapping(id);
				if (cpt != null) {
					return cpt;
				}
			}
			
			// handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if the id matches a uuid format
			if (isValidUuidFormat(id)) {
				cpt = Context.getConceptService().getConceptByUuid(id);
			}
			// finally, if input contains at least one period handle recursively as a code constant
			else if (id.contains(".")) {
				return getConcept(evaluateStaticConstant(id));
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
	 * This method doesn't support "SessionAttribute:", but is otherwise like the similarly-named
	 * method.
	 *
	 * @see #getLocation(String, FormEntryContext)
	 */
	public static Location getLocation(String id) {
		return getLocation(id, null);
	}
	
	/**
	 * Get the location by:
	 * <ol>
	 * <li>an integer id like 5090</li>
	 * <li>a uuid like "a3e12268-74bf-11df-9768-17cfc9833272"</li>
	 * <li>a name like "Boston"</li>
	 * <li>an id/name pair like "501 - Boston" (this format is used when saving a location on a obs as a
	 * value text)</li>
	 * <li>"GlobalProperty:property.name"</li>
	 * <li>"UserProperty:propertyName"</li>
	 * <li>"SessionAttribute:attributeName"</li>
	 * </ol>
	 *
	 * @param id
	 * @param context
	 * @return the location if exist, else null <strong>Should</strong> find a location by its
	 *         locationId <strong>Should</strong> find a location by name <strong>Should</strong> find a
	 *         location by its uuid <strong>Should</strong> find a location by global property
	 *         <strong>Should</strong> find a location by user property <strong>Should</strong> find a
	 *         location by session attribute <strong>Should</strong> not fail if trying to find a
	 *         location by session attribute and we have no session <strong>Should</strong> return null
	 *         otherwise
	 */
	public static Location getLocation(String id, FormEntryContext context) {
		
		Location location = null;
		
		if (id != null) {
			
			id = id.trim();
			
			// handle "SystemDefault" setting
			if (id.equals(HtmlFormEntryConstants.SYSTEM_DEFAULT)) {
				location = Context.getLocationService().getDefaultLocation();
				if (location != null) {
					return location;
					
				}
			}
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
			
			//get by mapping "source:code"
			location = getMetadataByMapping(Location.class, id);
			if (location != null) {
				return location;
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
	 * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) name of *associated concept* (not name of program),
	 * like "MDR-TB Program"
	 *
	 * @param id
	 * @return the program if exist, else null <strong>Should</strong> find a program by its id
	 *         <strong>Should</strong> find a program by name of associated concept
	 *         <strong>Should</strong> find a program by its uuid <strong>Should</strong> return null
	 *         otherwise
	 */
	public static Program getProgram(String id) {
		
		Program program = null;
		
		if (id != null) {
			
			id = id.trim();
			
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
			
			//get Program by mapping
			program = getMetadataByMapping(Program.class, id);
			if (program != null) {
				return program;
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
	 * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) a username like "mgoodrich" or 4) an id/name pair
	 * like "5090 - Bob Jones" (this format is used when saving a person on a obs as a value text)
	 *
	 * @param id
	 * @return the person if exist, else null <strong>Should</strong> find a person by its id
	 *         <strong>Should</strong> find a person by its uuid <strong>Should</strong> find a person
	 *         by username of corresponding user <strong>Should</strong> return null otherwise
	 */
	public static Person getPerson(String id) {
		
		Person person = null;
		
		if (id != null) {
			
			id = id.trim();
			
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
	 * @return the identifier type if exist, else null <strong>Should</strong> find an identifier type
	 *         by its id <strong>Should</strong> find an identifier type by its uuid
	 *         <strong>Should</strong> find an identifier type by its name <strong>Should</strong>
	 *         return null otherwise
	 */
	public static PatientIdentifierType getPatientIdentifierType(String id) {
		PatientIdentifierType identifierType = null;
		
		if (id != null) {
			
			id = id.trim();
			
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
			
			//get PatientIdentifierType by mapping
			identifierType = getMetadataByMapping(PatientIdentifierType.class, id);
			if (identifierType != null) {
				return identifierType;
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
	
	public static ProgramWorkflow getWorkflow(Integer id) {
		for (Program p : Context.getProgramWorkflowService().getAllPrograms()) {
			for (ProgramWorkflow w : p.getAllWorkflows()) {
				if (w.getProgramWorkflowId().equals(id)) {
					return w;
				}
			}
		}
		return null;
	}
	
	/**
	 * Looks up a {@link ProgramWorkflow} by id, uuid or by concept map of the underlying concept
	 */
	@SuppressWarnings("deprecation")
	public static ProgramWorkflow getWorkflow(String identifier) {
		ProgramWorkflow workflow = null;
		
		if (identifier != null) {
			
			identifier = identifier.trim();
			
			// first try to fetch by id
			try {
				Integer id = Integer.valueOf(identifier);
				workflow = getWorkflow(id);
				
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
	 * uuid, or by a concept map to the the underlying concept (Note that if there are multiple states
	 * associated with the same concept in the program, this method will return an arbitrary one if
	 * fetched by concept mapping)
	 *
	 * @param identifier the programWorkflowStateId, uuid or the concept name to match against
	 * @param program
	 * @return <strong>Should</strong> return the state with the matching id <strong>Should</strong>
	 *         return the state with the matching uuid <strong>Should</strong> return the state
	 *         associated with a concept that matches the passed concept map
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
			identifier = identifier.trim();
			
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
	 * Looks up a {@link ProgramWorkflowState} from the specified workflow by programWorkflowStateId,
	 * uuid, or by a concept map to the the underlying concept (Note that if there are multiple states
	 * associated with the same concept in the workflow, this method will return an arbitrary one if
	 * fetched by concept mapping)
	 *
	 * @param identifier the programWorkflowStateId, uuid or the concept name to match against
	 * @param workflow
	 * @return <strong>Should</strong> return the state with the matching id <strong>Should</strong>
	 *         return the state with the matching uuid <strong>Should</strong> return the state
	 *         associated with a concept that matches the passed concept map
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
			identifier = identifier.trim();
			
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
	
	public static List<Location> getLocationsByTags(String attributeName, Map<String, String> parameters) {
		List<Location> locations = null;
		
		String locationTags = parameters.get(attributeName);
		
		if (locationTags != null) {
			List<LocationTag> tags = new ArrayList<LocationTag>();
			String[] temp = locationTags.split(",");
			for (String s : temp) {
				if (s != null && !s.isEmpty()) {
					LocationTag tag = getLocationTag(s);
					if (tag == null) {
						throw new RuntimeException("Cannot find tag: " + tag);
					}
					tags.add(tag);
				}
			}
			locations = new ArrayList<Location>();
			locations.addAll(Context.getLocationService().getLocationsHavingAnyTag(tags));
		}
		return locations;
	}
	
	/**
	 * Fetches a location tag by name or id (Will add support for uuid once we stop supporting OpenMRS
	 * 1.6, which doesn't a uuid on location tag)
	 *
	 * @param identifier
	 * @return
	 */
	public static LocationTag getLocationTag(String identifier) {
		
		LocationTag tag = null;
		
		if (identifier != null) {
			
			identifier = identifier.trim();
			
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
	
	public static ConceptClass getConceptClass(String lookup) {
		ConceptClass ret = null;
		if (StringUtils.isNotBlank(lookup)) {
			try {
				ret = Context.getConceptService().getConceptClassByUuid(lookup);
				if (ret == null) {
					ret = Context.getConceptService().getConceptClassByName(lookup);
				}
				if (ret == null) {
					ret = Context.getConceptService().getConceptClass(Integer.parseInt(lookup));
				}
			}
			catch (Exception e) {}
		}
		return ret;
	}
	
	public static ConceptDatatype getConceptDatatype(String lookup) {
		ConceptDatatype ret = null;
		if (StringUtils.isNotBlank(lookup)) {
			try {
				ret = Context.getConceptService().getConceptDatatypeByUuid(lookup);
				if (ret == null) {
					ret = Context.getConceptService().getConceptDatatypeByName(lookup);
				}
				if (ret == null) {
					ret = Context.getConceptService().getConceptDatatype(Integer.parseInt(lookup));
				}
			}
			catch (Exception e) {}
		}
		return ret;
	}
	
	public static ConceptNameType getConceptNameType(String lookup) {
		ConceptNameType ret = null;
		if (StringUtils.isNotBlank(lookup)) {
			try {
				return ConceptNameType.valueOf(lookup);
			}
			catch (Exception e) {}
		}
		return ret;
	}
	
	public static ConceptNameTag getConceptNameTag(String lookup) {
		ConceptNameTag ret = null;
		if (StringUtils.isNotBlank(lookup)) {
			try {
				ret = Context.getConceptService().getConceptNameTagByUuid(lookup);
				if (ret == null) {
					ret = Context.getConceptService().getConceptNameTagByName(lookup);
				}
				if (ret == null) {
					ret = Context.getConceptService().getConceptNameTag(Integer.parseInt(lookup));
				}
			}
			catch (Exception e) {}
		}
		return ret;
	}
	
	/**
	 * Returns the control id part out of an OpenMRS data object's form namespace and path. Eg:
	 * "my_condition_tag" out of "HtmlFormEntry^MyForm.1.0/my_condition_tag-0"
	 *
	 * @param openmrsData The form recordable OpenMRS data object
	 * @return The control id or null if the form recordable has no form namespace and path set
	 */
	public static String getControlId(FormRecordable openmrsData) {
		
		// Validate if getFormFieldPath exists.
		if (StringUtils.isEmpty(openmrsData.getFormFieldPath())) {
			return null;
		}
		
		// Get the control id
		String controlId = openmrsData.getFormFieldPath().split("/")[1];
		String[] controlIdSplitted = controlId.split("-(?!.*-)");
		
		// Check if it has a control counter
		if (NumberUtils.isDigits(controlIdSplitted[1])) {
			return controlIdSplitted[0];
		} else {
			return controlId;
		}
	}
	
	/**
	 * Tells whether a CodedOrFreeText instance is empty.
	 *
	 * @param codedOrFreeText The CodedOrFreeText instance to check.
	 * @return true if the underlying coded concept is null
	 * @return true if the underlying non-coded string value is blank
	 * @return true if the underlying specific concept name is null
	 */
	public static boolean isEmpty(CodedOrFreeText codedOrFreeText) {
		
		if (codedOrFreeText.getCoded() != null) {
			return false;
		}
		
		if (StringUtils.isNotBlank(codedOrFreeText.getNonCoded())) {
			return false;
		}
		
		if (codedOrFreeText.getSpecificName() != null) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Finds the first ancestor (including the current location) that is tagged with the specified
	 * location tag
	 *
	 * @param location
	 * @param locationTag
	 * @return
	 */
	public static Location getFirstAncestorWithTag(Location location, LocationTag locationTag) {
		if (location == null || locationTag == null) {
			return null;
		} else if (location.hasTag(locationTag.getName())) {
			return location;
		} else if (location.getParentLocation() != null) {
			return getFirstAncestorWithTag(location.getParentLocation(), locationTag);
		} else {
			return null;
		}
	}
	
	/**
	 * Iterates through the "locations" list and removed all that are not equal to, or a descendent of,
	 * the "testLocation"
	 *
	 * @param locations
	 * @param testLocation
	 * @return
	 */
	public static List<Location> removeLocationsNotEqualToOrDescendentOf(List<Location> locations, Location testLocation) {
		
		if (testLocation == null) {
			return locations;
		}
		
		Iterator<Location> i = locations.iterator();
		
		while (i.hasNext()) {
			if (!isLocationEqualToOrDescendentOf(i.next(), testLocation)) {
				i.remove();
			}
		}
		
		return locations;
	}
	
	/**
	 * Returns true/false whether the given location is equal to, or a descendent of, "testLocation"
	 *
	 * @param location
	 * @param testLocation
	 * @return
	 */
	public static Boolean isLocationEqualToOrDescendentOf(Location location, Location testLocation) {
		if (location == null) {
			return false;
		} else if (location.equals(testLocation)) {
			return true;
		} else {
			return isLocationEqualToOrDescendentOf(location.getParentLocation(), testLocation);
		}
	}
	
	private static List<ProgramWorkflowState> getStates(boolean includeRetired) {
		List<ProgramWorkflowState> ret = new ArrayList<ProgramWorkflowState>();
		for (Program p : Context.getProgramWorkflowService().getAllPrograms()) {
			for (ProgramWorkflow w : p.getAllWorkflows()) {
				for (ProgramWorkflowState s : w.getStates()) {
					if (includeRetired || !s.isRetired()) {
						ret.add(s);
					}
				}
			}
		}
		return ret;
	}
	
	private static ProgramWorkflowState getState(Integer id) {
		for (ProgramWorkflowState s : getStates(true)) {
			if (s.getProgramWorkflowStateId().equals(id)) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * Looks up a {@link ProgramWorkflowState} from the specified workflow by programWorkflowStateId, or
	 * uuid
	 *
	 * @param identifier the programWorkflowStateId or uuid to match against
	 * @param
	 * @return <strong>Should</strong> return the state with the matching id <strong>Should</strong>
	 *         return the state with the matching uuid
	 */
	@SuppressWarnings("deprecation")
	public static ProgramWorkflowState getState(String identifier) {
		ProgramWorkflowState state = null;
		
		if (identifier != null) {
			try {
				
				identifier = identifier.trim();
				
				Integer id = Integer.valueOf(identifier);
				state = getState(id);
				
				if (state != null) {
					return state;
				}
			}
			catch (NumberFormatException e) {}
			
			//get ProgramWorkflowState by mapping
			state = getMetadataByMapping(ProgramWorkflowState.class, identifier);
			if (state != null) {
				return state;
			}
			
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
	 * Determines if the passed string is in valid uuid format By OpenMRS standards, a uuid must be 36
	 * characters in length and not contain whitespace, but we do not enforce that a uuid be in the
	 * "canonical" form, with alphanumerics seperated by dashes, since the MVP dictionary does not use
	 * this format (We also are being slightly lenient and accepting uuids that are 37 or 38 characters
	 * in length, since the uuid data field is 38 characters long)
	 */
	public static boolean isValidUuidFormat(String uuid) {
		if (uuid.length() < 36 || uuid.length() > 38 || uuid.contains(" ") || uuid.contains(".")) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Evaluates the specified Java constant using reflection
	 *
	 * @param fqn the fully qualified name of the constant
	 * @return the constant value
	 */
	protected static String evaluateStaticConstant(String fqn) {
		int lastPeriod = fqn.lastIndexOf(".");
		String clazzName = fqn.substring(0, lastPeriod);
		String constantName = fqn.substring(lastPeriod + 1);
		
		try {
			Class<?> clazz = Context.loadClass(clazzName);
			Field constantField = clazz.getField(constantName);
			Object val = constantField.get(null);
			return val != null ? String.valueOf(val) : null;
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Unable to evaluate " + fqn, ex);
		}
	}
	
	/**
	 * Gets all patient identifier types
	 *
	 * @return the patient identifier types
	 */
	public static List<PatientIdentifierType> getPatientIdentifierTypes() {
		return Context.getPatientService().getAllPatientIdentifierTypes();
	}
	
	/**
	 * Utility method to void an encounter. If the voidEncounterByHtmlFormSchema global property has
	 * been set to true, OR if the Html Form Flowsheet module has been started AND the
	 * voidEncounterByHtmlFormSchema global property has not been explicitly set to false, void the
	 * encounter via the special voidEncounterByHtmlFormSchema algorithm. Otherwise simply void the
	 * encounter normally. This test is done because when using the Html Form Flowsheet module we only
	 * want to void observations associated with the current form, not the entire encounter
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
	 * Utility method that sets all matched obs and orders to voided, and voids encounter if all obs and
	 * orders in encounter are voided. Does not call save, just updates the voided fields on all objects
	 * in encounter Uses a 'dummy' FormEntrySession to use htmlformentry schema matching mechanism, and
	 * then examines the leftover Obs, Orders from the FormEntrySession constructor
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
				if (lfca instanceof OrderSubmissionElement) {
					OrderSubmissionElement dse = (OrderSubmissionElement) lfca;
					matchedOrders.addAll(dse.getExistingOrders());
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
	 * Returns a copy of an Obs. Recurses through GroupMembers to return copies of those also, so the
	 * whole Obs tree is a copy.
	 *
	 * @param obsToCopy
	 * @param replacements
	 * @return
	 * @throws Exception
	 */
	private static Obs returnObsCopy(Obs obsToCopy, Map<Obs, Obs> replacements) throws Exception {
		Obs newObs = (Obs) returnCopy(obsToCopy);
		
		if (obsToCopy.isObsGrouping()) {
			newObs.setGroupMembers(new HashSet<Obs>());
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
	 * Utility to return a copy of an Object. Copies all properties that are referencese by getters and
	 * setters and *are not* collection
	 *
	 * @param source
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
	 * @param value use "now" for the current timestamp, "today" for the current date with a timestamp
	 *            of 00:00, or a date string that can be parsed by SimpleDateFormat with the format
	 *            parameter.
	 * @param format the pattern SimpleDateTime will use to parse the value, if other than "now" or
	 *            "today".
	 * @return Date on success; null for an invalid value
	 * @throws IllegalArgumentException if a date string cannot be parsed with the format string you
	 *             provided
	 * @see SimpleDateFormat <strong>Should</strong> return a Date object with current date and time for
	 *      "now" <strong>Should</strong> return a Date with current date, but time of 00:00:00:00, for
	 *      "today" <strong>Should</strong> return a Date object matching the value param if a format is
	 *      specified <strong>Should</strong> return null for null value <strong>Should</strong> return
	 *      null if format is null and value not in [ null, "now", "today" ] <strong>Should</strong>
	 *      fail if date parsing fails
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
	 * Given a list of patient programs and a program, returns the first patient program that matches
	 * the specified program
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
	 * Return all non-voided PatientPrograms for the given patient and program
	 *
	 * @param patient
	 * @param program
	 * @return
	 */
	public static List<PatientProgram> getPatientPrograms(Patient patient, Program program) {
		return Context.getProgramWorkflowService().getPatientPrograms(patient, program, null, null, null, null, false);
	}
	
	/**
	 * Given a patient and a program workflow, returns the first patient program that contains a state
	 * in the specified workflow
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
	 * If the specified patient is enrolled in the specified program on the specified date, return the
	 * associated patient program, otherwise return null
	 *
	 * @param patient
	 * @param program
	 * @param date
	 * @return
	 */
	public static PatientProgram getPatientProgramByProgramOnDate(Patient patient, Program program, Date date) {
		
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient, program, null,
		    date, date, null, false);
		
		if (patientPrograms.size() > 1) {
			throw new APIException("Simultaneous program enrollments in same program not supported");
		}
		
		if (patientPrograms.size() == 1) {
			return patientPrograms.get(0);
		} else {
			return null;
		}
		
	}
	
	/**
	 * Checks whether the encounter has a provider specified (including ugly reflection code for 1.9+)
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
			return EncounterCompatibility.getProvider(e) != null;
		}
	}
	
	/**
	 * Checks if the user is enrolled in a program at the specified date
	 *
	 * @param patient the patient that should be enrolled in the program
	 * @param program the program the patient be enrolled in
	 * @param date the date at which to check
	 * @return <strong>Should</strong> return true if the patient is enrolled in the program at the
	 *         specified date <strong>Should</strong> return false if the patient is not enrolled in the
	 *         program <strong>Should</strong> return false if the program was completed
	 *         <strong>Should</strong> return false if the date is before the existing patient program
	 *         enrollment date
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
		
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient, program, null,
		    date, date, null, false);
		
		return (patientPrograms.size() > 0);
		
	}
	
	/**
	 * Checks to see if the patient has a program enrollment in the specified program after the given
	 * date If multiple patient programs, returns the earliest enrollment If no enrollments, returns
	 * null
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
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient, program, date,
		    null, null, null, false);
		
		for (PatientProgram pp : patientPrograms) {
			if ((closestProgram == null || pp.getDateEnrolled().before(closestProgram.getDateEnrolled()))
			        && pp.getDateEnrolled().after(date)) {
				closestProgram = pp;
			}
			
		}
		
		return closestProgram;
	}
	
	/**
	 * Checks to see if the patient has a program enrollment in the specified program after the given
	 * date If multiple patient programs, returns the earliest enrollment If no enrollments, returns
	 * null
	 */
	public static PatientProgram getCurrentOrNextFutureProgramEnrollment(Patient patient, Program program, Date date) {
		PatientProgram closest = null;
		for (PatientProgram pp : getPatientPrograms(patient, program)) {
			if (pp.getActive(date)) {
				return pp;
			}
			if (pp.getDateEnrolled().compareTo(date) >= 0) {
				if (closest == null) {
					closest = pp;
				} else {
					if (pp.getDateEnrolled().before(closest.getDateEnrolled())) {
						closest = pp;
					} else if (pp.getDateEnrolled().equals(closest.getDateEnrolled())) {
						if (OpenmrsUtil.compareWithNullAsLatest(pp.getDateCompleted(), closest.getDateCompleted()) > 0) {
							closest = pp;
						}
					}
				}
			}
		}
		return closest;
	}
	
	/**
	 * The patient state that is active for the given patient program, in the given workflow, on the
	 * given date
	 * 
	 * @param pp the PatientProgram associated with the PatientState
	 * @param workflow the ProgramWorkflow associated with the PatientState
	 * @param onDate the date on which the PatientState must be active
	 * @return
	 */
	public static PatientState getPatientStateOnDate(PatientProgram pp, ProgramWorkflow workflow, Date onDate) {
		for (PatientState patientState : pp.statesInWorkflow(workflow, false)) {
			if (patientState.getActive(onDate)) {
				return patientState;
			}
		}
		return null;
	}
	
	/**
	 * @return the state that is active in the given workflow, on the given date, for the given patient
	 */
	public static PatientState getPatientStateOnDate(Patient patient, ProgramWorkflow workflow, Date onDate) {
		PatientProgram patientProgram = getPatientProgramByProgramOnDate(patient, workflow.getProgram(), onDate);
		if (patientProgram == null) {
			return null;
		}
		return getPatientStateOnDate(patientProgram, workflow, onDate);
	}
	
	/**
	 * Given a Date object, returns a Date object for the same date but with the time component (hours,
	 * minutes, seconds & milliseconds) removed
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
	 * @return a SimpleDateFormat object for the current locale, using the global property
	 */
	public static SimpleDateFormat getDateTimeFormat() {
		String df = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_FORMATTER_DATETIME,
		    "yyyy-MM-dd, HH:mm:ss");
		if (StringUtils.isNotBlank(df)) {
			return new SimpleDateFormat(df, Context.getLocale());
		} else {
			return Context.getDateTimeFormat();
		}
	}
	
	/**
	 * @param date the date to check
	 * @return true if the given date is not at midnight
	 */
	public static boolean hasTimeComponent(Date date) {
		return date != null && !DateUtils.isSameInstant(date, clearTimeComponent(date));
	}
	
	public static Date increment(Date date, int months, int days, int hours, int minutes, int seconds) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.DATE, days);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.SECOND, seconds);
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
	 * @param id
	 * @return the encounter type if exist, else null <strong>Should</strong> find a encounter type by
	 *         its encounterTypeId <strong>Should</strong> find a encounter type by name
	 *         <strong>Should</strong> find a encounter type by its uuid <strong>Should</strong> return
	 *         null otherwise
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
			
			//get EncounterType by mapping
			encounterType = getMetadataByMapping(EncounterType.class, id);
			if (encounterType != null) {
				return encounterType;
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
	
	public static EncounterRole getEncounterRole(String id) {
		EncounterRole role = null;
		if (StringUtils.isNotBlank(id)) {
			id = id.trim();
			try {
				int encounterRoleId = Integer.parseInt(id);
				role = Context.getEncounterService().getEncounterRole(encounterRoleId);
			}
			catch (Exception ex) {
				role = Context.getEncounterService().getEncounterRoleByUuid(id);
			}
		}
		return role;
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
	 * @return
	 */
	public static String format(OpenmrsMetadata md) {
		return format(md, Context.getLocale());
	}
	
	/**
	 * Formats a piece of Metadata for Display TODO This was copied from UiFramework--we probably should
	 * come up with a way to inject a formatter directly into HFE
	 *
	 * @param md
	 * @param locale
	 * @return
	 */
	public static String format(OpenmrsMetadata md, Locale locale) {
		String override = getLocalization(locale, md.getClass().getSimpleName(), md.getUuid());
		return override != null ? override : StringEscapeUtils.escapeHtml(md.getName());
	}
	
	/**
	 * See if there is a custom name for this message in the messages.properties files TODO This was
	 * copied from UiFramework--we probably should come up with a way to inject a formatter directly
	 * into HFE
	 *
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
	
	/**
	 * Given a include/exclude string. fetch the test expression
	 *
	 * @param teststr
	 * @return a substring of a test expression
	 * @throws BadFormDesignException <strong>Should</strong> extract the correct expression from
	 *             teststr
	 */
	public static String getTestStr(String teststr) throws BadFormDesignException {
		if (StringUtils.isBlank(teststr))
			throw new BadFormDesignException("Can't extract the test expression from " + teststr);
		
		//get the text inside the quotes, i.e the expression
		String[] actualExpression = StringUtils.substringsBetween(teststr, "\"", "\"");
		
		if (actualExpression == null || actualExpression.length != 1 || StringUtils.isBlank(actualExpression[0])) {
			throw new BadFormDesignException("Can't extract the test expression from " + teststr);//throw bad design exception here
		}
		
		return actualExpression[0];
	}
	
	/**
	 * Read the global property htmlformentry.archiveDir and return the correct path
	 *
	 * @return String representation of archive directory path <strong>Should</strong> return null if
	 *         htmlformentry.archiveDir is not defined <strong>Should</strong> replace %Y with a four
	 *         digit year value <strong>Should</strong> replace %M with a 2 digit month value
	 *         <strong>Should</strong> prepend the application data if specified value is not absolute.
	 */
	public static String getArchiveDirPath() {
		String value = Context.getAdministrationService().getGlobalProperty("htmlformentry.archiveDir");
		if (value != null && org.springframework.util.StringUtils.hasLength(value)) {
			
			//Replace %Y and %M if any
			Date today = new Date();
			GregorianCalendar gCal = new GregorianCalendar();
			value = value.replace("%Y", String.valueOf(gCal.get(Calendar.YEAR)));
			value = value.replace("%y", String.valueOf(gCal.get(Calendar.YEAR)));
			
			int month = gCal.get(Calendar.MONTH);
			month++;
			if (month < 10) {
				value = value.replace("%M", "0" + month);
				value = value.replace("%m", "0" + month);
			} else {
				value = value.replace("%M", String.valueOf(month));
				value = value.replace("%m", String.valueOf(month));
			}
			
			//Check if not absolute concatenate with application directory
			File path = new File(value);
			if (!path.isAbsolute()) {
				return OpenmrsUtil.getApplicationDataDirectory() + File.separator + value;
			}
			return value;
		}
		return null;
	}
	
	/**
	 * Attempts to parse the passed string as an Integer and fetch the provider role with that id If no
	 * match, or the string is unparseable as an Integer, try to fetch by uuid
	 *
	 * @param id
	 * @return
	 */
	public static Object getProviderRole(String id) {
		
		Object providerRole = null;
		
		if (StringUtils.isNotBlank(id)) {
			
			// see if this is parseable int; if so, try looking up by id
			Integer providerRoleId = null;
			
			try {
				providerRoleId = Integer.parseInt(id);
				providerRole = getProviderRoleById(providerRoleId);
				
				if (providerRole != null) {
					return providerRole;
				}
				
			}
			catch (Exception e) {
				// ignore this, move to try by uuid
			}
			
			// if no match by id, look up by uuid
			providerRole = getProviderRoleByUuid(id);
			
		}
		
		return providerRole;
	}
	
	public static List<Provider> getProviders(List<String> providerRoleIds, boolean returnAllIfNoRolesSpecified) {
		return getProviders(providerRoleIds, returnAllIfNoRolesSpecified, false);
	}
	
	public static List<Provider> getProviders(List<String> providerRoleIds, boolean returnAllIfNoRolesSpecified,
	        boolean includeRetired) {
		if (providerRoleIds == null || providerRoleIds.isEmpty()) {
			if (returnAllIfNoRolesSpecified) {
				return getAllProviders(includeRetired);
			}
			return new ArrayList<Provider>();
		} else {
			List providerRoles = new ArrayList();
			for (String providerRoleId : providerRoleIds) {
				Object providerRole = getProviderRole(providerRoleId);
				if (providerRole == null) {
					throw new IllegalArgumentException("Unable to find provider role: " + providerRoleId);
				}
				providerRoles.add(providerRole);
			}
			return getProviders(providerRoles);
		}
	}
	
	public static List<Provider> getAllProviders(boolean includeRetired) {
		return Context.getProviderService().getAllProviders(includeRetired);
	}
	
	private static Object getProviderRoleById(Integer providerRoleId) {
		
		// we have to fetch the provider role by reflection, since the provider management module is not a required dependency
		
		try {
			Class<?> providerManagementServiceClass = Context
			        .loadClass("org.openmrs.module.providermanagement.api.ProviderManagementService");
			Object providerManagementService = Context.getService(providerManagementServiceClass);
			Method getProviderRole = providerManagementServiceClass.getMethod("getProviderRole", Integer.class);
			return getProviderRole.invoke(providerManagementService, providerRoleId);
		}
		catch (Exception e) {
			throw new RuntimeException(
			        "Unable to get provider role by id; the Provider Management module needs to be installed if using the providerRoles attribute",
			        e);
		}
		
	}
	
	private static Object getProviderRoleByUuid(String providerRoleUuid) {
		
		// we have to fetch the provider roles by reflection, since the provider management module is not a required dependency
		
		try {
			Class<?> providerManagementServiceClass = Context
			        .loadClass("org.openmrs.module.providermanagement.api.ProviderManagementService");
			Object providerManagementService = Context.getService(providerManagementServiceClass);
			Method getProviderRoleByUuid = providerManagementServiceClass.getMethod("getProviderRoleByUuid", String.class);
			return getProviderRoleByUuid.invoke(providerManagementService, providerRoleUuid);
		}
		catch (Exception e) {
			throw new RuntimeException(
			        "Unable to get provider role by uuid; the Provider Management module needs to be installed if using the providerRoles attribute",
			        e);
		}
		
	}
	
	public static List<Provider> getProviders(List<ProviderRole> providerRoles) {
		
		if (providerRoles == null || providerRoles.size() == 0) {
			return new ArrayList<Provider>();
		}
		
		ProviderManagementService providerManagementService = Context.getService(ProviderManagementService.class);
		//Service returns list of org.openmrs.module.providermanagement.Provider, not org.openmrs.Provider
		return new ArrayList<Provider>(providerManagementService.getProvidersByRoles(providerRoles));
	}
	
	/**
	 * @return the provider with the given id, where the id can be either the primary key id or uuid of
	 *         the provider If id passed in is "currentuser", then return the first provider record
	 *         associated with the currently authenticated user
	 */
	public static Provider getProvider(String id) {
		Provider provider = null;
		if ("currentUser".equals(id)) {
			User currentUser = Context.getAuthenticatedUser();
			if (currentUser != null) {
				Collection<Provider> candidates = Context.getProviderService().getProvidersByPerson(currentUser.getPerson());
				if (candidates.size() > 0) {
					provider = candidates.iterator().next();
				}
			}
		} else {
			try {
				provider = Context.getProviderService().getProvider(Integer.valueOf(id));
			}
			catch (Exception ex) {
				provider = Context.getProviderService().getProviderByUuid(id);
			}
		}
		return provider;
	}
	
	/**
	 * Given a provider, returns the provider name by first looking at the name of any associated person
	 * and, if none, falling back to provider.getName()
	 * 
	 * @param provider
	 * @return
	 */
	public static String getProviderName(Provider provider) {
		if (provider != null) {
			return provider.getPerson() != null
			        ? HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(provider.getPerson().getPersonName())
			        : provider.getName();
		}
		return "";
	}
	
	/**
	 * Convenience method to get all the names of this PersonName and concatonating them together with
	 * family name compoenents first, separated by a comma from given and middle names. If any part of
	 * {@link PersonName#getPrefix()}, {@link PersonName#getGivenName()},
	 * {@link PersonName#getMiddleName()}, etc are null, they are not included in the returned name
	 *
	 * @return all of the parts of this {@link PersonName} joined with spaces <strong>Should</strong>
	 *         not put spaces around an empty middle name
	 */
	public static String getFullNameWithFamilyNameFirst(PersonName personName) {
		
		if (personName == null) {
			return "[" + Context.getMessageSourceService().getMessage("htmlformentry.unknownProviderName") + "]";
		}
		
		StringBuffer nameString = new StringBuffer();
		
		if (StringUtils.isNotBlank(personName.getFamilyNamePrefix())) {
			nameString.append(personName.getFamilyNamePrefix() + " ");
		}
		if (StringUtils.isNotBlank(personName.getFamilyName())) {
			nameString.append(personName.getFamilyName() + " ");
		}
		if (StringUtils.isNotBlank(personName.getFamilyName2())) {
			nameString.append(personName.getFamilyName2() + " ");
		}
		if (StringUtils.isNotBlank(personName.getFamilyNameSuffix())) {
			nameString.append(personName.getFamilyNameSuffix() + " ");
		}
		
		if (nameString.length() > 0) {
			nameString.deleteCharAt(nameString.length() - 1); // delete trailing space
			nameString.append(", ");
		}
		
		if (StringUtils.isNotBlank(personName.getPrefix())) {
			nameString.append(personName.getPrefix() + " ");
		}
		if (StringUtils.isNotBlank(personName.getGivenName())) {
			nameString.append(personName.getGivenName() + " ");
		}
		if (StringUtils.isNotBlank(personName.getMiddleName())) {
			nameString.append(personName.getMiddleName() + " ");
		}
		if (StringUtils.isNotBlank(personName.getDegree())) {
			nameString.append(personName.getDegree() + " ");
		}
		
		if (nameString.length() > 1) {
			nameString.deleteCharAt(nameString.length() - 1); // delete trailing space
		}
		
		return nameString.toString();
	}
	
	/**
	 * Converts a collection of providers domain object into simple stub representation
	 *
	 * @param providers
	 * @return
	 */
	public static List<ProviderStub> getProviderStubs(Collection<Provider> providers) {
		List<ProviderStub> providerStubList = new LinkedList<ProviderStub>();
		if (providers != null && !providers.isEmpty()) {
			for (Provider p : providers) {
				providerStubList.add(new ProviderStub(p));
			}
		}
		return providerStubList;
	}
	
	public static List<ProviderStub> getProviderStubs(Collection<Provider> providers, String searchParam, MatchMode mode) {
		if (mode != null && StringUtils.isNotBlank(searchParam)) {
			ProviderTransformer transformer = new ProviderTransformer();
			switch (mode) {
				case START:
					return (List<ProviderStub>) transformer.transform(providers, startsWith(searchParam));
				case END:
					return (List<ProviderStub>) transformer.transform(providers, endsWith(searchParam));
				default:
					return (List<ProviderStub>) transformer.transform(providers, contains(searchParam));
			}
		}
		return getProviderStubs(providers);
	}
	
	private static Predicate<Provider> startsWith(final String param) {
		return new Predicate<Provider>() {
			
			@Override
			public boolean test(Provider provider) {
				String searchParam = param.toLowerCase();
				for (String f : getProviderFieldsToSearch(provider)) {
					if (f.startsWith(searchParam)) {
						return true;
					}
				}
				return false;
			}
		};
	}
	
	private static Predicate<Provider> endsWith(final String param) {
		return new Predicate<Provider>() {
			
			@Override
			public boolean test(Provider provider) {
				String searchParam = param.toLowerCase();
				for (String f : getProviderFieldsToSearch(provider)) {
					if (f.endsWith(searchParam)) {
						return true;
					}
				}
				return false;
			}
		};
	}
	
	private static Predicate<Provider> contains(final String param) {
		return new Predicate<Provider>() {
			
			@Override
			public boolean test(Provider provider) {
				String searchParam = param.toLowerCase();
				for (String f : getProviderFieldsToSearch(provider)) {
					if (f.contains(searchParam)) {
						return true;
					}
				}
				return false;
			}
		};
	}
	
	private static List<String> getProviderFieldsToSearch(Provider provider) {
		List<String> ret = new ArrayList<String>();
		String identifier = provider.getIdentifier();
		if (StringUtils.isNotBlank(identifier)) {
			ret.add(provider.getIdentifier().toLowerCase().trim());
		}
		Person person = provider.getPerson();
		if (person != null) {
			PersonName pn = person.getPersonName();
			if (pn != null) {
				if (StringUtils.isNotBlank(pn.getGivenName())) {
					ret.add(pn.getGivenName().toLowerCase().trim());
				}
				if (StringUtils.isNotBlank(pn.getMiddleName())) {
					ret.add(pn.getMiddleName().toLowerCase().trim());
				}
				if (StringUtils.isNotBlank(pn.getFamilyName())) {
					ret.add(pn.getFamilyName().toLowerCase().trim());
				}
			}
		}
		if (StringUtils.isNotBlank(provider.getName())) {
			for (String n : provider.getName().split(" ")) {
				ret.add(n.toLowerCase().trim());
			}
		}
		return ret;
	}
	
}
