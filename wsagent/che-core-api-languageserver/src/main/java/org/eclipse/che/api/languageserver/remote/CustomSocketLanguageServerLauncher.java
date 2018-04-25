package org.eclipse.che.api.languageserver.remote;

import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;

public interface CustomSocketLanguageServerLauncher extends LanguageServerLauncher {
  /** Allow the CustomSocketLanguageServerLauncher port to be set by the workspace configuration */
  void setPort(int port);

  /** Allow the CustomSocketLanguageServerLauncher host to be set by the workspace configuration */
  void setHost(String host);

  /** Allow to get the CustomSocketLanguageServerLauncher port */
  int getPort();

  /** Allow to get the CustomSocketLanguageServerLauncher host */
  String getHost();

  /** Allow to get the LanguageDescription */
  LanguageServerDescription getLanguageDescription();
}
