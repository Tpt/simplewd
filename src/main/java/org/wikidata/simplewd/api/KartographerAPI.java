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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.CharStreams;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.geojson.GeoJsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Pellissier Tanon
 */
public class KartographerAPI {

    private static final Pattern WIKIDATA_ITEM_URI_PATTERN = Pattern.compile("^http://www\\.wikidata\\.org/entity/Q\\d+$");
    private static final Pattern FEATURE_COLLECTION_PATTERN = Pattern.compile("\\{.*\"features\":\\[\\{.*\"geometry\":(.*)\\}\\].*\\}");
    private static final String EMPTY_FEATURE_COLLECTION = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final GeoJsonReader GEO_JSON_READER = new GeoJsonReader(GEOMETRY_FACTORY);
    private static final Geometry EMPTY_GEOMETRY = GEOMETRY_FACTORY.createGeometryCollection(new Geometry[]{});
    private LoadingCache<String, Geometry> shapeCache = CacheBuilder.newBuilder()
            .maximumSize(16384) //TODO: configure?
            .expireAfterWrite(14, TimeUnit.DAYS)
            .build(new CacheLoader<String, Geometry>() {
                @Override
                public Geometry load(String itemId) throws IOException, ParseException {
                    return requestShapeForItemId(itemId);
                }
            });

    public Geometry getShapeForItemId(String itemURI) throws IOException {
        if (!WIKIDATA_ITEM_URI_PATTERN.matcher(itemURI).matches()) {
            return EMPTY_GEOMETRY;
        }

        String itemId = itemURI.replace("http://www.wikidata.org/entity/", "");
        try {
            return shapeCache.get(itemId);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    private Geometry requestShapeForItemId(String itemId) throws IOException, ParseException {
        URL targetURLShape = new URL("https://maps.wikimedia.org/geoshape?getgeojson=1&ids=" + itemId);
        Geometry geoShape = geoGeoJSONRequest(targetURLShape);
        if (!geoShape.isEmpty()) {
            return geoShape;
        }

        URL targetURLLine = new URL("https://maps.wikimedia.org/geoline?getgeojson=1&ids=" + itemId);
        return geoGeoJSONRequest(targetURLLine);
    }

    private Geometry geoGeoJSONRequest(URL targetURL) throws IOException, ParseException {
        try (InputStream inputStream = targetURL.openStream()) {
            String geoJSON = CharStreams.toString(new InputStreamReader(inputStream));
            if (geoJSON.equals(EMPTY_FEATURE_COLLECTION)) {
                return EMPTY_GEOMETRY;
            }

            Matcher matcher = FEATURE_COLLECTION_PATTERN.matcher(geoJSON);
            if (matcher.matches()) {
                return GEO_JSON_READER.read(matcher.group(1));
            } else {
                throw new ParseException("The GeoJSON root should be a FeatureCollection");
            }
        }
    }
}
