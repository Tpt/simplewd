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

package org.wikidata.simplewd.model;

import org.wikidata.simplewd.model.value.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.net.URI;
import java.util.Locale;

public class Claim {
    private String property;
    private Value value;

    public Claim(String property, Value value) {
        this.property = Namespaces.reduce(property);
        this.value = value;
    }

    public Claim(String property, String value) {
        this(property, new StringValue(value));
    }

    public Claim(String property, String value, Locale locale) {
        this(property, new LocaleStringValue(value, locale));
    }

    public Claim(String property, String value, String languageCode) {
        this(property, new LocaleStringValue(value, languageCode));
    }

    public Claim(String property, XMLGregorianCalendar value) {
        this(property, new CalendarValue(value));
    }

    public Claim(String property, URI value) {
        this(property, new URIValue(value));
    }

    public Claim(String property, BigInteger value) {
        this(property, new IntegerValue(value));
    }

    public String getProperty() {
        return property;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return property.hashCode() ^ value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Claim)) {
            return false;
        }
        Claim claim = (Claim) other;
        return property.equals(claim.property) && value.equals(claim.value);
    }
}
