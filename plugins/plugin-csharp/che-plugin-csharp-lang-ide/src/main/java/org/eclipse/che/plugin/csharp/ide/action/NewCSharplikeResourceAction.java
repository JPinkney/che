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
package org.eclipse.che.plugin.csharp.ide.action;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.newresource.AbstractNewResourceAction;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Base class for ne C# resource
 *
 * Show/hide action depend on project type
 *
 * @author Anatolii Bazko
 */
public abstract class NewCSharplikeResourceAction extends AbstractNewResourceAction {

    protected final AppContext appContext;

    /**
     * Creates new action.
     *
     * @param title
     *         action's title
     * @param description
     *         action's description
     * @param svgIcon
     */
    public NewCSharplikeResourceAction(String title,
                                       String description,
                                       SVGResource svgIcon,
                                       DialogFactory dialogFactory,
                                       CoreLocalizationConstant coreLocalizationConstant,
                                       EventBus eventBus,
                                       AppContext appContext,
                                       NotificationManager notificationManager) {
        super(title, description, svgIcon, dialogFactory, coreLocalizationConstant, eventBus, appContext, notificationManager);
        this.appContext = appContext;
    }
}
