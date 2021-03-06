/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.websocket;

import static com.google.gwt.user.client.Window.Location.getHost;
import static com.google.gwt.user.client.Window.Location.getProtocol;

/**
 * The implementation of {@link MessageBus}.
 *
 * @author Artem Zatsarynnyi
 * @author Dmitry Shnurenko
 */
public class MessageBusImpl extends AbstractMessageBus {

  public MessageBusImpl() {
    super(
        (getProtocol().equals("https:") ? "wss://" : "ws://")
            + getHost()
            + getRestContext()
            + "/ws");
  }

  private static native String getRestContext() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.restContext;
        } else {
            return null;
        }
    }-*/;
}
