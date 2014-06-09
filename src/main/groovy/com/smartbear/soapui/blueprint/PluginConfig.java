package com.smartbear.soapui.blueprint;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;

/**
 * Created by ole on 08/06/14.
 */

@PluginConfiguration( groupId = "com.smartbear.soapui.plugins", name = "API Blueprint Plugin", version = "1.0",
    autoDetect = true, description = "Provides API Blueprint import/export functionality for REST APIs",
    infoUrl = "https://github.com/olensmar/soapui-blueprint-plugin")
public class PluginConfig extends PluginAdapter {
    @Override
    public void initialize() {
        super.initialize();
    }
}
