division_code = source.DivisionSn ? source.DivisionSn.trim().toUpperCase() : '';
dep_code = source.DepartmentSn ? source.DepartmentSn.trim().toUpperCase() : '';
sector_code = source.FunctionSn ? source.FunctionSn.trim().toUpperCase() : '';
subsector_code = source.SubFunctionSn ? source.SubFunctionSn.trim().toUpperCase() : '';
division_name = source.DivisionLn ? source.DivisionLn.trim() : '';
dep_name = source.DepartmentLn ? source.DepartmentLn.trim() : '';
sector_name = source.FunctionLn ? source.FunctionLn.trim() : '';
subsector_name = source.SubFunctionLn ? source.SubFunctionLn.trim() : '';

if(division_code.length > 1) {
    obj = openidm.read('managed/division/'+division_code);
    if (obj == null) {
        try {
            obj = openidm.create('managed/division/',null,{'_id':division_code,'code':division_code,'name':division_name});
        }
        catch (error) {
            // Error is likely due to race conditions divisiont _id reference is still good
            obj = {'_id':division_code};
        }
    }
    target.division = {'_ref': 'managed/division/'+ obj._id};
}
if(dep_code.length > 1) {
    obj = openidm.read('managed/department/'+dep_code);
    if (obj == null) {
        new_object = {'_id':dep_code,'code':dep_code,'name':dep_name}
        if (target.division != null) {
            new_object['division'] = target.division;
        }
        try {
            obj = openidm.create('managed/department/',null,new_object);
        }
        catch (error) {
            // Error is likely due to race conditions divisiont _id reference is still good
            obj = {'_id':dep_code};
        }
    }
    target.department = {'_ref': 'managed/department/' + obj._id};
}
if(sector_code.length > 1) {
    obj = openidm.read('managed/sector/'+sector_code);
    if (obj == null) {
        new_object = {'_id':sector_code,'code':sector_code,'name':sector_name}
        if (target.division != null) {
            new_object['division'] = target.division;
        }
        if (target.department != null) {
            new_object['department'] = target.department;
        }
        try {
            obj = openidm.create('managed/sector/',null,new_object);
        }
        catch (error) {
            // Error is likely due to race conditions divisiont _id reference is still good
            obj = {'_id':sector_code};
        }
    }
    target.sector = {'_ref': 'managed/sector/' + obj._id};
}
if(subsector_code.length > 1) {
    obj = openidm.read('managed/sector/'+subsector_code);
    if (obj == null) {
        new_object = {'_id':subsector_code,'code':subsector_code,'name':subsector_name};
        if (target.sector != null && subsector_code != target.sector) {
            new_object['parentSector'] = target.sector;
        }
        if (target.division != null) {
            new_object['division'] = target.division;
        }
        if (target.department != null) {
            new_object['department'] = target.department;
        }
        try {
            obj = openidm.create('managed/sector/',null,new_object);
        }
        catch (error) {
            // Error is likely due to race conditions divisiont _id reference is still good
            obj = {'_id':subsector_code};
        }
    }
    target.sector = {'_ref': 'managed/sector/' + obj._id};
}
if(!!target.reports && target.reports.length > 2) {
    log.info("Could make: Team " + source.givenName + " team size: " + source.reports.length);
}
target;
