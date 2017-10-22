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

public class MainRenderer extends HTMLRenderer {
    public String render() {
        ContainerTag content = div(section(
                p(join("SimpleWD provides a simple REST API that exposes Wikidata content in ", a("JSON-LD").withHref("https://json-ld.org"), " using the ", a("schema.org").withHref("https://schema.org"), " vocabulary.")),
                p(join("The API documentation is available ", a("here").withHref("/simplewd/swagger.html"), ".")),
                p(join("Some examples of entities rendered in HTML: ", ul(
                        li(a("Douglas Adams").withHref("/simplewd/entity/wd:Q42")),
                        li(a("Iron Man").withHref("/simplewd/entity/wd:Q192724")),
                        li(a("Helsinki").withHref("/simplewd/entity/wd:Q1757"))
                )))
        ).withClass("mdc-card__supporting-text")).withClass("mdc-card");
        return super.render("SimpleWD", "/simplewd", content);
    }
}
