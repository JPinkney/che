package org.eclipse.che.api.languageserver.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.launcher.LaunchingStrategy;
import org.eclipse.che.api.languageserver.launcher.PerWorkspaceLaunchingStrategy;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class SocketLanguageServerLauncher implements LanguageServerLauncher {

  private final LanguageServerDescription languageServerDescription;
  private final String host;
  private final int port;

  SocketLanguageServerLauncher(
      LanguageServerDescription languageServerDescription, String host, int port) {
    this.languageServerDescription = languageServerDescription;
    this.host = host;
    this.port = port;
  }

  public SocketLanguageServerLauncher() {
    this.languageServerDescription = null;
    this.host = null;
    this.port = 0;
  }

  @Override
  public LanguageServer launch(String projectPath, LanguageClient client)
      throws LanguageServerException {
    try {
      Socket socket = new Socket(host, port);
      socket.setKeepAlive(true);
      InputStream inputStream = socket.getInputStream();
      OutputStream outputStream = socket.getOutputStream();

      Launcher<LanguageServer> launcher =
          Launcher.createLauncher(client, LanguageServer.class, inputStream, outputStream);

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
}
