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

package org.wikidata.simplewd.mapping.statement;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.value.GeoValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 */
class GlobeCoordinatesStatementMapper implements StatementMainGlobeCoordinatesValueMapper {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private String targetFieldName;

    GlobeCoordinatesStatementMapper(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    @Override
    public Stream<Claim> mapMainGlobeCoordinatesValue(GlobeCoordinatesValue value) throws InvalidWikibaseValueException {
        if (!value.getGlobe().equals(GlobeCoordinatesValue.GLOBE_EARTH)) {
            return Stream.empty(); //TODO: support other globes
        }

        return Stream.of(
                new Claim(targetFieldName, GeoValue.buildGeoValue(valueToGeometry(value)))
        );
    }

    private Geometry valueToGeometry(GlobeCoordinatesValue value) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(
                roundDegrees(value.getLongitude(), value.getPrecision()),
                roundDegrees(value.getLatitude(), value.getPrecision())
        ));
    }

    private double roundDegrees(double degrees, double precision) {
        if (precision <= 0) {
            precision = 1 / 3600;
        }
        double sign = degrees > 0 ? 1 : -1;
        double reduced = Math.round(Math.abs(degrees) / precision);
        double expanded = reduced * precision;
        return sign * expanded;
    }
}
