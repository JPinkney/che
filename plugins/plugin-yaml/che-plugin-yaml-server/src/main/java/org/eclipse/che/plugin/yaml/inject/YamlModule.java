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
package org.eclipse.che.plugin.yaml.inject;

import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.yaml.languageserver.YamlLanguageServerLauncher;
import org.eclipse.che.plugin.yaml.languageserver.YamlService;

/**
 * @author Anatolii Bazko
 */
@DynaModule
public class YamlModule extends AbstractModule {
    public static final String   LANGUAGE_ID = "yaml";
    private static final String[] EXTENSIONS  = new String[]{"yaml", "yml"};
    private static final String MIME_TYPE  = "text/yaml";

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), LanguageServerLauncher.class).addBinding().to(YamlLanguageServerLauncher.class);
        LanguageDescription description = new LanguageDescription();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(LANGUAGE_ID);
        description.setMimeType(MIME_TYPE);
        Multibinder.newSetBinder(binder(), LanguageDescription.class).addBinding().toInstance(description);

        //bind(YamlService.class);

    }
} 
