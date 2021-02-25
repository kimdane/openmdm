import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.identityconnectors.common.logging.Log
import org.forgerock.openicf.misc.scriptedcommon.OperationType

import java.sql.Connection


def operation = operation as OperationType
def configuration = configuration as ScriptedSQLConfiguration
def connection = new Sql(connection as Connection)
def log = log as Log

log.info("Entering NameMeSQL TestScript");

def sql = new Sql(connection);

// TODO: Replace with query which throws an error, so that the
// connector is not enabled if the view can't be found.
sql.firstRow("SELECT MAX(last_update) AS last_update FROM right_table_in_agresso_db")
