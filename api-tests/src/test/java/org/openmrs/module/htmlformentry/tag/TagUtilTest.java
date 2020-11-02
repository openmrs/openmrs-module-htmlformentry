package org.openmrs.module.htmlformentry.tag;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterRole;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.Provider;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;

public class TagUtilTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void parseValue_shouldReturnNullIfValueIsNull() {
		assertThat(TagUtil.parseValue(null, Object.class), nullValue());
	}
	
	@Test
	public void parseValue_shouldParseString() {
		assertThat(TagUtil.parseValue("Hello World", String.class), is("Hello World"));
	}
	
	@Test
	public void parseValue_shouldParseInteger() {
		assertThat(TagUtil.parseValue("12", Integer.class), is(12));
	}
	
	@Test
	public void parseValue_shouldParseDouble() {
		assertThat(TagUtil.parseValue("12.5", Double.class), is(12.5));
	}
	
	@Test
	public void parseValue_shouldParseBoolean() {
		assertThat(TagUtil.parseValue("true", Boolean.class), is(Boolean.TRUE));
	}
	
	@Test
	public void parseValue_shouldParseLocale() {
		assertThat(TagUtil.parseValue("en_US", Locale.class), is(Locale.US));
	}
	
	@Test
	public void parseValue_shouldParseConcept() {
		assertThat(TagUtil.parseValue("28", Concept.class).getConceptId(), is(28));
	}
	
	@Test
	public void parseValue_shouldParseDrug() {
		assertThat(TagUtil.parseValue("2", Drug.class).getId(), is(2));
	}
	
	@Test
	public void parseValue_shouldParseOrderFrequency() {
		assertThat(TagUtil.parseValue("1", OrderFrequency.class).getId(), is(1));
	}
	
	@Test
	public void parseValue_shouldParseOrderType() {
		assertThat(TagUtil.parseValue("1", OrderType.class).getId(), is(1));
	}
	
	@Test
	public void parseValue_shouldParseDate() {
		Date d = TagUtil.parseValue("2020-05-25", Date.class);
		assertThat(new SimpleDateFormat("yyyy-MM-dd").format(d), is("2020-05-25"));
	}
	
	@Test
	public void parseValue_shouldParseClass() {
		assertThat(TagUtil.parseValue("org.openmrs.Concept", Class.class), is(Concept.class));
	}
	
	@Test
	public void parseValue_shouldParseDrugOrder() {
		assertThat(TagUtil.parseValue("111", DrugOrder.class).getId(), is(111));
	}
	
	@Test
	public void parseValue_shouldParseEncounterRole() {
		assertThat(TagUtil.parseValue("1", EncounterRole.class).getId(), is(1));
	}
	
	@Test
	public void parseValue_shouldParseProvider() {
		assertThat(TagUtil.parseValue("1", Provider.class).getId(), is(1));
	}
	
	@Test
	public void parseValue_shouldParseEnum() {
		assertThat(TagUtil.parseValue("DISCONTINUE", Order.Action.class), is(Order.Action.DISCONTINUE));
	}
	
	@Test
	public void parseValue_shouldThrowIllegalArgumentExceptionForInvalidValue() {
		Exception e = null;
		try {
			TagUtil.parseValue("Twenty", Date.class);
		}
		catch (Exception exception) {
			e = exception;
		}
		assertThat(e, notNullValue());
		assertThat(e.getClass(), is(IllegalArgumentException.class));
	}
}
