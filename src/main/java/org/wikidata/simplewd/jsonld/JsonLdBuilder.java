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

import com.vividsolutions.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.api.CommonsAPI;
import org.wikidata.simplewd.api.KartographerAPI;
import org.wikidata.simplewd.api.WikipediaAPI;
import org.wikidata.simplewd.model.*;
import org.wikidata.simplewd.model.value.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 */
public class JsonLdBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLdBuilder.class);
    private static final ShaclSchema SCHEMA = ShaclSchema.getSchema();
    private static final ShaclSchema.NodeShape IMAGE_OBJECT_SHAPE = SCHEMA.getShapeForClass("ImageObject");
    private static final ShaclSchema.NodeShape ARTICLE_SHAPE = SCHEMA.getShapeForClass("Article");
    private static final Optional<Set<String>> LANG_STRING_RANGE = Optional.of(Collections.singleton("rdf:langString"));
    private static final KartographerAPI KARTOGRAPHER_API = new KartographerAPI();

    private EntityLookup entityLookup;
    private CommonsAPI commonsAPI;
    private WikipediaAPI wikipediaAPI;

    public JsonLdBuilder(EntityLookup entityLookup, CommonsAPI commonsAPI, WikipediaAPI wikipediaAPI) {
        this.entityLookup = entityLookup;
        this.commonsAPI = commonsAPI;
        this.wikipediaAPI = wikipediaAPI;
    }

    public JsonLdRoot<JsonLdEntity> buildEntity(EntityValue entity, LocaleFilter localeFilter) {
        return new JsonLdRoot<>(
                buildContext(localeFilter.isMultilingualAccepted()),
                mapEntity(entity, true, localeFilter)
        );
    }

    private JsonLdEntity mapEntity(EntityValue entity, boolean withChildren, LocaleFilter localeFilter) {
        return mapEntity(entity, withChildren, localeFilter, SCHEMA.getShapeForClasses(entity.getTypes()));
    }

    private JsonLdEntity mapEntity(EntityValue entity, boolean withChildren, LocaleFilter localeFilter, ShaclSchema.NodeShape nodeShape) {
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

        Map<String, Object> propertyValues = new HashMap<>();
        nodeShape.getProperties().forEach(propertyShape -> {
            String property = propertyShape.getProperty();
            if (propertyShape.getDatatypes().equals(LANG_STRING_RANGE)) {
                if (localeFilter.isMultilingualAccepted()) {
                    if (propertyShape.isUniqueLang()) {
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
                } else {
                    if (propertyShape.isUniqueLang()) {
                        propertyValues.put(property, null);
                        localeFilter.getBestValues(entity.getValues(property)).findAny()
                                .ifPresent(value -> propertyValues.put(property, value));
                    } else {
                        propertyValues.put(property,
                                localeFilter.getBestValues(entity.getValues(property))
                                        .toArray());
                    }
                }
            } else {
                Stream<Object> values = entity.getValues(property).flatMap(value -> {
                    if (value instanceof EntityValue) {
                        return Stream.of(propertyShape.getNodeShape()
                                .map(rangeShape -> mapEntity((EntityValue) value, withChildren, localeFilter, rangeShape))
                                .orElseGet(() -> mapEntity((EntityValue) value, withChildren, localeFilter))
                        );
                    } else if (withChildren && value instanceof EntityIdValue) {
                        try {
                            return Stream.of(entityLookup.getEntityForIRI(value.toString())
                                    .map(e -> (Object) propertyShape.getNodeShape()
                                            .map(rangeShape -> mapEntity(e, false, localeFilter, rangeShape))
                                            .orElseGet(() -> mapEntity(e, false, localeFilter))
                                    ).orElse(value));
                        } catch (Exception e) {
                            LOGGER.info(e.getMessage(), e);
                        }
                    } else if (value instanceof CommonsFileValue) {
                        if (withChildren) {
                            try {
                                return Stream.of(mapEntity(commonsAPI.getImage(value.toString()), false, localeFilter, IMAGE_OBJECT_SHAPE));
                            } catch (Exception e) {
                                LOGGER.info(e.getMessage(), e);
                            }
                        }
                    } else {
                        boolean plainSerialization = propertyShape.getDatatypes().map(dts -> dts.size() == 1).orElse(false);
                        if (plainSerialization) {
                            return Stream.of(value.toString());
                        } else {
                            return Stream.of(value);
                        }
                    }
                    return Stream.empty();
                });
                if (propertyShape.getMaxCount() <= 1) {
                    propertyValues.put(property, null);
                    values.findAny().ifPresent(value -> propertyValues.put(property, value));
                } else {
                    propertyValues.put(property, values.collect(Collectors.toList()));
                }
            }
        });

        if (withChildren) {
            buildArticleFromWikipedia(entity, localeFilter)
                    .ifPresent(article -> propertyValues.put("mainEntityOfPage", new Object[]{
                            mapEntity(article, true, localeFilter, ARTICLE_SHAPE)
                    }));
            buildGeoValueFromKartographer(entity)
                    .ifPresent(geoValue -> propertyValues.put("shape", geoValue));
        }

        return new JsonLdEntity(entity.getIRI(), entity.getTypes().collect(Collectors.toList()), propertyValues);
    }

    private Optional<GeoValue> buildGeoValueFromKartographer(EntityValue entity) {
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

    private Optional<EntityValue> buildArticleFromWikipedia(EntityValue entity, LocaleFilter localeFilter) {
        //We only do geo shape lookup for Places in order to avoid unneeded requests
        return entity.getValues("sameAs")
                .map(v -> (URIValue) v)
                .filter(uri -> uri.getURI().getHost().equals(localeFilter.getBestLocale().getLanguage() + ".wikipedia.org"))
                .findAny()
                .flatMap(uri -> {
                    try {
                        return Optional.of(wikipediaAPI.getWikipediaArticle(uri.toString()));
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }

    private JsonLdContext buildContext(boolean multilingual) {
        JsonLdContext context = new JsonLdContext();

        SCHEMA.getNodeShapes().forEach(nodeShape -> nodeShape.getProperties().forEach(propertyShape -> {
            propertyShape.getNodeShape().ifPresent(rangeShape ->
                    context.addPropertyRange(propertyShape.getProperty(), "@id")
            );
            propertyShape.getDatatypes().ifPresent(datatypes -> {
                if (datatypes.size() == 1) {
                    if (datatypes.equals(Collections.singleton("rdf:langString"))) {
                        if (multilingual) {
                            context.addLanguageContainerToProperty(propertyShape.getProperty());
                        }
                    } else {
                        context.addPropertyRange(propertyShape.getProperty(), datatypes.iterator().next());
                    }
                }
            });
        }));
        return context;
    }
}
