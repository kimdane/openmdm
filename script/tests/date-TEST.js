var scriptName = "util/date.js";
print("Running tests for script: " + scriptName);

load('common.js');
load("../" + scriptName);

var date_from = "2014-12-20 00:00:00.0";
var date_to = "2015-05-20 00:00:00.0";
var date_to_winter = "2015-11-20 00:00:00.0";
var post_date_from = "2015-01-01 00:00:00.0";
var perm_date_from = null;

var sunriseDate = getSunriseDate(date_from, post_date_from);
var sunsetDate = getSunsetDate(date_to, perm_date_from);
var sunsetDateWinter = getSunsetDate(date_to_winter, perm_date_from);

assertEquals(sunriseDate, "2014-12-19T23:00:00.000Z", "sunriseDate");
assertEquals(sunsetDate, "2015-05-20T22:00:00.000Z", "sunsetDate");
assertEquals(sunsetDateWinter, "2015-11-20T23:00:00.000Z", "sunsetDateWinter");
