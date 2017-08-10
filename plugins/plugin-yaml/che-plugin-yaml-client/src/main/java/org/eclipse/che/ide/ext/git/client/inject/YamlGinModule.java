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
package org.eclipse.che.ide.ext.git.client.inject;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Singleton;
import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.preferences.PreferencePagePresenter;
import org.eclipse.che.ide.ext.git.client.preferences.YamlExtensionManagerPresenter;
import org.eclipse.che.ide.ext.git.client.preferences.YamlExtensionManagerView;
import org.eclipse.che.ide.ext.git.client.preferences.YamlExtensionManagerViewImpl;

/** @author Andrey Plotnikov */
@ExtensionGinModule
public class YamlGinModule extends AbstractGinModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bind(YamlExtensionManagerView.class).to(YamlExtensionManagerViewImpl.class).in(Singleton.class);
        GinMultibinder<PreferencePagePresenter> prefBinder = GinMultibinder.newSetBinder(binder(), PreferencePagePresenter.class);
        prefBinder.addBinding().to(YamlExtensionManagerPresenter.class);
    }
}
