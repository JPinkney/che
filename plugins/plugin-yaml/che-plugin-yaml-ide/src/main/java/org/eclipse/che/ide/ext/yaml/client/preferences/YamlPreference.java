/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.yaml.client.preferences;

public class YamlPreference {

    private final String url;
    private final String glob;

    public YamlPreference(String url, String glob){
        this.url = url;
        this.glob = glob;
    }

    public String getUrl(){
        return this.url;
    }

    public String getGlob(){
        return this.glob;
    }

}
