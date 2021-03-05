import org.forgerock.json.resource.CreateRequest
import org.forgerock.json.resource.NotSupportedException

import org.forgerock.openidm.core.IdentityServer

java.util.logging.Logger log = java.util.logging.Logger.getLogger ""

log.warning("Running k8s endpoint")

def created = 0
def updated = 0
def createOrUpdatePod = { pod ->
    old = openidm.read('managed/pod/' + pod.uid) 
    if (old == null || old.resourceVersion != pod.resourceVersion) {
        pod['_id'] = pod.uid
        annotations = pod.get('annotations',[:])
        parts = annotations.get('adp.com/git-url','').split('/')
        if (parts.length > 3){
            pod['project'] = ['_ref':'managed/scmProject/' + parts[-2].toUpperCase()]
            result = openidm.query('managed/scmRepo/',['_queryFilter':'slug eq \"' + parts[-1].replace(".git","") + '\"']).result
            if (result.size == 1) {
                pod['repo'] = ['_ref':'managed/scmRepo/' + result[0]._id]
            }
        }
        branch = annotations.get('adp.com/git-branch',false)
        if (branch) {
            pod['branch'] = branch
        }
        commit = annotations.get('adp.com/git-commit',false) 
        if (commit) {
            pod['commit'] = ['_ref':'managed/scmCommit/' + commit]
        }
        //pod['build-url'] = annotations.get('adp.com/build-url')
    }
    if (old == null) {
        openidm.create('managed/pod/',null,pod)
        created += 1
    }
    else {
        rev = old._rev
        openidm.update('managed/pod/' + pod.uid, old._rev ,pod)
        updated += 1
    }
}

if(request instanceof CreateRequest) {
    def json = request.content.getObject()
    def pods = json.get('pods',['error'])
    pods.each() { pod ->
        createOrUpdatePod(pod)
    }
    
    return [
        method: "create",
        parameters: request.additionalParameters,
        content: "Created:"+created+" and Updated:"+updated+" managed Pods"
    ]
}
else {
    throw new NotSupportedException(request.getClass().getName());
}

