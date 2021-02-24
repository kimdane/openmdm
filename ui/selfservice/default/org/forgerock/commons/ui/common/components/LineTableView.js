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

define("org/forgerock/commons/ui/common/components/LineTableView", ["jquery", "backbone"], function ($, Backbone) {

    var LineTableView = Backbone.View.extend({

        events: {
            "click a[name=moreItems]": "moreItems"
        },

        items: [],

        parentRender: function parentRender(params) {
            this.setElement(params.el);
            this.items = params.items;
            this.rebuildView();
        },

        rebuildView: function rebuildView() {
            var i, limit, showMoreItemsLink, height;

            this.$el.find("#itemsView").remove();
            this.$el.append('<div id="itemsView"></div>');

            if (this.items.length === 0) {
                this.$el.find("#itemsView").append(this.noItemsMessage());
            } else {
                this.$el.find("#itemsView").append('<div id="items"></div>');
                if (this.maxToShow > 0 && this.items.length > this.maxToShow) {
                    limit = this.maxToShow;
                    showMoreItemsLink = true;
                } else {
                    limit = this.items.length;
                }

                height = this.getHeightForItemsNumber(limit);
                this.$el.find("#items").height(height + "px");

                for (i = 0; i < limit; i++) {
                    this.$el.find("#items").append(this.generateItemView(this.items[i]));
                }

                if (showMoreItemsLink) {
                    this.$el.find("#itemsView").append("<a name='moreItems' class='ice itemLeftIdent' href='#' >" + this.seeMoreItemsMessage() + "</a>");
                }
            }
        },

        getHeightForItemsNumber: function getHeightForItemsNumber(itemsNumber) {
            return this.itemHeight * itemsNumber;
        },

        render: function render(params) {
            this.parentRender(params);
        },

        moreItems: function moreItems(event) {
            event.preventDefault();
            console.log("See more");
        },

        generateItemView: function generateItemView(item) {
            return '<div class="item"><label>' + item._id + '</label></div>';
        },

        seeMoreItemsMessage: function seeMoreItemsMessage(item) {
            return $.t("openidm.ui.common.components.LineTableView.seeMoreItems");
        },

        maxToShow: 0,

        itemHeight: 65,

        noItemsMessage: function noItemsMessage(item) {
            return $.t("openidm.ui.common.components.LineTableView.noItems");
        },

        removeItemAndRebuild: function removeItemAndRebuild(itemId) {
            var i;
            for (i = 0; i < this.items.length; i++) {
                if (this.items[i]._id === itemId) {
                    this.items.splice(i, 1);
                }
            }
            this.rebuildView();
        }

    });

    return LineTableView;
});
