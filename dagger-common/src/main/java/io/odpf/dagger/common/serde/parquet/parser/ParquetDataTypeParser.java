package io.odpf.dagger.common.serde.parquet.parser;

import org.apache.parquet.example.data.simple.SimpleGroup;

import java.util.function.Supplier;

public interface ParquetDataTypeParser {
    Object deserialize(SimpleGroup simpleGroup, String fieldName);

    boolean canHandle(SimpleGroup simpleGroup, String fieldName);

    static Object getValueOrDefault(Supplier<Object> valueSupplier, Object defaultValue) {
        Object deserializedValue = valueSupplier.get();
        return (deserializedValue == null) ? defaultValue : deserializedValue;
    }
}