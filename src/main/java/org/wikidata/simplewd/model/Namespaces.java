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


import java.util.Map;
import java.util.TreeMap;

/**
 * @author Thomas Pellissier Tanon
 */
public class Namespaces {

    public static final String DEFAULT_NAMESPACE = "http://schema.org/";

    public static final Map<String, String> NAMESPACES = new TreeMap<>();

    static {
        //Some common namespaces
        NAMESPACES.put("goog", "http://schema.googleapis.com/");
        NAMESPACES.put("kg", "http://g.co/kg");
        NAMESPACES.put("owl", "http://www.w3.org/2002/07/owl#");
        NAMESPACES.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        NAMESPACES.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        NAMESPACES.put("schema", "http://schema.org/");
        NAMESPACES.put("wd", "http://www.wikidata.org/entity/");
        NAMESPACES.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    }

    public static String expand(String qualifiedName) {
        int namespaceEnd = qualifiedName.indexOf(':');
        if (namespaceEnd == -1) {
            return DEFAULT_NAMESPACE + qualifiedName;
        }

        String prefix = qualifiedName.substring(0, namespaceEnd);
        if (NAMESPACES.containsKey(prefix)) {
            return NAMESPACES.get(prefix) + qualifiedName.substring(namespaceEnd + 1);
        } else {
            return qualifiedName;
        }
    }

    public static String reduce(String IRI) {
        if (IRI.startsWith(DEFAULT_NAMESPACE)) {
            IRI = IRI.substring(DEFAULT_NAMESPACE.length());
        }
        for (Map.Entry<String, String> namespace : NAMESPACES.entrySet()) {
            if (IRI.startsWith(namespace.getValue())) {
                IRI = namespace.getKey() + ":" + IRI.substring(namespace.getValue().length());
                break;
            }
        }
        return IRI;
    }
}
