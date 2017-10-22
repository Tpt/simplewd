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
import org.wikidata.simplewd.http.html.EntityRenderer;
import org.wikidata.simplewd.http.html.MainRenderer;
import org.wikidata.simplewd.http.html.SwaggerRenderer;
import org.wikidata.simplewd.jsonld.JsonLdBuilder;
import org.wikidata.simplewd.mapping.ItemMapper;
import org.wikidata.simplewd.model.Entity;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Pattern ITEM_URI_PATTERN = Pattern.compile("^wd:Q\\d+$");
    private static final JsonLdBuilder JSON_LD_BUILDER = new JsonLdBuilder();

    private ItemMapper itemMapper;

    private Main() throws IOException {
        itemMapper = new ItemMapper((new DumpProcessingController("wikidatawiki")).getSitesInformation());
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

        id = Namespaces.reduce(id);
        if (!ITEM_URI_PATTERN.matcher(id).matches()) {
            throw new HaltException(400, "This entity URI is not supported: " + Namespaces.expand(id));
        }

        try {
            EntityDocument entity = WikibaseDataFetcher.getWikidataDataFetcher().getEntityDocument(id.replace("wd:", ""));
            if (entity == null) {
                throw new HaltException(404, "Entity not found");
            } else if (entity instanceof ItemDocument) {
                return itemMapper.map((ItemDocument) entity);
            } else {
                throw new HaltException(400, "Not supported entity type");
            }
        } catch (MediaWikiApiErrorException e) {
            if (e.getErrorCode().equals("no-such-entity")) {
                throw new HaltException(404, "Entity not found");
            } else {
                LOGGER.warn(e.getMessage(), e);
                throw new HaltException(500);
            }
        }
    }
}
