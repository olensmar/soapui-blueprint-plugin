/**
 *  Copyright 2014 SmartBear Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.smartbear.soapui.blueprint.actions;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.settings.XmlBeansSettingsImpl;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.smartbear.soapui.blueprint.BlueprintExporter;

import java.io.File;
import java.io.FileWriter;

@ActionConfiguration( actionGroup = "RestServiceActions", afterAction = "ExportWadlAction", separatorBefore = true )
public class ExportBlueprintAction extends AbstractSoapUIAction<RestService> {
    private static final String TARGET_PATH = Form.class.getName() + Form.FOLDER;
    private XFormDialog dialog;

    public ExportBlueprintAction() {
        super("Export API Blueprint", "Creates an API Blueprint for selected REST API");
    }

    public void perform(RestService restService, Object param) {
        // initialize form
        XmlBeansSettingsImpl settings = restService.getSettings();
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);

            String name = restService.getName();
            if (name.startsWith("/") || name.toLowerCase().startsWith("http"))
                name = restService.getProject().getName() + " - " + name;

            dialog.setValue(Form.NAME, name);
            dialog.setValue(Form.FOLDER, settings.getString(TARGET_PATH, ""));
        }

        while (dialog.show()) {
            try {
                BlueprintExporter exporter = new BlueprintExporter(restService.getProject());

                String name = dialog.getValue(Form.NAME);
                String blueprint = exporter.createBlueprint(name, restService);

                String folder = dialog.getValue(Form.FOLDER);

                File file = new File(folder, name + ".blueprint" );
                FileWriter writer = new FileWriter(file);
                writer.write(blueprint);
                writer.close();

                UISupport.showInfoMessage("API Blueprint has been created at [" + file.getAbsolutePath() + "]");

                settings.setString(TARGET_PATH, dialog.getValue(Form.FOLDER));

                break;
            } catch (Exception ex) {
                UISupport.showErrorMessage(ex);
            }
        }
    }

    @AForm(name = "Export API Blueprint", description = "Creates an API Blueprint for selected REST API")
    public interface Form {
        @AField(name = "Target Folder", description = "Where to save the API Blueprint", type = AField.AFieldType.FOLDER)
        public final static String FOLDER = "Target Folder";

        @AField(name = "Name", description = "The API Name", type = AField.AFieldType.STRING)
        public final static String NAME = "Name";
    }
}
