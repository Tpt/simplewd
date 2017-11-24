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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author Thomas Pellissier Tanon
 */
public class CalendarValue implements Value {

    private static DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private XMLGregorianCalendar value;

    public CalendarValue(XMLGregorianCalendar value) {
        this.value = value;
    }

    public CalendarValue(String value) {
        this(DATATYPE_FACTORY.newXMLGregorianCalendar(value));
    }

    @Override
    @JsonProperty("precision")
    public String getType() {
        QName datatype = value.getXMLSchemaType();
        if (datatype.equals(DatatypeConstants.DATETIME)) {
            return "second";
        } else if (datatype.equals(DatatypeConstants.DATE)) {
            return "day";
        } else if (datatype.equals(DatatypeConstants.GYEARMONTH)) {
            return "month";
        } else if (datatype.equals(DatatypeConstants.GYEAR)) {
            return "year";
        } else {
            return "xsd:" + datatype.getLocalPart();
        }
    }

    @Override
    @JsonProperty("value")
    public String toString() {
        return value.toXMLFormat();
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object value) {
        return (value instanceof CalendarValue) && ((CalendarValue) value).value.equals(value);
    }

    @Override
    public int compareTo(Value o) {
        if (o instanceof CalendarValue) {
            return value.compare(((CalendarValue) o).value);
        } else {
            return toString().compareTo(o.toString());
        }
    }
}
