package org.eclipse.che.plugin.yaml.server.languageserver;

import com.google.inject.Inject;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.plugin.yaml.shared.YamlDTO;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageServer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("yaml")
public class YamlService {

    @POST
    @Path("schemas")
    @Consumes(MediaType.APPLICATION_JSON)
    public void putSchemas(Map<String, String> schemas) throws ApiException {
        System.out.println(schemas);
        YamlDTO dto = DtoFactory.getInstance().createDto(YamlDTO.class);
        System.out.println(dto);
        System.out.println(dto.getSchemas());
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
    @Path("test3")
    public String getSchemas3   (){
        return "Updated 2";
    }


}
