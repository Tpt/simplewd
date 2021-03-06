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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Thomas Pellissier Tanon
 */
public class URIValue implements Value {

    private URI value;

    public URIValue(URI value) {
        this.value = value;
    }

    public URIValue(String value) throws URISyntaxException {
        this.value = new URI(value);
    }

    @JsonIgnore
    public URI getValue() {
        return value;
    }

    @Override
    @JsonProperty("type")
    public String getType() {
        return "xsd:anyURI";
    }

    @Override
    @JsonProperty("value")
    public String toString() {
        return value.toString();
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object value) {
        return (value instanceof URIValue) && ((URIValue) value).value.equals(value);
    }
}
