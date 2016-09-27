package org.openmrs.module.htmlformentry;

import org.openmrs.OpenmrsMetadata;
import org.openmrs.api.context.Context;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class MetadataMappingResolver  {


    public <T extends OpenmrsMetadata> T getMetadataItem(Class<T> type, String identifier) {
        if(identifier.contains(":")) {
            int index = identifier.indexOf(":");
            String metadataSourceName = identifier.substring(0, index).trim();
            String metadataTermCode = identifier.substring(index + 1, identifier.length()).trim();
            try {
                getClass().getClassLoader().loadClass("org.openmrs.module.metadatamapping.api.MetadataMappingService");

                return Context.getService(MetadataMappingService.class).getMetadataItem(type, metadataSourceName, metadataTermCode);
            } catch (ClassNotFoundException e) {
                //MetadataMappingService not installed
            }
        }
        return null;
    }
}
