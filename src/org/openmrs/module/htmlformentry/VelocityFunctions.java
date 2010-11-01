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
import org.openmrs.logic.result.EmptyResult;
import org.openmrs.logic.result.Result;


public class VelocityFunctions {
	
	private FormEntrySession session;
	private ObsService obsService;
	private LogicService logicService;
	
	public VelocityFunctions(FormEntrySession session) {
		this.session = session;
	}
	
	private ObsService getObsService() {
		if (obsService == null)
			obsService = Context.getObsService();
		return obsService;
	}
	
	private LogicService getLogicService() {
		if (logicService == null)
			logicService = Context.getLogicService();
		return logicService;
	}
	
	private void cannotBePreviewed() {
		if ("testing-html-form-entry".equals(session.getPatient().getUuid()))
			throw new CannotBePreviewedException();
    }

	public List<Obs> allObs(Integer conceptId) {
		if (session.getPatient() == null)
			return new ArrayList<Obs>();
		cannotBePreviewed();
		Patient p = session.getPatient();
		if (p == null)
			return new ArrayList<Obs>();
		else
			return getObsService().getObservationsByPersonAndConcept(p, new Concept(conceptId));
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
		if (session.getPatient() == null)
			return new EmptyResult();
		cannotBePreviewed();
		LogicCriteria lc = getLogicService().parse(expression);
		return getLogicService().eval(session.getPatient(), lc);
	}

}
