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

import org.wikidata.simplewd.mapping.ItemMapper;
import org.wikidata.simplewd.model.Entity;
import org.wikidata.simplewd.model.EntityLookup;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

public class WikidataAPI implements EntityLookup {
    private static final Pattern ITEM_URI_PATTERN = Pattern.compile("^wd:Q\\d+$");

    private ItemMapper itemMapper;

    public WikidataAPI() throws IOException {
        itemMapper = new ItemMapper((new DumpProcessingController("wikidatawiki")).getSitesInformation());
    }

    @Override
    public Optional<Entity> getEntityForIRI(String id) throws IOException {
        id = Namespaces.reduce(id);
        if (!ITEM_URI_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("This entity IRI is not supported: " + Namespaces.expand(id));
        }

        try {
            EntityDocument entity = WikibaseDataFetcher.getWikidataDataFetcher().getEntityDocument(id.replace("wd:", ""));
            if (entity == null) {
                return Optional.empty();
            } else if (entity instanceof ItemDocument) {
                return Optional.of(itemMapper.map((ItemDocument) entity));
            } else {
                throw new IllegalArgumentException("This is not the IRI of an item: " + Namespaces.expand(id));
            }
        } catch (MediaWikiApiErrorException e) {
            if (e.getErrorCode().equals("no-such-entity")) {
                return Optional.empty();
            } else {
                throw new IOException(e);
            }
        }
    }
}
