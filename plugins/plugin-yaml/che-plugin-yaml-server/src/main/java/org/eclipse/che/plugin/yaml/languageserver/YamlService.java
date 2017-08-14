package org.eclipse.che.plugin.yaml.languageserver;

import org.eclipse.che.api.core.ApiException;
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
    public void putSchemas(Map<String, ArrayList<String>> schemas) throws ApiException {
        LanguageServer yamlLS = YamlLanguageServerLauncher.getYamlLanguageServer();
        Endpoint endpoint = ServiceEndpoints.toEndpoint(yamlLS);
        YamlSchemaAssociations serviceObject = ServiceEndpoints.toServiceObject(endpoint, YamlSchemaAssociations.class);
        Map<String, String[]> associations = new HashMap<>();

        for(Map.Entry<String, ArrayList<String>> schema : schemas.entrySet()){
            associations.put(schema.getKey(), schema.getValue().toArray(new String[schema.getValue().size()]));
        }

        serviceObject.yamlSchemaAssociation(associations);
    }

    @GET
    @Path("test")
    public String getSchemas(){
        System.out.println("testing");
        return "Testing the route";
    }


}
