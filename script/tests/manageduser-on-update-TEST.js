var scriptName = "sync/manageduser-on-update.js";
print("Running tests for script: " + scriptName);

load('common.js');

var target = {
    "date_from" : "2016-05-01 00:00:00.0",
    "sunrise" : {
        "date" : "2014-01-01 00:00:00.0",
        "taskCompleted" : null
    }
};

var oldTarget = {

};

var source = {
    "date_from" : "2014-04-01 00:00:00.0",
    "post_date_from" : "2015-10-01 00:00:00.0",
    "endDate" : "2014-04-01 00:00:00.0",
    "accountStatus" : "inactive"
};

load("../" + scriptName);

print(target.sunrise.date);
