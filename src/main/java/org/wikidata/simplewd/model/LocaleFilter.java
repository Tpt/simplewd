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

package org.wikidata.simplewd.model;

import org.wikidata.simplewd.model.value.LocaleStringValue;
import org.wikidata.simplewd.model.value.Value;

import java.util.*;
import java.util.stream.Stream;

public class LocaleFilter {
    private static final Locale MULTILINGUAL = Locale.forLanguageTag("mul");

    private List<Locale.LanguageRange> priorityList;

    public LocaleFilter(String localRanges) {
        priorityList = Locale.LanguageRange.parse(localRanges);
    }

    public Stream<LocaleStringValue> getBestValues(Stream<Value> values) {
        Set<Locale> availableLocales = new HashSet<>();
        LocaleStringValue[] localeValues = values
                .filter(value -> value instanceof LocaleStringValue)
                .map(value -> (LocaleStringValue) value)
                .peek(value -> availableLocales.add(value.getLocale()))
                .toArray(LocaleStringValue[]::new); //TODO: check if it is working
        Locale localeToUse = Locale.filter(priorityList, availableLocales).stream().findAny().orElse(MULTILINGUAL);
        return Stream.of(localeValues).filter(value -> value.getLocale().equals(localeToUse));
    }

    public boolean isMultilingualAccepted() {
        return !Locale.filter(priorityList, Collections.singletonList(MULTILINGUAL)).isEmpty();
    }

    public Locale getBestLocale() {
        return Locale.lookup(priorityList, Arrays.asList(Locale.getAvailableLocales()));
    }
}
