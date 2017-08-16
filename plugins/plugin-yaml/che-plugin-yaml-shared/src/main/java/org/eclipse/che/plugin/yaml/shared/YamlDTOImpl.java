package org.eclipse.che.plugin.yaml.shared;

import java.util.Map;

/**
 * Created by jpinkney on 15/08/17.
 */
public class YamlDTOImpl implements YamlDTO {

    private Map<String, String> schemas;

    @Override
    public Map<String, String> getSchemas() {
        return this.schemas;
    }

    @Override
    public void setSchemas(Map<String, String> schemas) {
        this.schemas = schemas;
    }

    @Override
    public YamlDTO withSchemas(Map<String, String> schemas) {
        this.schemas = schemas;
        return this;
    }

}
