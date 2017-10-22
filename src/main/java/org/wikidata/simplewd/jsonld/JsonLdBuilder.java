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

package org.wikidata.simplewd.jsonld;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.api.KartographerAPI;
import org.wikidata.simplewd.model.Entity;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.simplewd.model.value.GeoValue;
import org.wikidata.simplewd.model.value.LocaleStringValue;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Thomas Pellissier Tanon
 */
public class JsonLdBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLdBuilder.class);
    private static final Set<String> FUNCTIONAL_PROPERTIES = Sets.newHashSet(
            "birthDate", "birthPlace",
            "deathDate", "deathPlace", "description",
            "flightNumber",
            "gender", "geo",
            "iataCode", "icaoCode", "isicV4", "isrcCode", "iswcCode",
            "leiCode",
            "naics", "name",
            "url"
    ); //TODO: schema
    private static final Set<String> PROPERTIES_WITH_BY_LANGUAGE_CONTAINER = Sets.newHashSet("alternateName", "description", "name");

    public JsonLdRoot<JsonLdEntity> buildEntity(Entity entity) {
        JsonLdContext context = new JsonLdContext();
        Map<String, Object> propertyValues = new HashMap<>();

        entity.getProperties().stream().sorted().forEach(property -> {
            if (PROPERTIES_WITH_BY_LANGUAGE_CONTAINER.contains(property)) {
                if (FUNCTIONAL_PROPERTIES.contains(property)) {
                    Map<String, String> valuesByLanguage = new HashMap<>();
                    entity.getValues(property).forEach(value -> {
                        if (value instanceof LocaleStringValue) {
                            valuesByLanguage.put(((LocaleStringValue) value).getLanguageCode(), value.toString());
                        } else {
                            LOGGER.warn("Value for a rdf:langString property that is not a language tagged string: " + value.toString());
                        }
                    });
                    propertyValues.put(property, valuesByLanguage);
                } else {
                    Map<String, List<String>> valuesByLanguage = new HashMap<>();
                    entity.getValues(property).forEach(value -> {
                        if (value instanceof LocaleStringValue) {
                            valuesByLanguage.computeIfAbsent(((LocaleStringValue) value).getLanguageCode(), k -> new ArrayList<>()).add(value.toString());
                        } else {
                            LOGGER.warn("Value for a rdf:langString property that is not a language tagged string: " + value.toString());
                        }
                    });
                    propertyValues.put(property, valuesByLanguage);
                }
                context.addPropertyContainer(property, "@language");
            } else {
                if (FUNCTIONAL_PROPERTIES.contains(property)) {
                    entity.getValue(property).ifPresent(value -> {
                        if (value.hasPlainSerialization()) {
                            context.addPropertyRange(property, value.getType());
                            propertyValues.put(property, value.toString());
                        } else {
                            propertyValues.put(property, value);
                        }
                    });
                } else {
                    entity.getValue(property).ifPresent(valueSample -> {
                        if (valueSample.hasPlainSerialization()) {
                            context.addPropertyRange(property, valueSample.getType());
                            propertyValues.put(property, entity.getValues(property).map(Object::toString).collect(Collectors.toList()));
                        } else {
                            propertyValues.put(property, entity.getValues(property).collect(Collectors.toList()));
                        }
                    });

                }
            }
        });

        buildGeoValueFromKartographer(entity)
                .ifPresent(geoValue -> propertyValues.put("geo", geoValue));

        return new JsonLdRoot<>(context, new JsonLdEntity(
                entity.getIRI(),
                entity.getTypes().collect(Collectors.toList()),
                propertyValues
        ));
    }

    private Optional<Object> buildGeoValueFromKartographer(Entity entity) {
        try {
            //We only do geo shape lookup for Places in order to avoid unneeded requests
            if (entity.getTypes().anyMatch(type -> type.equals("Place"))) {
                Geometry shape = KartographerAPI.getInstance()
                        .getShapeForItemId(Namespaces.expand(entity.getIRI()));
                if (!shape.isEmpty()) {
                    return Optional.of(GeoValue.buildGeoValue(shape));
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Optional.empty();
    }
}
