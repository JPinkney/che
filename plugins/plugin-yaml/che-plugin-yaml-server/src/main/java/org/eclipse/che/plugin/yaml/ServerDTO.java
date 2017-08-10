package org.eclipse.che.plugin.yaml;

import org.eclipse.che.dto.shared.DTO;
import org.eclipse.lsp4j.services.LanguageServer;

@DTO
public interface ServerDTO {
    LanguageServer getYamlLanguageServer();
}