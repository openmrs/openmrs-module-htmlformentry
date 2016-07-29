package org.openmrs.module.htmlformentry;

import org.openmrs.OpenmrsMetadata;

/**
 *
 */
public interface MetadataMappingServiceWrapper {

    public <T extends OpenmrsMetadata> T getMetadataItem(Class<T> type, String metadataSourceName, String metadataTermCode);
}
