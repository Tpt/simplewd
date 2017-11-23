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

package org.wikidata.simplewd.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.wikidata.simplewd.mapping.ItemMapper;
import org.wikidata.simplewd.model.EntityLookup;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.simplewd.model.value.EntityValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.NoSuchEntityErrorException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class WikidataAPI implements EntityLookup {
    private static final Pattern ITEM_URI_PATTERN = Pattern.compile("^wd:Q\\d+$");
    private static final WikibaseDataFetcher DATA_FETCHER = new WikibaseDataFetcher(
            new ApiConnection("https://www.wikidata.org/w/api.php"),
            "http://www.wikidata.org/entity/"
    );

    public static final WikibaseDataFetcher getDataFetcher() {
        return DATA_FETCHER;
    }

    private ItemMapper itemMapper;
    private Cache<String, EntityValue> entityCache = CacheBuilder.newBuilder()
            .maximumSize(65536) //TODO: configure?
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

    public WikidataAPI() throws IOException {
        itemMapper = new ItemMapper((new DumpProcessingController("wikidatawiki")).getSitesInformation());
    }

    @Override
    public Map<String, EntityValue> getEntitiesForIRI(String... ids) throws IOException {
        Map<String, EntityValue> entities = new HashMap<>();
        List<String> idsToRetrieve = new ArrayList<>();
        for (String inputId : ids) {
            String id = Namespaces.reduce(inputId);
            if (!ITEM_URI_PATTERN.matcher(id).matches()) {
                throw new IllegalArgumentException("This entity IRI is not supported: " + Namespaces.expand(id));
            }

            //TODO: JDK 9+: cleanup with ifPresentOrElse
            Optional<EntityValue> entityOptional = Optional.ofNullable(entityCache.getIfPresent(id));
            if (entityOptional.isPresent()) {
                entityOptional.ifPresent(entity -> entities.put(id, entity));
            } else {
                idsToRetrieve.add(id);
            }
        }

        retrieveEntitiesForIRI(idsToRetrieve).forEach((id, entity) -> {
            entities.put(id, entity);
            entityCache.put(id, entity);
        });

        return entities;
    }

    private Map<String, EntityValue> retrieveEntitiesForIRI(List<String> ids) throws IOException {
        try {
            Map<String, EntityDocument> documents = DATA_FETCHER.getEntityDocuments(
                    ids.stream().map(id -> id.replace("wd:", "")).toArray(String[]::new)
            );

            Map<String, EntityValue> entities = new HashMap<>();
            for (Map.Entry<String, EntityDocument> entry : documents.entrySet()) {
                EntityDocument document = entry.getValue();
                if (document instanceof ItemDocument) {
                    entities.put("wd:" + entry.getKey(), itemMapper.map((ItemDocument) document));
                } else if (document != null) {
                    throw new IOException("It seems to not be the IRI of an item: http://www.wikidata.org/entity/" + entry.getKey());
                }
            }
            return entities;
        } catch (NoSuchEntityErrorException e) {
            //An entity does not exists, we get entities one by one if there are more than 1
            if (ids.size() > 1) {
                Map<String, EntityValue> result = new HashMap<>();
                for (String id : ids) {
                    retrieveEntitiesForIRI(Collections.singletonList(id)).forEach(result::put);
                }
                return result;
            } else {
                return Collections.emptyMap();
            }
        } catch (MediaWikiApiErrorException e) {
            throw new IOException(e);
        }
    }
}
