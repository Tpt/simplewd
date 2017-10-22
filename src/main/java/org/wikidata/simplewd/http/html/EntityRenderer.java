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

package org.wikidata.simplewd.http.html;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.simplewd.model.Resource;
import org.wikidata.simplewd.model.value.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static j2html.TagCreator.*;

public class EntityRenderer extends HTMLRenderer {

    private static final String BASE_URL = "/simplewd/v0/entity/";

    private Locale locale;

    public EntityRenderer(Locale locale) {
        this.locale = locale;
    }

    public String render(Resource resource) {
        String title = filterForLocale(resource, "name").findAny().map(LocaleStringValue::toString).orElse(resource.getIRI());
        Optional<DomContent> subtitle = filterForLocale(resource, "description").findAny().map(this::render);

        ContainerTag card = div(
                section(
                        h1(title).attr("lang", locale.toLanguageTag()).withClasses("mdc-card__title", "mdc-card__title--large"),
                        subtitle.map(sub -> h2(sub).withClass("mdc-card__subtitle")).orElse(span())
                ).withClass("mdc-card__primary")
        ).withClass("mdc-card");


        //Generates the data table
        Map<Value, List<Value>> data = new HashMap<>();
        data.put(new StringValue("type"), resource.getTypes().map(ConstantValue::new).collect(Collectors.toList()));
        resource.getProperties().forEach(property ->
                data.put(
                        new ConstantValue(property),
                        resource.getValues(property).collect(Collectors.toList())
                )
        );
        List<DomContent> tableRows = new ArrayList<>();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            tableRows.add(tr(
                    th(render(entry.getKey())).attr("scope", "row").attr("rowspan", entry.getValue().size()),
                    td(render(entry.getValue().get(0)))
            ));
            for (int i = 1; i < entry.getValue().size(); i++) {
                tableRows.add(tr(td(render(entry.getValue().get(i)))));
            }
        });

        ContainerTag table = table(
                thead(tr(
                        th("property").withClass("mdl-data-table__cell--non-numeric"),
                        th("values").withClass("mdl-data-table__cell--non-numeric")
                )),
                tbody(tableRows.toArray(new DomContent[0]))
        ).withClasses("mdl-data-table", "mdc-elevation--z2");

        return super.render("SimpleWD - " + title, "", div(card, table));
    }

    private Stream<LocaleStringValue> filterForLocale(Resource resource, String property) {
        return resource.getValues(property)
                .filter(value -> value instanceof LocaleStringValue)
                .map(value -> (LocaleStringValue) value)
                .filter(value -> value.getLocale().equals(locale)); //TODO: get also sub-locale
    }

    private DomContent render(Value value) {
        if (value instanceof CalendarValue) {
            return render((CalendarValue) value);
        } else if (value instanceof ConstantValue) {
            return render((ConstantValue) value);
        } else if (value instanceof GeoValue) {
            return render((GeoValue) value);
        } else if (value instanceof LocaleStringValue) {
            return render((LocaleStringValue) value);
        } else if (value instanceof ResourceValue) {
            return render((ResourceValue) value);
        } else if (value instanceof StringValue) {
            return render((StringValue) value);
        } else if (value instanceof URIValue) {
            return render((URIValue) value);
        } else {
            throw new IllegalArgumentException("Not supported value: " + value.toString());
        }
    }

    private DomContent render(CalendarValue value) {
        return time(value.toString()).attr("datetime", value.toString()); //TODO: formatting
    }

    private DomContent render(ConstantValue value) {
        return a(value.toString()).withHref(Namespaces.expand(value.toString()));
    }

    private DomContent render(GeoValue value) {
        return text(value.toString()); //TODO
    }

    private DomContent render(LocaleStringValue value) {
        return span(value.toString()).attr("lang", value.getLanguageCode()).withTitle(value.getLocale().getDisplayName(locale));
    }

    private DomContent render(ResourceValue value) {
        return a(value.toString()).withHref(BASE_URL + value.toString());
    }

    private DomContent render(StringValue value) {
        return text(value.toString());
    }

    private DomContent render(URIValue value) {
        return a(value.toString()).withHref(value.toString());
    }
}
