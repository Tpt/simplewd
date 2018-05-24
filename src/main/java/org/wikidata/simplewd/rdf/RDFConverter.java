package org.wikidata.simplewd.rdf;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.simplewd.model.Namespaces;
import org.wikidata.simplewd.model.ShaclSchema;
import org.wikidata.simplewd.model.value.*;

import java.util.stream.Stream;

public class RDFConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFConverter.class);

    private final ValueFactory valueFactory = SimpleValueFactory.getInstance();
    private final ShaclSchema schema = ShaclSchema.getSchema();

    public Stream<Statement> toRDF(EntityValue entityValue) {
        return toRDF(entityValue, schema.getShapeForClasses(entityValue.getTypes()));
    }

    private Stream<Statement> toRDF(EntityValue entityValue, ShaclSchema.NodeShape shape) {
        Resource iri = parseId(entityValue.getIRI());
        return Stream.concat(
                entityValue.getTypes().map(this::parseId).map(type -> valueFactory.createStatement(
                        iri, RDF.TYPE, type
                )),
                shape.getProperties().flatMap(property ->
                        entityValue.getValues(property.getProperty()).flatMap(value -> mapValue(iri, property, value))
                )
        );
    }

    private Stream<Statement> mapValue(Resource subject, ShaclSchema.PropertyShape property, Value value) {
        IRI predicate = valueFactory.createIRI(Namespaces.expand(property.getProperty()));
        if (value instanceof CalendarValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createLiteral(((CalendarValue) value).getValue())
            ));
        } else if (value instanceof CommonsFileValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createIRI("http://commons.wikimedia.org/wiki/Special:FilePath/", value.toString().replace(' ', '_'))
            ));
        } else if (value instanceof ConstantValue || value instanceof EntityIdValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate, parseId(value.toString())));
        } else if (value instanceof EntityValue) {
            EntityValue entity = (EntityValue) value;
            Resource id = parseId(entity.getIRI());
            return Stream.concat(
                    Stream.of(valueFactory.createStatement(subject, predicate, id)),
                    property.getNodeShape().map(shape -> toRDF(entity, shape)).orElseGet(() -> toRDF(entity))
            );
        } else if (value instanceof GeoCoordinatesValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createIRI(((GeoCoordinatesValue) value).getIRI())
            ));
        } else if (value instanceof IntegerValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createLiteral(((IntegerValue) value).getValue())
            ));
        } else if (value instanceof LocaleStringValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createLiteral(value.toString(), ((LocaleStringValue) value).getLanguageCode())
            ));
        } else if (value instanceof StringValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createLiteral(value.toString())
            ));
        } else if (value instanceof URIValue) {
            return Stream.of(valueFactory.createStatement(subject, predicate,
                    valueFactory.createLiteral(value.toString(), XMLSchema.ANYURI)
            ));
        } else {
            LOGGER.warn("Unsupported value class: " + value.getClass());
            return Stream.empty();
        }
    }

    private Resource parseId(String id) {
        id = Namespaces.expand(id);
        if (id.startsWith("_:")) {
            return valueFactory.createBNode(id.replace("_:", ""));
        } else {
            return valueFactory.createIRI(id);
        }
    }
}
