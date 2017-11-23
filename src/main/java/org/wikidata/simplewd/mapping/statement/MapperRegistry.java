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

package org.wikidata.simplewd.mapping.statement;

import com.google.common.collect.ImmutableMap;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Pellissier Tanon
 */
public class MapperRegistry {

    private Map<PropertyIdValue, StatementMapper> mapperForProperty = new HashMap<>();

    public MapperRegistry() {
        buildMappers();
    }

    private void buildMappers() {
        //TODO: IMDB, LinkedIn, Myspace, Pinterest, Tumblr...
        addTruthyMapping("P18", new CommonsFileSnakMapper("image"));
        addTruthyMapping("P19", new SimpleItemIdSnakMapper("birthPlace"));
        addTruthyMapping("P20", new SimpleItemIdSnakMapper("deathPlace"));
        addTruthyMapping("P21", new ConstantItemIdSnakMapper("gender", ImmutableMap.of(
                "Q6581097", "Male",
                "Q6581072", "Female"
        )));
        addTruthyMapping("P22", new SimpleItemIdSnakMapper("parent"));
        addTruthyMapping("P25", new SimpleItemIdSnakMapper("parent"));
        addRoleMapping("P26", "spouse");
        addRoleMapping("P27", "nationality");
        addTruthyMapping("P31", TypeMapper.getInstance());
        addTruthyMapping("P40", new SimpleItemIdSnakMapper("children"));
        addTruthyMapping("P50", new SimpleItemIdSnakMapper("author"));
        addTruthyMapping("P57", new SimpleItemIdSnakMapper("director"));
        addRoleMapping("P69", "alumniOf");
        //TODO: P86 composer vs musicBy
        addTruthyMapping("P98", new SimpleItemIdSnakMapper("editor"));
        addRoleMapping("P108", "worksFor");
        addTruthyMapping("P110", new SimpleItemIdSnakMapper("illustrator"));
        addTruthyMapping("P112", new SimpleItemIdSnakMapper("founder"));
        addTruthyMapping("P123", new SimpleItemIdSnakMapper("publisher"));
        addTruthyMapping("P131", new SimpleItemIdSnakMapper("containedInPlace"));
        addTruthyMapping("P136", new SimpleItemIdSnakMapper("genre"));
        addTruthyMapping("P144", new SimpleItemIdSnakMapper("isBasedOn"));
        addTruthyMapping("P150", new SimpleItemIdSnakMapper("containsPlace"));
        addRoleMapping("P161", "actor");
        addTruthyMapping("P162", new SimpleItemIdSnakMapper("producer"));
        addTruthyMapping("P166", new SimpleItemIdSnakMapper("award"));
        addTruthyMapping("P170", new SimpleItemIdSnakMapper("creator"));
        addTruthyMapping("P175", new SimpleItemIdSnakMapper("byArtist"));
        addTruthyMapping("P176", new SimpleItemIdSnakMapper("provider"));
        addTruthyMapping("P186", new SimpleItemIdSnakMapper("material"));
        addTruthyMapping("P212", new ISBNSnakMapper());
        addTruthyMapping("P214", new ExternalIdentifierSnakMapper("http://viaf.org/viaf/$1", "[1-9]\\d(\\d{0,7}|\\d{17,20})"));
        addTruthyMapping("P229", new SimpleStringSnakMapper("iataCode", "[A-Z0-9]{2}"));
        addTruthyMapping("P230", new SimpleStringSnakMapper("icaoCode", "[A-Z]{3}"));
        addTruthyMapping("P236", new ISSNSnakMapper());
        addTruthyMapping("P238", new SimpleStringSnakMapper("iataCode", "[A-Z]{3}"));
        addTruthyMapping("P239", new SimpleStringSnakMapper("icaoCode", "([A-Z]{2}|[CKY][A-Z0-9])[A-Z0-9]{2}"));
        //TODO: P249 tickerSymbol have ISO15022 compliant code
        addTruthyMapping("P272", new SimpleItemIdSnakMapper("productionCompany"));
        addTruthyMapping("P275", new SimpleItemIdSnakMapper("license"));
        addTruthyMapping("P276", new SimpleItemIdSnakMapper("location"));
        addTruthyMapping("P361", new SimpleItemIdSnakMapper("isPartOf"));
        addTruthyMapping("P433", new SimpleStringSnakMapper("issueNumber"));
        addTruthyMapping("P434", new ExternalIdentifierSnakMapper("http://musicbrainz.org/artist/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P435", new ExternalIdentifierSnakMapper("http://musicbrainz.org/work/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P436", new ExternalIdentifierSnakMapper("http://musicbrainz.org/release-group/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P453", new SimpleItemIdSnakMapper("roleName"));
        addRoleMapping("P463", "memberOf");
        addTruthyMapping("P478", new SimpleStringSnakMapper("volumeNumber"));
        addTruthyMapping("P483", new SimpleItemIdSnakMapper("recordedAt"));
        addTruthyMapping("P495", new SimpleItemIdSnakMapper("countryOfOrigin"));
        addTruthyMapping("P527", new SimpleItemIdSnakMapper("hasPart"));
        addTruthyMapping("P551", new SimpleItemIdSnakMapper("homeLocation"));
        addTruthyMapping("P569", new TimeSnakMapper("birthDate"));
        addTruthyMapping("P570", new TimeSnakMapper("deathDate"));
        addTruthyMapping("P571", new TimeSnakMapper("dateCreated"));
        addTruthyMapping("P577", new TimeSnakMapper("datePublished"));
        addTruthyMapping("P625", new GlobeCoordinatesSnakMapper("geo"));
        addTruthyMapping("P646", new ExternalIdentifierSnakMapper("http://g.co/kg$1", "(/m/0[0-9a-z_]{2,6}|/m/01[0123][0-9a-z_]{5})"));
        addTruthyMapping("P655", new SimpleItemIdSnakMapper("translator"));
        addTruthyMapping("P674", new SimpleItemIdSnakMapper("character"));
        addTruthyMapping("P676", new SimpleItemIdSnakMapper("lyricist"));
        addTruthyMapping("P734", new SimpleItemIdSnakMapper("familyName"));
        addTruthyMapping("P735", new SimpleItemIdSnakMapper("givenName"));
        addTruthyMapping("P767", new SimpleItemIdSnakMapper("contributor"));
        addTruthyMapping("P840", new SimpleItemIdSnakMapper("contentLocation"));
        addTruthyMapping("P856", new URIStatementMapper("url"));
        addTruthyMapping("P859", new SimpleItemIdSnakMapper("sponsor"));
        addTruthyMapping("P921", new SimpleItemIdSnakMapper("about"));
        addTruthyMapping("P957", new ISBNSnakMapper());
        addTruthyMapping("P966", new ExternalIdentifierSnakMapper("http://musicbrainz.org/label/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P982", new ExternalIdentifierSnakMapper("http://musicbrainz.org/area/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P1004", new ExternalIdentifierSnakMapper("http://musicbrainz.org/place/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P1113", new IntegerStatementMapper("numberOfEpisodes"));
        //TODO: P1191: firstPerformance move the statement to an Event using qualifiers
        addTruthyMapping("P1243", new SimpleStringSnakMapper("isrcCode", "[A-Z]{2}[A-Z0-9]{3}[0-9]{7}"));
        addTruthyMapping("P1278", new SimpleStringSnakMapper("leiCode", "[0-9A-Z]{18}[0-9]{2}"));
        addTruthyMapping("P1281", new ExternalIdentifierSnakMapper("http://www.flickr.com/places/info/$1", "[1-9][0-9]{0,9}"));
        addTruthyMapping("P1330", new ExternalIdentifierSnakMapper("http://musicbrainz.org/instrument/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P1407", new ExternalIdentifierSnakMapper("http://musicbrainz.org/series/$1", "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        addTruthyMapping("P1566", new ExternalIdentifierSnakMapper("http://sws.geonames.org/$1", "[1-9]\\d{0,8}"));
        addTruthyMapping("P1657", new SimpleItemIdSnakMapper("contentRating"));
        addTruthyMapping("P1716", new SimpleItemIdSnakMapper("brand"));
        addTruthyMapping("P1733", new ExternalIdentifierSnakMapper("http://store.steampowered.com/app/$1", "[1-9]\\d{0,5}"));
        addTruthyMapping("P1796", new SimpleStringSnakMapper("isicV4", "([A-U]|\\d{2,4})"));
        addTruthyMapping("P1827", new SimpleStringSnakMapper("iswcCode", "T-[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]"));
        addTruthyMapping("P1874", new ExternalIdentifierSnakMapper("http://www.netflix.com/title/$1", "\\d{6,8}"));
        addTruthyMapping("P1953", new ExternalIdentifierSnakMapper("http://www.discogs.com/artist/$1", "[1-9][0-9]*"));
        addTruthyMapping("P1954", new ExternalIdentifierSnakMapper("http://www.discogs.com/master/$1", "[1-9][0-9]*"));
        addTruthyMapping("P1902", new ExternalIdentifierSnakMapper("http://open.spotify.com/artist/$1", "[0-9A-Za-z]{22}"));
        addTruthyMapping("P1968", new ExternalIdentifierSnakMapper("http://foursquare.com/v/$1", "[0-9a-f]+"));
        addTruthyMapping("P2002", new ExternalIdentifierSnakMapper("http://twitter.com/$1", "[A-Za-z0-9_]{1,15}"));
        addTruthyMapping("P2003", new ExternalIdentifierSnakMapper("http://www.instagram.com/$1", "[a-z0-9_\\.]{1,30}"));
        addTruthyMapping("P2013", new ExternalIdentifierSnakMapper("http://www.facebook.com/$1", "[A-Za-zА-Яа-яёäöüßЁ0-9.-]+"));
        addTruthyMapping("P2019", new ExternalIdentifierSnakMapper("http://www.allmovie.com/artist/$1", "p[1-9][0-9]*"));
        addTruthyMapping("P2037", new ExternalIdentifierSnakMapper("http://github.com/$1", "[A-Za-z0-9]([A-Za-z0-9\\-]{0,37}[A-Za-z0-9])?"));
        addTruthyMapping("P2205", new ExternalIdentifierSnakMapper("http://open.spotify.com/album/$1", "[0-9A-Za-z]{22}"));
        addTruthyMapping("P2207", new ExternalIdentifierSnakMapper("http://open.spotify.com/track/$1", "[0-9A-Za-z]{22}"));
        addTruthyMapping("P2397", new ExternalIdentifierSnakMapper("http://www.youtube.com/channel/$1", "UC([A-Za-z0-9_\\-]){22}"));
        addTruthyMapping("P2437", new IntegerStatementMapper("numberOfSeasons"));
        addTruthyMapping("P2671", new ExternalIdentifierSnakMapper("http://g.co/kg$1", "\\/g\\/[0-9a-zA-Z]+"));
        addTruthyMapping("P2847", new ExternalIdentifierSnakMapper("http://plus.google.com/$1", "\\d{22}|\\+[-\\w_\\u00C0-\\u00FF]+"));
        addTruthyMapping("P2360", new SimpleItemIdSnakMapper("audience"));
        addTruthyMapping("P2860", new SimpleItemIdSnakMapper("citation"));
        addTruthyMapping("P3040", new ExternalIdentifierSnakMapper("http://soundcloud.com/$1", "[a-zA-Z0-9/_-]+"));
        addTruthyMapping("P3090", new SimpleStringSnakMapper("flightNumber", "([A-Z]{2,3}|[A-Z][0-9]|[0-9][A-Z])\\d{1,4}[A-Z]?"));
        addTruthyMapping("P3108", new ExternalIdentifierSnakMapper("http://www.yelp.com/biz/$1", "[^\\s]+")); //TODO: bad validation
        addTruthyMapping("P3192", new ExternalIdentifierSnakMapper("http://www.last.fm/music/$1", "[^\\s]+")); //TODO: bad validation
        addTruthyMapping("P3224", new SimpleStringSnakMapper("naics", "\\d{2,6}"));
        addTruthyMapping("P3267", new ExternalIdentifierSnakMapper("http://www.flickr.com/photos/$1", "[a-zA-Z0-9@]+"));
        addTruthyMapping("P3207", new ExternalIdentifierSnakMapper("http://vine.co/u/$1", "\\d+"));
        addTruthyMapping("P3417", new ExternalIdentifierSnakMapper("http://www.quora.com/topic/$1", "[^\\s\\/]+"));
    }

    private void addTruthyMapping(String propertyId, SnakMapper mainSnakMapper) {
        mapperForProperty.put(Datamodel.makeWikidataPropertyIdValue(propertyId), new TruthyStatementMapper(mainSnakMapper));
    }

    private void addRoleMapping(String propertyId, String targetRelation) {
        FullStatementMapper fullStatementMapper = new FullStatementMapper(targetRelation, new SimpleItemIdSnakMapper(targetRelation));
        fullStatementMapper.addQualifierMapper(Datamodel.makeWikidataPropertyIdValue("P580"), new TimeSnakMapper("startDate"));
        fullStatementMapper.addQualifierMapper(Datamodel.makeWikidataPropertyIdValue("P582"), new TimeSnakMapper("endDate"));
        mapperForProperty.put(Datamodel.makeWikidataPropertyIdValue(propertyId), fullStatementMapper);
    }


    public Optional<StatementMapper> getMapperForProperty(PropertyIdValue propertyId) {
        return Optional.ofNullable(mapperForProperty.get(propertyId));
    }
}
