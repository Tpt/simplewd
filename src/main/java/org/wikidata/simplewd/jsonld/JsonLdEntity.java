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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Thomas Pellissier Tanon
 */
public class JsonLdEntity {

    private String IRI;
    private List<String> types;
    private Map<String, Object> propertyValues;

    JsonLdEntity(String IRI, List<String> types, Map<String, Object> propertyValues) {
        this.IRI = IRI;
        this.types = types;
        this.propertyValues = propertyValues;
    }

    @JsonCreator
    public JsonLdEntity(Map<String, Object> content) {
        this((String) content.get("@id"), (List) content.get("@type"), content);
    }

    @JsonProperty("@id")
    public String getIRI() {
        return IRI;
    }

    @JsonProperty("@type")
    public List<String> getTypes() {
        return types;
    }

    @JsonAnyGetter
    public Map<String, Object> getPropertyValues() {
        return propertyValues;
    }

    public Object getPropertyValue(String property) {
        return propertyValues.get(property);
    }

    @JsonAnySetter
    public void setPropertyValue(String property, Object value) {
        propertyValues.put(property, value);
    }
}
