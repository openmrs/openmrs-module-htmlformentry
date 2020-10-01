package org.openmrs.module.htmlformentry.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptSearchResult;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.propertyeditor.ConceptClassEditor;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HtmlFormSearchController {
	
	@Autowired
	private ConceptService conceptService;
	
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(ConceptClass.class, new ConceptClassEditor());
		binder.registerCustomEditor(Concept.class, new ConceptEditor());
	}
	
	/**
	 * Concept Search
	 */
	@RequestMapping("/module/htmlformentry/conceptSearch")
	public void conceptSearch(ModelMap model, HttpServletRequest request, HttpServletResponse response,
	        @RequestParam(required = true, value = "term") String query,
	        @RequestParam(required = false, value = "answerids") String allowedconceptids,
	        @RequestParam(required = false, value = "answerclasses") String answerclasses,
	        @RequestParam(required = false, value = "answerSetIds") String answerSetIds) throws Exception {
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		
		List<Locale> l = new Vector<Locale>();
		l.add(Context.getLocale());
		
		List<ConceptClass> cptClassList = new ArrayList<ConceptClass>();
		Set<Integer> allowedConceptsIdSet = new HashSet<Integer>();
		if (!"null".equals(allowedconceptids) && !"".equals(allowedconceptids)) {
			// we filter this by conceptids
			for (StringTokenizer st = new StringTokenizer(allowedconceptids, ","); st.hasMoreTokens();) {
				allowedConceptsIdSet.add(Integer.parseInt(st.nextToken()));
			}
		} else if (!"null".equals(answerclasses) && !"".equals(answerclasses)) {
			for (StringTokenizer st = new StringTokenizer(answerclasses, ","); st.hasMoreTokens();) {
				cptClassList.add(conceptService.getConceptClassByName(st.nextToken()));
			}
		} else if (!"null".equals(answerSetIds) && !"".equals(answerSetIds)) {
			for (StringTokenizer st = new StringTokenizer(answerSetIds, ","); st.hasMoreTokens();) {
				Concept answerConceptSet = HtmlFormEntryUtil.getConcept(st.nextToken());
				List<Concept> answerConcepts = conceptService.getConceptsByConceptSet(answerConceptSet);
				for (Concept conceptInSet : answerConcepts) {
					allowedConceptsIdSet.add(conceptInSet.getConceptId());
				}
			}
		} else {
			throw new Exception("You must specify either answerconceptids, answerclasses, or answerSetIds");
		}
		
		List<ConceptSearchResult> results = conceptService.getConcepts(query, l, false, cptClassList, null, null, null, null,
		    null, null);
		if (!allowedConceptsIdSet.isEmpty()) {
			for (Iterator<ConceptSearchResult> it = results.iterator(); it.hasNext();) {
				if (!allowedConceptsIdSet.contains(it.next().getConcept().getConceptId())) {
					it.remove();
				}
			}
		}
		
		// return in JSON object list format
		//[ { "id": "Dromas ardeola", "label": "Crab-Plover", "value":"Crab-Plover" },
		out.print("[");
		for (Iterator<ConceptSearchResult> i = results.iterator(); i.hasNext();) {
			ConceptSearchResult res = i.next();
			String ds = res.getConcept().getDisplayString();
			out.print("{ \"value\":\"");
			if (res.getConceptName().isPreferred() || res.getConceptName().getName().equalsIgnoreCase(ds)) {
				out.print(WebUtil.escapeQuotes(res.getConceptName().getName()));
			} else {
				out.print(WebUtil.escapeQuotes(res.getConcept().getDisplayString()));
			}
			out.print("\",\"id\"");
			out.print(":\"" + res.getConcept().getId());
			out.print("\"}");
			if (i.hasNext())
				out.print(",");
		}
		out.print("]");
	}
	
	@RequestMapping("/module/htmlformentry/drugSearch")
	public void drugSearch(@RequestParam("term") String query, HttpServletResponse response) throws IOException {
		
		List<Drug> drugs;
		
		// we want to use a later API method from 1.8+ if it is available, so we need to access it via reflection
		if (OpenmrsConstants.OPENMRS_VERSION_SHORT.startsWith("1.6")
		        || OpenmrsConstants.OPENMRS_VERSION_SHORT.startsWith("1.7")) {
			drugs = conceptService.getDrugs(query); // this method returns retired drugs, so it is not ideal
		} else {
			try {
				Object conceptService = Context.getService(Context.loadClass("org.openmrs.api.ConceptService"));
				Method getDrugsMethod = conceptService.getClass().getMethod("getDrugs", String.class, Concept.class,
				    boolean.class, boolean.class, boolean.class, Integer.class, Integer.class);
				
				drugs = (List<Drug>) getDrugsMethod.invoke(conceptService, query, null, true, false, true, 0, 100); // this method excludes retired drugs
				
			}
			catch (Exception ex) {
				throw new RuntimeException("Unable to access ConceptService getDrugs method via reflection", ex);
			}
		}
		
		List<Map<String, Object>> simplified = simplify(drugs);
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		
		new ObjectMapper().writeValue(out, simplified);
	}
	
	public List<Map<String, Object>> simplify(List<Drug> drugs) {
		List<Map<String, Object>> simplified = new ArrayList<Map<String, Object>>();
		Locale locale = Context.getLocale();
		for (Drug drug : drugs) {
			Map<String, Object> item = new LinkedHashMap<String, Object>();
			item.put("id", drug.getId());
			item.put("name", drug.getName());
			item.put("retired", drug.getRetired().booleanValue());
			if (drug.getDosageForm() != null) {
				item.put("dosageForm", drug.getDosageForm().getName(locale).getName());
			}
			item.put("combination", drug.getCombination());
			if (drug.getConcept() != null) {
				item.put("concept", drug.getConcept().getName(locale).getName());
			}
			simplified.add(item);
		}
		return simplified;
	}
}
