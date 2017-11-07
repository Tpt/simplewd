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
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Thomas Pellissier Tanon
 */
class IntegerStatementMapper implements StatementMainQuantityValueMapper {

    private String targetFieldName;

    IntegerStatementMapper(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    @Override
    public Stream<Claim> mapMainQuantityValue(QuantityValue value) throws InvalidWikibaseValueException {
        if (!isNullOrZero(value.getLowerBound()) || !isNullOrZero(value.getUpperBound())) {
            throw new InvalidWikibaseValueException(value + " has not null precision bounds.");
        }
        if(!value.getUnit().isEmpty()) {
            throw new InvalidWikibaseValueException(value + " has a unit.");
        }
        try {
            return Stream.of(new Claim(targetFieldName, value.getNumericValue().toBigIntegerExact()));
        } catch (ArithmeticException e) {
            throw new InvalidWikibaseValueException(value + " is not an integer.");
        }
    }

    private boolean isNullOrZero(BigDecimal value) {
        return value == null || value.equals(BigDecimal.ZERO);
    }
}
