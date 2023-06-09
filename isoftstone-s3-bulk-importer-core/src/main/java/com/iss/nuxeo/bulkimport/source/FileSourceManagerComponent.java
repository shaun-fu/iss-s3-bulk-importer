package com.iss.nuxeo.bulkimport.source;

import com.iss.nuxeo.bulkimport.ImportContext;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.HashMap;
import java.util.Map;

public class FileSourceManagerComponent extends DefaultComponent implements FileSourceManager {
    protected Map<String, FileSourceProvider> providers = new HashMap<String, FileSourceProvider>();
    protected static final String XP = "configuration";

    @Override
    public void deactivate(ComponentContext context) {

        super.deactivate(context);

        providers.clear();
    }


    @Override
    public FileSourceProvider getFileSourceProvider(String id) {
        FileSourceProvider provider = providers.get(id);

        if(provider != null) {
            return provider;
        }

        FileSourceProviderDescriptor desc = this.getDescriptor(XP, id);
        if(desc != null) {
            provider = createProvider(desc);
        }

        return provider;
    }

    @Override
    public FileSourceProvider getFileSourceProvider(ImportContext context) {
        return getFileSourceProvider(DEFAULT_PROVIDER);
    }

    protected FileSourceProvider createProvider(FileSourceProviderDescriptor desc) {
        Class<?> klass = desc.getProviderClass();
        Map<String, String> properties = desc.getProperties();

        FileSourceProvider provider;
        try {
            if (FileSourceProvider.class.isAssignableFrom(klass)) {
                @SuppressWarnings("unchecked")
                Class<? extends FileSourceProvider> providerClass = (Class<? extends FileSourceProvider>) klass;
                provider = providerClass.getDeclaredConstructor().newInstance();
            } else {
                throw new RuntimeException("Unknown class for file source provider: " + klass);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        try {
            provider.initialize(desc.getId(), properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        providers.put(desc.getId(), provider);

        return provider;
    }
}
