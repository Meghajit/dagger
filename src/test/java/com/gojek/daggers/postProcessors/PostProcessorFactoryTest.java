package com.gojek.daggers.postProcessors;

import com.gojek.daggers.core.StencilClientOrchestrator;
import com.gojek.daggers.postProcessors.common.PostProcessor;
import com.gojek.daggers.postProcessors.longbow.LongbowProcessor;
import com.gojek.daggers.postProcessors.telemetry.TelemetryProcessor;
import com.gojek.daggers.postProcessors.telemetry.processor.MetricsTelemetryExporter;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static com.gojek.daggers.utils.Constants.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class PostProcessorFactoryTest {

    @Mock
    private Configuration configuration;

    @Mock
    private StencilClientOrchestrator stencilClientOrchestrator;

    @Mock
    private MetricsTelemetryExporter metricsTelemetryExporter;

    private String[] columnNames;

    private String jsonArray = "[\n"
            + "        {\n"
            + "            \"EVENT_TIMESTAMP_FIELD_INDEX\": \"4\",\n"
            + "            \"KAFKA_CONSUMER_CONFIG_AUTO_COMMIT_ENABLE\": \"false\",\n"
            + "            \"KAFKA_CONSUMER_CONFIG_AUTO_OFFSET_RESET\": \"latest\",\n"
            + "            \"KAFKA_CONSUMER_CONFIG_BOOTSTRAP_SERVERS\": \"p-esb-kafka-mirror-b-01:6667\",\n"
            + "            \"KAFKA_CONSUMER_CONFIG_GROUP_ID\": \"flink-sql-flud-gp0330\",\n"
            + "            \"PROTO_CLASS_NAME\": \"com.gojek.esb.booking.BookingLogMessage\",\n"
            + "            \"TABLE_NAME\": \"data_stream\",\n"
            + "            \"TOPIC_NAMES\": \"GO_RIDE-booking-log\"\n"
            + "        }\n"
            + "]";

    @Before
    public void setup() {
        initMocks(this);
        columnNames = new String[]{"a", "b", "longbow_duration"};
    }


    @Test
    public void shouldReturnLongbowProcessor() {
        columnNames = new String[]{"longbow_key", "longbow_data", "event_timestamp", "rowtime", "longbow_duration"};
        when(configuration.getBoolean(ASYNC_IO_ENABLED_KEY, ASYNC_IO_ENABLED_DEFAULT)).thenReturn(false);
        when(configuration.getString(SQL_QUERY, SQL_QUERY_DEFAULT)).thenReturn("longbow_key");
        when(configuration.getBoolean(POST_PROCESSOR_ENABLED_KEY, POST_PROCESSOR_ENABLED_KEY_DEFAULT)).thenReturn(false);
        when(configuration.getString(INPUT_STREAMS, "")).thenReturn(jsonArray);

        List<PostProcessor> postProcessors = PostProcessorFactory.getPostProcessors(configuration, stencilClientOrchestrator, columnNames, metricsTelemetryExporter);

        Assert.assertEquals(1, postProcessors.size());
        Assert.assertEquals(LongbowProcessor.class, postProcessors.get(0).getClass());
    }

    @Test
    public void shouldReturnParentPostProcessor() {
        when(configuration.getBoolean(ASYNC_IO_ENABLED_KEY, ASYNC_IO_ENABLED_DEFAULT)).thenReturn(false);
        when(configuration.getString(SQL_QUERY, SQL_QUERY_DEFAULT)).thenReturn("test-sql");
        when(configuration.getBoolean(POST_PROCESSOR_ENABLED_KEY, POST_PROCESSOR_ENABLED_KEY_DEFAULT)).thenReturn(true);

        List<PostProcessor> postProcessors = PostProcessorFactory.getPostProcessors(configuration, stencilClientOrchestrator, columnNames, metricsTelemetryExporter);

        Assert.assertEquals(1, postProcessors.size());
        Assert.assertEquals(ParentPostProcessor.class, postProcessors.get(0).getClass());
    }

    @Test
    public void shouldReturnTelemetryPostProcessor() {
        when(configuration.getBoolean(ASYNC_IO_ENABLED_KEY, ASYNC_IO_ENABLED_DEFAULT)).thenReturn(false);
        when(configuration.getString(SQL_QUERY, SQL_QUERY_DEFAULT)).thenReturn("test-sql");
        when(configuration.getBoolean(POST_PROCESSOR_ENABLED_KEY, POST_PROCESSOR_ENABLED_KEY_DEFAULT)).thenReturn(false);
        when(configuration.getBoolean(TELEMETRY_ENABLED_KEY, TELEMETRY_ENABLED_VALUE_DEFAULT)).thenReturn(true);

        List<PostProcessor> postProcessors = PostProcessorFactory.getPostProcessors(configuration, stencilClientOrchestrator, columnNames, metricsTelemetryExporter);

        Assert.assertEquals(1, postProcessors.size());
        Assert.assertEquals(TelemetryProcessor.class, postProcessors.get(0).getClass());
    }

    @Test
    public void shouldNotReturnAnyPostProcessor() {
        when(configuration.getBoolean(ASYNC_IO_ENABLED_KEY, ASYNC_IO_ENABLED_DEFAULT)).thenReturn(false);
        when(configuration.getString(SQL_QUERY, SQL_QUERY_DEFAULT)).thenReturn("test-sql");
        when(configuration.getBoolean(POST_PROCESSOR_ENABLED_KEY, POST_PROCESSOR_ENABLED_KEY_DEFAULT)).thenReturn(false);
        List<PostProcessor> postProcessors = PostProcessorFactory.getPostProcessors(configuration, stencilClientOrchestrator, columnNames, metricsTelemetryExporter);

        Assert.assertEquals(0, postProcessors.size());
    }
}