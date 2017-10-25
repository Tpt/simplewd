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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Thomas Pellissier Tanon
 * TODO: use Entity for that
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonsImage {

    private String contentURI;
    private String descriptionURI;
    private String contentType;
    private int width;
    private int height;
    private License license;

    public CommonsImage(String contentURI, String descriptionURI, String contentType, int width, int height, License license) {
        this.contentURI = contentURI;
        this.descriptionURI = descriptionURI;
        this.contentType = contentType;
        this.width = width;
        this.height = height;
        this.license = license;
    }

    @JsonProperty("@type")
    public String[] getTypes() {
        return new String[]{"ImageObject"};
    }

    @JsonProperty("contentUrl")
    public String getContentURI() {
        return contentURI;
    }

    //TODO: find a good property
    public String getDescriptionURI() {
        return descriptionURI;
    }

    @JsonProperty("fileFormat")
    public String getFileFormat() {
        return contentType;
    }

    @JsonProperty("width")
    public int getWidth() {
        return width;
    }

    @JsonProperty("height")
    public int getHeight() {
        return height;
    }

    @JsonProperty("license")
    public License getLicense() {
        return license;
    }

    public static class License {
        private String id;
        private String name;

        public License(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @JsonProperty("@id")
        public String getId() {
            return id;
        }

        @JsonProperty("@type")
        public String[] getType() {
            return new String[]{"CreativeWork"};
        }

        @JsonProperty("name")
        public String getName() {
            return name;
        }
    }
}
