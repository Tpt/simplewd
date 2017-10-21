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
import org.wikidata.simplewd.model.Resource;
import org.wikidata.simplewd.model.value.LocaleStringValue;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemMapper.class);
    private static final PropertyIdValue P31 = Datamodel.makeWikidataPropertyIdValue("P31");
    private Sites sites;
    private MapperRegistry mapperRegistry;

    public ItemMapper(Sites sites) {
        this.sites = sites;
        this.mapperRegistry = new MapperRegistry();
    }

    public Resource map(ItemDocument document) {
        Resource resource = new Resource(document.getEntityId().getIri());
        addTermsToResource(document, resource);
        addSiteLinksToResource(document, resource);
        addStatementsToResource(document, resource);
        return resource;
    }

    private void addTermsToResource(TermedDocument termedDocument, Resource resource) {
        termedDocument.getLabels().values().forEach(label -> {
            LocaleStringValue value = convert(label);
            resource.addClaim("name", value);
        });
        termedDocument.getDescriptions().values().forEach(description ->
                resource.addClaim("description", convert(description))
        );
        termedDocument.getAliases().values().forEach(aliases ->
                aliases.forEach(alias -> {
                    LocaleStringValue value = convert(alias);
                    resource.addClaim("alternateName", value);
                })
        );
    }

    private void addSiteLinksToResource(ItemDocument itemDocument, Resource resource) {
        itemDocument.getSiteLinks().values().stream()
                .filter(siteLink -> sites.getGroup(siteLink.getSiteKey()).equals("wikipedia"))
                .forEach(siteLink ->
                        resource.addClaim(new Claim("sameAs", URI.create(sites.getSiteLinkUrl(siteLink).replace("https://", "http://"))))
                );
    }

    private void addStatementsToResource(StatementDocument statementDocument, Resource resource) {
        statementDocument.getStatementGroups().forEach(group ->
                getBestStatements(group).forEach(statement ->
                        mapperRegistry.getMapperForProperty(statement.getClaim().getMainSnak().getPropertyId()).ifPresent(mapper -> {
                            try {
                                mapper.mapStatement(statement).forEach(resource::addClaim);
                            } catch (InvalidWikibaseValueException e) {
                                //LOGGER.warn(e.getMessage(), e);
                            }
                        })
                )
        );
    }

    private List<Statement> getBestStatements(StatementDocument statementDocument, PropertyIdValue property) {
        return Optional.ofNullable(statementDocument.findStatementGroup(property))
                .map(this::getBestStatements)
                .orElse(Collections.emptyList());
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
        if (preferred.isEmpty()) {
            return normals;
        } else {
            return preferred;
        }
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
