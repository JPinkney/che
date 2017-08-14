package org.eclipse.che.ide.ext.yaml.ide;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;

import java.util.ArrayList;
import java.util.Map;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;

@Singleton
public class YamlServiceClientImpl implements YamlServiceClient {
    private static final String SCHEMAS = "/api/yaml/schemas";

    private final LoaderFactory loaderFactory;
    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader loader;

    @Inject
    public YamlServiceClientImpl(LoaderFactory loaderFactory,
                                    AsyncRequestFactory asyncRequestFactory){
        this.loaderFactory = loaderFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loaderFactory.newLoader();
    }

    @Override
    public Promise<Void> putSchemas(Map<String, ArrayList<String>> schemas) {

        //Do to json

        return asyncRequestFactory.createPostRequest(SCHEMAS, null).loader(loader).header(CONTENTTYPE, APPLICATION_JSON).send();
    }
}
