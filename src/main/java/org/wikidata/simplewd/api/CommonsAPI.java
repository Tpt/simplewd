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
import org.wikidata.simplewd.model.CommonsImage;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Thomas Pellissier Tanon
 */
public class CommonsAPI {

    private static final String URL_TEMPLATE = "https://commons.wikimedia.org/w/api.php?action=query&format=json&titles=File:$1&prop=imageinfo&iiprop=timestamp|url|size|mime|mediatype|extmetadata&iiextmetadatafilter=DateTime|DateTimeOriginal|ObjectName|ImageDescription|License|LicenseShortName|UsageTerms|LicenseUrl|Credit|Artist|AuthorCount|GPSLatitude|GPSLongitude|Permission|Attribution|AttributionRequired|NonFree|Restrictions|DeletionReason";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LoadingCache<String, CommonsImage> imageCache = CacheBuilder.newBuilder()
            .maximumSize(16384) //TODO: configure?
            .expireAfterWrite(14, TimeUnit.DAYS)
            .build(new CacheLoader<String, CommonsImage>() {
                @Override
                public CommonsImage load(String title) throws IOException {
                    return requestImage(title);
                }
            });

    public CommonsImage getImage(String title) throws IOException {
        try {
            return imageCache.get(title);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    private CommonsImage requestImage(String title) throws IOException {
        URL apiURL = new URL(URL_TEMPLATE.replace("$1", URLEncoder.encode(title, "UTF-8")));
        JsonNode rootNode = OBJECT_MAPPER.readTree(apiURL.openStream());
        if (rootNode.isObject() && rootNode.has("query")) {
            JsonNode queryNode = rootNode.get("query");
            if (queryNode.isObject() && queryNode.has("pages")) {
                for (JsonNode pageNode : queryNode.get("pages")) {
                    if (pageNode.isObject() && pageNode.has("imageinfo")) {
                        for (JsonNode infoNode : pageNode.get("imageinfo")) {
                            CommonsImage.License license = null;
                            if (infoNode.has("extmetadata")) {
                                JsonNode metadata = infoNode.get("extmetadata");
                                String name = "";
                                if (metadata.has("UsageTerms")) {
                                    name = metadata.get("UsageTerms").get("value").asText();
                                }
                                if (metadata.has("LicenseUrl")) {
                                    license = new CommonsImage.License(metadata.get("LicenseUrl").get("value").asText(), name);
                                }
                            }
                            return new CommonsImage(
                                    infoNode.get("url").asText(),
                                    infoNode.get("descriptionurl").asText(),
                                    infoNode.get("mime").asText(),
                                    infoNode.get("width").asInt(),
                                    infoNode.get("height").asInt(),
                                    license
                                    //TODO infoNode.get("timestamp").asText()
                            );
                        }
                    }
                }
            }
        }
        throw new IOException("Image data not found: " + OBJECT_MAPPER.writeValueAsString(rootNode));
    }
}
