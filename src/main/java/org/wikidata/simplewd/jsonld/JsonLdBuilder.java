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
import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.simplewd.model.Resource;
import org.wikidata.simplewd.model.value.GeoValue;
import org.wikidata.simplewd.model.value.LocaleStringValue;
import org.wikidata.simplewd.model.value.Value;

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
            "iataCode", "icaoCode",
            "name",
            "url"
    ); //TODO: schema
    private static final Set<String> PROPERTIES_WITH_BY_LANGUAGE_CONTAINER = Sets.newHashSet("alternateName", "description", "name");

    public JsonLdRoot<Entity> buildEntity(Resource resource) {
        Context context = new Context();
        Map<String, Object> propertyValues = new HashMap<>();

        resource.getClaims().collect(Collectors.groupingBy(Claim::getProperty)).forEach((property, claims) -> {
            if (PROPERTIES_WITH_BY_LANGUAGE_CONTAINER.contains(property)) {
                if (FUNCTIONAL_PROPERTIES.contains(property)) {
                    Map<String, String> valuesByLanguage = new HashMap<>();
                    claims.forEach(claim -> {
                        if (claim.getValue() instanceof LocaleStringValue) {
                            LocaleStringValue value = (LocaleStringValue) claim.getValue();
                            valuesByLanguage.put(value.getLanguageCode(), value.toString());
                        } else {
                            LOGGER.warn("Value for a rdf:langString property that is not a language tagged string: " + claim.getValue().toString());
                        }
                    });
                    propertyValues.put(property, valuesByLanguage);
                } else {
                    Map<String, List<String>> valuesByLanguage = new HashMap<>();
                    claims.forEach(claim -> {
                        if (claim.getValue() instanceof LocaleStringValue) {
                            LocaleStringValue value = (LocaleStringValue) claim.getValue();
                            valuesByLanguage.computeIfAbsent(value.getLanguageCode(), k -> new ArrayList<>()).add(value.toString());
                        } else {
                            LOGGER.warn("Value for a rdf:langString property that is not a language tagged string: " + claim.getValue().toString());
                        }
                    });
                    propertyValues.put(property, valuesByLanguage);
                }
                context.addPropertyContainer(property, "@language");
            } else {
                if (FUNCTIONAL_PROPERTIES.contains(property)) {
                    Value value = claims.get(0).getValue();
                    if (value.hasPlainSerialization()) {
                        context.addPropertyRange(property, value.getType());
                        propertyValues.put(property, value.toString());
                    } else {
                        propertyValues.put(property, value);
                    }
                } else {
                    if (claims.get(0).getValue().hasPlainSerialization()) {
                        context.addPropertyRange(property, claims.get(0).getValue().getType());
                        propertyValues.put(property, claims.stream().map(Claim::getValue).map(Object::toString).collect(Collectors.toList()));
                    } else {
                        propertyValues.put(property, claims.stream().map(Claim::getValue).collect(Collectors.toList()));
                    }
                }
            }
        });

        buildGeoValueFromKartographer(resource)
                .ifPresent(geoValue -> propertyValues.put("geo", geoValue));

        return new JsonLdRoot<>(context, new Entity(
                resource.getIRI(),
                resource.getTypes().collect(Collectors.toList()),
                propertyValues
        ));
    }

    private Optional<Object> buildGeoValueFromKartographer(Resource resource) {
        try {
            //We only do geo shape lookup for Places in order to avoid unneeded requests
            if (resource.getTypes().anyMatch(type -> type.equals("Place"))) {
                Geometry shape = KartographerAPI.getInstance()
                        .getShapeForItemId(Namespaces.expand(resource.getIRI()));
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
