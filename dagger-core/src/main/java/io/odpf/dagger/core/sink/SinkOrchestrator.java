package io.odpf.dagger.core.sink;

import io.odpf.dagger.core.sink.influx.ErrorHandler;
import io.odpf.dagger.core.sink.influx.InfluxDBFactoryWrapper;
import io.odpf.dagger.core.sink.influx.InfluxRowSink;
import io.odpf.dagger.core.sink.log.LogSink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducerBase;
import org.apache.flink.types.Row;

import io.odpf.dagger.core.metrics.telemetry.TelemetryPublisher;
import io.odpf.dagger.common.core.StencilClientOrchestrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.OUTPUT_PROTO;
import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.OUTPUT_TOPIC;
import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.OUTPUT_STREAM;
import static io.odpf.dagger.core.metrics.telemetry.TelemetryTypes.SINK_TYPE;
import static io.odpf.dagger.core.utils.Constants.*;

/**
 * The Sink orchestrator.
 * Responsible for handling the sink type.
 */
public class SinkOrchestrator implements TelemetryPublisher {

    private Map<String, List<String>> metrics = new HashMap<>();

    @Override
    public Map<String, List<String>> getTelemetry() {
        return metrics;
    }

    /**
     * Gets sink.
     *
     * @param param             the param
     * @param columnNames               the column names
     * @param stencilClientOrchestrator the stencil client orchestrator
     * @return the sink
     */
    public SinkFunction<Row> getSink(ParameterTool param, String[] columnNames, StencilClientOrchestrator stencilClientOrchestrator) {
        String sinkType = param.get("SINK_TYPE", "influx");
        addMetric(SINK_TYPE.getValue(), sinkType);
        SinkFunction<Row> sink;
        switch (sinkType) {
            case "kafka":
                String outputTopic = param.get(SINK_KAFKA_TOPIC_KEY, "");
                String outputProtoKey = param.get(SINK_KAFKA_PROTO_KEY, null);
                String outputProtoMessage = param.get(SINK_KAFKA_PROTO_MESSAGE_KEY, "");
                String outputStream = param.get(SINK_KAFKA_STREAM_KEY, "");
                addMetric(OUTPUT_TOPIC.getValue(), outputTopic);
                addMetric(OUTPUT_PROTO.getValue(), outputProtoMessage);
                addMetric(OUTPUT_STREAM.getValue(), outputStream);

                ProtoSerializer protoSerializer = new ProtoSerializer(outputProtoKey, outputProtoMessage, columnNames, stencilClientOrchestrator, outputTopic);
                FlinkKafkaProducer<Row> rowFlinkKafkaProducer = new FlinkKafkaProducer<>(outputTopic, protoSerializer, getProducerProperties(param), FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
                sink = new FlinkKafkaProducerCustom(rowFlinkKafkaProducer, param);
                break;
            case "log":
                sink = new LogSink(columnNames);
                break;
            default:
                sink = new InfluxRowSink(new InfluxDBFactoryWrapper(), columnNames, param, new ErrorHandler());
        }
        notifySubscriber();
        return sink;
    }

    private void addMetric(String key, String value) {
        metrics.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    /**
     * Gets producer properties.
     *
     * @param param the configuration
     * @return the producer properties
     */
    protected Properties getProducerProperties(ParameterTool param) {
        String outputBrokerList = param.get(SINK_KAFKA_BROKERS_KEY, "");
        Properties kafkaProducerConfigs = FlinkKafkaProducerBase.getPropertiesFromBrokerList(outputBrokerList);
        if (param.getBoolean(SINK_KAFKA_PRODUCE_LARGE_MESSAGE_ENABLE_KEY, SINK_KAFKA_PRODUCE_LARGE_MESSAGE_ENABLE_DEFAULT)) {
            kafkaProducerConfigs.setProperty(SINK_KAFKA_COMPRESSION_TYPE_KEY, SINK_KAFKA_COMPRESSION_TYPE_DEFAULT);
            kafkaProducerConfigs.setProperty(SINK_KAFKA_MAX_REQUEST_SIZE_KEY, SINK_KAFKA_MAX_REQUEST_SIZE_DEFAULT);
        }
        return kafkaProducerConfigs;
    }
}
