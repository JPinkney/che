package org.eclipse.che.plugin.yaml.ide;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;
import org.eclipse.che.plugin.yaml.shared.YamlDTO;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;

@Singleton
public class YamlServiceClientImpl implements YamlServiceClient {

    private final LoaderFactory loaderFactory;
    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader loader;
    private final AppContext appContext;
    private final DtoFactory dtoFactory;

    @Inject
    public YamlServiceClientImpl(LoaderFactory loaderFactory,
                                 AsyncRequestFactory asyncRequestFactory,
                                 AppContext appContext,
                                 DtoFactory dtoFactory){
        this.loaderFactory = loaderFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loaderFactory.newLoader();
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public Promise<Void> putSchemas() {
        YamlDTO schemaAddition = dtoFactory.createDto(YamlDTO.class);
        String schemasLocation = getWsAgentBaseUrl() + "/api/yaml/schemas";
        return asyncRequestFactory.createPostRequest(schemasLocation, null).loader(loader).header(CONTENTTYPE, APPLICATION_JSON).send();
    }

    private String getWsAgentBaseUrl() {
        return appContext.getDevMachine().getWsAgentBaseUrl();
    }

}
