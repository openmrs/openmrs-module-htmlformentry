package org.openmrs.htmlformentry;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsClassLoader;
import org.xml.sax.InputSource;

public abstract class BaseHtmlFormEntry23Test extends BaseModuleContextSensitiveTest {
	
	static Map<String, IDataSet> cachedDataSets = new HashMap<>();
	
	public void executeDataSetFor23(String datasetName) throws Exception {
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
			contents = contents.replace("precise=", "allow_decimal=");
			
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
}
