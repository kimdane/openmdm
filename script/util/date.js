function pad(n) {
    return n < 10 ? '0' + n : n;
}

function dateToLocalNO(date, includeTime) {
	if (date == null) {return date;}
    var dateString = pad(date.getDate()) + "." + pad(date.getMonth() + 1) + "." + date.getFullYear();
    if (includeTime) {
        dateString += " " + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds());
    }
    return dateString;
}

function dateToISO(date) {
	if (date == null) {return date;}
	if (!(date instanceof Date)){ return date;}
    var dateString = date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate());
    return dateString; 
}

function sqlDateToDate(date) {
    try {
        var year = date.substring(0,4);
        var month = date.substring(5,7) - 1;
        var day = date.substring(8,10);
        var dateObj = new Date(year, month, day);
        return dateObj;
    } catch(e) {
        logger.warn("Unable to convert date string to date: " + date);
        return null;
    }
}

function daysBetween(date1, date2) {
	try {
		date1 = sqlDateToDate(date1);
		date2 = sqlDateToDate(date2);
		return ((+date1) - (+date2)) / 8.64e7
    } catch(e) {
        logger.warn("Unable to compare dates: " + date1 + " and " + date2);
        return null;
    }
}

function sqlDateToISO(date) {
    try {
        var isoDate = sqlDateToDate(date).toISOString();
        return isoDate;
    } catch(e) {
        return null;
    }
}

function addDays(date, days) {
	if (date == null) {return date;}
    var result = new Date(date);
    result.setDate(date.getDate() + days);
    return result;
}

function subDays(date, days) {
	if (date == null) {return date;}
    var result = new Date(date);
    result.setDate(date.getDate() - days);
    return result;
}

function todayAtMidnight() {
    var today = new Date();
    today.setHours(0);
    today.setMinutes(0);
    today.setSeconds(0);
    today.setMilliseconds(0);
    return today;
}

function getSunriseDate(namemesql_date_from, namemesql_post_date_from) {
	var date_from = null; 
	var post_date_from = null;
	if(namemesql_date_from != undefined) {
    	date_from = sqlDateToDate(namemesql_date_from);
	}
	if(namemesql_post_date_from != undefined) {
	    post_date_from = sqlDateToDate(namemesql_post_date_from);
	}
    var today = todayAtMidnight();
    if(!!date_from && date_from > subDays(today, 30)) {
        return date_from.toISOString();
    }

    if(!!post_date_from && post_date_from >= today) {
        return post_date_from.toISOString();
    }
	
	if(!!date_from) {
    	return date_from.toISOString();
	}
	return namemesql_date_from;
}

function getSunsetDate(namemesql_date_to, namemesql_perm_date_from, namemesql_perm_date_to) {
	if (namemesql_date_to == null) {return namemesql_date_to;}

    if (!namemesql_perm_date_from) {
        // perm date is not set. Update the sunset date to the new namemesql_date_to plus 1
        var date_to = sqlDateToDate(namemesql_date_to);
        var sunsetDate = addDays(date_to, 1);
        return sunsetDate.toISOString();
    }

    if (namemesql_date_to == namemesql_perm_date_to) {
		// Person ended employment while furloughed. Employment end date
		// becomes furloughed end date
        var date_to = sqlDateToDate(namemesql_date_to);
        var sunsetDate = addDays(date_to, 1);
        return sunsetDate.toISOString();

    }  else {
		// Person currently furloughed. Employment end date left unchanged
        return target.sunset.date;
    }
}
