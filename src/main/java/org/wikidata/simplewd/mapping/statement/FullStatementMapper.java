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

import org.wikidata.simplewd.model.Claim;
import org.wikidata.simplewd.model.value.EntityValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 * <p>
 * Maps a statement to a schema:Role structure
 */
class FullStatementMapper implements StatementMapper {

    private String targetFieldName;
    private SnakMapper mainSnakMapper;
    private Map<PropertyIdValue, SnakMapper> qualifiersMappers = new HashMap<>();

    FullStatementMapper(String targetFieldName, SnakMapper mainSnakMapper) {
        this.targetFieldName = targetFieldName;
        this.mainSnakMapper = mainSnakMapper;
    }

    void addQualifierMapper(PropertyIdValue propertyId, SnakMapper snakMapper) {
        qualifiersMappers.put(propertyId, snakMapper);
    }

    @Override
    public Stream<Claim> mapStatement(Statement statement) throws InvalidWikibaseValueException {
        return mainSnakMapper.mapSnak(statement.getClaim().getMainSnak()).map(mainClaim -> {
            EntityValue compoundValue = new EntityValue("wds:" + statement.getStatementId());
            compoundValue.addClaim(mainClaim);
            compoundValue.addType("Role");

            statement.getClaim().getAllQualifiers().forEachRemaining(qualifier -> {
                Optional.ofNullable(qualifiersMappers.get(qualifier.getPropertyId())).ifPresent(mapper -> {
                    try {
                        mapper.mapSnak(qualifier).forEach(compoundValue::addClaim);
                    } catch (InvalidWikibaseValueException e) {
                        //TODO: logging
                    }
                });
            });

            return new Claim(targetFieldName, compoundValue);
        });
    }

    @Override
    public boolean onlyBestRank() {
        return false;
    }
}
