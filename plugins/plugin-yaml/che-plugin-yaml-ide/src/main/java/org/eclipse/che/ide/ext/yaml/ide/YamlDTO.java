package org.eclipse.che.ide.ext.yaml.ide;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;


@DTO
public interface YamlDTO {
    /**
     * Returns name service that will use yaml DTO.
     */
    String getSchemas();

    void setSchemas(Map<String, String> schemas);

    YamlDTO withSchemas(String service);
}
