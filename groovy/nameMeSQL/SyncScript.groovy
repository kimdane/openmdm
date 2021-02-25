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
 *
 * @author Gael Allioux <gael.allioux@forgerock.com>
 *
 */

import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

import java.sql.Connection

def operation = operation as OperationType
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def log = log as Log
def DEPARTMENT = new ObjectClass("__DEPARTMENT__")
def POST = new ObjectClass("__POST__")
def TYPE = new ObjectClass("__TYPE__")
def objectClass = objectClass as ObjectClass


log.info("Entering NameMeSQL " + operation + " Script");
def sql = new Sql(connection);

switch (operation) {
    case OperationType.SYNC:
        def options = options as OperationOptions
        def token = token as Object
        
        def tstamp = null
        if(token != null) {
            tstamp = new java.sql.Timestamp(token)
        } else {
            def today = new Date()
            tstamp = new java.sql.Timestamp(today.time)
        }
        
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                sql.eachRow("SELECT * FROM right_table_in_agresso_db WHERE last_update > ${tstamp}", { row ->

                    handler {
                        syncToken row.last_update.getTime()
                        CREATE_OR_UPDATE()
                        object {
                            uid row.resource_id as String
                            id row.short_name
                            ////////
                            attribute 'resource_id', row.resource_id as String
                            attribute 'short_name', row.short_name
                            attribute 'name', row.name
                            attribute 'first_name', row.first_name
                            attribute 'surname', row.surname
                            attribute 'client', row.client
                            attribute 'status', row.status
                            attribute 'telephone_1', row.telephone_1
                            attribute 'pos_type', row.pos_type
                            attribute 'pos_type_next', row.pos_type_next
                            attribute 'resource_typ', row.resource_typ
                            attribute 'sex', row.sex
                            attribute 'date_from', row.date_from as String
                            attribute 'date_to', row.date_to as String
                            attribute 'expired_date', row.expired_date as String
                            attribute 'exit_reason', row.exit_reason
                            attribute 'post_id', row.post_id
                            attribute 'post_name', row.post_name
                            attribute 'post_date_from', row.post_date_from as String
                            attribute 'post_name_next', row.post_name_next
                            attribute 'post_name_from_next', row.post_name_next
                            attribute 'superior', row.superior
                            attribute 'superior_name', row.superior_name
                            attribute 'superior_next', row.superior_next
                            attribute 'superior_next_name', row.superior_next_name
                            attribute 'superior_next_date', row.superior_next_date as String
                            attribute 'department_1', row.department_1
                            attribute 'department_2', row.department_2
                            attribute 'department_3', row.department_3
                            attribute 'department_4', row.department_4
                            attribute 'dept_name_1', row.dept_name_1
                            attribute 'dept_name_2', row.dept_name_2
                            attribute 'dept_name_3', row.dept_name_3
                            attribute 'dept_name_4', row.dept_name_4
                            attribute 'parttime_pct_1', row.parttime_pct_1
                            attribute 'parttime_pct_2', row.parttime_pct_2
                            attribute 'parttime_pct_3', row.parttime_pct_3
                            attribute 'parttime_pct_4', row.parttime_pct_4
                            attribute 'department_next_1', row.department_next_1
                            attribute 'department_next_fromdate', row.department_next_fromdate as String
                            attribute 'dept_name_next_1', row.dept_name_next_1
                            attribute 'permcode', row.permcode
                            attribute 'permavt', row.permavt
                            attribute 'perm_date_from', row.perm_date_from as String
                            attribute 'perm_date_to', row.perm_date_to  as String
                            attribute 'last_update', row.last_update as String
                            attribute 'parttime_pct_pos', row.parttime_pct_pos as Integer
                            attribute 'parttime_pct_pos_next', row.parttime_pct_pos_next as Integer
                        }
                    }
                });
                break
    		case DEPARTMENT:
				sql.eachRow("SELECT dept_description, dept_name_1, department_1, last_update FROM (SELECT *, ROW_NUMBER() OVER(PARTITION BY department_1 ORDER BY last_update DESC) rn FROM right_table_in_agresso_db WHERE department_1 like '[A-Z][0-9][0-9]') t WHERE rn = 1 AND last_update > ${tstamp}", { row ->
                    handler {
                        syncToken row.last_update.getTime()
                        CREATE_OR_UPDATE()
                        object {
                			uid row.department_1 as String
			                id row.dept_description
                            ////////
            			    attribute 'dept_name_1', row.dept_name_1
                			attribute 'last_update', row.last_update as String
						}
					}
                });
                break
    		case POST:
				sql.eachRow("SELECT post_id, post_name, pos_type, resource_typ, last_update FROM (SELECT *, ROW_NUMBER() OVER(PARTITION BY post_id ORDER BY last_update DESC) rn FROM right_table_in_agresso_db WHERE LEN(post_id) > 2) t WHERE rn = 1 AND last_update > ${tstamp}", { row ->
                    handler {
                        syncToken row.last_update.getTime()
                        CREATE_OR_UPDATE()
                        object {
			                uid row.post_id as String
                			id row.post_name as String
                            ////////
                			attribute 'pos_type', row.pos_type as String
                			attribute 'resource_typ', row.resource_typ as String
                			attribute 'last_update', row.last_update as String
						}
					}
                });
                break
    		case TYPE:
				sql.eachRow("SELECT pos_type, resource_typ, last_update FROM (SELECT *, ROW_NUMBER() OVER(PARTITION BY pos_type ORDER BY last_update DESC) rn FROM right_table_in_agresso_db WHERE LEN(pos_type) > 0) t WHERE rn = 1 AND last_update > ${tstamp}", { row ->
                    handler {
                        syncToken row.last_update.getTime()
                        CREATE_OR_UPDATE()
                        object {
			                uid row.pos_type as String
			                id row.pos_type as String
                            ////////
                			attribute 'resource_typ', row.resource_typ as String
                			attribute 'last_update', row.last_update as String
						}
					}
                });
                break
            default:
				log.warn(operation.name() + " operation of type: " +
                    objectClass.objectClassValue + " is not supported.")
                throw new UnsupportedOperationException(operation.name() + " operation of type: " +
                    objectClass.objectClassValue + " is not supported.")
        }
        
        break
        
    case OperationType.GET_LATEST_SYNC_TOKEN:
        switch(objectClass) {
            case ObjectClass.ACCOUNT:
                row = sql.firstRow("SELECT MAX(last_update) AS last_update FROM right_table_in_agresso_db")
                break;
            case DEPARTMENT:
                row = sql.firstRow("SELECT MAX(last_update) AS last_update FROM right_table_in_agresso_db")
                break;
            case POST:
                row = sql.firstRow("SELECT MAX(last_update) AS last_update FROM right_table_in_agresso_db")
                break;
            case TYPE:
                row = sql.firstRow("SELECT MAX(last_update) AS last_update FROM right_table_in_agresso_db")
                break;
            default:
				log.warn(operation.name() + " operation of type: " +
                    objectClass.objectClassValue + " is not supported.")
                throw new UnsupportedOperationException(operation.name() + " operation of type: " +
                    objectClass.objectClassValue + " is not supported.")
        }
        
        log.ok("Get Latest Sync Token script: Last token is: " + row["last_update"])
        return row["last_update"].getTime()
        break;
    default:
		log.warn("NameMeSQL SyncScript does not support operation: " + operation.name())
        throw new ConnectorException("NameMeSQL SyncScript does not support operation: " + operation.name())
}
