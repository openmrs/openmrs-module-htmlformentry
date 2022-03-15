package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FormEntryContext2_3 extends FormEntryContext {
	
	public FormEntryContext2_3(Mode mode) {
		super(mode);
	}
	
	/**
	 * Returns the Obs from the current {@see ObsGroup} with the specified concept and controlId
	 *
	 * @param controlId The control id, eg "my_condition_tag"
	 * @return the Obs from the current {@see ObsGroup} with the specified concept and answer concept
	 */
	public Obs getObsFromCurrentGroup(String controlId) {
		return Optional.ofNullable(currentObsGroupMembers).orElse(Collections.emptyList()).stream()
		        .filter(obs -> StringUtils.equals(HtmlFormEntryUtil2_3.getControlId(obs), controlId))
		        .collect(Collectors.reducing((obs1, obs2) -> {
			        throw new IllegalStateException("Multiple obs are matching the control id '" + controlId + "'.");
		        })).orElse(null);
	}
	
	/**
	 * Returns the Obs with the specified concept and controlId
	 *
	 * @param controlId The control id, eg "my_condition_tag"
	 * @return the Obs from the current {@see ObsGroup} with the specified concept and answer concept
	 */
	public Obs getObsFromExistingObs(Concept concept, String controlId) {
		List<Obs> obsList = existingObs.get(concept);
		
		return Optional.ofNullable(obsList).orElse(Collections.emptyList()).stream()
		        .filter(obs -> StringUtils.equals(HtmlFormEntryUtil2_3.getControlId(obs), controlId))
		        .collect(Collectors.reducing((obs1, obs2) -> {
			        throw new IllegalStateException("Multiple obs are matching the control id '" + controlId + "'.");
		        })).orElse(null);
	}
	
	/**
	 * Returns the Obs with the specified controlId
	 *
	 * @param controlId The control id, eg "my_condition_tag"
	 * @return the Obs from the current {@see ObsGroup} with the specified concept and answer concept
	 */
	public Obs getObsFromExistingObs(String controlId) {
		
		return existingObs.entrySet().stream().map(entry -> {
			return entry.getValue().stream()
			        .filter(obs -> StringUtils.equals(HtmlFormEntryUtil2_3.getControlId(obs), controlId))
			        .collect(Collectors.reducing((obs1, obs2) -> {
				        throw new IllegalStateException("Multiple obs are matching the control id '" + controlId + "'.");
			        })).orElse(null);
		}).filter(obs -> obs != null).collect(Collectors.reducing((obs1, obs2) -> {
			throw new IllegalStateException("Multiple obs are matching the control id '" + controlId + "'.");
		})).orElse(null);
		
	}
}
