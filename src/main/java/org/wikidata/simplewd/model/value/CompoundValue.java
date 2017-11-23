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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.Namespaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Pellissier Tanon
 */
public class CompoundValue implements Value {

    private String IRI;

    private List<Claim> claims = new ArrayList<>();

    public CompoundValue(String IRI) {
        this.IRI = Namespaces.reduce(IRI);
    }

    public void addClaim(Claim claim) {
        claims.add(claim);
    }

    @JsonAnyGetter
    public Map<String, Value> getPropertyValues() {
        Map<String, Value> propertyValues = new HashMap<>();
        claims.forEach(claim -> propertyValues.put(claim.getProperty(), claim.getValue()));
        return propertyValues;
    }

    @Override
    @JsonIgnore
    public String getType() {
        return "Role";
    }

    @Override
    @JsonProperty("@id")
    public String toString() {
        return IRI;
    }

    @Override
    public int hashCode() {
        return IRI.hashCode();
    }

    @Override
    public boolean equals(Object value) {
        return (value instanceof CompoundValue) && ((CompoundValue) value).IRI.equals(value);
    }

    @Override
    public boolean hasPlainSerialization() {
        return false;
    }
}
