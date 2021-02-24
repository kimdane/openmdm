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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

/**
 * @author jdabrowski
 */

define("org/forgerock/commons/ui/common/components/GridTableView", ["jquery", "backbone"], function ($, Backbone) {

    var GridTableView = Backbone.View.extend({

        items: [],

        events: {
            "click li": "onClick",
            "sortupdate": "orderChanged"
        },

        render: function render(params) {
            var i;
            this.setElement(params.el);
            if (params.items) {
                this.items = params.items;
            }
            this.rebuildView();
        },

        generateItemView: function generateItemView(item) {
            return '<li class="ui-state-default">' + item._id + '</li>';
        },

        onClick: function onClick(event) {
            event.preventDefault();
            this.removeItemAndRebuild($(event.target).find("input").val());
        },

        getItems: function getItems() {
            var optionTexts = [],
                value;
            this.$el.find("ul li").each(function () {
                value = $(this).text();
                if (value) {
                    optionTexts.push(value);
                }
            });
            return optionTexts;
        },

        orderChanged: function orderChanged() {
            console.log("Order has changed");
            this.items = this.getItems();
        },

        removeItemAndRebuild: function removeItemAndRebuild(itemId) {
            var i;
            for (i = 0; i < this.items.length; i++) {
                if (this.items[i]._id === itemId) {
                    this.items.splice(i, 1);
                }
            }
            this.rebuildView();
        },

        addItemAndRebuild: function addItemAndRebuild(item) {
            if (this.itemExists(item._id)) {
                return false;
            } else {
                this.items.push(item);
                this.rebuildView();
                return true;
            }
        },

        itemExists: function itemExists(itemId) {
            var i;
            for (i = 0; i < this.items.length; i++) {
                if (this.items[i]._id === itemId) {
                    return true;
                }
            }
            return false;
        },

        rebuildView: function rebuildView() {
            var i;
            this.$el.find("#sortable").remove();
            this.$el.find("#noItemsMessage").remove();

            if (this.items.length > 0) {
                this.$el.append('<ul id="sortable" class="sortable"></ul>');

                this.sort();
                for (i = 0; i < this.items.length; i++) {
                    this.$el.find("#sortable").append(this.generateItemView(this.items[i]));
                }
                this.installAdditionalFunctions();
            }

            if (this.$el.find("#sortable").children().size() === 0) {
                if (this.noItemsMessage()) {
                    this.$el.append('<div id="noItemsMessage" class="offset10">' + this.noItemsMessage() + '</div>');
                }
            }
            this.runAfterRebuildSteps();
        },

        sort: function sort() {},

        runAfterRebuildSteps: function runAfterRebuildSteps() {},

        noItemsMessage: function noItemsMessage() {
            return "No items";
        },

        installAdditionalFunctions: function installAdditionalFunctions() {
            this.$el.find("#sortable").sortable();
            this.$el.find("#sortable").disableSelection();
        }

    });

    return GridTableView;
});
