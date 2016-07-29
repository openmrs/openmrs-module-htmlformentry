package org.openmrs.module.htmlformentry;

import org.openmrs.OpenmrsMetadata;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.context.Context;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
@OpenmrsProfile(modules = {"metadatamapping:1.1.*"})
public class MetadataMappingServiceWrapperImpl implements MetadataMappingServiceWrapper{

    public <T extends OpenmrsMetadata> T getMetadataItem(Class<T> type, String metadataSourceName, String metadataTermCode) {
        return Context.getService(MetadataMappingService.class).getMetadataItem(type, metadataSourceName, metadataTermCode);
    }

}