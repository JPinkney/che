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
