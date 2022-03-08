package io.odpf.dagger.common.serde.proto.protohandler;

import io.odpf.dagger.common.serde.parquet.parser.validation.SimpleGroupValidation;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.odpf.dagger.common.exceptions.serde.EnumFieldNotFoundException;
import org.apache.parquet.example.data.simple.SimpleGroup;

/**
 * The type Enum proto handler.
 */
public class EnumProtoHandler implements ProtoHandler {
    private Descriptors.FieldDescriptor fieldDescriptor;

    /**
     * Instantiates a new Enum proto handler.
     *
     * @param fieldDescriptor the field descriptor
     */
    public EnumProtoHandler(Descriptors.FieldDescriptor fieldDescriptor) {
        this.fieldDescriptor = fieldDescriptor;
    }

    @Override
    public boolean canHandle() {
        return fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM && !fieldDescriptor.isRepeated();
    }

    @Override
    public DynamicMessage.Builder transformForKafka(DynamicMessage.Builder builder, Object field) {
        if (!canHandle() || field == null) {
            return builder;
        }
        String stringValue = String.valueOf(field).trim();
        Descriptors.EnumValueDescriptor valueByName = fieldDescriptor.getEnumType().findValueByName(stringValue);
        if (valueByName == null) {
            throw new EnumFieldNotFoundException("field: " + stringValue + " not found in " + fieldDescriptor.getFullName());
        }
        return builder.setField(fieldDescriptor, valueByName);
    }

    @Override
    public Object transformFromPostProcessor(Object field) {
        String input = field != null ? field.toString() : "0";
        try {
            int enumPosition = Integer.parseInt(input);
            Descriptors.EnumValueDescriptor valueByNumber = fieldDescriptor.getEnumType().findValueByNumber(enumPosition);
            return valueByNumber != null ? valueByNumber.getName() : fieldDescriptor.getEnumType().findValueByNumber(0).getName();
        } catch (NumberFormatException e) {
            Descriptors.EnumValueDescriptor valueByName = fieldDescriptor.getEnumType().findValueByName(input);
            return valueByName != null ? valueByName.getName() : fieldDescriptor.getEnumType().findValueByNumber(0).getName();
        }
    }

    @Override
    public Object transformFromKafka(Object field) {
        return String.valueOf(field).trim();
    }

    @Override
    public Object transformFromParquet(Object field) {
        String defaultEnumValue = fieldDescriptor.getEnumType().findValueByNumber(0).getName();
        String fieldName = fieldDescriptor.getName();
        if (field instanceof SimpleGroup) {
            SimpleGroup simpleGroup = (SimpleGroup) field;
            if (SimpleGroupValidation.checkFieldExistsAndIsInitialized(simpleGroup, fieldName)) {
                String parquetEnumValue = simpleGroup.getString(fieldName, 0);
                Descriptors.EnumValueDescriptor enumValueDescriptor = fieldDescriptor.getEnumType().findValueByName(parquetEnumValue);
                return enumValueDescriptor == null ? defaultEnumValue : enumValueDescriptor.getName();
            }
        }
        return defaultEnumValue;
    }

    @Override
    public Object transformToJson(Object field) {
        return field;
    }

    @Override
    public TypeInformation getTypeInformation() {
        return Types.STRING;
    }

}
