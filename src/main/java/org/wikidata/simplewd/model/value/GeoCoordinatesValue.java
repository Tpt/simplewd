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
import com.vividsolutions.jts.geom.Point;

/**
 * @author Thomas Pellissier Tanon
 */
public class GeoCoordinatesValue extends GeoValue {

    private Point point;

    GeoCoordinatesValue(Point point) {
        super(point);

        this.point = point;
    }

    @JsonProperty("@id")
    public String getIRI() {
        return "geo:" + getLatitude() + "," + getLongitude();
    }

    @Override
    @JsonProperty("@type")
    public String getType() {
        return "GeoCoordinates";
    }

    @JsonProperty("latitude")
    public double getLatitude() {
        return point.getY();
    }

    @JsonProperty("longitude")
    public double getLongitude() {
        return point.getX();
    }
}
