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

import static org.eclipse.che.plugin.java.languageserver.JavaLanguageServerLauncher.createServerDescription;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Proxy;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.che.api.languageserver.remote.CustomSocketLanguageServerLauncher;
import org.eclipse.che.api.languageserver.remote.SocketLanguageServerLauncher;
import org.eclipse.che.api.languageserver.service.FileContentAccess;
import org.eclipse.che.api.languageserver.util.DynamicWrapper;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This launcher allows jdt.ls to be wrapped by the jdt.ls extension when launched via socket
 *
 * @author Joshua Pinkney
 */
@Singleton
public class JavaSocketLanguageServerLauncher extends SocketLanguageServerLauncher
    implements CustomSocketLanguageServerLauncher {
  private static final Logger LOG = LoggerFactory.getLogger(JavaSocketLanguageServerLauncher.class);
  private static final LanguageServerDescription DESCRIPTION = createServerDescription();
  private final ProcessorJsonRpcCommunication processorJsonRpcCommunication;

  @Inject
  public JavaSocketLanguageServerLauncher(
      ProcessorJsonRpcCommunication processorJsonRpcCommunication) {
    super(JavaLanguageServer.class, DESCRIPTION, "", 0);
    this.processorJsonRpcCommunication = processorJsonRpcCommunication;
  }

  @Override
  public boolean isAbleToLaunch() {
    return true;
  }

  @Override
  public LanguageServer launch(String projectPath, LanguageClient client)
      throws LanguageServerException {

    Object javaLangClient =
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] {LanguageClient.class, JavaLanguageClient.class},
            new DynamicWrapper(this, client));
    JavaLanguageServer proxy = super.launch(projectPath, (LanguageClient) javaLangClient);
    LanguageServer wrapped =
        (LanguageServer)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {LanguageServer.class, FileContentAccess.class},
                new DynamicWrapper(new JavaLSWrapper(proxy), proxy));
    return wrapped;
  }

  public LanguageServerDescription getDescription() {
    return DESCRIPTION;
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
}
