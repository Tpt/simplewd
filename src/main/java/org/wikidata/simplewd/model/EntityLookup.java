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

package org.wikidata.simplewd.model;

import org.wikidata.simplewd.model.value.EntityValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface EntityLookup {

    default Optional<EntityValue> getEntityForIRI(String id) throws IOException {
        return Optional.ofNullable(getEntitiesForIRI(id).get(id));
    }

    /**
     * @return Map indexed by reduced IRI
     */
    default Map<String, EntityValue> getEntitiesForIRI(String... ids) throws IOException {
        Map<String, EntityValue> entities = new HashMap<>();
        for (String id : ids) {
            getEntityForIRI(id).ifPresent(entity -> entities.put(Namespaces.reduce(id), entity));
        }
        return entities;
    }
}
