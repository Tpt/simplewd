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
public class JsonLdContext {

    private static final Map<String, Object> BASIC_CONTEXT = new TreeMap<>();

    static {
        BASIC_CONTEXT.put("@vocab", Namespaces.DEFAULT_NAMESPACE);
        BASIC_CONTEXT.putAll(Namespaces.NAMESPACES);

        //Aliases without @
        BASIC_CONTEXT.put("id", "@id");
        BASIC_CONTEXT.put("language", "@language");
        BASIC_CONTEXT.put("type", "@type");
        BASIC_CONTEXT.put("value", "@value");

        //Calendar values precisions
        BASIC_CONTEXT.put("precision", "@type");
        BASIC_CONTEXT.put("second", "xsd:dateTime");
        BASIC_CONTEXT.put("day", "xsd:date");
        BASIC_CONTEXT.put("month", "xsd:gYearMonth");
        BASIC_CONTEXT.put("year", "xsd:gYear");

        BASIC_CONTEXT.put("shape", Collections.singletonMap("@id", "geo"));
        BASIC_CONTEXT.put("wkt", idAndTypeMap("http://www.opengis.net/ont/geosparql#asWKT", "http://www.opengis.net/ont/geosparql#wktLiteral"));
    }

    private static Map<String, Object> idAndTypeMap(String id, String type) {
        Map<String, Object> map = new TreeMap<>();
        map.put("@id", id);
        map.put("@type", type);
        return map;
    }

    private Map<String, Object> context;

    JsonLdContext() {
        context = new HashMap<>(BASIC_CONTEXT);
    }

    @JsonAnyGetter
    public Map<String, Object> getDefinition() {
        return context;
    }

    void addPropertyRange(String property, String range) {
        context.put(property, Collections.singletonMap("@type", range));
    }

    void addLanguageContainerToProperty(String property) {
        context.put(property, Collections.singletonMap("@container", "@language"));
    }
}
