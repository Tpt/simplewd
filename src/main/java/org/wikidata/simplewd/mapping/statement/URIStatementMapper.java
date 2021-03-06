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
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 */
class URIStatementMapper implements StringSnakMapper {

    private String targetFieldName;

    URIStatementMapper(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    @Override
    public Stream<Claim> mapStringValue(StringValue value) throws InvalidWikibaseValueException {
        try {
            URI parsedURL = new URI(value.getString()).normalize();
            if ((parsedURL.getScheme().equals("http") || parsedURL.getScheme().equals("https")) && parsedURL.getPath().isEmpty()) {
                parsedURL = parsedURL.resolve("/");
            }
            return Stream.of(new Claim(targetFieldName, parsedURL));
        } catch (URISyntaxException e) {
            throw new InvalidWikibaseValueException(value + " is an invalid URI", e);
        }
    }
}
