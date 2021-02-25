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
 */

import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.MapFilterVisitor
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.AttributeBuilder
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter

import java.sql.Connection
import java.text.SimpleDateFormat

def operation = operation as OperationType
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def filter = filter as Filter
def log = log as Log
def DEPARTMENT = new ObjectClass("__DEPARTMENT__")
def POST = new ObjectClass("__POST__")
def TYPE = new ObjectClass("__TYPE__")
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions


log.info("Entering NameMeSQL " + operation + " Script");

def sql = new Sql(connection);
def where = " WHERE 1=1 ";
def whereParams = []

// Set where and whereParams if they have been passed in the request for paging
if (options.pagedResultsCookie != null) {
    def cookieProps = options.pagedResultsCookie.split(",");
    assert cookieProps.size() == 2
    // The timestamp and id are for this example only.
    // The user can use their own properties to sort on.
    // For paging it is important that the properties that you use must identify
    // a distinct set of pages for each iteration of the
    // pagedResultsCookie, which can be decided by last record of the previous set.
    where =  " WHERE timestamp >= ? AND id > ? "
    whereParams = [ cookieProps[0], cookieProps[1].toInteger()]
}

// Determine what properties will be used to sort the query
def orderBy = []
if (options.sortKeys != null && options.sortKeys.size() > 0) {
    options.sortKeys.each {
        def key = it.toString();
        if (key.substring(0,1) == "+") {
            orderBy.add(key.substring(1,key.size()) + " ASC")
        } else {
            orderBy.add(key.substring(1,key.size()) + " DESC")
        }
    }
    orderBy = " ORDER BY " + orderBy.join(",")
} else {
    orderBy = ""
}

def limit = ""
// LIMIT not supported by MSSQL 2014
//if (options.pageSize != null) {
//    limit = " LIMIT " + options.pageSize.toString()
//}

// keep track of lastTimestamp and lastId so we can
// use it for the next request to do paging
def lastTimestamp
def lastId

