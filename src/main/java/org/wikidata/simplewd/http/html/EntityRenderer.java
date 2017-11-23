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
import j2html.tags.UnescapedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.api.CommonsAPI;
import org.wikidata.simplewd.model.*;
import org.wikidata.simplewd.model.value.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static j2html.TagCreator.*;

public class EntityRenderer extends HTMLRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRenderer.class);
    private static final String BASE_URL = "/simplewd/v0/entity/";

    private EntityLookup entityLookup;
    private CommonsAPI commonsAPI;
    private LocaleFilter localeFilter;

    public EntityRenderer(EntityLookup entityLookup, CommonsAPI commonsAPI) {
        this.entityLookup = entityLookup;
        this.commonsAPI = commonsAPI;
    }

    public String render(Entity entity, LocaleFilter localeFilter) {
        this.localeFilter = localeFilter;
        return render(entity);
    }

    private String render(Entity entity) {
        //We preload entities
        try {
            entityLookup.getEntitiesForIRI(entity.getClaims().map(Claim::getValue).flatMap(value ->
                    (value instanceof EntityValue) ? Stream.of(value.toString()) : Stream.empty()
            ).toArray(String[]::new));
        } catch (Exception e) {
            //We ignore the errors
        }


        DomContent title = localeFilter.getBestValues(entity.getValues("name")).findAny().map(this::simpleRender).orElse(text(entity.getIRI()));
        Optional<DomContent> subtitle = localeFilter.getBestValues(entity.getValues("description")).findAny().map(this::simpleRender);
        Optional<CommonsImage> image = entity.getValue("image").flatMap(value -> {
            try {
                return Optional.of(commonsAPI.getImage(value.toString()));
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
                return Optional.empty();
            }
        });

        ContainerTag cardHeader = div(
                section(
                        h1(title).withClasses("mdc-card__title", "mdc-card__title--large"),
                        subtitle.map(sub -> h2(sub).withClass("mdc-card__subtitle")).orElse(span())
                ).withClass("mdc-card__primary")
        ).withClass("mdc-card__horizontal-block");
        image.ifPresent(desc ->
                cardHeader.with(a(img().withSrc(
                        desc.getContentURI()).withClass("mdc-card__media-item")
                ).withHref(desc.getDescriptionURI()))
        );
        ContainerTag card = div(cardHeader).withClass("mdc-card");

        //Generates the data table
        Map<Value, List<Value>> data = new HashMap<>();
        if (entity.getTypes().count() > 0) {
            data.put(new StringValue("type"), entity.getTypes().map(ConstantValue::new).collect(Collectors.toList()));
        }
        entity.getProperties().forEach(property ->
                data.put(
                        new ConstantValue(property),
                        entity.getValues(property).collect(Collectors.toList())
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

    private DomContent render(Value value) {
        if (value instanceof CommonsFileValue) {
            return render((CommonsFileValue) value);
        } else if (value instanceof CalendarValue) {
            return render((CalendarValue) value);
        } else if (value instanceof CompoundValue) {
            return render((CompoundValue) value);
        } else if (value instanceof ConstantValue) {
            return render((ConstantValue) value);
        } else if (value instanceof GeoValue) {
            return render((GeoValue) value);
        } else if (value instanceof LocaleStringValue) {
            return render((LocaleStringValue) value);
        } else if (value instanceof EntityValue) {
            return render((EntityValue) value);
        } else if (value instanceof StringValue) {
            return render((StringValue) value);
        } else if (value instanceof URIValue) {
            return render((URIValue) value);
        } else {
            LOGGER.info("Not supported value: " + value.toString());
            return text(value.toString());
        }
    }

    private DomContent render(CommonsFileValue value) {
        try {
            String URL = "https://commons.wikimedia.org/wiki/File:" + URLEncoder.encode(value.toString().replace(" ", "_"), "UTF-8");
            return a(value.toString()).withHref(URL);
        } catch (UnsupportedEncodingException e) {
            return text(value.toString());
        }
    }

    private DomContent render(CompoundValue value) {
        return join(value.getPropertyValues().entrySet().stream().map(entry ->
                join(render(new ConstantValue(entry.getKey())), ": ", render(entry.getValue()))
        ).toArray(UnescapedText[]::new));
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
        return span(join(
                value.toString(),
                sup(join("(", value.getLocale().getDisplayName(localeFilter.getBestLocale()), ")"))
        )).attr("lang", value.getLanguageCode());
    }

    private DomContent simpleRender(LocaleStringValue value) {
        return span(value.toString()).attr("lang", value.getLanguageCode());
    }

    private DomContent render(EntityValue value) {
        DomContent basicRendering = a(value.toString()).withHref(BASE_URL + value.toString());
        try {
            return entityLookup.getEntityForIRI(value.toString())
                    .map(entity -> (DomContent)
                            a(localeFilter.getBestValues(entity.getValues("name")).findAny().map(Object::toString).orElseGet(value::toString))
                                    .withHref(BASE_URL + value.toString())
                                    .withTitle(localeFilter.getBestValues(entity.getValues("description")).findAny().map(Object::toString).orElse(""))
                    ).orElse(basicRendering);
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
        return basicRendering;
    }

    private DomContent render(StringValue value) {
        return text(value.toString());
    }

    private DomContent render(URIValue value) {
        return a(value.toString()).withHref(value.toString());
    }
}
