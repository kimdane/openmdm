load(identityServer.getProjectLocation() + "/script/util/util.js");

var organization = openidm.read("config/custom/departments");

function resolveGroupsForDepartment(departmentId) {
    var assignedGroups = [];
    if(!!departmentId && departmentId.length > 0) {
        var current = organization;
        for(var i=0;i<departmentId.length;i++) {
            if(!!current && !!current[departmentId[i]]) {
                var groups = current[departmentId[i]].groups;
                if(!!groups) {
                    for(var g=0;g<groups.length;g++) {
                        // Append mailing list group full DN to assignedGroups
                        assignedGroups.push("CN=" + groups[g] + "," + mailGroupsBaseDN);
                    }
                }
                
                if(!!current[departmentId[i]].children) {
                    current = current[departmentId[i]].children;
                } else {
                    current = null;
                }
            }
        }        
    }
    
    return assignedGroups;
}

function resolveGroupsForResource(resourceType, departmentIds) {
    var resolvedGroups = ad.groups['common'];

		// Resolve all groups for employees
		var employeeGroups = ad.groups['employees'];
        for(var i=0;i<employeeGroups.length;i++) {
			resolvedGroups.push(employeeGroups[i]);
		}

        // Resolve groups for all departments the user is part of
        for(var i=0;i<departmentIds.length;i++) {
            var departmentGroups = resolveGroupsForDepartment(departmentIds[i]);
            for(var j=0;j<departmentGroups.length;j++) {
                resolvedGroups.push(departmentGroups[j]);
            }
        }
    
    return resolvedGroups;
}

// Returns _all_ groups defined in config/custom/departments, used to
// determine which groups the system should care about.
function getAllMailGroups(organization) {
    var mailGroups = [];
    for(var key in organization) {
        if(organization[key].hasOwnProperty("groups")) {
            var groups = organization[key].groups;
            for(var g=0;g<groups.length;g++) {
                mailGroups.push("CN=" + groups[g] + "," + mailGroupsBaseDN);
            }
        }
        
        var recurse = getAllMailGroups(organization[key]);
        for(var rg=0;rg<recurse.length;rg++) {
            mailGroups.push(recurse[rg]);
        }
    }
    
    return mailGroups;
}

function setGroups() {
    var groupList = resolveGroupsForResource(resourceType, departmentIds);
    var managedGroups = {};
    var assignedGroups = {};
   
    // Actions
    var addGroups = [];
    var removeGroups = [];

    // For lookup purposes. Lowercase group names.
    for(var i=0;i<groupList.length;i++) {
        assignedGroups[groupList[i].toLowerCase()] = 1;
    }
    
    if(isEmployee(resourceType)) {
        // For lookup purposes. Lowercase group names.
        for(var i=0;i<mailGroups.length;i++) {
            managedGroups[mailGroups[i].toLowerCase()] = 1;
        }    
    }
    
    // For lookup purposes. Lowercase group names.
    for(var i=0;i<groupList.length;i++) {
        managedGroups[groupList[i].toLowerCase()] = 1;
    }
    
    var memberOf = [];
    try {
        var existing = target.memberOf;
        if(!!existing) { memberOf = existing; }
    } catch(e) { }
        
    if(memberOf.length > 0) {
        var existingGroups = {};
        
        // Remove groups that the user is no longer assigned to and
        // which are managed by system. Groups to touch are:
        //        
        //   - managedGroups resolved from static list of common
        //     groups for all AD users (custom/config/ad/groups)
        //   - dynamic groups defined in custom/config/departments
        for(var i=0;i<memberOf.length;i++) {
            existingGroups[memberOf[i].toLowerCase()] = 1;
            if(managedGroups.hasOwnProperty(memberOf[i].toLowerCase())) {
                if(!assignedGroups[memberOf[i].toLowerCase()]) {
                    removeGroups.push(memberOf[i]);
                }
            }
        }
        
        // Add groups the user is assigned to, isn't already a member
        // of, and which are managed by system.
        for(var i=0;i<groupList.length;i++) {
            if(!existingGroups[groupList[i].toLowerCase()]) {
                addGroups.push(groupList[i]);
            }
        }        
    } else {
        // No existing groups.
        addGroups = groupList;
    }
    if(addGroups.length > 0) {
        logger.debug(logPrefix + "Adding groups for " + formatManaged(source) + ": " + addGroups);
        target.addGroups = addGroups;
    }
    
    if(removeGroups.length > 0) {
        logger.debug(logPrefix + "Removing groups for " + formatManaged(source) + ": " + removeGroups);
        target.removeGroups = removeGroups;
    }
}