if (filter != null) {

    def query = filter.accept(MapFilterVisitor.INSTANCE, null)
    //Need to handle the __UID__ and __NAME__ in queries - this map has entries for each objectType,
    //and is used to translate fields that might exist in the query object from the ICF identifier
    //back to the real property name.
    def fieldMap = [
            "__ACCOUNT__" : [
                    "__UID__" : "resource_id",
                    "__NAME__": "short_name"
            ],
            "__POST__" : [
                    "__UID__" : "post_id",
                    "__NAME__": "post_name"
            ],
            "__TYPE__" : [
                    "__UID__" : "pos_type",
                    "__NAME__" : "pos_type"
            ],
            "__DEPARTMENT__" : [
                    "__UID__" : "department_1",
                    "__NAME__": "dept_description"
            ]
    ]

    def whereTemplates = [
            CONTAINS          : '$left ${not ? "NOT " : ""}LIKE ?',
            ENDSWITH          : '$left ${not ? "NOT " : ""}LIKE ?',
            STARTSWITH        : '$left ${not ? "NOT " : ""}LIKE ?',
            EQUALS            : '$left ${not ? "<>" : "="} ?',
            GREATERTHAN       : '$left ${not ? "<=" : ">"} ?',
            GREATERTHANOREQUAL: '$left ${not ? "<" : ">="} ?',
            LESSTHAN          : '$left ${not ? ">=" : "<"} ?',
            LESSTHANOREQUAL   : '$left ${not ? ">" : "<="} ?'
    ];

    // this closure function recurses through the (potentially complex) query object in order to build an equivalent SQL 'where' expression
    def queryParser
    queryParser = { queryObj ->

        if (queryObj.operation == "OR" || queryObj.operation == "AND") {
            return "(" + queryParser(queryObj.right) + " " + queryObj.operation + " " + queryParser(queryObj.left) + ")";
        } else {

            if (fieldMap[objectClass.objectClassValue] && fieldMap[objectClass.objectClassValue][queryObj.get("left")]) {
                queryObj.put("left", fieldMap[objectClass.objectClassValue][queryObj.get("left")]);
            }

            def engine = new groovy.text.SimpleTemplateEngine()
            def wt = whereTemplates.get(queryObj.get("operation"))
            def binding = [left: queryObj.get("left"), not: queryObj.get("not")]
            def template = engine.createTemplate(wt).make(binding)

            if (queryObj.get("operation") == "CONTAINS") {
                whereParams.push("%" + queryObj.get("right") + "%")
            } else if (queryObj.get("operation") == "ENDSWITH") {
                whereParams.push("%" + queryObj.get("right"))
            } else if (queryObj.get("operation") == "STARTSWITH") {
                whereParams.push(queryObj.get("right") + "%")
            } else {
                whereParams.push(queryObj.get("right"))
            }
            return template.toString()
        }
    }

    where = where + " AND "+ queryParser(query)
    log.ok("Search WHERE clause is: " + where)
}
def resultCount = 0
switch ( objectClass ) {
    case DEPARTMENT:
		def statement = "SELECT dept_description, dept_name_1, department_1, last_update FROM (SELECT *, ROW_NUMBER() OVER(PARTITION BY department_1 ORDER BY last_update DESC) rn FROM right_table_in_agresso_db WHERE department_1 like '[A-Z][0-9][0-9]') t ${where} AND rn = 1 ${orderBy} ${limit}";
        sql.eachRow(statement, whereParams, { row ->
            handler {
                //__UID__ -> _id
                uid row.department_1 as String
                // __NAME__ -> <YOU CONFIGURE>
                id row.dept_description as String
                ////////
                attribute 'dept_name_1', row.dept_name_1 as String
                attribute 'last_update', row.last_update as String
            }
        });
        break;
    case POST:
		def statement = "SELECT post_id, post_name, pos_type, resource_typ, last_update FROM (SELECT *, ROW_NUMBER() OVER(PARTITION BY post_id ORDER BY last_update DESC) rn FROM right_table_in_agresso_db WHERE LEN(post_id) > 2) t ${where} AND rn = 1 ${orderBy} ${limit}";
        sql.eachRow(statement, whereParams, { row ->
            handler {
                //__UID__ -> _id
                uid row.post_id as String
                // __NAME__ -> <YOU CONFIGURE>
                id row.post_name as String
                ////////
                attribute 'pos_type', row.pos_type as String
                attribute 'resource_typ', row.resource_typ as String
                attribute 'last_update', row.last_update as String
            }
        });
        break;
    case TYPE:
		def statement = "SELECT pos_type, resource_typ, last_update FROM (SELECT *, ROW_NUMBER() OVER(PARTITION BY pos_type ORDER BY last_update DESC) rn FROM right_table_in_agresso_db WHERE LEN(pos_type) > 0) t ${where} AND rn = 1 ${orderBy} ${limit}";
        sql.eachRow(statement, whereParams, { row ->
            handler {
                //__UID__ -> _id
			    uid row.pos_type as String
                // __NAME__ -> <YOU CONFIGURE>
			    id row.pos_type as String
                ////////
                attribute 'resource_typ', row.resource_typ as String
                attribute 'last_update', row.last_update as String
            }
        });
        break;
    case ObjectClass.ACCOUNT:

        def statement = """
            SELECT
            a.resource_id,
            a.short_name,
            a.name,
            a.first_name,
            a.surname,
            a.client,
            a.status,
            a.telephone_1,
            a.pos_type,
            a.pos_type_next,
            a.resource_typ,
            a.sex,
            a.date_from,
            a.date_to,
            a.expired_date,
            a.exit_reason,
            a.post_id,
            a.post_name,
            a.post_date_from,
            a.post_name_next,
            a.post_date_from_next,
            a.superior,
            a.superior_name,
            a.superior_next,
            a.superior_next_name,
            a.superior_next_date,
            a.department_1,
            a.department_2,
            a.department_3,
            a.department_4,
            a.dept_name_1,
            a.dept_name_2,
            a.dept_name_3,
            a.dept_name_4,
            a.parttime_pct_1,
            a.parttime_pct_2,
            a.parttime_pct_3,
            a.parttime_pct_4,
            a.department_next_1,
            a.department_next_fromdate,
            a.dept_name_next_1,
            a.permcode,
            a.permavt,
            a.perm_date_from,
            a.perm_date_to,
            a.last_update,
            a.parttime_pct_pos,
            a.parttime_pct_pos_next
            FROM
            right_table_in_agresso_db a
            ${where}
            ${orderBy}
            ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->

            handler {
                //__UID__ -> _id
                uid row.resource_id as String
                // __NAME__ -> <YOU CONFIGURE>
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
        });
        break;

    default:
        break;

}
