package org.openmrs.module.htmlformentry;

import org.openmrs.customdatatype.CustomDatatypeHandler;
import org.openmrs.customdatatype.datatype.LongFreeTextDatatype;

public class TestDataTypeHandler implements CustomDatatypeHandler<LongFreeTextDatatype, String> {
	
	@Override
	public void setHandlerConfiguration(String s) {
		
	}
}
