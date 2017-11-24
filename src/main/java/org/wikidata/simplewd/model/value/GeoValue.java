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

package org.wikidata.simplewd.model.value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * @author Thomas Pellissier Tanon
 */
public abstract class GeoValue implements Value {

    private static final WKTReader WKT_READER = new WKTReader();
    private static final WKTWriter WKT_WRITER = new WKTWriter();
    private Geometry geometry;

    GeoValue(Geometry geometry) {
        this.geometry = geometry;
    }

    public static GeoValue buildGeoValue(String WKT) {
        try {
            return buildGeoValue(WKT_READER.read(WKT));
        } catch (ParseException e) {
            throw new IllegalArgumentException(WKT + " should be a valid Well-Known-Text string", e);
        }
    }

    public static GeoValue buildGeoValue(Geometry geometry) {
        if (geometry instanceof Point) {
            return new GeoCoordinatesValue((Point) geometry);
        } else {
            return new GeoShapeValue(geometry);
        }
    }

    @Override
    @JsonProperty("http://www.opengis.net/ont/geosparql#asWKT")
    public String toString() {
        return WKT_WRITER.write(geometry);
    }

    @Override
    public int hashCode() {
        return geometry.hashCode();
    }

    @Override
    public boolean equals(Object value) {
        return (value instanceof GeoValue) && ((GeoValue) value).geometry.equals(value);
    }
}
