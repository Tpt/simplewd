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

import io.javalin.HaltException;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.api.WikidataAPI;
import org.wikidata.simplewd.http.html.EntityRenderer;
import org.wikidata.simplewd.http.html.MainRenderer;
import org.wikidata.simplewd.http.html.SwaggerRenderer;
import org.wikidata.simplewd.jsonld.JsonLdBuilder;
import org.wikidata.simplewd.model.Entity;
import org.wikidata.simplewd.model.EntityLookup;
import org.wikidata.simplewd.model.Namespaces;

import java.io.IOException;
import java.util.Locale;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final JsonLdBuilder JSON_LD_BUILDER = new JsonLdBuilder();

    private EntityLookup entityLookup;

    private Main() throws IOException {
        entityLookup = new WikidataAPI();
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
                    String accept = ctx.header("Accept");
                    if (accept != null && accept.contains("text/html")) {
                        ctx.html((new EntityRenderer(Locale.ENGLISH)).render(main.getResource(ctx.param("id"))));
                    } else {
                        ctx.json(JSON_LD_BUILDER.buildEntity(main.getResource(ctx.param("id"))));
                        if (accept != null && accept.contains("application/ld+json")) {
                            ctx.contentType("application/ld+json");
                        }
                    }
                })
                .port(getPort())
                .start();
    }

    private static int getPort() {
        String port = System.getenv("PORT");
        return (port != null) ? Integer.valueOf(port) : 7000;
    }

    private Entity getResource(String id) {
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
}
