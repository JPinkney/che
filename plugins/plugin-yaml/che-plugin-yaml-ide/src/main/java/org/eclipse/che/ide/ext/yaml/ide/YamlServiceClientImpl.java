package org.eclipse.che.ide.ext.yaml.ide;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;

import java.util.Map;

@Singleton
public class YamlServiceClientImpl implements YamlServiceClient {
    private static final String SCHEMAS = "/yaml/schemas";

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
    public Promise<Void> putSchemas(Map<String, String[]> schemas) {

        //Do to json

        return asyncRequestFactory.createPostRequest(SCHEMAS, null).loader(loader).send();
    }
}
