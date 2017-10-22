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

import org.wikidata.simplewd.model.value.ResourceValue;
import org.wikidata.simplewd.model.value.Value;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Entity {
    private String IRI;
    private Set<String> types = new HashSet<>();
    private Set<Claim> claims = new HashSet<>();

    public Entity(String IRI) {
        this.IRI = Namespaces.reduce(IRI);
    }

    public String getIRI() {
        return IRI;
    }

    public Stream<String> getTypes() {
        return types.stream();
    }

    public void addType(String typeIRI) {
        types.add(Namespaces.reduce(typeIRI));
    }

    public Stream<Claim> getClaims() {
        return claims.stream();
    }

    public Set<String> getProperties() {
        return claims.stream().map(Claim::getProperty).collect(Collectors.toSet());
    }

    public Optional<Value> getValue(String property) {
        return claims.stream().filter(claim -> claim.getProperty().equals(property)).findAny().map(Claim::getValue);
    }

    public Stream<Value> getValues(String property) {
        return claims.stream().filter(claim -> claim.getProperty().equals(property)).map(Claim::getValue);
    }

    public void addClaim(Claim claim) {
        if (claim.getProperty().equals("@type")) {
            if (claim.getValue() instanceof ResourceValue) {
                types.add(claim.getValue().toString());
            } else {
                throw new IllegalArgumentException("The range of rdf:type is Entity");
            }
        } else {
            claims.add(claim);
        }
    }

    public void addClaim(String property, Value value) {
        addClaim(new Claim(property, value));
    }
}
