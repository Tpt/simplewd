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
import j2html.tags.DomContent;

import static j2html.TagCreator.*;

public class MainRenderer extends HTMLRenderer {
    public String render() {
        ContainerTag content = div(section(
                p(join("SimpleWD provides a simple REST API that exposes Wikidata content in ", a("JSON-LD").withHref("https://json-ld.org"), " using the ", a("schema.org").withHref("https://schema.org"), " vocabulary.")),
                p(join("The API documentation is available ", a("here").withHref("/simplewd/swagger.html"), " and the source code and bug tracker ", a("on GitHub").withHref("https://github.com/Tpt/simplewd/"), ".")),
                p(join("The ", a("SHACL").withHref("https://www.w3.org/TR/shacl/"), " file defining the data schema is available ", a("here").withHref("https://tools.wmflabs.org/simplewd/declaration.ttl"), ".")),
                p(join("Some examples of entities rendered in HTML: ", ul(
                        example("wd:Q42", "Douglas Adams"),
                        example("wd:Q192724", "Iron Man"),
                        example("wd:Q1757", "Helsinki"),
                        example("wd:Q12418", "Mona Lisa"),
                        example("wd:Q83186", "Romeo and Juliet")
                )))
        ).withClass("mdc-card__supporting-text")).withClass("mdc-card");
        return super.render("SimpleWD", "/simplewd", content);
    }

    private DomContent example(String id, String name) {
        return li(join(
                a(name).withHref("/simplewd/v0/entity/" + id),
                " ",
                a(small("(JSON)")).withHref("/simplewd/v0/entity/" + id + ".json"),
                " ",
                a(small("(JSON-LD)")).withHref("/simplewd/v0/entity/" + id + ".jsonld"),
                " ",
                a(small("(Turtle)")).withHref("/simplewd/v0/entity/" + id + ".ttl"),
                " ",
                a(small("(N-Triples)")).withHref("/simplewd/v0/entity/" + id + ".nt"),
                " ",
                a(small("(RDF/XML)")).withHref("/simplewd/v0/entity/" + id + ".rdf")
        ));
    }
}
