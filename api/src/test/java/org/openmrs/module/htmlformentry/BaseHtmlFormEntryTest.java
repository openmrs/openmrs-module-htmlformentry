package org.openmrs.module.htmlformentry;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.openmrs.module.ModuleUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsClassLoader;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.context.ContextConfiguration;
import org.xml.sax.InputSource;

@ContextConfiguration(locations = { "classpath:applicationContext-service.xml", "classpath*:TestingApplicationContext.xml",
        "classpath*:moduleApplicationContext.xml" }, inheritLocations = false)
public abstract class BaseHtmlFormEntryTest extends BaseModuleContextSensitiveTest {
	
	static Map<String, IDataSet> cachedDataSets = new HashMap<>();
	
	public void executeVersionedDataSet(String datasetName) throws Exception {
		IDataSet dataSet = cachedDataSets.get(datasetName);
		if (dataSet == null) {
			InputStream is = OpenmrsClassLoader.getInstance().getResourceAsStream(datasetName);
			String contents;
			try {
				contents = IOUtils.toString(is, "UTF-8");
			}
			catch (Exception e) {
				throw new RuntimeException("Unable to load " + datasetName, e);
			}
			
			// Change "precise" to "allow_decimal"
			contents = updateDataSetContents(contents);
			
			StringReader reader = new StringReader(contents);
			InputSource inputSource = new InputSource(reader);
			FlatXmlProducer fxp = new FlatXmlProducer(inputSource, false, true, false);
			FlatXmlDataSet fxds = new FlatXmlDataSet(fxp);
			
			ReplacementDataSet replacementDataSet = new ReplacementDataSet(fxds);
			replacementDataSet.addReplacementObject("[NULL]", (Object) null);
			dataSet = replacementDataSet;
			reader.close();
			
			cachedDataSets.put(datasetName, dataSet);
		}
		executeDataSet(dataSet);
	}
	
	protected boolean shouldApplyChangesFor22() {
		String currentVersion = OpenmrsConstants.OPENMRS_VERSION_SHORT;
		return ModuleUtil.compareVersion(currentVersion, "2.2") >= 0;
	}
	
	protected String updateDataSetContents(String contents) {
		if (shouldApplyChangesFor22()) {
			contents = contents.replace("precise=", "allow_decimal=");
		}
		return contents;
	}
}
