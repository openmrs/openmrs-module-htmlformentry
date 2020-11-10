package org.openmrs.module.htmlformentry;

import java.util.Date;

import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

public class TestObsValue {
	
	public Integer conceptId; // required
	
	public Object value; // can be null
	
	public TestObsValue(Integer cId, Object val) {
		conceptId = cId;
		value = val;
	}
	
	@Override
	public String toString() {
		return conceptId + "->" + value;
	}
	
	public boolean matches(Obs obs) {
		
		if (!obs.getConcept().getConceptId().equals(conceptId)) {
			return false;
		}
		String valueAsString = null;
		if (value instanceof Date) {
			valueAsString = TestUtil.formatYmd((Date) value);
		} else {
			valueAsString = TestUtil.valueAsStringHelper(value);
		}
		return OpenmrsUtil.nullSafeEquals(valueAsString, obs.getValueAsString(Context.getLocale()));
	}
}
