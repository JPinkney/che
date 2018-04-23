/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.java.languageserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncherTemplate;
import org.eclipse.che.api.languageserver.registry.DocumentFilter;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.che.api.languageserver.registry.ServerInitializerObserver;
import org.eclipse.che.api.languageserver.service.FileContentAccess;
import org.eclipse.che.api.languageserver.util.DynamicWrapper;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Evgen Vidolob
 * @author Anatolii Bazko
 * @author Thomas Mäder
 */
@Singleton
public class JavaLanguageServerTemplate extends LanguageServerLauncherTemplate
        implements ServerInitializerObserver {
    private static final Logger LOG = LoggerFactory.getLogger(JavaLanguageServerTemplate.class);

    private static final LanguageServerDescription DESCRIPTION = createServerDescription();

    @Override
    public boolean isAbleToLaunch() {
        return false;
    }

    public void sendStatusReport(StatusReport report) {
        LOG.info("{}: {}", report.getType(), report.getMessage());
    }

    /**
     * The show message notification is sent from a server to a client to ask the client to display a
     * particular message in the user interface.
     *
     * @param report information about report
     */
    public void sendProgressReport(ProgressReport report) {
        processorJsonRpcCommunication.sendProgressNotification(report);
    }

    public LanguageServerDescription getDescription() {
        return DESCRIPTION;
    }

    private static LanguageServerDescription createServerDescription() {
        LanguageServerDescription description =
                new LanguageServerDescription(
                        "org.eclipse.che.plugin.java.languageserver",
                        Arrays.asList("javaSource", "javaClass"),
                        Arrays.asList(
                                new DocumentFilter(null, null, "jdt"), new DocumentFilter(null, null, "chelib")),
                        Arrays.asList(
                                "glob:**/*.java",
                                "glob:**/pom.xml",
                                "glob:**/*.gradle",
                                "glob:**/.project",
                                "glob:**/.classpath",
                                "glob:**/settings/*.prefs"));
        return description;
    }

    @Override
    public void onServerInitialized(
            LanguageServerLauncher launcher,
            LanguageServer server,
            ServerCapabilities capabilities,
            String rootPath) {
        Map<String, String> settings =
                Collections.singletonMap("java.configuration.updateBuildConfiguration", "automatic");
        server.getWorkspaceService().didChangeConfiguration(new DidChangeConfigurationParams(settings));
    }
}
