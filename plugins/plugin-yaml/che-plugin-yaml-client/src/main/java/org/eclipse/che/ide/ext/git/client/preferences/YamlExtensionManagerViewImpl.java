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
package org.eclipse.che.ide.ext.git.client.preferences;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ui.cellview.CellTableResources;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The implementation of {@link YamlExtensionManagerView}.
 *
 * @author Andrey Plotnikov
 */
@Singleton
public class YamlExtensionManagerViewImpl extends Composite implements YamlExtensionManagerView {
    interface YamlExtensionManagerViewImplUiBinder extends UiBinder<Widget, YamlExtensionManagerViewImpl> {}

    private static YamlExtensionManagerViewImplUiBinder uiBinder = GWT.create(YamlExtensionManagerViewImplUiBinder.class);

    @UiField
    Button addUrl;
    @UiField(provided = true)
    CellTable<YamlPreference> keys;
    private ActionDelegate delegate;

    @Inject
    protected YamlExtensionManagerViewImpl(CellTableResources res) {
        initYamlExtensionTable(res);
        initWidget(uiBinder.createAndBindUi(this));
    }

    /** Creates table what contains list of available yaml keys. */
    private void initYamlExtensionTable(final CellTable.Resources res) {
        keys = new CellTable<>(20, res);
        Column<YamlPreference, String> hostColumn = new Column<YamlPreference, String>(new EditTextCell()) {
            @Override
            public String getValue(YamlPreference object) {
                return object.getUrl();
            }

            @Override
            public void render(Context context, YamlPreference object, SafeHtmlBuilder sb) {
                sb.appendHtmlConstant("<div id=\"" + UIObject.DEBUG_ID_PREFIX + "-preferences-cellTable-url-" + context.getIndex() + "\">");
                super.render(context, object, sb);
            }
        };
        hostColumn.setSortable(true);

        Column<YamlPreference, String> publicKeyColumn = new Column<YamlPreference, String>(new EditTextCell()) {
            @Override
            public String getValue(YamlPreference object) {
                return object.getGlob();
            }

            @Override
            public void render(Context context, YamlPreference object, SafeHtmlBuilder sb) {
                sb.appendHtmlConstant("<div id=\"" + UIObject.DEBUG_ID_PREFIX + "-preferences-cellTable-glob-" + context.getIndex() + "\">");
                if (object != null) {
                    super.render(context, object, sb);
                }
            }
        };

        Column<YamlPreference, String> deleteKeyColumn = new Column<YamlPreference, String>(new ButtonCell()) {
            @Override
            public String getValue(YamlPreference object) {
                return "Delete";
            }

            @Override
            public void render(Context context, YamlPreference object, SafeHtmlBuilder sb) {
                sb.appendHtmlConstant("<div id=\"" + UIObject.DEBUG_ID_PREFIX + "-preferences-cellTable-delete-" + context.getIndex() + "\">");
                super.render(context, object, sb);
            }
        };

        // Creates handler on button clicked
        deleteKeyColumn.setFieldUpdater(new FieldUpdater<YamlPreference, String>() {
            @Override
            public void update(int index, YamlPreference object, String value) {
                delegate.onDeleteClicked(object);
            }
        });

        keys.addColumn(hostColumn, "Url");
        keys.addColumn(publicKeyColumn, "Glob");
        keys.addColumn(deleteKeyColumn, "Delete");
        keys.setWidth("100%", true);
        keys.setColumnWidth(hostColumn, 40, Style.Unit.PCT);
        keys.setColumnWidth(publicKeyColumn, 35, Style.Unit.PCT);
        keys.setColumnWidth(deleteKeyColumn, 20, Style.Unit.PCT);

        // don't show loading indicator
        keys.setLoadingIndicator(null);
    }

    /** {@inheritDoc} */
    @Override
    public void setPairs(@NotNull List<YamlPreference> pairs) {
        this.keys.setRowData(pairs);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("addUrl")
    public void onAddUrlClicked(ClickEvent event) {
        delegate.onAddUrlClicked();
    }

}
