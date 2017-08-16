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
package org.eclipse.che.plugin.yaml.ide.preferences;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import org.eclipse.che.ide.api.dialogs.CancelCallback;
import org.eclipse.che.ide.api.dialogs.ConfirmCallback;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.dialogs.InputCallback;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.preferences.AbstractPreferencePagePresenter;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.plugin.yaml.ide.YamlLocalizationConstant;
import org.eclipse.che.plugin.yaml.ide.YamlServiceClient;
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
    private YamlServiceClient service;
    private NotificationManager notificationManager;
    private boolean dirty = false;

    @Inject
    public YamlExtensionManagerPresenter(YamlExtensionManagerView view,
                                         DialogFactory dialogFactory,
                                         PreferencesManager preferencesManager,
                                         YamlLocalizationConstant ylc,
                                         YamlServiceClient service,
                                         NotificationManager notificationManager) {
        super("Yaml", "Language Server Settings");
        this.view = view;
        this.dialogFactory = dialogFactory;
        this.view.setDelegate(this);
        this.notificationManager = notificationManager;
        this.ylc = ylc;
        this.service = service;
        this.preferencesManager = preferencesManager;
        if(preferencesManager.getValue(preferenceName) == null || preferencesManager.getValue(preferenceName) == ""){
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
        Map<String, List<String>> schemaMap = yamlPreferenceToMap(this.yamlPreferences);

        Map<String, String> jsonSchemaMap = new HashMap<String, String>();
        for(Map.Entry<String, List<String>> entry : schemaMap.entrySet()){
            jsonSchemaMap.put(entry.getKey(), toJSON(entry.getValue()));
        }

        service.putSchemas(jsonSchemaMap);
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

    private Map<String, List<String>> yamlPreferenceToMap(List<YamlPreference> pref){
        Map<String, List<String>> preferenceMap = new HashMap<String, List<String>>();

        for(YamlPreference prefItr : pref){

            if(preferenceMap.containsKey(prefItr.getGlob())){
                ArrayList<String> prefList = new ArrayList<String>(preferenceMap.get(prefItr.getGlob()));
                prefList.add(prefItr.getUrl());
                preferenceMap.put(prefItr.getGlob(), prefList);
            }else{
                ArrayList<String> prefList = new ArrayList<String>();
                prefList.add(prefItr.getUrl());
                preferenceMap.put(prefItr.getGlob(), prefList);
            }

        }

        return preferenceMap;
    }

    private List<YamlPreference> jsonToYamlPreference(){

        String jsonStr = preferencesManager.getValue(preferenceName);

        ArrayList yamlPreferences = new ArrayList<YamlPreference>();
        JsonObject parsedJson = Json.parse(jsonStr);
        for(String glob : parsedJson.keys()){
            System.out.println(glob);
//            JsonObject jsonGlob = Json.parse(parsedJson.get(glob));
//            for(String url : jsonGlob.keys()){
//                YamlPreference newYamlPref = new YamlPreference(url, glob);
//                yamlPreferences.add(newYamlPref);
//            }
            //yamlPreferences.add(new YamlPreference("test", glob));
        }

        return yamlPreferences;
    }

    private String toJSON(List<String> lst){
        StringBuilder out = new StringBuilder("[");
        int i;
        for (i = 0; i < lst.size() - 1; i++) {
            out.append(new JSONString(lst.get(i)) + ",");
        }
        if (lst.size() > 0) {
            out.append(new JSONString(lst.get(i)));
        }
        out.append(']');
        return out.toString();
    }

    @Override
    public void storeChanges() {

        Map<String, String> preferenceList = new HashMap<String, String>();
        for(Map.Entry<String, List<String>> pref: yamlPreferenceToMap(yamlPreferences).entrySet()){
            preferenceList.put(pref.getKey(), toJSON(pref.getValue()));
        }

        preferencesManager.setValue(this.preferenceName, JsonHelper.toJson(preferenceList));
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