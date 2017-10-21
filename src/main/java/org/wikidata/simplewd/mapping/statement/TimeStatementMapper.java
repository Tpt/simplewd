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

import org.wikidata.simplewd.model.Claim;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 */
class TimeStatementMapper implements StatementMainTimeValueMapper {

    private static DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private String targetFieldName;

    TimeStatementMapper(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    @Override
    public Stream<Claim> mapMainTimeValue(TimeValue value) throws InvalidWikibaseValueException {
        return convertTimeValue(value).map(calendarValue ->
                new Claim(targetFieldName, calendarValue)
        );
    }

    private Stream<XMLGregorianCalendar> convertTimeValue(TimeValue value) throws InvalidWikibaseValueException {
        if (value.getBeforeTolerance() != 0 || value.getAfterTolerance() != 0) {
            throw new InvalidWikibaseValueException("Time values with before/after tolerances are not supported.");
        }

        BigInteger year;
        int month = DatatypeConstants.FIELD_UNDEFINED;
        int day = DatatypeConstants.FIELD_UNDEFINED;
        int hour = DatatypeConstants.FIELD_UNDEFINED;
        int minute = DatatypeConstants.FIELD_UNDEFINED;
        int second = DatatypeConstants.FIELD_UNDEFINED;
        int timezone = DatatypeConstants.FIELD_UNDEFINED;
        switch (value.getPrecision()) {
            case TimeValue.PREC_SECOND:
                timezone = 0;
                second = value.getSecond();
                minute = value.getMinute();
                hour = value.getHour();
            case TimeValue.PREC_HOUR:
            case TimeValue.PREC_MINUTE:
            case TimeValue.PREC_DAY:
                day = value.getDay();
            case TimeValue.PREC_MONTH:
                month = value.getMonth();
            case TimeValue.PREC_YEAR:
                year = BigInteger.valueOf(value.getYear());
                break;
            default:
                return Stream.empty(); //TODO: Precision not supported. We ignore the value.
        }

        try {
            return Stream.of(DATATYPE_FACTORY.newXMLGregorianCalendar(year, month, day, hour, minute, second, null, timezone));
        } catch (IllegalArgumentException e) {
            throw new InvalidWikibaseValueException("Calendar value not supported by Java", e);
        }
    }
}
