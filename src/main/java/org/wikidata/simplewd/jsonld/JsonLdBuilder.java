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
import org.wikidata.simplewd.api.CommonsAPI;
import org.wikidata.simplewd.api.KartographerAPI;
import org.wikidata.simplewd.model.*;
import org.wikidata.simplewd.model.value.CommonsFileValue;
import org.wikidata.simplewd.model.value.EntityValue;
import org.wikidata.simplewd.model.value.GeoValue;
import org.wikidata.simplewd.model.value.LocaleStringValue;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private static final KartographerAPI KARTOGRAPHER_API = new KartographerAPI();

	private EntityLookup entityLookup;
	private CommonsAPI commonsAPI;

	public JsonLdBuilder(EntityLookup entityLookup, CommonsAPI commonsAPI) {
		this.entityLookup = entityLookup;
		this.commonsAPI = commonsAPI;
	}

    public JsonLdRoot<JsonLdEntity> buildEntity(Entity entity, LocaleFilter localeFilter) {
        JsonLdContext context = new JsonLdContext();
        return new JsonLdRoot<>(context, mapEntity(entity, true, localeFilter, context));
    }

    private JsonLdEntity mapEntity(Entity entity, boolean withChildren, LocaleFilter localeFilter, JsonLdContext context) {
        Map<String, Object> propertyValues = new HashMap<>();

		if (withChildren) {
			//We preload entities
			try {
				entityLookup.getEntitiesForIRI(entity.getClaims().map(Claim::getValue).flatMap(value ->
						(value instanceof EntityValue) ? Stream.of(value.toString()) : Stream.empty()
				).toArray(String[]::new));
			} catch (Exception e) {
				//We ignore the errors
			}
		}

		entity.getProperties().stream().sorted().forEach(property -> {
			if (PROPERTIES_WITH_BY_LANGUAGE_CONTAINER.contains(property)) {
                if (localeFilter.isMultilingualAccepted()) {
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
                    context.addLanguageContainerToProperty(property);
                } else {
                    if (FUNCTIONAL_PROPERTIES.contains(property)) {
                        localeFilter.getBestValues(entity.getValues(property)).findAny().ifPresent(value -> {
                            propertyValues.put(property, value.toString());
                            context.addPropertyLocale(property, value.getLocale());
                        });
                    } else {
                        propertyValues.put(property,
                                localeFilter.getBestValues(entity.getValues(property))
                                        .peek(value -> context.addPropertyLocale(property, value.getLocale()))
                                        .map(Object::toString)
                                        .toArray(String[]::new));
                    }
                }
            } else {
				Stream<Object> values = entity.getValues(property).flatMap(value -> {
					if (withChildren && value instanceof EntityValue) {
						try {
							return Stream.of(entityLookup.getEntityForIRI(value.toString())
                                    .map(e -> (Object) mapEntity(e, false, localeFilter, context))
                                    .orElse(value));
						} catch (Exception e) {
							LOGGER.info(e.getMessage(), e);
						}
					} else if (value instanceof CommonsFileValue) {
						if (withChildren) {
							try {
								return Stream.of(commonsAPI.getImage(value.toString()));
							} catch (Exception e) {
								LOGGER.info(e.getMessage(), e);
							}
						}
					} else if (value.hasPlainSerialization()) {
						context.addPropertyRange(property, value.getType());
						return Stream.of(value.toString());
					} else {
						return Stream.of(value);
					}
					return Stream.empty();
				});
				if (FUNCTIONAL_PROPERTIES.contains(property)) {
					values.findAny().ifPresent(value -> propertyValues.put(property, value));
				} else {
					propertyValues.put(property, values.collect(Collectors.toList()));
				}
			}
		});

		if (withChildren) {
			buildGeoValueFromKartographer(entity)
					.ifPresent(geoValue -> propertyValues.put("geo", geoValue));
		}

		return new JsonLdEntity(entity.getIRI(), entity.getTypes().collect(Collectors.toList()), propertyValues);
	}

	private Optional<Object> buildGeoValueFromKartographer(Entity entity) {
		try {
			//We only do geo shape lookup for Places in order to avoid unneeded requests
			if (entity.getTypes().anyMatch(type -> type.equals("Place"))) {
				Geometry shape = KARTOGRAPHER_API.getShapeForItemId(Namespaces.expand(entity.getIRI()));
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
