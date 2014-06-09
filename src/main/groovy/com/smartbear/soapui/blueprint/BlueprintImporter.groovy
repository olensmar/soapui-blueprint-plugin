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

package com.smartbear.soapui.blueprint

import com.eviware.soapui.impl.rest.*
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.rest.support.RestUtils
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.support.StringUtils
import groovy.json.JsonSlurper

/**
 * A simple API Blueprint importer
 *
 * @author Ole Lensmar
 */

class BlueprintImporter {

    private final WsdlProject project
    private boolean createSampleRequests

    public BlueprintImporter(WsdlProject project) {
        this.project = project
    }

    public RestService importBlueprint(String url, String endpoint) {

        def slurper = new JsonSlurper()
        def ast = slurper.parseText(new URL(url).text)

        def service = createRestService(ast)

        ast.resourceGroups.each {
            it.resources.each {
                addResource(service, it)
            }
        }

        if (StringUtils.hasContent(endpoint)) {
            URL endpointUrl = new URL(endpoint);
            String basePath = endpointUrl.path;

            if (StringUtils.hasContent(basePath)) {
                service.addEndpoint(endpoint.substring(0, endpoint.length() - basePath.length()));
                service.setBasePath(basePath);
            } else {
                service.addEndpoint(endpoint);
            }
        }

        return service
    }

    private def addResource(RestService service, ast) {
        def resource = service.addNewResource(ast.name, ast.uriTemplate)
        resource.description = ast.description.trim()

        if (resource.name.length() == 0)
            resource.name = resource.path

        resource.path = extractParams( resource.path, resource.params, ast )

        ast.actions.each {
            addMethod(resource, it)
        }
    }

    def extractParams( String path, RestParamsPropertyHolder params, ast )
    {
        if( path != null ) {
            RestUtils.extractTemplateParams(path).each {

                if (it.startsWith("?")) {
                    it.substring(1).split().each {
                        RestParameter param = params.addProperty(it)
                        param.style = RestParamsPropertyHolder.ParameterStyle.QUERY
                    }

                    path = path.substring(0, path.lastIndexOf("{?"))
                } else {
                    RestParameter param = params.addProperty(it)
                    param.style = RestParamsPropertyHolder.ParameterStyle.TEMPLATE
                }
            }
        }

        ast.parameters.each
        {
            RestParameter param = params.getProperty( it.name )

            if( param == null )
                param = params.addProperty( it.name )

            param.required = it.required
            param.defaultValue = it.default
            param.description = it.description.trim()
        }

        return path
    }

    private def addMethod(RestResource resource, ast) {
        RestMethod method = resource.addNewMethod(ast.name)
        method.description = ast.description.trim()
        method.method = RestRequestInterface.HttpMethod.valueOf(ast.method.toUpperCase())

        if (method.name.length() == 0)
            method.name = ast.method + " Action"

        extractParams( null, method.params, ast )

        ast.examples.each {

            it.requests.each{
                def request = method.addNewRequest( it.name )
                request.description = it.description.trim()

                request.requestContent = it.body
                def headers = request.requestHeaders
                it.headers.each {
                    headers.add( it.name, it.value )
                }

                request.requestHeaders = headers
            }

            it.responses.each {
                RestRepresentation representation = method.addNewRepresentation(RestRepresentation.Type.RESPONSE)
                representation.status = [Integer.parseInt(it.name)]

                it.headers.each {
                    if (it.name.equals("Content-Type"))
                        representation.mediaType = it.value
                }
            }
        }

        if (createSampleRequests && method.requestCount == 0)
            method.addNewRequest("Sample Request")
    }

    private RestService createRestService(def bp) {

        RestService restService = project.addNewInterface(bp.name, RestServiceFactory.REST_TYPE)
        restService.description = bp.description.trim()
        return restService
    }

    public void setCreateSampleRequests(boolean createSampleRequests) {
        this.createSampleRequests = createSampleRequests;
    }
}
