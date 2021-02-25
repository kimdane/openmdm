function formatNameMeSQL(object) {
    return "[" +
        object.short_name + "/" +
        object.resource_id + "/" +
        object.first_name + " " +
        object.surname +
    "]";
}

function formatManaged(object) {
    return "[" +
        object.userName + "/" +
        object.resourceId + "/" +
        object._id + "/" +
        object.givenName + " " +
        object.sn +
    "]";
}

function formatAd(object) {
    return "[" +
        object.sAMAccountName + "/" +
        object.employeeID + "/" +
        object.objectGUID + "/" +
        object.givenName + " " +
        object.sn +
    "]";
}
