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

package org.wikidata.simplewd.http;

import io.javalin.Context;
import io.javalin.HaltException;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.api.CommonsAPI;
import org.wikidata.simplewd.api.WikidataAPI;
import org.wikidata.simplewd.api.WikipediaAPI;
import org.wikidata.simplewd.http.html.EntityRenderer;
import org.wikidata.simplewd.http.html.MainRenderer;
import org.wikidata.simplewd.http.html.SwaggerRenderer;
import org.wikidata.simplewd.jsonld.JsonLdBuilder;
import org.wikidata.simplewd.jsonld.JsonLdEntity;
import org.wikidata.simplewd.jsonld.JsonLdRoot;
import org.wikidata.simplewd.model.EntityLookup;
import org.wikidata.simplewd.model.LocaleFilter;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.simplewd.model.value.EntityValue;

import java.io.IOException;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private EntityLookup entityLookup;
    private JsonLdBuilder jsonLdBuilder;
    private EntityRenderer entityRenderer;

    private Main() throws IOException {
        entityLookup = new WikidataAPI();
        CommonsAPI commonsAPI = new CommonsAPI();
        WikipediaAPI wikipediaAPI = new WikipediaAPI();
        jsonLdBuilder = new JsonLdBuilder(entityLookup, commonsAPI, wikipediaAPI);
        entityRenderer = new EntityRenderer(entityLookup, commonsAPI, wikipediaAPI);
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        Javalin.create()
                .enableCorsForOrigin("*")
                .enableStaticFiles("/public")
                .get("", ctx -> ctx.redirect("/simplewd"))
                .get("/simplewd", ctx -> ctx.html((new MainRenderer()).render()))
                .get("/simplewd/swagger.html", ctx -> ctx.html((new SwaggerRenderer()).render()))
                .get("/simplewd/entity/:id", ctx -> ctx.redirect("/simplewd/v0/entity/" + ctx.param("id")))
                .get("/simplewd/v0/entity/:id", ctx -> {
                    LocaleFilter localeFilter = getLocaleFilter(ctx);
                    String id = ctx.param("id");
                    if (id == null) {
                        throw new HaltException(404, "You should provide an entity ID");
                    }
                    String type = null;
                    if (id.contains(".")) {
                        String[] parts = id.split("\\.");
                        id = parts[0];
                        type = parts[1];
                    }
                    switch (getResponseContentType(ctx, type)) {
                        case JSON_LD:
                            ctx.json(main.getResourceAsJson(id, localeFilter));
                            ctx.contentType("application/ld+json");
                            break;
                        case JSON:
                            ctx.json(main.getResourceAsJson(id, localeFilter).getContent());
                            break;
                        case HTML:
                            ctx.html(main.getResourceAsHTML(id, localeFilter));
                            break;
                    }
                })
                .port(getPort())
                .start();
    }

    private static int getPort() {
        String port = System.getenv("PORT");
        return (port != null) ? Integer.valueOf(port) : 7000;
    }

    private static ContentType getResponseContentType(Context ctx, String type) {
        if (type != null && type.length() > 0) {
            switch (type) {
                case "json-ld":
                case "jsonld":
                    return ContentType.JSON_LD;
                case "json":
                    return ContentType.JSON;
                case "html":
                    return ContentType.HTML;
                default:
                    throw new HaltException(406, "The " + type + " format is not supported");
            }
        }

        String accept = ctx.header("Accept");
        if (accept != null) {
            if (accept.contains("text/html")) {
                return ContentType.HTML;
            } else if (accept.contains("application/ld+json")) {
                return ContentType.JSON_LD;
            } else if (accept.contains("application/json")) {
                return ContentType.JSON;
            }
        }
        if (accept == null || accept.length() == 0) {
            return ContentType.JSON;
        }

        throw new HaltException(406, "This endpoint only supports JSON and HTML");
    }

    private static LocaleFilter getLocaleFilter(Context ctx) {
        String acceptLanguage = ctx.queryParam("lang");
        if (acceptLanguage == null || acceptLanguage.length() == 0) {
            acceptLanguage = ctx.header("Accept-Language");
        }
        if (acceptLanguage == null || acceptLanguage.length() == 0) {
            acceptLanguage = "mul";
        }
        
        try {
            return new LocaleFilter(acceptLanguage);
        } catch (IllegalArgumentException e) {
            throw new HaltException(400, "Your Accept-Language header is not valid");
        }
    }

    private JsonLdRoot<JsonLdEntity> getResourceAsJson(String id, LocaleFilter localeFilter) {
        return jsonLdBuilder.buildEntity(getResource(id), localeFilter);
    }

    private String getResourceAsHTML(String id, LocaleFilter localeFilter) {
        return entityRenderer.render(getResource(id), localeFilter);
    }

    private EntityValue getResource(String id) {
        LOGGER.info("Retrieving: " + id);
        try {
            return entityLookup.getEntityForIRI(id).orElseThrow(() -> new HaltException(404, Namespaces.expand(id) + " not found"));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new HaltException(500);
        } catch (IllegalArgumentException e) {
            throw new HaltException(400, Namespaces.expand(id) + " is not a supported entity");
        }
    }

    private enum ContentType {
        JSON_LD,
        JSON,
        HTML
    }
}
