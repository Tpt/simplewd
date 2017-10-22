/*
 * Copyright (C) 2017 Simple WD Developers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wikidata.simplewd.http.html;

import j2html.tags.ContainerTag;

import static j2html.TagCreator.*;

class HTMLRenderer {

    String render(String pageTitle, String pageURL, ContainerTag body) {
        return html(
                head(
                        title(pageTitle),
                        meta().withCharset("utf-8"),
                        meta().withName("viewport").withContent("width=device-width,initial-scale=1"),
                        link().withRel("stylesheet").withHref("/simplewd/node_modules/material-components-web/dist/material-components-web.css"),
                        link().withRel("stylesheet").withHref("/simplewd/main.css")
                ),
                body(
                        header(
                                div(
                                        section(
                                                span("SimpleWD").withClass("mdc-toolbar__title")
                                        ).withClass("mdc-toolbar__section"),
                                        section(
                                                nav(
                                                        navigationLink("/simplewd", "Main", pageURL),
                                                        navigationLink("/simplewd/swagger.html", "API", pageURL)
                                                ).withClass("mdc-tab-bar")
                                        ).withClasses("mdc-toolbar__section", "mdc-toolbar__section--align-end")
                                ).withClass("mdc-toolbar__row")
                        ).withClass("mdc-toolbar mdc-toolbar--fixed"),
                        main(body).withClass("mdc-toolbar-fixed-adjust")
                ).withClass("mdc-typography")
        ).attr("lang", "en").render();
    }

    private ContainerTag navigationLink(String url, String title, String currentURL) {
        return a(title).withHref(url)
                .withClass("mdc-tab")
                .withCondClass(currentURL.equals(url), "mdc-tab mdc-tab--active");
    }
}
