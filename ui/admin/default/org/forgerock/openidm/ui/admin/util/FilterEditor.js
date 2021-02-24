"use strict";

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
 * Copyright 2015-2016 ForgeRock AS.
 */

define("org/forgerock/openidm/ui/admin/util/FilterEditor", ["jquery", "underscore", "handlebars", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/UIUtils"], function ($, _, Handlebars, AbstractView, conf, uiUtils) {
    var FilterEditor = AbstractView.extend({
        template: "templates/admin/util/SetupFilter.html",
        data: {
            config: {
                ops: ["and", "or", "not", "expr"],
                tags: ["equalityMatch", "greaterOrEqual", "lessOrEqual"]
            },
            showSubmitButton: true
        },
        events: {
            "change .expressionTree :input": "updateNodeValue",
            "click .expressionTree .add-btn": "addNodeAndReRender",
            "click .expressionTree .remove-btn": "removeNode"
        },
        getExpressionContext: function getExpressionContext(e) {
            e.preventDefault();
            var objectPath = _.map($(e.target).parentsUntil(".expressionTree fieldset>div", ".node[index]"), function (li) {
                return $(li).attr("index");
            }).reverse(),
                previousNode = null,
                node = this.data.filter;

            _.each(objectPath, function (index) {
                if (index.length) {
                    previousNode = node;
                    node = node.children[index];
                }
            });

            return { current: node, parent: previousNode, path: objectPath };
        },

        removeNode: function removeNode(e, callback) {
            this.deleteNode(e);
            this.renderExpressionTree(callback);
        },

        deleteNode: function deleteNode(e, callback) {
            var context = this.getExpressionContext(e),
                parent,
                children;

            if (!_.isNull(context.parent)) {
                children = context.children;

                children = _.reject(children, function (c) {
                    return c === context.current;
                });
                if (children.length !== 0) {
                    this.setFilterString();
                } else {
                    e.target = $(":input:first", $(e.target).parents(".node[index]")[1])[0];
                    return this.removeNode(e);
                }
            } else {
                context.current.children.pop();
            }
        },

        createNode: function createNode(e) {
            var context = this.getExpressionContext(e),
                node = context.current;
            if (!node.children) {
                node.children = [];
            }
            node.children.push({ name: "", value: "", tag: "equalityMatch", children: [], op: "expr" });
            this.setFilterString();
        },

        addNodeAndReRender: function addNodeAndReRender(e, callback) {
            this.createNode(e);
            this.renderExpressionTree(callback);
        },

        updateNodeValue: function updateNodeValue(e, callback) {
            var context = this.getExpressionContext(e),
                node = context.current,
                field = $(e.target),
                createNode = this.createNode.bind(this, e),
                deleteNode = this.deleteNode.bind(this, e),
                redrawContainer = false;

            // handle field types
            if (field.hasClass("op")) {
                redrawContainer = true;
                node.op = field.val();

                if (node.op === "none") {
                    _.times(node.children.length, deleteNode);
                } else if (node.op === "expr") {
                    node.name = "";
                    node.value = "";
                    node.tag = "equalityMatch";
                    node.children = [];
                } else if (node.op === "not") {
                    if (!node.children || !node.children.length) {
                        this.createNode(e);
                    } else {
                        _.times(node.children.length, deleteNode);
                        this.createNode(e);
                    }
                } else if (node.op === "and" || node.op === "or") {
                    if (node.children) {
                        if (node.children.length < 2) {
                            // add as many as it takes to get to 2 nodes
                            _.times(2 - node.children.length, createNode);
                        }
                    } else {
                        _.times(2, createNode);
                    }
                }
                // end of op handlers
            } else if (field.hasClass("name")) {
                if (field.parent().siblings(".tag-body").find(".tag").val() === "extensibleMatchAND") {
                    node.extensible.matchType = field.val();
                    node.name = field.val() + ":1.2.840.113556.1.4.803";
                } else if (field.parent().siblings(".tag-body").find(".tag").val() === "extensibleMatchOR") {
                    node.extensible.matchType = field.val();
                    node.name = field.val() + ":1.2.840.113556.1.4.804";
                } else {
                    if (node) {
                        node.name = field.val();
                    }
                }
                // end of name handlers
            } else if (field.hasClass("tag")) {

                switch (field.val()) {
                    case "extensibleMatchAND":
                        node.tag = "extensibleMatch";
                        node.extensible = {
                            matchType: field.parent().siblings(".name-body").find(".name").val(),
                            rule: "1.2.840.113556.1.4.803",
                            value: field.parent().siblings(".value-body").find(".value").val(),
                            type: 169 // comes from ldapjs protocol definition for FILTER_EXT
                        };
                        node.name = field.parent().siblings(".name-body").find(".name").val() + ":1.2.840.113556.1.4.803";
                        break;

                    case "extensibleMatchOR":
                        node.tag = "extensibleMatch";
                        node.extensible = {
                            matchType: field.parent().siblings(".name-body").find(".name").val(),
                            rule: "1.2.840.113556.1.4.804",
                            value: field.parent().siblings(".value-body").find(".value").val(),
                            type: 169 // comes from ldapjs protocol definition for FILTER_EXT
                        };
                        node.name = field.parent().siblings(".name-body").find(".name").val() + ":1.2.840.113556.1.4.804";
                        break;

                    case "pr":
                        delete node.extensible;
                        field.parent().siblings(".value-body").css("display", "none");
                        node.name = field.parent().siblings(".name-body").find(".name").val();
                        node.tag = field.val();
                        break;

                    default:
                        delete node.extensible;
                        field.parent().siblings(".value-body").css("display", "");
                        node.name = field.parent().siblings(".name-body").find(".name").val();
                        node.tag = field.val();
                }
                // end of tag handlers
            } else if (field.hasClass("value")) {
                if (field.parent().siblings(".tag-body").find(".tag").val().match(/^extensibleMatch/)) {
                    node.extensible.value = field.val();
                }

                node.value = field.val();
            }
            // end of field type control flow.

            if (node && node.op !== "none") {
                this.setFilterString();
            } else {
                this.setFilterString("");
            }

            if (redrawContainer) {
                this.renderExpressionTree(callback);
            } else {
                this.$el.find(".filter").text(this.getFilterString());
            }
        },

        renderExpressionTree: function renderExpressionTree(callback) {
            if (callback) {
                uiUtils.renderTemplate(this.template, this.$el, _.extend({}, conf.globalData, this.data), callback, "replace");
            } else {
                uiUtils.renderTemplate(this.template, this.$el, _.extend({}, conf.globalData, this.data), $.noop(), "replace");
            }
        },

        setFilterString: function setFilterString(string) {
            if (string) {
                this.data.filterString = string;
            } else {
                this.data.filterString = this.getFilterString();
            }
        }
    }),
        filterDisplayClosure;

    Handlebars.registerHelper("filterDisplay", function (rules, config, options) {
        var returnVal = '',
            ops = _.clone(config.ops),
            tags = _.clone(config.tags);

        if (options.fn !== undefined) {
            ops.unshift("none");
            filterDisplayClosure = options.fn;
        }

        if (rules.children && rules.children.length) {
            rules.children = _.map(rules.children, function (c, index) {
                return _.extend(c, { index: index, lastChild: false, hasMultiple: rules.children.length > 1 });
            });
            rules.children[rules.children.length - 1].lastChild = true;
        }

        returnVal += filterDisplayClosure(_.extend(rules, {
            "options": _.map(ops, function (o) {
                return {
                    "value": o,
                    "label": $.t("templates.util.filter.options." + o),
                    "selected": rules.op === o
                };
            }),
            "tags": _.map(tags, function (o) {
                if (o === "extensibleMatchAND") {

                    return {
                        "value": o,
                        "label": $.t("templates.util.filter.tags.extensible.rules.bitAnd"),
                        "selected": rules.extensible && rules.extensible.rule === "1.2.840.113556.1.4.803"
                    };
                } else if (o === "extensibleMatchOR") {

                    return {
                        "value": o,
                        "label": $.t("templates.util.filter.tags.extensible.rules.bitOr"),
                        "selected": rules.extensible && rules.extensible.rule === "1.2.840.113556.1.4.804"
                    };
                } else {

                    return {
                        "value": o,
                        "label": $.t("templates.util.filter.tags." + o),
                        "selected": rules.tag === o
                    };
                }
            }),
            "delimiter": $.t($.t("templates.util.filter.delimiters." + rules.op)), // will only be valid for and and or
            "isExpr": rules.op === "expr",
            "unary": rules.tag === "pr", // there might be other unary tags later
            "isMultiValueType": rules.op === "and" || rules.op === "or",
            "config": config
        }));

        return returnVal;
    });

    return FilterEditor;
});
