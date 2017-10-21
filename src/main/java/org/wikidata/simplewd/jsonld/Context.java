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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.wikidata.simplewd.model.Namespaces;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Thomas Pellissier Tanon
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Context {

    private static final Map<String, Object> BASIC_CONTEXT = new TreeMap<>();

    static {
        BASIC_CONTEXT.put("@vocab", Namespaces.DEFAULT_NAMESPACE);
        BASIC_CONTEXT.putAll(Namespaces.NAMESPACES);
    }

    private Map<String, Object> context;

    Context() {
        context = new HashMap<>(BASIC_CONTEXT);
    }

    @JsonAnyGetter
    public Map<String, Object> getDefinition() {
        return context;
    }

    void addPropertyRange(String property, String range) {
        context.put(property, Collections.singletonMap("@type", range));
    }

    void addPropertyContainer(String property, String container) {
        context.put(property, Collections.singletonMap("@container", container));
    }
}
