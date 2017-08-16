package org.eclipse.che.plugin.yaml.server.languageserver;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.plugin.yaml.shared.YamlDTO;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageServer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Path("yaml")
public class YamlService {

    @POST
    @Path("schemas")
    @Consumes(MediaType.APPLICATION_JSON)
    public void putSchemas(YamlDTO yamlDto) throws ApiException {

        Map<String, String> schemas = yamlDto.getSchemas();

        LanguageServer yamlLS = YamlLanguageServerLauncher.getYamlLanguageServer();
        Endpoint endpoint = ServiceEndpoints.toEndpoint(yamlLS);
        YamlSchemaAssociations serviceObject = ServiceEndpoints.toServiceObject(endpoint, YamlSchemaAssociations.class);
        Map<String, String[]> associations = new HashMap<>();

        for(Map.Entry<String, String> schema : schemas.entrySet()){
            associations.put(schema.getKey(), new Gson().fromJson(schema.getValue(), String[].class));
        }

        associations.put("/bower.yaml", new String[]{"http://json.schemastore.org/bower.json"});
        associations.put("/composer.yaml", new String[]{"https://getcomposer.org/schema.json"});
        associations.put("/news.yaml", new String[]{"http://json.schemastore.org/ninjs.json"});
        associations.put("/config.yaml", new String[]{"http://json.schemastore.org/config"});

        serviceObject.yamlSchemaAssociation(associations);
    }

}
