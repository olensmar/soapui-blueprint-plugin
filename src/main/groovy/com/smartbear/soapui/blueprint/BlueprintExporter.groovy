package com.smartbear.soapui.blueprint

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.wsdl.WsdlProject

class BlueprintExporter {

    private final WsdlProject project

    public BlueprintExporter(WsdlProject project) {
        this.project = project
    }

    String createBlueprint(String title, RestService service) {


    }


}
