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

package org.wikidata.simplewd.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.mapping.statement.InvalidWikibaseValueException;
import org.wikidata.simplewd.mapping.statement.MapperRegistry;
import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.Entity;
import org.wikidata.simplewd.model.value.LocaleStringValue;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemMapper.class);
    private static final PropertyIdValue P31 = Datamodel.makeWikidataPropertyIdValue("P31");
    private Sites sites;
    private MapperRegistry mapperRegistry;

    public ItemMapper(Sites sites) {
        this.sites = sites;
        this.mapperRegistry = new MapperRegistry();
    }

    public Entity map(ItemDocument document) {
        Entity entity = new Entity(document.getEntityId().getIri());
        addTermsToResource(document, entity);
        addSiteLinksToResource(document, entity);
        addStatementsToResource(document, entity);
        return entity;
    }

    private void addTermsToResource(TermedDocument termedDocument, Entity entity) {
        termedDocument.getLabels().values().forEach(label -> {
            LocaleStringValue value = convert(label);
            entity.addClaim("name", value);
        });
        termedDocument.getDescriptions().values().forEach(description ->
                entity.addClaim("description", convert(description))
        );
        termedDocument.getAliases().values().forEach(aliases ->
                aliases.forEach(alias -> {
                    LocaleStringValue value = convert(alias);
                    entity.addClaim("alternateName", value);
                })
        );
    }

    private void addSiteLinksToResource(ItemDocument itemDocument, Entity entity) {
        itemDocument.getSiteLinks().values().stream()
                .filter(siteLink -> sites.getGroup(siteLink.getSiteKey()).equals("wikipedia"))
                .forEach(siteLink ->
                        entity.addClaim(new Claim("sameAs", URI.create(sites.getSiteLinkUrl(siteLink).replace("https://", "http://"))))
                );
    }

    private void addStatementsToResource(StatementDocument statementDocument, Entity entity) {
        statementDocument.getStatementGroups().forEach(group ->
                mapperRegistry.getMapperForProperty(group.getProperty()).ifPresent(mapper -> {
                    Stream<Statement> statements = mapper.onlyBestRank()
                            ? getBestStatements(group).stream()
                            : group.getStatements().stream().filter(statement -> !statement.getRank().equals(StatementRank.DEPRECATED));
                    statements.flatMap(statement -> {
                        try {
                            return mapper.mapStatement(statement);
                        } catch (InvalidWikibaseValueException e) {
                            //LOGGER.warn(e.getMessage(), e);
                            return Stream.empty();
                        }
                    }).forEach(entity::addClaim);
                })
        );
    }

    private List<Statement> getBestStatements(StatementGroup statementGroup) {
        List<Statement> preferred = new ArrayList<>();
        List<Statement> normals = new ArrayList<>();
        for (Statement statement : statementGroup.getStatements()) {
            if (statement.getRank().equals(StatementRank.PREFERRED)) {
                preferred.add(statement);
            } else if (statement.getRank().equals(StatementRank.NORMAL)) {
                normals.add(statement);
            }
        }
        return preferred.isEmpty() ? normals : preferred;
    }

    private LocaleStringValue convert(MonolingualTextValue value) {
        try {
            return new LocaleStringValue(value.getText(), WikimediaLanguageCodes.getLanguageCode(value.getLanguageCode()));
        } catch (IllegalArgumentException e) {
            LOGGER.warn(e.getMessage());
            return new LocaleStringValue(value.getText(), value.getLanguageCode());
        }
    }
}
