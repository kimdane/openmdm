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
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
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
def restApi = "/rest/api/${apiVersion}"

log.info("Entering RESTcon " + operation + " Script")

def totalPagedResults = -1, remainingPagedResults = -1
def pagedResultsCookie = options.pagedResultsCookie
def query = [:]
def queryFilter = ''

if (filter != null) {
    queryFilter = filter.accept(new FilterTranslator(), [
            translateName: { String name ->
                if (AttributeUtil.namesEqual(name, Uid.NAME)) {
                    return "id"
                } else if (AttributeUtil.namesEqual(name, Name.NAME)) {
                    return "name"
                } else {
                    return name
                }
            },
            convertValue : { Attribute value ->
                    return AttributeUtil.getStringValue(value)
            }] as VisitorParameter).toString();
}

if (null != options.pageSize) {
    query['limit'] = options.pageSize
    if (null != options.pagedResultsOffset) {
        query['start'] = options.pagedResultsOffset
    }
}

def getOneById = { objectType, objectId ->
	def searchResult = connection.request(GET) { req ->
	    uri.path = "${restApi}/${objectType}/${objectId}"
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
    return new SearchResult("0", 0);
}

def getManyById = { objectType, matches ->
	def map = ['result':[]]
	matches.each() { match ->
		objectId = match[1] 
		connection.request(GET) { req ->
	    	uri.path = "${restApi}/${objectType}/${objectId}"
		    response.success = { resp, json ->
		        json.values.each() { record ->
    	        	map['result'] += record 
    	        }
    	    }
    	}
	}
    map.result.each() { record ->
        resultHandler {
            uid record.id
            if (objectType == 'projects') { id record.key }
            else { id record.name }
            record.each() { key, value -> attribute key, value }
        }
    }
    return new SearchResult("0", 0);
}

def getSearchResults = { objectType ->
    if (objectType == "projects") {
        queryFilter = queryFilter.replace("__NAME__", "key")
    } else {
        queryFilter = queryFilter.replace("__NAME__", "name")
    }
	def searchResult = connection.request(GET) { req ->
	    uri.path = "${restApi}/${objectType}"
	    uri.query = queryFilter
	
	    log.info("RESTcon URI path:" + uri.path + " query:"+ uri.query +" Script")
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
    return new SearchResult("0", 0);
}


def doSearch = { objectType, idPattern ->
   if ((m = queryFilter =~ "^id = ${idPattern}\$")) {
       return getOneById(objectType, m[0][1])
   }
   else if ((m = queryFilter =~ "(?: OR )id = ${idPattern}")) {
       return getManyById(objectType, m)
   }
   else {
       return getSearchResults(objectType)
   }
}


switch (objectClass) {
    case 'ObjectClass: project':
		doSearch('projects', /'([0-9]+)'/)
		break;
    case 'ObjectClass: repo':
		doSearch('repos', /'([0-9]+)'/)
		break;
	default:
		log.info("SEARCH Script got bad objectClass '${objectClass}'")
		break;
}
