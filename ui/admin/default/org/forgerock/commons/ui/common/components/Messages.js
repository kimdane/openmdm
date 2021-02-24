"use strict";

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define, window */

/**
 * @author jkigwana
 */
define("org/forgerock/commons/ui/common/components/Messages", ["jquery", "underscore", "backbone", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/main/Configuration"], function ($, _, Backbone, AbstractConfigurationAware, conf) {
    var obj = new AbstractConfigurationAware(),
        Messages;

    obj.TYPE_SUCCESS = "success";
    obj.TYPE_INFO = "info";
    obj.TYPE_WARNING = "warning";
    obj.TYPE_DANGER = "error";

    Messages = Backbone.View.extend({

        list: [],
        el: "#messages",
        events: {
            "click div": "removeAndNext"
        },
        delay: 2500,
        timer: null,

        displayMessageFromConfig: function displayMessageFromConfig(event) {
            var _this = obj.messages;
            if ((typeof event === "undefined" ? "undefined" : _typeof(event)) === "object") {
                if (typeof event.key === "string") {
                    _this.addMessage({
                        message: $.t(obj.configuration.messages[event.key].msg, event),
                        type: obj.configuration.messages[event.key].type
                    });
                }
            } else if (typeof event === "string") {
                _this.addMessage({
                    message: $.t(obj.configuration.messages[event].msg),
                    type: obj.configuration.messages[event].type
                });
            }
        },

        addMessage: function addMessage(msg) {
            var i,
                _this = obj.messages;
            if (!msg.message && msg.response && _typeof(msg.response.responseJSON) === "object" && typeof msg.response.responseJSON.message === "string") {
                msg.message = msg.response.responseJSON.message;
            }
            for (i = 0; i < _this.list.length; i++) {
                if (_this.list[i].message === msg.message) {
                    console.log("duplicated message");
                    return;
                }
            }
            if (msg.escape !== false) {
                msg.message = _.escape(msg.message);
            }
            console.info(msg.type + ":", msg.message, msg);
            _this.list.push(msg);
            if (_this.list.length <= 1) {
                _this.showMessage(msg);
            }
        },

        nextMessage: function nextMessage() {
            var _this = obj.messages;
            _this.list.shift();
            if (_this.list.length > 0) {
                _this.showMessage();
            }
        },

        removeAndNext: function removeAndNext() {
            var _this = obj.messages;
            window.clearTimeout(obj.messages.timer);
            _this.$el.find("div").fadeOut(300, function () {
                $(this).remove();
                _this.nextMessage();
            });
        },

        showMessage: function showMessage() {
            var _this = this,
                alertClass = "alert-info",
                alertIcon = "alert-message-icon",
                delay = _this.delay + this.list[0].message.length * 20;

            switch (this.list[0].type) {
                case obj.TYPE_DANGER:
                    alertClass = "alert-danger";
                    break;
                case obj.TYPE_SUCCESS:
                    alertClass = "alert-success";
                    alertIcon = "fa-check-circle-o";
                    break;
                case obj.TYPE_WARNING:
                    alertClass = "alert-warning";
                    break;
                case obj.TYPE_INFO:
                    alertClass = "alert-info";
                    break;
                default:
                    alertClass = "alert-info";
                    break;
            }

            if (this.hasNavigation()) {
                _this.$el.addClass('logged-user');
            } else {
                _this.$el.removeClass('logged-user');
            }

            this.$el.append("<div role='alert' class='alert-system alert-message alert " + alertClass + "'><i class='fa " + alertIcon + "'></i><span class='message'>" + this.list[0].message + "</span></div>");
            this.$el.find("div:last").fadeIn(300, function () {
                _this.timer = window.setTimeout(_this.removeAndNext, delay);
            });
        },

        hideMessages: function hideMessages() {
            var _this = obj.messages;
            if (_this.list.length > 1) {
                _this.list = [_this.list[1]];
            }
        },

        hasNavigation: function hasNavigation() {
            return $('#mainNavHolder').length > 0;
        }

    });

    obj.messages = new Messages();

    obj.addMessage = function (msg) {
        obj.messages.addMessage(msg);
    };

    return obj;
});
