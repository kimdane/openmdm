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

def apiVersion = identityServer.getProperty("RESTcon.apiVersion", "v1");

log.info("Entering RESTcon " + operation + " Script")

def query = [:]
def queryFilter = 'true'
if (filter != null) {
    queryFilter = filter.accept(CRESTFilterVisitor.VISITOR, [
            translateName: { String name ->
                if (AttributeUtil.namesEqual(name, Uid.NAME)) {
                    return "id"
                } else if (AttributeUtil.namesEqual(name, Name.NAME)) {
                    return "id"
                } else if (AttributeUtil.namesEqual(name, "telephoneNumber")) {
                    return "contactInformation/telephoneNumber"
                } else if (AttributeUtil.namesEqual(name, "emailAddress")) {
                    return "contactInformation/emailAddress"
                } else if (AttributeUtil.namesEqual(name, "familyName")) {
                    return "name/familyName"
                } else if (AttributeUtil.namesEqual(name, "givenName")) {
                    return "name/givenName"
                } else if (AttributeUtil.namesEqual(name, "displayName")) {
                    return "displayName"
                } else if (AttributeUtil.namesEqual(name, "members")) {
                    return "members"
                } else {
                    throw new IllegalArgumentException("Unknown field name: ${name}");
                }
            },
            convertValue : { Attribute value ->
                if (AttributeUtil.namesEqual(value.name, "members")) {
                    return value.value
                } else {
                    return AttributeUtil.getStringValue(value)
                }
            }] as VisitorParameter).toString();
}

query['_queryFilter'] = queryFilter

if (null != options.pageSize) {
    query['_pageSize'] = options.pageSize
    if (null != options.pagedResultsCookie) {
        query['_pagedResultsCookie'] = options.pagedResultsCookie
    }
    if (null != options.pagedResultsOffset) {
        query['_pagedResultsOffset'] = options.pagedResultsOffset
    }
}

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def searchResult = connection.request(GET) { req ->
            uri.path = "/api/${apiVersion}/contact-persons"
            uri.query = query

            response.success = { resp, json ->
                json.result.each() { record ->
                    resultHandler {
                        uid record.id
                        id record.id
                    	record.each() { key, value -> attribute key, value }
                    }
                }
                json
            }
        }
        return new SearchResult(searchResult.pagedResultsCookie, searchResult.remainingPagedResults)
}
