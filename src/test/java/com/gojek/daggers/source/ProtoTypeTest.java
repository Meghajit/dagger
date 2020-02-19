package com.gojek.daggers.source;

import com.gojek.daggers.core.StencilClientOrchestrator;
import com.gojek.daggers.exception.DescriptorNotFoundException;
import com.gojek.esb.booking.BookingLogMessage;
import com.gojek.esb.consumer.TestNestedRepeatedMessage;
import com.gojek.esb.login.LoginRequestMessage;
import com.gojek.esb.participant.ParticipantLogMessage;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.Types;
import org.apache.flink.types.Row;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.gojek.daggers.utils.Constants.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

//import gojek.esb.clevertap.ClevertapEventLog;

public class ProtoTypeTest {

    @Mock
    public Configuration configuration;

    private StencilClientOrchestrator stencilClientOrchestrator;

    @Before
    public void setup() {
        initMocks(this);
        when(configuration.getString(REFRESH_CACHE_KEY, REFRESH_CACHE_DEFAULT)).thenReturn(REFRESH_CACHE_DEFAULT);
        when(configuration.getString(TTL_IN_MINUTES_KEY, TTL_IN_MINUTES_DEFAULT)).thenReturn(TTL_IN_MINUTES_DEFAULT);
        when(configuration.getBoolean(STENCIL_ENABLE_KEY, STENCIL_ENABLE_DEFAULT)).thenReturn(STENCIL_ENABLE_DEFAULT);
        when(configuration.getString(STENCIL_URL_KEY, STENCIL_URL_DEFAULT)).thenReturn(STENCIL_URL_DEFAULT);
        stencilClientOrchestrator = new StencilClientOrchestrator(configuration);
    }


    @Test
    public void shouldGiveAllColumnNamesOfProto() throws ClassNotFoundException {
        ProtoType participantKeyProtoType = new ProtoType("com.gojek.esb.participant.ParticipantLogKey", "rowtime", stencilClientOrchestrator);
        ProtoType bookingKeyProtoType = new ProtoType("com.gojek.esb.booking.BookingLogKey", "rowtime", stencilClientOrchestrator);

        assertArrayEquals(
                new String[]{"order_id", "status", "event_timestamp", "bid_id", "service_type", "participant_id", "audit"},
                participantKeyProtoType.getFieldNames());

        assertArrayEquals(
                new String[]{"service_type", "order_number", "order_url", "status", "event_timestamp", "audit"},
                bookingKeyProtoType.getFieldNames());
    }

    @Test(expected = DescriptorNotFoundException.class)
    public void shouldThrowConfigurationExceptionWhenClassNotFound() {
        new ProtoType("com.gojek.esb.participant.ParticipantLogKey211", "rowtime", stencilClientOrchestrator);
    }

    @Test(expected = DescriptorNotFoundException.class)
    public void shouldThrowConfigurationExceptionWhenClassIsNotProto() {
        new ProtoType(String.class.getName(), "rowtime", stencilClientOrchestrator);
    }

    @Test
    public void shouldGiveSimpleMappedFlinkTypes() {
        ProtoType participantMessageProtoType = new ProtoType(ParticipantLogMessage.class.getName(), "rowtime", stencilClientOrchestrator);

        TypeInformation[] fieldTypes = participantMessageProtoType.getFieldTypes();

        assertEquals(Types.STRING(), fieldTypes[participantLogFieldIndex("order_id")]);
        assertEquals(Types.INT(), fieldTypes[participantLogFieldIndex("customer_straight_line_distance")]);
        assertEquals(Types.DOUBLE(), fieldTypes[participantLogFieldIndex("receive_delay")]);
    }

    @Test
    public void shouldGiveSubRowMappedField() {
        ProtoType participantMessageProtoType = new ProtoType(ParticipantLogMessage.class.getName(), "rowtime", stencilClientOrchestrator);

        TypeInformation[] fieldTypes = participantMessageProtoType.getFieldTypes();

        TypeInformation<Row> expectedTimestampRow = Types.ROW(new String[]{"seconds", "nanos"},
                new TypeInformation<?>[]{Types.LONG(), Types.INT()});
        TypeInformation<Row> driverLocationRow = Types.ROW(new String[]{"latitude", "longitude", "altitude", "accuracy", "speed"},
                new TypeInformation<?>[]{Types.DOUBLE(), Types.DOUBLE(), Types.DOUBLE(), Types.DOUBLE(), Types.DOUBLE()});

        assertEquals(expectedTimestampRow, fieldTypes[participantLogFieldIndex("event_timestamp")]);
        assertEquals(driverLocationRow, fieldTypes[participantLogFieldIndex("location")]);
    }

