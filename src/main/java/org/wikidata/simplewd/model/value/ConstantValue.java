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
import com.fasterxml.jackson.annotation.JsonValue;
import org.wikidata.simplewd.model.Namespaces;

/**
 * @author Thomas Pellissier Tanon
 */
public class ConstantValue implements Value {

    private String IRI;

    public ConstantValue(String IRI) {
        this.IRI = Namespaces.reduce(IRI);
    }

    @Override
    @JsonIgnore
    public String getType() {
        return "@id";
    }

    @Override
    @JsonValue
    public String toString() {
        return IRI;
    }

    @Override
    public int hashCode() {
        return IRI.hashCode();
    }

    @Override
    public boolean equals(Object value) {
        return (value instanceof ConstantValue) && ((ConstantValue) value).IRI.equals(value);
    }
}
