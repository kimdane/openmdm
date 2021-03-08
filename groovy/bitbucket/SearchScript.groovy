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
def pagedResultsCookie = options.getPagedResultsCookie()
def pagedResultsOffset = options.getPagedResultsOffset()
def pageSize = options.getPageSize()
def attributesToGet = options.attributesToGet

log.info("Options RESTcon pagedResultsCookie " + pagedResultsCookie + " Script")
log.info("Options RESTcon pagedResultsOffset " + pagedResultsOffset + " Script")
log.info("Options RESTcon pageSize " + pageSize + " Script")
log.info("Options RESTcon attributesToGet " + attributesToGet + " Script")

def query = [:]
def queryFilter = ''
if (attributesToGet != null) {
    def one = 1
}   

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

def getProjectRepos = { key ->
    def results = []
    if (!key) {
        return results
    }
    connection.request(GET) { req ->
        uri.path = "${restApi}/projects/${key}/repos"
        uri.query = query
        response.success = { resp, json ->
            results = json.values
        }
        response.failure = { resp, json ->
            log.warn("RESTcon getProjectRepos failure ${key} " + json + " Script")
        }
    }
	log.info("RESTcon getProjectRepos key " + key + "  repos " + results.size() + " Script")
    return results
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
            results = json.values
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
            results = json.values
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
    def path = slug ? "${restApi}/projects/${key}/permissions" : "${restApi}/projects/${key}/repos/${slug}/permissions"
    connection.request(GET) { req ->
        uri.path = "${path}/users"
        uri.query = query
        response.success = { resp, json ->
            results = json.values
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

def checkForExtendedAttributes = { objectType, record ->
    if (objectType == 'repos' && record.containsKey('project') && record.slug.startsWith('a')) {
        if (attributesToGet.contains('branches')) {
            record['branches'] = getBranches(record.get('project',[:]).get('key','NONE'), record.slug)
        }
        if (attributesToGet.contains('commits')) {
            record['commits'] = getCommits(record.get('project',[:]).get('key','NONE'), record.slug)
        }
        if (attributesToGet.contains('NOTpermissions')) {
            record['permissions'] = getPermissions(record.get('project',[:]).get('key','NONE'), record.slug)
        }
    }
    else if (objectType == 'projects' && record.containsKey('key')) {
        if (attributesToGet.contains('NOTrepositories')) {
            record['repositories'] = getProjectRepos(record.key)
        }
        if (attributesToGet.contains('NOTpermissions')) {
            record['permissions'] = getPermissions(record.key, false)
        }
    }
    return record
}


def getOneById = { objectType, objectId ->
	def searchResult = connection.request(GET) { req ->
        if (objectType == 'projects' || objectType == "users") {
    	    uri.path = "${restApi}/${objectType}/${objectId}"
        }
        else {
            parts = objectId.split("_in_project_")
            project = parts[1]
            slug = parts[0]
            uri.path = "${restApi}/projects/${project}/repos/${slug}"
        }
	    uri.query = query
	
	    log.info("RESTcon getOneById objectId " + objectId + "  URI " + uri + " Script")
	    response.success = { resp, record ->
            attributesToGet = ['repositories']
            record = checkForExtendedAttributes(objectType, record) 
	        resultHandler {
                if (objectType == 'projects') { 
                    uid record.key 
                }
                else if (objectType == 'users') { 
                    uid record.slug 
                }
                else { 
	                uid "${record.slug}_in_project_${record.get('project',[:]).get('key','NONE')}"
                }
                id record.name
	            
	        	record.each() { key, value -> attribute key, value }
	        }
	        record
	    }
	}
    return new SearchResult("0", 0);
}

def getManyById = { objectType, matches ->
	def map = ['result':[]]
	matches.each() { match ->
		objectId = match[1] 
	    log.info("RESTcon getManyById objectId " + objectId + " Script")
		connection.request(GET) { req ->
            if (objectType == "projects" || objectType == "users") {
	        	uri.path = "${restApi}/${objectType}/${objectId}"
            }
            else { 
                parts = objectId.split("_in_project_")
                project = parts[1]
                slug = parts[0]
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
            }
            else if (objectType == 'users') { 
                uid record.slug 
            }
            else { 
	            uid "${record.slug}_in_project_${record.get('project',[:]).get('key','NONE')}"
            }
            id record.name
            record.each() { key, value -> attribute key, value }
        }
    }
    map
    return new SearchResult("0", 0);
}

def getSearchResults = { objectType ->
	def map = ['result':[]]
    def isLastPage = false
    def max = 4000000
    query['limit'] = 1000
	while (!isLastPage && query['start'] < max) {
	    log.info("RESTcon getSearchResults nextPageStart " + query['start'] + " Script")
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
        resultHandler {
            if (objectType == 'projects') { 
                uid record.key 
            }
            else if (objectType == 'users') { 
                uid record.slug 
            }
            else { 
	            uid "${record.slug}_in_project_${record.get('project',[:]).get('key','NONE')}"
            }
            id record.name
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
	
	    log.info("RESTcon getSearchResultsWithLimit URI path:" + uri.path + " query:"+ uri.query +" Script")
	    response.success = { resp, json ->
	        json.values.each() { record ->
	            resultHandler {
                    if (objectType == 'projects') { 
                        uid record.key 
                    }
                    else if (objectType == 'users') { 
                        uid record.slug 
                    }
                    else { 
	                    uid "${record.slug}_in_project_${record.get('project',[:]).get('key','NONE')}"
                    }
                    id record.name
	                
	            	record.each() { key, value -> attribute key, value }
	            }
	        }
	        json
	    }
	}
    if(searchResult.isLastPage) {
        return new SearchResult("0", 0)
    } else {
        return new SearchResult("-1", searchResult.nextPageStart)
    }
}


def doSearch = { objectType, idPattern ->
    one = queryFilter =~ "^id eq \"${idPattern}\"\$"
    many = queryFilter =~ "id eq \"${idPattern}\""
    log.info("RESTcon queryFilter "+ queryFilter)
    if (attributesToGet.size() == 1) {
        return getSearchResults(objectType)
    }
    if (one) {
        log.info("RESTcon getOneById "+ objectType + " " + one[0][1])
        attributesToGet = []
        return getOneById(objectType, one[0][1])
    }
    else if (many && objectType != "repos") {
        log.info("RESTcon getManyById "+ objectType + " " + many)
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
		doSearch('projects', '([A-Z][A-Z0-9]+)')
		break;
    case 'ObjectClass: repo':
		doSearch('repos', '([^ ]+)')
		break;
    case 'ObjectClass: user':
		doSearch('users', '([a-zA-Z]+)')
		break;
	default:
		log.warn("SEARCH Script got bad objectClass " + objectClass)
		break;
}