    @Test
    public void shouldProcessArrayForObjectData() {
        ProtoType bookingLogMessageProtoType = new ProtoType(BookingLogMessage.class.getName(), "rowtime", stencilClientOrchestrator);

        TypeInformation[] fieldTypes = bookingLogMessageProtoType.getFieldTypes();

        TypeInformation<Row> locationType = Types.ROW(new String[]{"name", "address", "latitude", "longitude", "type",
                "note", "place_id"}, new TypeInformation<?>[]{Types.STRING(), Types.STRING(), Types.DOUBLE(), Types.DOUBLE(),
                Types.STRING(), Types.STRING(), Types.STRING()});
        TypeInformation<?> expectedRoutesRow = Types.OBJECT_ARRAY(Types.ROW(new String[]{"startTimer", "end",
                "distance_in_kms", "estimated_duration", "route_order"}, new TypeInformation<?>[]{locationType, locationType,
                Types.FLOAT(), Types.ROW(new String[]{"seconds", "nanos"}, new TypeInformation<?>[]{Types.LONG(),
                Types.INT()}), Types.INT()}));

        assertEquals(expectedRoutesRow, fieldTypes[bookingLogFieldIndex("routes")]);
    }

    @Test
    public void shouldProcessArrayForStringData() {
        ProtoType loginRequestMessageProtoType = new ProtoType(LoginRequestMessage.class.getName(), "rowtime", stencilClientOrchestrator);

        TypeInformation[] fieldTypes = loginRequestMessageProtoType.getFieldTypes();

        TypeInformation<?> registeredDeviceType = Types.OBJECT_ARRAY(Types.STRING());

        assertEquals(registeredDeviceType, fieldTypes[loginRequestFieldIndex("registered_device")]);
    }

    //Add test for array of primitive data type. Couldn't find a proto which uses array of primitive data type.

    @Test
    public void shouldGiveNamesAndTypes() {
        ProtoType participantMessageProtoType = new ProtoType(ParticipantLogMessage.class.getName(), "rowtime", stencilClientOrchestrator);

        RowTypeInfo rowType = (RowTypeInfo) participantMessageProtoType.getRowType();

        assertEquals("order_id", rowType.getFieldNames()[0]);
        assertEquals(Types.DOUBLE(), rowType.getFieldTypes()[participantLogFieldIndex("receive_delay")]);

    }

//  @Test
//  public void shouldGiveAllNamesAndTypesIncludingStructFields() {
//    ProtoType clevertapMessageProtoType = new ProtoType(ClevertapEventLog.ClevertapEventLogMessage.class.getName(), "rowtime", STENCIL_CLIENT);
//    assertEquals(10, clevertapMessageProtoType.getFieldNames().length);
//    assertEquals(10, clevertapMessageProtoType.getFieldTypes().length);
//  }
//
//  @Test
//  public void shouldReturnRowTypeForStructFields() {
//    ProtoType clevertapMessageProtoType = new ProtoType(ClevertapEventLog.ClevertapEventLogMessage.class.getName(), "rowtime", STENCIL_CLIENT);
//    assertEquals(Types.ROW(), clevertapMessageProtoType.getFieldTypes()[7]);
//    assertEquals(Types.ROW(), clevertapMessageProtoType.getFieldTypes()[8]);
//    assertEquals(Types.ROW(), clevertapMessageProtoType.getFieldTypes()[9]);
//    assertEquals("profile_data", clevertapMessageProtoType.getFieldNames()[7]);
//    assertEquals("event_properties", clevertapMessageProtoType.getFieldNames()[8]);
//    assertEquals("key_values", clevertapMessageProtoType.getFieldNames()[9]);
//  }

    @Test
    public void shouldGiveAllNamesAndTypesIncludingPrimitiveArrayFields() {
        ProtoType testNestedRepeatedMessage = new ProtoType(TestNestedRepeatedMessage.class.getName(), "rowtime", stencilClientOrchestrator);
        assertEquals(Types.PRIMITIVE_ARRAY(Types.INT()), testNestedRepeatedMessage.getFieldTypes()[3]);
    }

    @Test
    public void shouldGiveNameAndTypeForRepeatingStructType() {
        ProtoType testNestedRepeatedMessage = new ProtoType(TestNestedRepeatedMessage.class.getName(), "rowtime", stencilClientOrchestrator);
        assertEquals("metadata", testNestedRepeatedMessage.getFieldNames()[4]);
        assertEquals(Types.OBJECT_ARRAY(Types.ROW()), testNestedRepeatedMessage.getFieldTypes()[4]);
    }

    private int participantLogFieldIndex(String propertyName) {
        return ParticipantLogMessage.getDescriptor().findFieldByName(propertyName).getIndex();
    }

    private int bookingLogFieldIndex(String propertyName) {
        return BookingLogMessage.getDescriptor().findFieldByName(propertyName).getIndex();
    }

    private int loginRequestFieldIndex(String propertyName) {
        return LoginRequestMessage.getDescriptor().findFieldByName(propertyName).getIndex();
    }

}