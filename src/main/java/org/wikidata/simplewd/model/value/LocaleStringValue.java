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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * @author Thomas Pellissier Tanon
 */
public class LocaleStringValue implements Value {

    private String value;

    private Locale locale;

    public LocaleStringValue(String value, Locale locale) {
        this.value = value;
        this.locale = locale;
    }

    public LocaleStringValue(String value, String languageCode) {
        this(value, Locale.forLanguageTag(languageCode));
    }

    @JsonIgnore
    public String getType() {
        return "rdf:langString";
    }

    @JsonIgnore
    public Locale getLocale() {
        return locale;
    }

    @JsonProperty("@language")
    public String getLanguageCode() {
        return locale.toLanguageTag();
    }

    @Override
    @JsonProperty("@value")
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object value) {
        return (value instanceof LocaleStringValue) && ((LocaleStringValue) value).value.equals(value);
    }

    @Override
    public boolean hasPlainSerialization() {
        return false;
    }
}
