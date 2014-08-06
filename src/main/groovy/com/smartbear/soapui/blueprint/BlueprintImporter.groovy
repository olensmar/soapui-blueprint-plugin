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
import com.eviware.soapui.impl.rest.mock.RestMockService
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.rest.support.RestUtils
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport
import com.eviware.soapui.support.StringUtils
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A simple API Blueprint importer
 *
 * @author Ole Lensmar
 */

class BlueprintImporter {

    private static Logger logger = LoggerFactory.getLogger(BlueprintImporter.class)
    private final WsdlProject project
    private boolean createSampleRequests
    private RestMockService restMockService
    private Map<String,RestResource> resourceMap = new HashMap<>();

    public BlueprintImporter(WsdlProject project) {
        this.project = project
    }

    public RestService importBlueprint(String url, String endpoint) {

        def slurper = new JsonSlurper()
        def blueprint = new URL(url).text
        def ast = null

        try {
            ast = slurper.parseText(blueprint)
        }
        catch (Exception e) {
            logger.info("Converting Blueprint [$blueprint]")
            blueprint = convertBlueprintToAST(blueprint)
            logger.info("Converted to [$blueprint]")

            ast = slurper.parseText(blueprint).ast
        }

        def service = createRestService(ast, endpoint)

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

        def resource = resourceMap.get( ast.uriTemplate )
        if( resource == null )
        {
            resource = service.addNewResource(ast.name, ast.uriTemplate)
            resource.description = ast.description.trim()

            if (resource.name.length() == 0)
                resource.name = resource.path

            resource.path = extractParams( resource.path, resource.params, ast )

            resourceMap.put( ast.uriTemplate, resource )
        }

        ast.actions.each {
            addMethod(resource, it)
        }
    }

    def convertBlueprintToAST(String blueprint) {
        def post = new HttpPost("https://api.apiblueprint.org/parser")
        post.addHeader("Accept", "application/vnd.apiblueprint.parseresult.raw+json")
        post.setEntity(new StringEntity(blueprint, "text/vnd.apiblueprint+markdown; version=1A", "utf-8"))
        def response = HttpClientSupport.getHttpClient().execute(post);
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        response.entity.writeTo(out)
        return out.toString("utf-8")
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

            if( it.values != null )
            {
               def list = []

               it.values.each {
                  list.add( String.valueOf(it.value) )
               }

               param.options = list.toArray( new String[list.size()])
            }
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

                    if( it.name == "Content-Type")
                        request.mediaType = it.value
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

        if (restMockService != null) {
            def path = method.resource.getFullPath(true)
            def params = method.overlayParams

            params.each {
                RestParameter p = it.value
                if( p.style == RestParamsPropertyHolder.ParameterStyle.TEMPLATE )
                {
                    if( p.defaultValue != null && p.defaultValue.trim().length() > 0 )
                        path = path.replaceAll( "\\{" + it.key + "\\}", p.defaultValue )
                    else
                        path = path.replaceAll( "\\{" + it.key + "\\}", it.key )
                }
            }

            def mockAction = restMockService.addEmptyMockAction(method.method, path)

            ast.examples.each {
                it.responses.each {
                    int statusCode = Integer.parseInt(it.name)
                    def mockResponse = mockAction.addNewMockResponse("Response " + statusCode)

                    it.headers.each {
                        if (it.name.equals("Content-Type"))
                            mockResponse.contentType = it.value
                    }

                    if( it.body != null )
                        mockResponse.responseContent = it.body
                }
            }
        }


        if (createSampleRequests && method.requestCount == 0)
            method.addNewRequest("Sample Request")
    }

    private RestService createRestService(def bp, def name) {

        RestService restService = project.addNewInterface(bp.name == null ? name : bp.name, RestServiceFactory.REST_TYPE)
        restService.description = bp.description?.trim()
        return restService
    }

    public void setCreateSampleRequests(boolean createSampleRequests) {
        this.createSampleRequests = createSampleRequests;
    }

    public void setRestMockService(RestMockService restMockService) {
        this.restMockService = restMockService;
    }
}
