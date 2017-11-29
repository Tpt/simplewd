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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.value.EntityIdValue;
import org.wikidata.simplewd.model.value.EntityValue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Thomas Pellissier Tanon
 */
public class WikipediaAPI {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final EntityIdValue WIKIPEDIA_LICENSE = new EntityIdValue("wd:Q14946043");
    private LoadingCache<String, Summary> summaryCache = CacheBuilder.newBuilder()
            .maximumSize(16384) //TODO: configure?
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, Summary>() {
                @Override
                public Summary load(String pageIRI) throws IOException {
                    return buildSummary(pageIRI);
                }
            });

    public WikipediaAPI() {
    }

    public EntityValue getWikipediaArticle(String articleIRI) throws IOException {
        try {
            Summary summary = summaryCache.get(articleIRI);
            EntityValue entity = new EntityValue(articleIRI);
            entity.addType("Article");
            Locale articleLocale = Locale.forLanguageTag(summary.getLanguageCode());
            entity.addClaim(new Claim("name", summary.getTitle(), articleLocale));
            entity.addClaim(new Claim("articleBody", summary.getExtract(), articleLocale));
            entity.addClaim(new Claim("license", WIKIPEDIA_LICENSE));
            //TODO: inLanguage with the language object
            return entity;
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    private Summary buildSummary(String pageIRI) throws IOException {
        return MAPPER.readValue(getURLForPageAction("summary", pageIRI).openStream(), Summary.class);
    }

    private URL getURLForPageAction(String action, String pageIRI) throws MalformedURLException {
        URL pageURL = new URL(pageIRI);
        return new URL("https://" + pageURL.getHost() + pageURL.getPath().replaceFirst("/wiki/", "/api/rest_v1/page/" + action + "/") + "?redirect=false");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Summary {

        private String title;
        private String extract;
        private Image image;
        private String lang;
        private String dir;
        private String timestamp;

        @JsonCreator
        Summary(
                @JsonProperty("title") String title,
                @JsonProperty("extract") String extract,
                @JsonProperty("originalimage") Image image,
                @JsonProperty("lang") String lang,
                @JsonProperty("dir") String dir,
                @JsonProperty("timestamp") String timestamp
        ) {
            this.title = title;
            this.extract = extract;
            this.image = image;
            this.lang = lang;
            this.dir = dir;
            this.timestamp = timestamp;
        }

        String getTitle() {
            return title;
        }

        String getExtract() {
            return extract;
        }

        Optional<Image> getImage() {
            return Optional.ofNullable(image);
        }

        String getLanguageCode() {
            return lang;
        }

        String getLanguageDirection() {
            return dir;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Image {

        private String source;
        private int width;
        private int height;

        @JsonCreator
        Image(@JsonProperty("source") String source, @JsonProperty("width") int width, @JsonProperty("height") int height) {
            this.source = source;
            this.width = width;
            this.height = height;
        }

        String getSource() {
            return source;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }
    }
}
