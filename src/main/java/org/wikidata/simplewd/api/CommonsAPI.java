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

package org.wikidata.simplewd.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.value.EntityValue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Thomas Pellissier Tanon
 */
public class CommonsAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsAPI.class);
    private static final String URL_TEMPLATE = "https://commons.wikimedia.org/w/api.php?action=query&format=json&titles=File:$1&prop=imageinfo&iiprop=timestamp|url|size|mime|mediatype|extmetadata&iiextmetadatafilter=DateTime|DateTimeOriginal|ObjectName|ImageDescription|License|LicenseShortName|UsageTerms|LicenseUrl|Credit|Artist|AuthorCount|GPSLatitude|GPSLongitude|Permission|Attribution|AttributionRequired|NonFree|Restrictions|DeletionReason";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LoadingCache<String, EntityValue> imageCache = CacheBuilder.newBuilder()
            .maximumSize(16384) //TODO: configure?
            .expireAfterWrite(14, TimeUnit.DAYS)
            .build(new CacheLoader<String, EntityValue>() {
                @Override
                public EntityValue load(String title) throws IOException {
                    return requestImage(title);
                }
            });

    public EntityValue getImage(String title) throws IOException {
        try {
            return imageCache.get(title);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    private EntityValue requestImage(String title) throws IOException {
        URL apiURL = new URL(URL_TEMPLATE.replace("$1", URLEncoder.encode(title, "UTF-8")));
        JsonNode rootNode = OBJECT_MAPPER.readTree(apiURL.openStream());
        if (rootNode.isObject() && rootNode.has("query")) {
            JsonNode queryNode = rootNode.get("query");
            if (queryNode.isObject() && queryNode.has("pages")) {
                for (JsonNode pageNode : queryNode.get("pages")) {
                    if (pageNode.isObject() && pageNode.has("imageinfo")) {
                        for (JsonNode infoNode : pageNode.get("imageinfo")) {
                            try {
                                URI imageURI = new URI(infoNode.get("url").asText());
                                EntityValue image = new EntityValue(imageURI.toString());
                                image.addType("ImageObject");
                                image.addClaim(new Claim("contentUrl", imageURI));
                                image.addClaim(new Claim("fileFormat", infoNode.get("mime").asText()));
                                image.addClaim(new Claim("width", infoNode.get("width").asInt()));
                                image.addClaim(new Claim("height", infoNode.get("height").asInt()));

                                if (infoNode.has("extmetadata")) {
                                    JsonNode metadata = infoNode.get("extmetadata");
                                    String name = "";
                                    if (metadata.has("UsageTerms")) {
                                        name = metadata.get("UsageTerms").get("value").asText();
                                    }
                                    if (metadata.has("LicenseUrl")) {
                                        //TODO: map to Wikidata
                                        try {
                                            URI licenceURI = new URI(metadata.get("LicenseUrl").get("value").asText());
                                            EntityValue license = new EntityValue(licenceURI.toString());
                                            image.addType("CreativeWork");
                                            license.addClaim(new Claim("name", name));
                                            license.addClaim(new Claim("url", licenceURI));
                                            image.addClaim("license", license);
                                        } catch (URISyntaxException e) {
                                            LOGGER.warn("Invalid file URI: " + metadata.get("LicenseUrl").get("value").asText());
                                        }
                                    }
                                }
                                return image;
                            } catch (URISyntaxException e) {
                                throw new IOException("Invalid file URI: " + infoNode.get("url").asText(), e);
                            }
                        }
                    }
                }
            }
        }
        throw new IOException("Image data not found: " + OBJECT_MAPPER.writeValueAsString(rootNode));
    }
}
