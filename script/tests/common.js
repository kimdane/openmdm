function identityServerObject() {
    this.getProjectLocation = getProjectLocation;
    this.getProperty = getProperty;
    this.properties = {};

    function getProjectLocation() {
        return "../..";
    }

    function getProperty(name) {
        return this.properties[name];
    }
}

var identityServer = new identityServerObject();

function openidmObject() {
    this.query = query;
    this.encrypt = encrypt;
    this.action = action;
    this.read = read;
    this.update = update;
    this.create = create;
    this.patch = patch;
    this["delete"] = myDelete;
    this.resources = {};

    function query(resource, params) {
        return { "result" : [input] };
    }

    function action(resource, params) {
        return { "result" : ["OK"] };
    }

    function read(resource) {
        return this.resources[resource];
    }

    function update(id, rev, object) {
        print("# openidm.update(\"" + id + "\")");
    }

    function myDelete(id, rev) {
        print("# openidm.delete(\"" + id + "\")");
    }

    function encrypt(value, cipher, alias) {
        return value;
    }
    
    function create(id, rev, params) {
        print("# openidm.create(" + id + ")");
    }
    
    function patch(id, rev, params) {
        print("# openidm.patch(\"" + id + "\")");
    }
}

function loggerObject() {
    this.info = info;
    this.debug = debug;
    this.warn = warn;
    this.error = error;
    function info(text) {
        print("# [INFO ] " + text);
    }

    function debug(text) {
        print("# [DEBUG] " + text);
    }

    function warn(text) {
        print("# [WARN ] " + text);
    }

    function error(text) {
        print("# [ERROR] " + text);
    }
}

function assertEquals(expected, result, context) {
    if(expected !== result) {
        print("AssertionError: " + context + " failed. Expected " + expected + ", got " + result);
    } else {
        print(context + ": OK");
    }
}

// Can be overridden in tests
var openidm = new openidmObject();
var logger = new loggerObject();
