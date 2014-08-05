/**
 *  Copyright 2013 SmartBear Software, Inc.
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

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;
import com.smartbear.soapui.blueprint.BlueprintImporter;

import java.io.File;

/**
 * Shows a simple dialog for importing an API Blueprint
 *
 * @author Ole Lensmar
 */

@ActionConfiguration( actionGroup = "EnabledWsdlProjectActions", afterAction = "AddWadlAction", separatorBefore = true )
public class ImportBlueprintAction extends AbstractSoapUIAction<WsdlProject> {
    private XFormDialog dialog;

    public ImportBlueprintAction() {
        super("Import API Blueprint", "Imports an API Blueprint into SoapUI");
    }

    public void perform(final WsdlProject project, Object param) {
        // initialize form
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.setBooleanValue(Form.CREATE_REQUESTS, true);
        } else {
            dialog.setValue(Form.BLUEPRINT_URL, "");
        }


        while (dialog.show()) {
            try {
                // get the specified URL
                String url = dialog.getValue(Form.BLUEPRINT_URL).trim();
                if (StringUtils.hasContent(url)) {
                    // expand any property-expansions
                    String expUrl = PathUtils.expandPath(url, project);

                    // if this is a file - convert it to a file URL
                    if (new File(expUrl).exists())
                        expUrl = new File(expUrl).toURI().toURL().toString();

                    XProgressDialog dlg = UISupport.getDialogs().createProgressDialog("Importing API", 0, "", false);
                    dlg.run(new BlueprintImporterWorker(expUrl, dialog.getValue( Form.DEFAULT_ENDPOINT), project, dialog));

                    Analytics.trackAction("ImportBlueprint");
                    break;
                }
            } catch (Exception ex) {
                UISupport.showErrorMessage(ex);
            }
        }
    }

    @AForm(name = "Import API Blueprint", description = "Creates a REST API from the specified API Blueprint")
    public interface Form {
        @AField(name = "Import API Blueprint", description = "Location or URL of API Blueprint", type = AFieldType.FILE)
        public final static String BLUEPRINT_URL = "Import API Blueprint";

        @AField(name = "Default Endpoint", description = "The default endpoint for this API, including its base path", type = AFieldType.STRING )
        public final static String DEFAULT_ENDPOINT = "Default Endpoint";

        @AField(name = "Create Requests", description = "Create sample requests for imported methods", type = AFieldType.BOOLEAN)
        public final static String CREATE_REQUESTS = "Create Requests";

        @AField( name = "Generate MockService", description = "Generate a REST Mock Service from the API Blueprint definition", type = AField.AFieldType.BOOLEAN )
        public final static String GENERATE_MOCK = "Generate MockService";

        @AField( name = "Generate TestSuite", description = "Generate a skeleton TestSuite for the created REST API", type = AField.AFieldType.BOOLEAN )
        public final static String GENERATE_TESTSUITE = "Generate TestSuite";
    }
}
