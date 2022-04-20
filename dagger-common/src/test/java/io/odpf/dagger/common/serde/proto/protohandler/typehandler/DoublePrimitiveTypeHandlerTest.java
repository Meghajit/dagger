package io.odpf.dagger.common.serde.proto.protohandler.typehandler;

import com.google.protobuf.Descriptors;
import io.odpf.dagger.consumer.TestBookingLogMessage;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.schema.GroupType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE;
import static org.junit.Assert.*;

public class DoublePrimitiveTypeHandlerTest {

    @Test
    public void shouldHandleDoubleTypes() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        assertTrue(doublePrimitiveTypeHandler.canHandle());
    }

    @Test
    public void shouldNotHandleTypesOtherThanDouble() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("order_number");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        assertFalse(doublePrimitiveTypeHandler.canHandle());
    }

    @Test
    public void shouldFetchValueForFieldForFieldDescriptorOfTypeDouble() {
        double actualValue = 2.0D;

        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        Object value = doublePrimitiveTypeHandler.parseObject(actualValue);

        assertEquals(actualValue, value);
    }

    @Test
    public void shouldFetchParsedValueForFieldForFieldDescriptorOfTypeDouble() {
        double actualValue = 2.0D;

        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        Object value = doublePrimitiveTypeHandler.parseObject(String.valueOf(actualValue));

        assertEquals(actualValue, value);
    }

    @Test
    public void shouldFetchDefaultValueIfValueNotPresentForFieldDescriptorOfTypeDouble() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        Object value = doublePrimitiveTypeHandler.parseObject(null);

        assertEquals(0.0D, value);
    }

    @Test
    public void shouldReturnTypeInformation() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        assertEquals(Types.DOUBLE, doublePrimitiveTypeHandler.getTypeInformation());
    }

    @Test
    public void shouldReturnArrayTypeInformation() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        assertEquals(Types.PRIMITIVE_ARRAY(Types.DOUBLE), doublePrimitiveTypeHandler.getArrayType());
    }

    @Test
    public void shouldReturnArrayValues() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        ArrayList<Double> inputValues = new ArrayList<>(Arrays.asList(1D, 2D, 3D));
        double[] actualValues = (double[]) doublePrimitiveTypeHandler.getArray(inputValues);

        assertTrue(Arrays.equals(new double[]{1D, 2D, 3D}, actualValues));
    }

    @Test
    public void shouldReturnEmptyArrayOnNull() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        DoublePrimitiveTypeHandler doublePrimitiveTypeHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        Object actualValues = doublePrimitiveTypeHandler.getArray(null);

        assertEquals(0, ((double[]) actualValues).length);
    }

    @Test
    public void shouldFetchParsedValueForFieldOfTypeDoubleInSimpleGroup() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");
        GroupType parquetSchema = org.apache.parquet.schema.Types.requiredGroup()
                .required(DOUBLE).named("cash_amount")
                .named("TestGroupType");
        SimpleGroup simpleGroup = new SimpleGroup(parquetSchema);
        simpleGroup.add("cash_amount", 34.23D);

        DoublePrimitiveTypeHandler doubleHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);
        Object actualValue = doubleHandler.parseSimpleGroup(simpleGroup);

        assertEquals(34.23D, actualValue);
    }

    @Test
    public void shouldFetchDefaultValueIfFieldNotPresentInSimpleGroup() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");

        GroupType parquetSchema = org.apache.parquet.schema.Types.requiredGroup()
                .required(DOUBLE).named("some-other-field")
                .named("TestGroupType");
        SimpleGroup simpleGroup = new SimpleGroup(parquetSchema);
        DoublePrimitiveTypeHandler doubleHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);

        Object actualValue = doubleHandler.parseSimpleGroup(simpleGroup);

        assertEquals(0.0D, actualValue);
    }

    @Test
    public void shouldFetchDefaultValueIfFieldNotInitializedWithAValueInSimpleGroup() {
        Descriptors.FieldDescriptor fieldDescriptor = TestBookingLogMessage.getDescriptor().findFieldByName("cash_amount");

        /* The field is added to the schema but not assigned a value */
        GroupType parquetSchema = org.apache.parquet.schema.Types.requiredGroup()
                .required(DOUBLE).named("cash_amount")
                .named("TestGroupType");
        SimpleGroup simpleGroup = new SimpleGroup(parquetSchema);
        DoublePrimitiveTypeHandler doubleHandler = new DoublePrimitiveTypeHandler(fieldDescriptor);

        Object actualValue = doubleHandler.parseSimpleGroup(simpleGroup);

        assertEquals(0.0D, actualValue);
    }

}
