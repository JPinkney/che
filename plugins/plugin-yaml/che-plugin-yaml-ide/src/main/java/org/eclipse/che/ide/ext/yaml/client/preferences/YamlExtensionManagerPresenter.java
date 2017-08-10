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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.api.dialogs.CancelCallback;
import org.eclipse.che.ide.api.dialogs.ConfirmCallback;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.dialogs.InputCallback;
import org.eclipse.che.ide.api.preferences.AbstractPreferencePagePresenter;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.ext.yaml.client.YamlLocalizationConstant;
import org.eclipse.che.ide.json.JsonHelper;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * The presenter for managing ssh keys.
 *
 * @author Evgen Vidolob
 * @author Sergii Leschenko
 */
@Singleton
public class YamlExtensionManagerPresenter extends AbstractPreferencePagePresenter implements YamlExtensionManagerView.ActionDelegate {

    private final String preferenceName = "yaml.preferences";
    private DialogFactory              dialogFactory;
    private YamlExtensionManagerView   view;
    private PreferencesManager preferencesManager;
    private List<YamlPreference> yamlPreferences;
    private YamlLocalizationConstant ylc;
    //private DtoFactory dto;
    private boolean dirty = false;

    @Inject
    public YamlExtensionManagerPresenter(YamlExtensionManagerView view,
                                         DialogFactory dialogFactory,
                                         PreferencesManager preferencesManager,
                                         YamlLocalizationConstant ylc) {
        super("Yaml", "Language Server Settings");
        this.view = view;
        this.dialogFactory = dialogFactory;
        this.view.setDelegate(this);
        this.ylc = ylc;
        //this.dto = dto;
        this.preferencesManager = preferencesManager;
        if(preferencesManager.getValue(preferenceName) == null){
            this.yamlPreferences = new ArrayList<YamlPreference>();
        }else{
            this.yamlPreferences = jsonToYamlPreference();
        }

    }

    /** {@inheritDoc} */
    public void onDeleteClicked(@NotNull final YamlPreference pairKey) {
        dialogFactory.createConfirmDialog(ylc.deleteUrl(), "Delete the url",
                new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        deleteKey(pairKey);
                        refreshTable();
                        setSchemas();
                        dirty = true;
                        delegate.onDirtyChanged();
                    }
                },
                getCancelCallback()).show();

    }

    private CancelCallback getCancelCallback() {
        return new CancelCallback() {
            @Override
            public void cancelled() {
                //for now do nothing but it need for tests
            }
        };
    }

    private void deleteKey(final YamlPreference key) {
        this.yamlPreferences.remove(key);
    }

    /** {@inheritDoc} */
    public void onAddUrlClicked() {
        dialogFactory.createInputDialog(ylc.addUrlText(),
                "Url",
                new InputCallback() {
                    @Override
                    public void accepted(String url) {
                        addUrl(url);
                        refreshTable();
                        setSchemas();
                        dirty = true;
                        delegate.onDirtyChanged();
                    }
                },
                getCancelCallback())
                .show();
    }

    private void setSchemas(){
        Map<String, String> schemaMap = new HashMap<String, String>();
        schemaMap.put("/kubernetes.yaml", "http://central.maven.org/maven2/io/fabric8/kubernetes-model/1.1.0/kubernetes-model-1.1.0-schema.json");
        //dto.createDto(YamlDTO.class).setSchemas(schemaMap);
        //SchemaAssociations.updateServerSchemas();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirty() {
        return dirty;
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        refreshTable();
        container.setWidget(view);
    }

    private void addUrl(String url){
        YamlPreference pref = new YamlPreference(url, "/*");
        this.yamlPreferences.add(pref);
    }

    /** Refresh ssh keys. */
    private void refreshTable() {
        view.setPairs(this.yamlPreferences);
    }

    private Map<String, String> yamlPreferenceToMap(List<YamlPreference> pref){
        Map<String, String> preferenceMap = new HashMap<String, String>();

        ListIterator<YamlPreference> prefItr = pref.listIterator();
        while(prefItr.hasNext()){
            YamlPreference currPref = prefItr.next();
            preferenceMap.put(currPref.getUrl(), currPref.getGlob());
        }

        return preferenceMap;
    }

    private List<YamlPreference> jsonToYamlPreference(){
        Map<String, String> jsonPreferenceMap = new <String, String>HashMap(JsonHelper.toMap(preferencesManager.getValue(preferenceName)));
        ArrayList yamlPreferences = new ArrayList<YamlPreference>();

        for(Map.Entry<String, String> entry : jsonPreferenceMap.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();

            YamlPreference newPref = new YamlPreference(key, value);
            yamlPreferences.add(newPref);
        }

        return yamlPreferences;
    }

    @Override
    public void storeChanges() {
        preferencesManager.setValue(this.preferenceName, JsonHelper.toJson(yamlPreferenceToMap(yamlPreferences)));
        dirty = false;
        delegate.onDirtyChanged();
    }

    @Override
    public void revertChanges() {
        preferencesManager.getValue(this.preferenceName);
        dirty = false;
        delegate.onDirtyChanged();
    }

}