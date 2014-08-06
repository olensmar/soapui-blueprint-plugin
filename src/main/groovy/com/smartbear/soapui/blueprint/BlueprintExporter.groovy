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

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

class BlueprintExporter {

   private final WsdlProject project

   public BlueprintExporter(WsdlProject project) {
      this.project = project
   }

   String createBlueprint(String title, RestService service) {
      def builder = new groovy.json.JsonBuilder()

      builder.call(
         _version: "2.0",
         metadata: [],
         name: title,
         description: "",

         resourceGroups: [
            [
               name       : service.name,
               description: emptyOnNull(service.description),

               resources  : service.allResources.collect { it ->
                  [
                     name       : it.name,
                     description: emptyOnNull(it.description),
                     uriTemplate: buildUriTemplate(it.path, it.params),
                     model      : [:],
                     parameters : buildParams(it.params),

                     actions    :
                        it.restMethodList.collect {
                           [
                              name       : it.name,
                              description: emptyOnNull(it.description),
                              method     : it.method.toString(),
                              parameters : buildParams(it.params),

                              examples   : [
                                 [
                                    name       : "Examples",
                                    description: "Examples created from requests in SoapUI",
                                    requests   :
                                       it.requestList.collect {
                                          [
                                             name       : it.name,
                                             description: it.description,
                                             headers    : it.requestHeaders.toStringToStringMap(),
                                             body       : emptyOnNull(it.requestContent),
                                             schema     : ""
                                          ]
                                       },
                                    responses  : []
                                 ]
                              ] as List
                           ]
                        }

                  ]
               }
            ]
         ] as List
      )


      return convertAstToBlueprint(builder.toString());
   }

   def convertAstToBlueprint(String ast) {
//      System.out.println(ast)
      def post = new HttpPost("https://api.apiblueprint.org/composer")
      post.addHeader("Accept", "text/vnd.apiblueprint+markdown")
      post.setEntity(new StringEntity(ast, "application/vnd.apiblueprint.ast.raw+json; version=2.0", "utf-8"))
      def response = HttpClientSupport.getHttpClient().execute(post);
      ByteArrayOutputStream out = new ByteArrayOutputStream()
      response.entity.writeTo(out)
      return out.toString("utf-8")
   }

   def buildUriTemplate(String path, RestParamsPropertyHolder params) {
      def queryArgs = "?"
      params.values().each {
         RestParameter p = it
         if (p.style == RestParamsPropertyHolder.ParameterStyle.QUERY) {
            if (queryArgs.length() > 1)
               queryArgs += ","
            queryArgs += it.name
         }
      }

      if (queryArgs.length() > 1)
         path += "{" + queryArgs + "}";

      return path;
   }

   def buildParams(RestParamsPropertyHolder params) {
      def result = [] as List

      params.keySet().each {

         def p = params.get(it)
         result.add(
            [
               name       : p.name,
               description: emptyOnNull(p.description),
               type       : p.type.localPart,
               required   : p.required,
               default    : p.defaultValue,
               example    : "",
               values     : []
            ]
         )
      }

      return result
   }

   def emptyOnNull(def str) {
      return str == null ? "" : str;
   }

}
