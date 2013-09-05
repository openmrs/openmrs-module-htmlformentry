package org.openmrs.module.htmlformentry.web.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptWord;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.ConceptClassEditor;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.web.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

@Controller
public class HtmlFormSearchController {

    @Autowired
    private ConceptService conceptService;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(ConceptClass.class,
				new ConceptClassEditor());
		binder.registerCustomEditor(Concept.class, new ConceptEditor());
	}

	/**
	 * Concept Search
	 */
	@RequestMapping("/module/htmlformentry/conceptSearch")
	public void conceptSearch(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(required = true, value = "term") String query,
			@RequestParam(required = false, value = "answerids") String allowedconceptids,
			@RequestParam(required = false, value = "answerclasses") String answerclasses)
			throws Exception {

		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

		List<Locale> l = new Vector<Locale>();
		l.add(Context.getLocale());

		List<ConceptClass> cptClassList = new ArrayList<ConceptClass>();
		HashSet<Integer> set = new HashSet<Integer>();
		if ( !"null".equals(allowedconceptids) && !"".equals(allowedconceptids)) {
			// we filter this by conceptids
			for (StringTokenizer st = new StringTokenizer(allowedconceptids,
					","); st.hasMoreTokens();) {
				set.add(Integer.parseInt(st.nextToken()));
			}
		} else if (!"null".equals(answerclasses)&& !"".equals(answerclasses)) {
			for (StringTokenizer st = new StringTokenizer(answerclasses, ","); st
					.hasMoreTokens();) {
				cptClassList.add(conceptService
						.getConceptClassByName(st.nextToken()));
			}
		} else {
			throw new Exception(
					"answerconceptids set and answerclasses are both empty.");
		}
		List<ConceptWord> words = conceptService.getConceptWords(
                query, l, false, cptClassList, null, null, null, null, null,
                null);
		if (!set.isEmpty()) {
			for (Iterator<ConceptWord> it = words.iterator(); it.hasNext();) {
				if (!set.contains(it.next().getConcept().getConceptId())) {
					it.remove();
				}
			}
		}
		
		// return in JSON object list format
		//[ { "id": "Dromas ardeola", "label": "Crab-Plover", "value":"Crab-Plover" },
		out.print("[");
		for (Iterator<ConceptWord> i = words.iterator(); i.hasNext();) {
			ConceptWord w = i.next();
			String ds = w.getConcept().getDisplayString();
			out.print("{ \"value\":\"");
			if (w.getConceptName().isPreferred()
					|| w.getConceptName().getName().equalsIgnoreCase(ds)) {
				out.print(WebUtil.escapeQuotes(w.getConceptName().getName()));
			} else {
				out.print(WebUtil.escapeQuotes(w.getConcept().getDisplayString()));
			}
			out.print("\",\"id\"");
			out.print(":\"" + w.getConcept().getId());
			out.print("\"}");
			if (i.hasNext())
				out.print(",");
		}
		out.print("]");
	}

    @RequestMapping("/module/htmlformentry/drugSearch")
    public void localizedMessage(@RequestParam("term") String query,
                                 HttpServletResponse response) throws IOException {

        // this call would be better but it's from a later core version:
        // List<Drug> drugs = conceptService.getDrugs(query, null, true, true, false, 0, 100);
        List<Drug> drugs = conceptService.getDrugs(query);
        List<Map<String, Object>> simplified = simplify(drugs);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        new ObjectMapper().writeValue(out, simplified);
    }

    private List<Map<String, Object>> simplify(List<Drug> drugs) {
        List<Map<String, Object>> simplified = new ArrayList<Map<String, Object>>();
        Locale locale = Context.getLocale();
        for (Drug drug : drugs) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", drug.getId());
            item.put("name", drug.getName());
            if (drug.getDosageForm() != null) {
                item.put("dosageForm", drug.getDosageForm().getName(locale).getName());
            }
            if (drug.getRoute() != null) {
                item.put("route", drug.getRoute().getName(locale).getName());
            }
            item.put("doseStrength", drug.getDoseStrength());
            item.put("units", drug.getUnits());
            item.put("combination", drug.getCombination());
            if (drug.getConcept() != null) {
                item.put("concept", drug.getConcept().getName(locale).getName());
            }
            simplified.add(item);
        }
        return simplified;
    }

}