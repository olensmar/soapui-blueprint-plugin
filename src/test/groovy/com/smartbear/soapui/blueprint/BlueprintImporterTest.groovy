package com.smartbear.soapui.blueprint

import com.eviware.soapui.impl.rest.RestMethod
import com.eviware.soapui.impl.rest.RestRepresentation
import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.support.RestParamProperty
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.wsdl.WsdlProject

/**
 * Created by ole on 01/06/14.
 */
class BlueprintImporterTest extends GroovyTestCase {
    public void testImportLargeJson()
    {
        WsdlProject project = new WsdlProject();
        BlueprintImporter importer = new BlueprintImporter( project )

        RestService service = importer.importBlueprint( new File( "src/test/resources/large.json").toURI().toURL().toString(),
                    "http://www.test.com/basePath");

        assertNotNull( service )
        assertEquals( "Message of the Day API", service.name )
        assertEquals( "A simple [MOTD](http://en.wikipedia.org/wiki/Motd_(Unix)) API.", service.description )

        assertEquals( "/basePath", service.basePath )
        assertEquals( "http://www.test.com", service.getEndpoints()[0])

        RestResource resource = service.getResourceByFullPath( "/basePath/messages/{id}")
        assertNotNull( resource )

        assertEquals( "Message", resource.name )
        assertEquals( "This resource represents one particular message identified by its *id*.", resource.description )

        RestParamProperty param = resource.getParams().getProperty( "id" )
        assertEquals( RestParamsPropertyHolder.ParameterStyle.TEMPLATE, param.style )

        assertEquals( 2, resource.getRestMethodCount())
        RestMethod method = resource.getRestMethodByName( "Retrieve Message" )

        assertNotNull( method )
        assertEquals( "Retrieve a message by its *id*.", method.description )
        assertEquals(RestRequestInterface.HttpMethod.GET, method.method)

        RestRepresentation representation = method.getRepresentations( RestRepresentation.Type.RESPONSE, "text/plain")[0]
        assertNotNull( representation )
        assertEquals( 200, representation.status[0])

        method = resource.getRestMethodByName( "Delete Message" )

        assertNotNull( method )
        assertEquals( "Delete a message. **Warning:** This action **permanently** removes the message from the database.", method.description )
        assertEquals(RestRequestInterface.HttpMethod.DELETE, method.method)

        representation = method.getRepresentations( RestRepresentation.Type.RESPONSE, null )[0]
        assertNotNull( representation )
        assertEquals( 204, representation.status[0])
    }
}
