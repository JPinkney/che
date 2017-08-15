package org.eclipse.che.ide.ext.yaml.ide;

import org.eclipse.che.api.promises.client.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface YamlServiceClient {
    Promise<Void> putSchemas();
}