// Set target.addGroups and/or target.removeGroups list attributes.
function setGroups(resourceType, departmentIds) {
    var groupList = resolveGroupsForResource(resourceType, departmentIds);
    var mailGroups = getAllMailGroups(organization);
    var managedGroups = {};
    var assignedGroups = {};
   
    // Actions
    var addGroups = [];
    var removeGroups = [];

    // For lookup purposes. Lowercase group names.
    for(var i=0;i<groupList.length;i++) {
        assignedGroups[groupList[i].toLowerCase()] = 1;
    }
    
        // For lookup purposes. Lowercase group names.
        for(var i=0;i<mailGroups.length;i++) {
            managedGroups[mailGroups[i].toLowerCase()] = 1;
        }    
    
    // For lookup purposes. Lowercase group names.
    for(var i=0;i<groupList.length;i++) {
        managedGroups[groupList[i].toLowerCase()] = 1;
    }
    
    var memberOf = [];
    try {
        var existing = target.memberOf;
        if(!!existing) { memberOf = existing; }
    } catch(e) { }
        
    if(memberOf.length > 0) {
        var existingGroups = {};
        
        // Remove groups that the user is no longer assigned to and
        // which are managed by system. Groups to touch are:
        //        
        //   - managedGroups resolved from static list of common
        //     groups for all AD users (custom/config/ad/groups)
        //   - dynamic groups defined in custom/config/departments
        for(var i=0;i<memberOf.length;i++) {
            existingGroups[memberOf[i].toLowerCase()] = 1;
            if(managedGroups.hasOwnProperty(memberOf[i].toLowerCase())) {
                if(!assignedGroups[memberOf[i].toLowerCase()]) {
                    removeGroups.push(memberOf[i]);
                }
            }
        }
        
        // Add groups the user is assigned to, isn't already a member
        // of, and which are managed by IDM.
        for(var i=0;i<groupList.length;i++) {
            if(!existingGroups[groupList[i].toLowerCase()]) {
                addGroups.push(groupList[i]);
            }
        }        
    } else {
        // No existing groups.
        addGroups = groupList;
    }
    if(addGroups.length > 0) {
        logger.debug(logPrefix + "Adding groups for " + formatManaged(source) + ": " + addGroups);
        target.addGroups = addGroups;
    }
    
    if(removeGroups.length > 0) {
        logger.debug(logPrefix + "Removing groups for " + formatManaged(source) + ": " + removeGroups);
        target.removeGroups = removeGroups;
    }
}

function removeMailGroup(resourceType, departmentIds) {
    var groupList = resolveGroupsForResource(resourceType, departmentIds);

    var managedGroups = {};
    var removeGroups = [];

    // For lookup purposes. Lowercase group names.
    for(var i=0;i<groupList.length;i++) {
        managedGroups[groupList[i].toLowerCase()] = 1;
    }

    var memberOf = [];
    try {
        var existing = target.memberOf;
        if(!!existing) { memberOf = existing; }
    } catch(e) { }

    if(memberOf.length > 0) {
        for(var i=0;i<memberOf.length;i++) {
            if(managedGroups.hasOwnProperty(memberOf[i].toLowerCase())) {
                if (memberOf[i].toLowerCase().indexOf("OU=MailLister".toLowerCase())>-1 && hasMetEndDate(source)) {
                    logger.debug("[ad-on-update] Removing memberOf[i] = {}", memberOf[i] )
                    removeGroups.push(memberOf[i]);
                }
            }
        }
    }

    if(removeGroups.length > 0) {
        logger.debug(logPrefix + "Removing groups for " + formatManaged(source) + ": " + removeGroups);
        target.removeGroups = removeGroups;
    }
}
