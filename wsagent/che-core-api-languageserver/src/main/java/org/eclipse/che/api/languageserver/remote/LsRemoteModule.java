/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.languageserver.remote;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class LsRemoteModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<RemoteLsLauncherProvider> remoteLSBinder =
        Multibinder.newSetBinder(binder(), RemoteLsLauncherProvider.class);
    remoteLSBinder.addBinding().to(SocketLsLauncherProvider.class);
    remoteLSBinder.addBinding().to(CustomSocketLsLauncherProvider.class);
  }
}
