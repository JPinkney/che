package org.eclipse.che.plugin.yaml.ide;

import org.eclipse.che.api.promises.client.Promise;

import java.util.Map;

public interface YamlServiceClient {
    Promise<Void> putSchemas(Map<String, String> schemas);
}
