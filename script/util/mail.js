load(identityServer.getProjectLocation()  +  "/script/util/date.js");
/*global require, openidm, exports */

(function () {
    exports.sendMail = function (object, templatefilename) {
		if(identityServer.getProperty("mail.enabled") != true) {
            logger.info("Mail is disabled in boot.properties.");
            return {"result" : "error", "message" : "Mail is disabled in boot.properties."};
		}
        if (!object) {
            logger.info("No Object attribute was passed to function, mail was not sent.");
            return {"result" : "error", "message" : "No Object attribute was passed to function, mail was not sent."};
        }
        if (!object.mail && !object.mailto) {
            logger.info("Object has no mail (email address) attribute, and no mailto attribute, mail was not sent.");
            return {"result" : "error", "message" : "Object has no mail (email address) attribute, and no mailto attribute, mail was not sent."};
        }
        var now = new Date();
        if (!!object.endDate && object.endDate < now) {
            logger.info("Object has passed end date, mail was not sent.");
            return {"result" : "error", "message" : "Object has passed end date, mail was not sent."};
        }

        // if there is a configuration found, assume that it has been properly configured
        var emailConfig = openidm.read("config/external.email");
        var Handlebars = require('lib/handlebars'); 
        var emailTemplate = openidm.read("config/emailTemplate/" + templatefilename);

        if (emailConfig && emailConfig.host && emailTemplate && emailTemplate.enabled) {
            var email, password, template, templateSubject, result, locale = emailTemplate.defaultLocale;

            email =  {
                "from": identityServer.getProperty("mail.from", null, true),
                "to": !!object.mailto ? object.mailto : object.mail,
                "type": "text/html"
            };

			try {
				if(identityServer.getProperty("mail.debugMailCopy")) {
					email.to = email.to + "," + identityServer.getProperty("mail.debugMailCopy");
				}
			} catch (e) {
				logger.info("Met weird exception in script/util/mail.js: " + e.toString());
			}

            if(!!object.parttime_pct_pos){ object.parttime_pct_pos = object.parttime_pct_pos  +  "%"; }
            
            template = Handlebars.compile(emailTemplate.message[locale]);
            email.body = template({ "object": object });

            templateSubject = Handlebars.compile(emailTemplate.subject[locale]);
            email.subject = templateSubject({ "object": object });

            result = openidm.action("external/email", "send", email);
            logger.info("util/mail.js sending '" + templatefilename + "' mail from:'" + email.from + "' to: '" + email.to + "' with result:" + result + " using object:" + object);
            return {"result":result};
        } else {
            logger.warn("Email service not configured;  No '" + templatefilename + "' mail was sent.");
            return {"result" : "error", "message" : "Email service not configured;  No '" + templatefilename + "' mail was sent."};
        }
    };
}());
