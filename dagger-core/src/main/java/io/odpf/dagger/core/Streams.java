package io.odpf.dagger.core;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.types.Row;

import com.google.gson.Gson;
import io.odpf.dagger.common.configuration.Configuration;
import io.odpf.dagger.common.core.StencilClientOrchestrator;
import io.odpf.dagger.core.metrics.telemetry.TelemetryPublisher;
import io.odpf.dagger.core.source.ProtoDeserializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static io.odpf.dagger.common.core.Constants.INPUT_STREAMS;
import static io.odpf.dagger.common.core.Constants.STREAM_INPUT_SCHEMA_PROTO_CLASS;
import static io.odpf.dagger.common.core.Constants.STREAM_INPUT_SCHEMA_TABLE;
import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.INPUT_PROTO;
import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.INPUT_STREAM;
import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.INPUT_TOPIC;
import static io.odpf.dagger.core.utils.Constants.INPUT_STREAM_NAME_KEY;
import static io.odpf.dagger.core.utils.Constants.SOURCE_KAFKA_CONSUME_LARGE_MESSAGE_ENABLE_DEFAULT;
import static io.odpf.dagger.core.utils.Constants.SOURCE_KAFKA_CONSUME_LARGE_MESSAGE_ENABLE_KEY;
import static io.odpf.dagger.core.utils.Constants.SOURCE_KAFKA_MAX_PARTITION_FETCH_BYTES_DEFAULT;
import static io.odpf.dagger.core.utils.Constants.SOURCE_KAFKA_MAX_PARTITION_FETCH_BYTES_KEY;
import static io.odpf.dagger.core.utils.Constants.STREAM_INPUT_SCHEMA_EVENT_TIMESTAMP_FIELD_INDEX_KEY;
import static io.odpf.dagger.core.utils.Constants.STREAM_SOURCE_KAFKA_TOPIC_NAMES_KEY;

/**
 * The Streams.
 */
public class Streams implements TelemetryPublisher {
    private static final String KAFKA_PREFIX = "source_kafka_consumer_config_";

    private Map<String, KafkaSource> kafkaSources = new HashMap<>();
    private LinkedHashMap<String, String> protoClassForTable = new LinkedHashMap<>();
    private final Configuration configuration;
    private StencilClientOrchestrator stencilClientOrchestrator;
    private Map<String, List<String>> metrics = new HashMap<>();
    private List<String> topics = new ArrayList<>();
    private List<String> protoClassNames = new ArrayList<>();
    private List<String> streamNames = new ArrayList<>();
    private static final Gson GSON = new Gson();

    /**
     * Instantiates a new Streams.
     *
     * @param configuration             the configuration
     * @param rowTimeAttributeName      the row time attribute name
     * @param stencilClientOrchestrator the stencil client orchestrator
     */
    public Streams(Configuration configuration, String rowTimeAttributeName, StencilClientOrchestrator stencilClientOrchestrator) {
        this.configuration = configuration;
        this.stencilClientOrchestrator = stencilClientOrchestrator;
        String jsonArrayString = configuration.getString(INPUT_STREAMS, "");
        Map[] streamsConfig = GSON.fromJson(jsonArrayString, Map[].class);
        for (Map<String, String> streamConfig : streamsConfig) {
            String tableName = streamConfig.getOrDefault(STREAM_INPUT_SCHEMA_TABLE, "");
            kafkaSources.put(tableName, getKafkaSource(rowTimeAttributeName, streamConfig));
        }
    }


    public Map<String, KafkaSource> getKafkaSource() {
        return kafkaSources;
    }

    /**
     * Gets protos.
     *
     * @return the protos
     */
    public LinkedHashMap<String, String> getProtos() {
        return protoClassForTable;
    }

    @Override
    public void preProcessBeforeNotifyingSubscriber() {
        addTelemetry();
    }

    @Override
    public Map<String, List<String>> getTelemetry() {
        return metrics;
    }

    private static String parseVarName(String varName, String kafkaPrefix) {
        String[] names = varName.toLowerCase().replaceAll(kafkaPrefix, "").split("_");
        return String.join(".", names);
    }

    private KafkaSource<Row> getKafkaSource(String rowTimeAttributeName, Map<String, String> streamConfig) {
        String topicsForStream = streamConfig.getOrDefault(STREAM_SOURCE_KAFKA_TOPIC_NAMES_KEY, "");
        topics.add(topicsForStream);
        String protoClassName = streamConfig.getOrDefault(STREAM_INPUT_SCHEMA_PROTO_CLASS, "");
        protoClassNames.add(protoClassName);
        streamNames.add(streamConfig.getOrDefault(INPUT_STREAM_NAME_KEY, ""));
        String tableName = streamConfig.getOrDefault(STREAM_INPUT_SCHEMA_TABLE, "");
        protoClassForTable.put(tableName, protoClassName);
        int timestampFieldIndex = Integer.parseInt(streamConfig.getOrDefault(STREAM_INPUT_SCHEMA_EVENT_TIMESTAMP_FIELD_INDEX_KEY, ""));
        Properties kafkaProps = new Properties();
        streamConfig.entrySet()
                .stream()
                .filter(e -> e.getKey().toLowerCase().startsWith(KAFKA_PREFIX))
                .forEach(e -> kafkaProps.setProperty(parseVarName(e.getKey(), KAFKA_PREFIX), e.getValue()));

        setAdditionalConfigs(kafkaProps);


        // TODO : OffsetReset Strategy can be more matured
        KafkaSource<Row> source = KafkaSource.<Row>builder()
                .setTopicPattern(Pattern.compile(topicsForStream))
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.LATEST))
                .setProperties(kafkaProps)
                .setDeserializer(KafkaRecordDeserializationSchema.of(new ProtoDeserializer(protoClassName, timestampFieldIndex, rowTimeAttributeName, stencilClientOrchestrator)))
                .build();

        return source;
    }

    private void setAdditionalConfigs(Properties kafkaProps) {
        if (configuration.getBoolean(SOURCE_KAFKA_CONSUME_LARGE_MESSAGE_ENABLE_KEY, SOURCE_KAFKA_CONSUME_LARGE_MESSAGE_ENABLE_DEFAULT)) {
            kafkaProps.setProperty(SOURCE_KAFKA_MAX_PARTITION_FETCH_BYTES_KEY, SOURCE_KAFKA_MAX_PARTITION_FETCH_BYTES_DEFAULT);
        }
    }

    private void addTelemetry() {
        List<String> topicsToReport = new ArrayList<>();
        topics.forEach(topicsPerStream -> topicsToReport.addAll(Arrays.asList(topicsPerStream.split("\\|"))));
        topicsToReport.forEach(topic -> addMetric(INPUT_TOPIC.getValue(), topic));
        protoClassNames.forEach(protoClassName -> addMetric(INPUT_PROTO.getValue(), protoClassName));
        streamNames.forEach(streamName -> addMetric(INPUT_STREAM.getValue(), streamName));
    }

    private void addMetric(String key, String value) {
        metrics.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
}
