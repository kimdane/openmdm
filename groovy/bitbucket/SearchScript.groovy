/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.common.StringUtil
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter
import org.forgerock.openidm.core.IdentityServer

import static groovyx.net.http.Method.GET

// imports used for CREST based REST APIs
import org.forgerock.openicf.misc.crest.CRESTFilterVisitor
import org.forgerock.openicf.misc.crest.VisitorParameter

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def resultHandler = handler
def identityServer = IdentityServer.getInstance()

def apiVersion = identityServer.getProperty("RESTcon.apiVersion", "1.0");

log.info("Entering RESTcon " + operation + " Script")

def query = [:]
def queryFilter = 'true'
//def attr = null
//def attr_value = null
//if (filter != null) {
//    queryFilter = filter.accept(CRESTFilterVisitor.VISITOR, [
//            translateName: { String name ->
//                if (AttributeUtil.namesEqual(name, Uid.NAME)) {
//                    attr = "name"
//                    return "id"
//                } else if (AttributeUtil.namesEqual(name, Name.NAME)) {
//                    attr = "name"
//                    return "key"
//                } else {
//                    attr = name
//                    return name
//                    //throw new IllegalArgumentException("Unknown field name: ${name}");
//                }
//            },
//            convertValue : { Attribute value ->
//                if (AttributeUtil.namesEqual(value.name, "members")) {
//                    return value.value
//                } else {
//                    attr_value = AttributeUtil.getStringValue(value)
//                    return AttributeUtil.getStringValue(value)
//                }
//            }] as VisitorParameter).toString();
//}
//
query['_queryFilter'] = queryFilter
////if (null != attr && null != attr_value) {
////    query[attr] = attr_value
////}

if (null != options.pageSize) {
    query['limit'] = options.pageSize
    if (null != options.pagedResultsCookie) {
        query['_pagedResultsCookie'] = options.pagedResultsCookie
    }
    if (null != options.pagedResultsOffset) {
        query['start'] = options.pagedResultsOffset
    }
}
if (filter != null) {
    if (filter instanceof EqualsFilter && ((EqualsFilter) filter).attribute.is(Uid.NAME)) {
        def uid = AttributeUtil.getStringValue(((EqualsFilter) filter).attribute)
        query['name'] = uid
        log.info("Filter RESTcon " + query + " Script")
    }
}

switch (objectClass.objectClassValue) {
    case "project":
        def searchResult = connection.request(GET) { req ->
            uri.path = "/rest/api/${apiVersion}/projects"
            uri.query = query

            log.info("RESTcon URI " + uri + " Script")
            response.success = { resp, json ->
                json.values.each() { record ->
                    resultHandler {
                        uid record.key
                        id record.id.toString()
                        
                    	record.each() { key, value -> attribute key, value }
                    }
                }
                json
            }
        }
        return new SearchResult(null, -1)
    case "repo":
        def searchResult = connection.request(GET) { req ->
            uri.path = "/rest/api/${apiVersion}/repos"
            uri.query = query

            log.info("RESTcon " + uri + " Script")
            response.success = { resp, json ->
                json.values.each() { record ->
                    resultHandler {
                        uid record.name
                        id record.slug
                        
                    	record.each() { key, value -> attribute key, value }
                    }
                }
                json
            }
        }
        return new SearchResult(null, -1)
}
