var commits = [];
var branches = [];

function createCommit(commit) {
    obj = openidm.read('managed/scmCommit/'+commit.id);
    if (obj == null) {
        try {
            commit['_id'] = commit.id;
            result = openidm.query('managed/user/',{'_queryFilter':'userName eq \"' + commit.author.slug + '\"'}).result;
            if(result.length == 1) {
                commit['author'] = {'_ref': 'managed/user/' + result[0]};
                if(commit.author.slug == commit.committer.slug) {
                    commit['committer'] = {'_ref': 'managed/user/' + result[0]};
                }
            }
            obj = openidm.create('managed/scmCommit/',null,commit);
        }
        catch (error) {
            // Error is likely due to race conditions but _id reference is still good
            obj = {'_id':commit.id};
        }
    }
    parents = [];
    committer = {};
    commit.parents.forEach(function(p){parents.push({'_ref': 'managed/scmCommit/'+p.id})});
    result = openidm.query('managed/user/',{'_queryFilter':'userName eq \"' + commit.committer.slug + '\"'}).result;
    if(result.length == 1) {
        committer = {'_ref': 'managed/user/' + result[0]};
    }
    ops = [{"operation":"replace","field":"parents","value":parents},{"operation":"replace","field":"committer","value":committer}];
    openidm.patch('managed/scmCommit/' + obj._id, null, ops);

    commits.push({'_ref': 'managed/scmCommit/'+ obj._id});
};
function createBranch(branch) {
    obj = openidm.read('managed/scmBranch/'+branch.latestChangeset);
    if (obj == null) {
        try {
            branch['_id'] = branch.latestChangeset;
            branch['repo'] = {'_ref': 'managed/scmRepo/'+target._id};
            obj = openidm.create('managed/scmBranch/',null,branch);
        }
        catch (error) {
            // Error is likely due to race conditions but _id reference is still good
            obj = {'_id':branch.latestChangeset};
        }
    }
    ops = [{"operation":"replace","field":"latestCommit","value":branch['latestCommit']}];
    openidm.patch('managed/scmBranch/'+ obj.latestChangeset, null, ops);
    branches.push({'_ref': 'managed/scmBranch/'+ obj.latestChangeset});
};
if(!!source.commits && source.commits.length > 0){
    source.commits.forEach(createCommit);
    target.commits = commits;
}
if(!!source.branches && source.branches.length > 0){
    source.branches.forEach(createBranch);
    target.branches = branches;
}

target;
