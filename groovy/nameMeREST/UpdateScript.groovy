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

import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.forgerock.openidm.core.IdentityServer

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT

def operation = operation as OperationType
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid
def identityServer = IdentityServer.getInstance()

def apiVersion = identityServer.getProperty("RESTcon.apiVersion", "v1");

log.info("Entering RESTcon " + operation + " Script");

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def builder = new JsonBuilder()
        builder {
            contactInformation {
                telephoneNumber(updateAttributes.hasAttribute("telephoneNumber") ? updateAttributes.findString("telephoneNumber") : "")
                emailAddress(updateAttributes.hasAttribute("emailAddress") ? updateAttributes.findString("emailAddress") : "")
            }
            delegate.name({
                familyName(updateAttributes.hasAttribute("familyName") ? updateAttributes.findString("familyName") : "")
                givenName(updateAttributes.hasAttribute("givenName") ? updateAttributes.findString("givenName") : "")
            })
            displayName(updateAttributes.hasAttribute("displayName") ? updateAttributes.findString("displayName") : "")
            departmentCode(updateAttributes.hasAttribute("departmentCode") ? updateAttributes.findString("departmentCode") : "")
            divisionName(updateAttributes.hasAttribute("divisionName") ? updateAttributes.findString("divisionName") : "")
        }

        return connection.request(PUT, JSON) { req ->
            uri.path = "/api/${apiVersion}/contact-persons/${uid.uidValue}"
            body = builder.toString()
            headers.'If-Match' =  "*"

            response.success = { resp, json ->
				//new Uid(json.id, json._rev)
                uid
            }
        }
}
return uid
