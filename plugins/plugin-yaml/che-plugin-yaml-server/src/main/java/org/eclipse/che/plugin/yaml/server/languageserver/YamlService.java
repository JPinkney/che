package org.eclipse.che.plugin.yaml.server.languageserver;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageServer;

import javax.ws.rs.*;

@Path("yaml")
public class YamlService {

    @POST
    @Path("schemas")
    public void putSchemas() throws ApiException {
        //LanguageServer yamlLS = YamlLanguageServerLauncher.getYamlLanguageServer();
        //Endpoint endpoint = ServiceEndpoints.toEndpoint(yamlLS);
        //YamlSchemaAssociations serviceObject = ServiceEndpoints.toServiceObject(endpoint, YamlSchemaAssociations.class);
        //Map<String, String[]> associations = new HashMap<>();

//        for(Map.Entry<String, ArrayList<String>> schema : schemas.entrySet()){
//            associations.put(schema.getKey(), schema.getValue().toArray(new String[schema.getValue().size()]));
//        }

//        associations.put("/.bower.yaml", new String[]{"http://json.schemastore.org/bower"});
//        associations.put("/.bowerrc.yaml", new String[]{"http://json.schemastore.org/bowerrc"});
//        associations.put("/composer.yaml", new String[]{"https://getcomposer.org/schema.json"});
//        associations.put("/package.yaml", new String[]{"http://json.schemastore.org/package"});
//        associations.put("/jsconfig.yaml", new String[]{"http://json.schemastore.org/jsconfig"});
//        associations.put("/tsconfig.yaml", new String[]{"http://json.schemastore.org/tsconfig"});
//
//        serviceObject.yamlSchemaAssociation(associations);
    }

    @GET
    @Path("test")
    public String getSchemas(){
        System.out.println("testing");
        return "Testing the route";
    }

    @GET
    @Path("test2")
    public LanguageServer getSchemas2(){
        LanguageServer yamlLS = YamlLanguageServerLauncher.getYamlLanguageServer();
        Endpoint endpoint = ServiceEndpoints.toEndpoint(yamlLS);
        YamlSchemaAssociations serviceObject = ServiceEndpoints.toServiceObject(endpoint, YamlSchemaAssociations.class);
        return yamlLS;
    }

    @GET
    @Path("test3")
    public String getSchemas3   (){
        return "Updated";
    }


}
