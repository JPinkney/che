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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.Arrays;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.registry.DocumentFilter;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.che.api.languageserver.remote.CustomSocketLanguageServerLauncher;
import org.eclipse.che.api.languageserver.remote.SocketLanguageServerLauncher;
import org.eclipse.che.api.languageserver.service.FileContentAccess;
import org.eclipse.che.api.languageserver.util.DynamicWrapper;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JavaSocketSocketLanguageServerLauncher extends SocketLanguageServerLauncher
    implements CustomSocketLanguageServerLauncher {
  private static final Logger LOG = LoggerFactory.getLogger(JavaLanguageServerLauncher.class);
  private int port;
  private String host;

  private static final LanguageServerDescription DESCRIPTION = createServerDescription();

  private ProcessorJsonRpcCommunication processorJsonRpcCommunication;

  @Inject
  public JavaSocketSocketLanguageServerLauncher(
      ProcessorJsonRpcCommunication processorJsonRpcCommunication) {
    super();
    this.processorJsonRpcCommunication = processorJsonRpcCommunication;
  }

  @Override
  public boolean isAbleToLaunch() {
    return true;
  }

  @Override
  public LanguageServer launch(String projectPath, LanguageClient client)
      throws LanguageServerException {
    try {
      Socket socket = new Socket(host, port);
      socket.setKeepAlive(true);
      InputStream inputStream = socket.getInputStream();
      OutputStream outputStream = socket.getOutputStream();

      Object javaLangClient =
          Proxy.newProxyInstance(
              getClass().getClassLoader(),
              new Class[] {LanguageClient.class, JavaLanguageClient.class},
              new DynamicWrapper(this, client));

      Launcher<JavaLanguageServer> launcher =
          Launcher.createLauncher(
              javaLangClient, JavaLanguageServer.class, inputStream, outputStream);
      launcher.startListening();
      JavaLanguageServer proxy = launcher.getRemoteProxy();
      LanguageServer wrapped =
          (LanguageServer)
              Proxy.newProxyInstance(
                  getClass().getClassLoader(),
                  new Class[] {LanguageServer.class, FileContentAccess.class},
                  new DynamicWrapper(new JavaLSWrapper(proxy), proxy));
      return wrapped;
    } catch (IOException e) {
      throw new LanguageServerException(
          "Can't launch language server for project: " + projectPath, e);
    }
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

  public void setPort(int port) {
    this.port = port;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public String getHost() {
    return this.host;
  }

  @Override
  public LanguageServerDescription getLanguageDescription() {
    return this.DESCRIPTION;
  }
}
