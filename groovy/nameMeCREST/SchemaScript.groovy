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

import org.forgerock.json.resource.Connection
import org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log

def operation = operation as OperationType
def configuration = configuration as ScriptedCRESTConfiguration
def connection = connection as Connection
def log = log as Log

def file = new File(System.getProperty("launcher.project.location") + "/conf/provisioner.openicf-namemecrest.json")

def schema = SchemaSlurper.parse(file.toURI().toURL())

configuration.propertyBag.putAll(schema)

builder.schema({
    schema.each { key, value ->
        objectClass {
            type key
            value.attributes.values().each {
                attribute it.attributeInfo
            }
        }
    }
})
