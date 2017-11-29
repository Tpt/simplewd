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

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.api.CommonsAPI;
import org.wikidata.simplewd.model.*;
import org.wikidata.simplewd.model.value.*;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Stream;

import static j2html.TagCreator.*;

public class EntityRenderer extends HTMLRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRenderer.class);
    private static final ShaclSchema SCHEMA = ShaclSchema.getSchema();
    private static final String BASE_URL = "/simplewd/v0/entity/";

    private EntityLookup entityLookup;
    private CommonsAPI commonsAPI;
    private LocaleFilter localeFilter;

    public EntityRenderer(EntityLookup entityLookup, CommonsAPI commonsAPI) {
        this.entityLookup = entityLookup;
        this.commonsAPI = commonsAPI;
    }

    public String render(EntityValue entity, LocaleFilter localeFilter) {
        this.localeFilter = localeFilter;
        return render(entity);
    }

    private String render(EntityValue entity) {
        //We preload entities
        try {
            entityLookup.getEntitiesForIRI(entity.getClaims().map(Claim::getValue).flatMap(value ->
                    (value instanceof EntityIdValue) ? Stream.of(value.toString()) : Stream.empty()
            ).toArray(String[]::new));
        } catch (Exception e) {
            //We ignore the errors
        }

        String nameString = localeFilter.getBestValues(entity.getValues("name"))
                .findAny()
                .map(LocaleStringValue::toString)
                .orElse(entity.getIRI());

        return super.render(nameString + " - SimpleWD", "", div(
                Stream.concat(
                        Stream.of(renderThingCard(entity)),
                        SCHEMA.getShapesForClasses(entity.getTypes())
                                .filter(shape -> !shape.getName().equals("Thing"))
                                .sorted(Comparator.comparing(ShaclSchema.NodeShape::getName))
                                .map(shape -> renderShapeCard(entity, shape))
                ).toArray(DomContent[]::new)
        ));
    }

    private DomContent renderThingCard(EntityValue entity) {
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

        List<ContainerTag> actions = new ArrayList<>();
        actions.add(a("Wikidata item").withHref(Namespaces.expand(entity.getIRI())));
        entity.getValues("sameAs").sorted().forEach(value -> {
            if (value instanceof URIValue) {
                URI uri = ((URIValue) value).getURI();
                if (uri.getHost().equals(localeFilter.getBestLocale().getLanguage() + ".wikipedia.org")) {
                    actions.add(a("Wikipedia article").withHref(uri.toString()));
                }
            }
        });
        entity.getValue("url").ifPresent(url -> actions.add(a("Official website").withHref(url.toString())));

        return div(
                cardHeader,
                section(actions.stream().peek(action -> action.withClasses("mdc-button", "mdc-button--compact", "mdc-card__action")).toArray(DomContent[]::new)).withClass("mdc-card--actions")
        ).withClass("mdc-card");
    }

    private DomContent renderShapeCard(EntityValue entity, ShaclSchema.NodeShape shape) {
        return div(
                section(
                        h1(
                                shape.getClasses()
                                        .map(ConstantValue::new)
                                        .map(this::renderValue)
                                        .toArray(DomContent[]::new)
                        ).withClasses("mdc-card__title", "mdc-card__title--large")
                ).withClass("mdc-card__primary"),
                section(
                        shape.getProperties()
                                .filter(property -> entity.getValues(property.getProperty()).findAny().isPresent())
                                .sorted(Comparator.comparing(ShaclSchema.PropertyShape::getProperty))
                                .map(property -> {
                                    boolean twoLines = property.getDatatypes().isPresent() ||
                                            property.getNodeShape().map(sh -> !sh.getClasses().findAny().isPresent()).orElse(false);
                                    return section(
                                            h3(renderValue(new ConstantValue(property.getProperty()))),
                                            (property.getMaxCount() > 1)
                                                    ? ul(
                                                    entity.getValues(property.getProperty())
                                                            .map(value -> {
                                                                if (value instanceof EntityValue) {
                                                                    return renderRoleRow((EntityValue) value, property);
                                                                } else {
                                                                    return renderValue(value);
                                                                }
                                                            })
                                                            .map(value -> li(value).withClass("mdc-list-item"))
                                                            .toArray(DomContent[]::new)
                                            ).withClasses("mdc-list", "mdc-list mdc-list--dense").withCondClass(twoLines, "mdc-list mdc-list--two-line")
                                                    : entity.getValue(property.getProperty())
                                                    .map(this::renderValue)
                                                    .map(TagCreator::div)
                                                    .orElse(div())
                                    );
                                }).toArray(DomContent[]::new)
                ).withClass("mdc-card__supporting-text")
        ).withClass("mdc-card");
    }

    private DomContent renderRoleRow(EntityValue role, ShaclSchema.PropertyShape property) {
        List<DomContent> annotations = new ArrayList<>();
        if (role.hasValueFor("character")) {
            annotations.add(join(
                    renderValue(new ConstantValue("character")),
                    ": ",
                    role.getValue("character").map(this::renderValue).orElse(text("")) //TODO: multiple characters
            ));
        }
        if (role.hasValueFor("startDate") || role.hasValueFor("endDate")) {
            annotations.add(join(
                    "(",
                    role.getValue("startDate").map(this::renderValue).orElse(text("")),
                    " - ",
                    role.getValue("endDate").map(this::renderValue).orElse(text("")),
                    ")"
            ));
        }

        return span(
                role.getValue(property.getProperty())
                        .map(this::renderValue)
                        .orElse(span()),
                span(annotations.toArray(new DomContent[0])).withClass("mdc-list-item__text__secondary")
        ).withClass("mdc-list-item__text");
    }

    private DomContent renderValue(Value value) {
        if (value instanceof CommonsFileValue) {
            return renderValue((CommonsFileValue) value);
        } else if (value instanceof CalendarValue) {
            return renderValue((CalendarValue) value);
        } else if (value instanceof ConstantValue) {
            return renderValue((ConstantValue) value);
        } else if (value instanceof GeoValue) {
            return renderValue((GeoValue) value);
        } else if (value instanceof LocaleStringValue) {
            return renderValue((LocaleStringValue) value);
        } else if (value instanceof EntityIdValue) {
            return renderValue((EntityIdValue) value);
        } else if (value instanceof StringValue) {
            return renderValue((StringValue) value);
        } else if (value instanceof URIValue) {
            return renderValue((URIValue) value);
        } else {
            LOGGER.info("Not supported value: " + value.toString());
            return text(value.toString());
        }
    }

    private DomContent renderValue(CommonsFileValue value) {
        try {
            String URL = "https://commons.wikimedia.org/wiki/File:" + URLEncoder.encode(value.toString().replace(" ", "_"), "UTF-8");
            return a(value.toString()).withHref(URL);
        } catch (UnsupportedEncodingException e) {
            return text(value.toString());
        }
    }

    private DomContent renderValue(CalendarValue value) {
        Date date = value.getXMLGregorianCalendar().toGregorianCalendar().getTime();
        QName datatype = value.getXMLGregorianCalendar().getXMLSchemaType();
        String serialization = value.toString();
        if (datatype.equals(DatatypeConstants.DATETIME)) {
            serialization = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, localeFilter.getBestLocale()).format(date);
        } else if (datatype.equals(DatatypeConstants.DATE)) {
            serialization = DateFormat.getDateInstance(DateFormat.DEFAULT, localeFilter.getBestLocale()).format(date);
        }
        DateFormat.getDateInstance(DateFormat.DEFAULT, localeFilter.getBestLocale());
        return time(serialization).attr("datetime", value.toString()); //TODO: formatting
    }

    private DomContent renderValue(ConstantValue value) {
        return a(value.toString()).withHref(Namespaces.expand(value.toString()));
    }

    private DomContent renderValue(GeoValue value) {
        return text(value.toString()); //TODO
    }

    private DomContent renderValue(LocaleStringValue value) {
        return span(join(
                value.toString(),
                sup(join("(", value.getLocale().getDisplayName(localeFilter.getBestLocale()), ")"))
        )).attr("lang", value.getLanguageCode());
    }

    private DomContent simpleRender(LocaleStringValue value) {
        return span(value.toString()).attr("lang", value.getLanguageCode());
    }

    private DomContent renderValue(EntityIdValue value) {
        DomContent basicRendering = a(value.toString()).withHref(BASE_URL + value.toString());
        try {
            return entityLookup.getEntityForIRI(value.toString())
                    .map(entity -> (DomContent)
                            a(localeFilter.getBestValues(entity.getValues("name"))
                                    .findAny().map(this::simpleRender).orElseGet(() -> text(value.toString())))
                                    .withHref(BASE_URL + value.toString())
                                    .withTitle(localeFilter.getBestValues(entity.getValues("description")).findAny().map(Object::toString).orElse(""))
                    ).orElse(basicRendering);
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
        return basicRendering;
    }

    private DomContent renderValue(StringValue value) {
        return text(value.toString());
    }

    private DomContent renderValue(URIValue value) {
        return a(value.toString()).withHref(value.toString());
    }
}
