package com.smartbear.soapui.blueprint

import com.eviware.soapui.impl.rest.RestMethod
import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.RestServiceFactory
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.wsdl.WsdlProject

/**
 * Created by ole on 05/08/14.
 */
class BlueprintExporterTest extends GroovyTestCase {
   public void testExport()
    {
        WsdlProject project = new WsdlProject();
        RestService restApi = project.addNewInterface( "My REST API", RestServiceFactory.REST_TYPE );
        RestResource restResource = restApi.addNewResource( "Test", "/login")
        restResource.getParams().addProperty( "id").style = RestParamsPropertyHolder.ParameterStyle.QUERY
        RestMethod method = restResource.addNewMethod( "Test Method")
        method.setMethod(RestRequestInterface.HttpMethod.GET)
        method.addNewRequest( "Test Request");

        BlueprintExporter exporter = new BlueprintExporter( project )
        System.out.println( exporter.createBlueprint( "Test", restApi ))
    }
}
