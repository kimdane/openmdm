function hasMetEndDate(managedUser) {
    if (!!managedUser && !!managedUser.endDate) {
        var endDate = sqlDateToDate(managedUser.endDate);
        return endDate != null && endDate < todayAtMidnight();
    } else {
        return false;
    }
}

function isValid(field) {
    return field !== undefined && field !== null && (field.length > 0 || Object.keys(field).length > 0);
}

function copyInput(mainObj) {
	var objCopy = {}; // objCopy will store a copy of the mainObj
	var key;
	for (key in mainObj) {
		objCopy[key] = mainObj[key]; // copies each property to the objCopy object
	}
	return objCopy;
}

