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
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 */
class NameAsStringSnakMapper implements StringSnakMapper {

    private static final Locale MULTILINGUAL = Locale.forLanguageTag("mul");
    private String targetFieldName;
    private String[] types;

    NameAsStringSnakMapper(String targetFieldName, String... types) {
        this.targetFieldName = targetFieldName;
        this.types = types;
    }

    @Override
    public Stream<Claim> mapStringValue(StringValue value) throws InvalidWikibaseValueException {
        EntityValue entity = new EntityValue("_:" + UUID.nameUUIDFromBytes(value.toString().getBytes()));
        entity.addTypes(types);
        entity.addClaim(new Claim("name", value.getString(), MULTILINGUAL));
        return Stream.of(new Claim(targetFieldName, entity));
    }
}
