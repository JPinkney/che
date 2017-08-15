package org.eclipse.che.plugin.yaml.ide;

import org.eclipse.che.api.promises.client.Promise;

public interface YamlServiceClient {
    Promise<Void> putSchemas();
}
