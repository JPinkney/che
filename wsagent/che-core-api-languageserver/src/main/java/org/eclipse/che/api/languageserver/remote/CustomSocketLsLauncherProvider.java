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
package org.eclipse.che.api.languageserver.remote;

import static java.util.Collections.emptySet;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.Runtime;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.model.workspace.runtime.Machine;
import org.eclipse.che.api.core.model.workspace.runtime.Server;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.slf4j.Logger;

/** Provides socket based language server launchers */
@Singleton
class CustomSocketLsLauncherProvider implements RemoteLsLauncherProvider {
  private static final Logger LOG = getLogger(SocketLsLauncherProvider.class);

  private final LsConfigurationDetector lsConfigurationDetector;
  private final LsConfigurationExtractor lsConfigurationExtractor;
  private final Set<CustomSocketLanguageServerLauncher> customSocketLanguageServerLaunchers;

  private final Map<String, LanguageServerLauncher> lslRegistry = new ConcurrentHashMap<>();

  @Inject
  public CustomSocketLsLauncherProvider(
      LsConfigurationDetector lsConfigurationDetector,
      LsConfigurationExtractor lsConfigurationExtractor,
      Set<CustomSocketLanguageServerLauncher> customSocketLanguageServerLauncher) {
    this.lsConfigurationDetector = lsConfigurationDetector;
    this.lsConfigurationExtractor = lsConfigurationExtractor;
    this.customSocketLanguageServerLaunchers = customSocketLanguageServerLauncher;
  }

  @Override
  public Set<LanguageServerLauncher> getAll(Workspace workspace) {
    Runtime runtime = workspace.getRuntime();
    if (runtime == null) {
      return emptySet();
    }

    for (Map.Entry<String, ? extends Machine> machineEntry : runtime.getMachines().entrySet()) {
      String machineName = machineEntry.getKey();
      Machine machine = machineEntry.getValue();
      Map<String, ? extends Server> servers = machine.getServers();

      for (Map.Entry<String, ? extends Server> serverEntry : servers.entrySet()) {
        String serverName = serverEntry.getKey();
        Server server = serverEntry.getValue();
        String serverUrl = server.getUrl();
        Map<String, String> serverAttributes = server.getAttributes();

        if (lslRegistry.keySet().contains(machineName + serverName)) {
          continue;
        }

        if (!lsConfigurationDetector.isDetected(serverAttributes)) {
          continue;
        }

        LanguageServerDescription description = lsConfigurationExtractor.extract(serverAttributes);

        try {
          URI uri = new URI(serverUrl);
          String host = uri.getHost();
          int port = uri.getPort();

          // Change the port and uri for the custom ones
          for (CustomSocketLanguageServerLauncher c : customSocketLanguageServerLaunchers) {
            if (c.getHost().equals(null) && c.getPort() == 0) {
              c.setHost(host);
              c.setPort(port);
            }
          }

        } catch (URISyntaxException e) {
          LOG.error("Can't parse server url: {}", serverUrl, e);
        }
      }
    }

    Set<LanguageServerLauncher> l = new HashSet<>();
    for (CustomSocketLanguageServerLauncher c : customSocketLanguageServerLaunchers) {
      l.add(c);
    }

    return l;
  }
}
