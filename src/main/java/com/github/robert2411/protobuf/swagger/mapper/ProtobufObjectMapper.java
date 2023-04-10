package com.github.robert2411.protobuf.swagger.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * The core class of this project
 * This class helps you to map between protobuf java objects and normal java objects
 */
public class ProtobufObjectMapper {
    private final ObjectMapper objectMapper;
    private final JsonFormat.Parser parser;
    private final JsonFormat.Printer printer;

    public ProtobufObjectMapper() {
        this(new ObjectMapper().registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false));
    }

    public ProtobufObjectMapper(ObjectMapper objectMapper) {
        this(objectMapper, JsonFormat.parser().ignoringUnknownFields());
    }

    public ProtobufObjectMapper(ObjectMapper objectMapper, JsonFormat.Parser parser) {
        this(objectMapper, parser, JsonFormat.printer().includingDefaultValueFields());
    }

    public ProtobufObjectMapper(ObjectMapper objectMapper, JsonFormat.Parser parser, JsonFormat.Printer printer) {
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.printer = printer;
    }

    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    public JsonFormat.Parser getParser() {
        return this.parser;
    }

    public JsonFormat.Printer getPrinter() {
        return this.printer;
    }

    /**
     * Map from one object to another
     *
     * @param object the original object
     * @param clazz  the class to what it has to be mapped
     * @return A mapped object
     */
    public <T> T map(Object object, Class<T> clazz) {
        return map(object, clazz, Collections.emptyList());
    }

    /**
     * Map from one object to another
     *
     * @param object             the original object
     * @param clazz              the class to what it has to be mapped
     * @param mappingCustomizers customizers that should be applied on the mapping
     * @return A mapped object
     */
    public <T> T map(Object object, Class<T> clazz, List<MappingCustomizer> mappingCustomizers) {
        String json = objectToJson(object);
        json = applyCustomizers(json, mappingCustomizers);
        return jsonToObject(json, clazz);
    }

    public <T extends Message> T map(Object swagger, Supplier<T.Builder> protobufBuilder) {
        return map(swagger, protobufBuilder, Collections.emptyList());
    }

    public <T extends Message> T map(Object swagger, Supplier<T.Builder> protobufBuilder, List<MappingCustomizer> mappingCustomizers) {
        String json = objectToJson(swagger);
        json = applyCustomizers(json, mappingCustomizers);
        return jsonToProto(json, protobufBuilder);
    }

    public <P extends Message, T extends Message> T map(P protobuf, Supplier<T.Builder> protobufBuilder) {
        return map(protobuf, protobufBuilder, Collections.emptyList());
    }

    public <P extends Message, T extends Message> T map(P protobuf, Supplier<T.Builder> protobufBuilder, List<MappingCustomizer> mappingCustomizers) {
        String json = protoToJson(protobuf);
        json = applyCustomizers(json, mappingCustomizers);
        return jsonToProto(json, protobufBuilder);
    }

    public <P extends Message, T> T map(P protobuf, Class<T> clazz) {
        return map(protobuf, clazz, Collections.emptyList());
    }

    public <P extends Message, T> T map(P protobuf, Class<T> clazz, List<MappingCustomizer> mappingCustomizers) {
        String json = protoToJson(protobuf);
        json = applyCustomizers(json, mappingCustomizers);
        return jsonToObject(json, clazz);
    }

    public String applyCustomizers(String json, List<MappingCustomizer> mappingCustomizers) {
        String tempJson = json;
        for (MappingCustomizer customizer : mappingCustomizers) {
            tempJson = customizer.apply(tempJson);
        }
        return tempJson;
    }


    @SuppressWarnings("unchecked")
    public <T extends Message> T jsonToProto(String json, Supplier<T.Builder> protobufBuilder) {
        try {
            T.Builder builder = protobufBuilder.get();
            parser.merge(json, builder);
            return (T) builder.build();
        } catch (InvalidProtocolBufferException e) {
            throw new MappingException(e);
        }
    }

    public String objectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MappingException(e);
        }
    }

    public <P extends Message> String protoToJson(P protobuf) {
        try {
            return printer.print(protobuf);
        } catch (InvalidProtocolBufferException e) {
            throw new MappingException(e);
        }
    }

    public <T> T jsonToObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new MappingException(e);
        }
    }
}