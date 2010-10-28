package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicService;
import org.openmrs.logic.result.Result;


public class VelocityFunctions {
	
	private FormEntrySession session;
	private ObsService obsService;
	private LogicService logicService;
	
	public VelocityFunctions(FormEntrySession session) {
		this.session = session;
		obsService = Context.getObsService();
		logicService = Context.getLogicService();
	}
	
	public List<Obs> allObs(Integer conceptId) {
		Patient p = session.getPatient();
		if (p == null)
			return new ArrayList<Obs>();
		else
			return obsService.getObservationsByPersonAndConcept(p, new Concept(conceptId));
	}
	
	public Obs latestObs(Integer conceptId) {
		List<Obs> obs = allObs(conceptId);
		if (obs == null || obs.isEmpty())
			return null;
		else
			return obs.get(obs.size() - 1);
	}
	
	public Obs earliestObs(Integer conceptId) {
		List<Obs> obs = allObs(conceptId);
		if (obs == null || obs.isEmpty())
			return null;
		else
			return obs.get(0);
	}
	
	public Result logic(String expression) {
		LogicCriteria lc = logicService.parse(expression);
		return logicService.eval(session.getPatient(), lc);
	}

}
