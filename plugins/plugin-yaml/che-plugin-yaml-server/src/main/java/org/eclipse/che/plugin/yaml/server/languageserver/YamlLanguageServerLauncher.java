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
package org.eclipse.che.plugin.yaml.server.languageserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncherTemplate;
import org.eclipse.che.api.languageserver.registry.DocumentFilter;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.che.api.languageserver.registry.ServerInitializerObserver;
import org.eclipse.che.plugin.yaml.server.inject.YamlModule;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Evgen Vidolob
 * @author Anatolii Bazko
 */
@Singleton
public class YamlLanguageServerLauncher extends LanguageServerLauncherTemplate implements ServerInitializerObserver {

    private static final Logger LOG = LoggerFactory.getLogger(YamlLanguageServerLauncher.class);

    private static final String                    REGEX       = ".*\\.(yaml|yml)";
    private static final LanguageServerDescription DESCRIPTION = createServerDescription();
    private static LanguageServer yamlLanguageServer;

    private final Path launchScript;

    @Inject
    public YamlLanguageServerLauncher() {
        launchScript = Paths.get(System.getenv("HOME"), "che/ls-yaml/launch.sh");
    }

    @Override
    public boolean isAbleToLaunch() {
        return Files.exists(launchScript);
    }

    protected LanguageServer connectToLanguageServer(final Process languageServerProcess, LanguageClient client) {
        Launcher<LanguageServer> launcher = Launcher.createLauncher(client, LanguageServer.class,
                                                                    languageServerProcess.getInputStream(),
                                                                    languageServerProcess.getOutputStream());
        launcher.startListening();
        setYamlLanguageServer(launcher.getRemoteProxy());
        return launcher.getRemoteProxy();
    }

    protected Process startLanguageServerProcess(String projectPath) throws LanguageServerException {
        ProcessBuilder processBuilder = new ProcessBuilder(launchScript.toString());
        processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new LanguageServerException("Can't start YAML language server", e);
        }
    }

    protected static LanguageServer getYamlLanguageServer(){
        return yamlLanguageServer;
    }

    protected static void setYamlLanguageServer(LanguageServer yamlLanguageServer){
        YamlLanguageServerLauncher.yamlLanguageServer = yamlLanguageServer;
    }

    @Override
    public void onServerInitialized(LanguageServerLauncher launcher,
                                    LanguageServer server,
                                    ServerCapabilities capabilities,
                                    String projectPath) {
//        Endpoint endpoint = ServiceEndpoints.toEndpoint(server);
//        YamlSchemaAssociations serviceObject = ServiceEndpoints.toServiceObject(endpoint, YamlSchemaAssociations.class);
//        Map<String, String[]> associations = new HashMap<String, String[]>();
//        associations.put("/*.schema.yaml", new String[]{"http://json-schema.org/draft-04/schema#"});
//        associations.put("/bower.yaml", new String[]{"http://json.schemastore.org/bower"});
//        serviceObject.yamlSchemaAssociation(associations);
    }

    public LanguageServerDescription getDescription() {
        return DESCRIPTION;
    }

    private static LanguageServerDescription createServerDescription() {
        LanguageServerDescription description = new LanguageServerDescription("org.eclipse.che.plugin.yaml.server.languageserver", null,
                        Arrays.asList(new DocumentFilter(YamlModule.LANGUAGE_ID, REGEX, null)));
        return description;
    }
}
