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

public class SwaggerRenderer extends HTMLRenderer {
    public String render() {
        ContainerTag content = div(
                link().withRel("stylesheet").withHref("/simplewd/node_modules/swagger-ui-dist/swagger-ui.css"),
                div().withId("swagger-ui"),
                script().withSrc("/simplewd/node_modules/swagger-ui-dist/swagger-ui-bundle.js"),
                script().withSrc("/simplewd/node_modules/swagger-ui-dist/swagger-ui-standalone-preset.js"),
                scriptWithInlineFile("/public/simplewd/swagger_setup.js")
        ).withClass("mdc-card");
        return super.render("SimpleWD", "/simplewd/swagger.html", content);
    }
}
