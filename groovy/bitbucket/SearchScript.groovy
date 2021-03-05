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

log.warn("Entering RESTcon " + operation + " Script")

def totalPagedResults = -1, remainingPagedResults = -1
def pagedResultsCookie = options.getPagedResultsCookie()
def pagedResultsOffset = options.getPagedResultsOffset()
def pageSize = options.getPageSize()

log.warn("Options RESTcon pagedResultsCookie " + pagedResultsCookie + " Script")
log.warn("Options RESTcon pagedResultsOffset " + pagedResultsOffset + " Script")
log.warn("Options RESTcon pageSize " + pageSize + " Script")

def query = [:]
def queryFilter = ''

if (filter != null) {
    queryFilter = filter.accept(CRESTFilterVisitor.VISITOR, [
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

if (null != pageSize) {
    query['limit'] = pageSize
}
if (null != pagedResultsOffset) {
    query['start'] = pagedResultsOffset
}


def getOneById = { objectType, objectId ->
	def searchResult = connection.request(GET) { req ->
        if (objectType == 'projects') {
    	    uri.path = "${restApi}/${objectType}/${objectId}"
        }
        else {
            uri.path = "${restApi}/${objectType}"
            query['project'] = 'unknown'
            query['slug'] = 'unknown'
        }
	    uri.query = query
	
	    log.warn("RESTcon getOneById objectId " + objectId + "  URI " + uri + " Script")
	    response.success = { resp, json ->
	            resultHandler {
                    if (objectType == 'projects') { 
                        uid json.key 
	                    id json.id.toString()
                    }
                    else { 
                        uid json.id.toString()
	                    id json.name
                    }
	                
	            	json.each() { key, value -> attribute key, value }
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
	    log.warn("RESTcon getManyById objectId " + objectId + " Script")
		connection.request(GET) { req ->
            if (objectType == "projects") {
	        	uri.path = "${restApi}/${objectType}/${objectId}"
            }
            else { 
                project = 'unknown'
                slug = 'unknown'
		        connection.request(GET) { subReq ->
	        	    uri.path = "http://localhost:8080/openidm/managed/scmRepo/"+objectId
		            response.success = { subResp, repo ->
    	        	    project = repo.project.key
                        slug = repo.slug
            	    }
                    response.failure = { subResp, repo ->
                        log.warn("RESTcon getManyById  failure ${objectId} " + repo + " Script")
                    }
                }
                uri.path = "${restApi}/projects/${project}/repos/${slug}"
    	    }
            uri.query = query
		    response.success = { resp, json ->
    	        	map['result'] += json 
    	    }
        } 
	}
    map.result.each() { record ->
        resultHandler {
            if (objectType == 'projects') { 
                uid record.key 
	            id record.id.toString()
            }
            else { 
                uid record.id.toString()
	            id record.name
            }
            record.each() { key, value -> attribute key, value }
        }
    }
    map
    return new SearchResult("0", 0);
}

def getBranches = { key, slug ->
    def results = []
    if (!key || !slug) {
        return results
    }
    connection.request(GET) { req ->
        uri.path = "${restApi}/projects/${key}/repos/${slug}/branches"
        uri.query = query
        response.success = { resp, json ->
            results += json.values
        }
        response.failure = { resp, json ->
            log.warn("RESTcon getBranches failure ${key} ${slug} " + json + " Script")
        }
    }
    return results
}

def getCommits = { key, slug ->
    def results = []
    if (!key || !slug) {
        return results
    }
    query['limit'] = 1000
    connection.request(GET) { req ->
        uri.path = "${restApi}/projects/${key}/repos/${slug}/commits"
        uri.query = query
        response.success = { resp, json ->
            results += json.values
        }
        response.failure = { resp, json ->
            log.warn("RESTcon getCommits failure ${key} ${slug} " + json + " Script")
        }
    }
    return results
}

def getPermissions = { key, slug ->
    def results = []
    if (!key) {
        return results
    }
    def path = ""
    if (!slug) {
        path = "${restApi}/projects/${key}/permissions"
    }
    else {
        path = "${restApi}/projects/${key}/repos/${slug}/permissions"
    }
    connection.request(GET) { req ->
        uri.path = "${path}/users"
        uri.query = query
        response.success = { resp, json ->
            results += json.values
        }
        response.failure = { resp, json ->
            log.warn("RESTcon getPermissions failure ${key} ${slug} " + json + " Script")
        }
    }
    connection.request(GET) { req ->
        uri.path = "${path}/groups"
        uri.query = query
        response.success = { resp, json ->
            results += json.values
        }
        response.failure = { resp, json ->
            log.warn("RESTcon getPermissions failure ${key} ${slug} " + json + " Script")
        }
    }
    return results
}

def getSearchResults = { objectType ->
	def map = ['result':[]]
    def isLastPage = false
    def max = 2000
    query['limit'] = 500
	while (!isLastPage && query['start'] < max) {
	    log.warn("RESTcon getSearchResults nextPageStart " + query['start'] + " Script")
		connection.request(GET) { req ->
            path = "${restApi}/${objectType}"
	    	uri.path = path
            uri.query = query
            response.failure = { resp, json ->
                log.warn("RESTcon getSearchResults failure " + json + " Script")
            }
		    response.success = { resp, json ->
                isLastPage = json.isLastPage
                query['start'] = json.nextPageStart
                query['limit'] = json.limit

	            json.values.each() { record ->
    	        	map['result'] += record
                }
    	    }
    	}
	}
    map.result.each() { record ->
        if (objectType == 'repos' && record.containsKey('project') && record.slug.startsWith('a')) {
            record['branches'] = getBranches(record.project.key, record.slug)
            record['commits'] = getCommits(record.project.key, record.slug)
            //record['permissions'] = getPermissions(record.project.key, record.slug)
        }
        //else {
        //    record['permissions'] = getPermissions(record.key, false)
        //}
                    
        resultHandler {
            if (objectType == 'projects') { 
                uid record.key 
	            id record.id.toString()
            }
            else { 
                uid record.id.toString()
	            id record.name
            }
            record.each() { key, value -> attribute key, value }
        }
    }
    map
    return new SearchResult("0", 0);
}

def getSearchResultsWithLimit = { objectType ->
	def searchResult = connection.request(GET) { req ->
	    uri.path = "${restApi}/${objectType}"
	    uri.query = query
	
	    log.warn("RESTcon getSearchResultsWithLimit URI path:" + uri.path + " query:"+ uri.query +" Script")
	    response.success = { resp, json ->
	        json.values.each() { record ->
	            resultHandler {
                    if (objectType == 'projects') { 
                        uid record.key 
	                    id record.id.toString()
                    }
                    else { 
                        uid record.id.toString()
	                    id record.name
                    }
	                
	            	record.each() { key, value -> attribute key, value }
	            }
	        }
	        json
	    }
	}
    //return new SearchResult(nextPageStart.toString(), nextPageStart);
	//log.warn("RESTcon JSON searchResult " + searchResult + " Script")
    if(searchResult.isLastPage) {
        return new SearchResult("0", 0)
    } else {
        return new SearchResult("-1", searchResult.nextPageStart)
    }
    //return new SearchResult(nextPageStart.toString(), SearchResult.CountPolicy.NONE, query['limit'], nextPageStart);
}


def doSearch = { objectType, idPattern ->
    one = queryFilter =~ "^id eq \"${idPattern}\"\$"
    many = queryFilter =~ "id eq \"${idPattern}\""
    log.warn("RESTcon queryFilter "+ queryFilter)
    if (one) {
        return getOneById(objectType, one[0][1])
    }
    else if (many && objectType != "repos") {
        return getManyById(objectType, many)
    }
    else if (pageSize) {
        return getSearchResultsWithLimit(objectType)
    }
    else {
        return getSearchResults(objectType)
    }
}


switch (objectClass) {
    case 'ObjectClass: project':
		doSearch('projects', '([A-Z]+)')
		break;
    case 'ObjectClass: repo':
		doSearch('repos', '([0-9]+)')
		break;
	default:
		log.warn("SEARCH Script got bad objectClass '${objectClass}'")
		break;
}
