package org.eclipse.che.api.languageserver.remote;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.annotation.PreDestroy;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.launcher.LaunchingStrategy;
import org.eclipse.che.api.languageserver.launcher.PerWorkspaceLaunchingStrategy;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;

/** Generic socket launcher for when language servers are started over sockets */
public class SocketLanguageServerLauncher implements LanguageServerLauncher {

  private static final Logger LOG = getLogger(SocketLanguageServerLauncher.class);

  private final LanguageServerDescription languageServerDescription;
  private String host;
  private int port;
  private Socket socket;
  private Class customLanguageServerClass;

  protected SocketLanguageServerLauncher(
      Class customLanguageServerClass,
      LanguageServerDescription languageServerDescription,
      String host,
      int port) {
    this.languageServerDescription = languageServerDescription;
    this.host = host;
    this.port = port;
    this.customLanguageServerClass = customLanguageServerClass;
  }

  @PreDestroy
  public void closeSocket() {
    try {
      socket.close();
    } catch (IOException e) {
      LOG.warn("Socket has not been closed successfully: " + e);
    }
  }

  @Override
  public <T extends LanguageServer> T launch(String projectPath, LanguageClient client)
      throws LanguageServerException {
    try {
      socket = new Socket(host, port);
      socket.setKeepAlive(true);
      InputStream inputStream = socket.getInputStream();
      OutputStream outputStream = socket.getOutputStream();

      Launcher<T> launcher =
          Launcher.createLauncher(client, customLanguageServerClass, inputStream, outputStream);

      launcher.startListening();
      return launcher.getRemoteProxy();
    } catch (IOException e) {
      throw new LanguageServerException(
          "Can't launch language server for project: " + projectPath, e);
    }
  }

  @Override
  public LaunchingStrategy getLaunchingStrategy() {
    return PerWorkspaceLaunchingStrategy.INSTANCE;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public LanguageServerDescription getDescription() {
    return languageServerDescription;
  }

  @Override
  public boolean isAbleToLaunch() {
    return host != null && languageServerDescription != null;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setHost(String host) {
    this.host = host;
  }
}
