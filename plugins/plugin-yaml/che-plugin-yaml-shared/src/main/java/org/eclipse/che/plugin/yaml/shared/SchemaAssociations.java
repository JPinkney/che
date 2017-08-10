/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.yaml.shared;

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageServer;

import java.util.HashMap;
import java.util.Map;

public class SchemaAssociations {

    public static LanguageServer yamlLanguageServer;

    public static void setYamlLanguageServer(LanguageServer yamlLS){
        yamlLanguageServer = yamlLS;
    }

    public static void updateServerSchemas(){
        Map<String, String> newSchemas = DtoFactory.getInstance().createDto(YamlDTO.class).getSchemas();
        Endpoint endpoint = ServiceEndpoints.toEndpoint(yamlLanguageServer);
        YamlSchemaAssociations serviceObject = ServiceEndpoints.toServiceObject(endpoint, YamlSchemaAssociations.class);

        Map<String, String[]> testMap = new HashMap<String, String[]>();
        for(Map.Entry<String, String> test : newSchemas.entrySet()){
            testMap.put(test.getKey(), new String[]{test.getValue()});
        }

        serviceObject.yamlSchemaAssociation(testMap);

    }

}
