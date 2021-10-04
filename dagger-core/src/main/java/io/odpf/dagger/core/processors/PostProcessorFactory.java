package io.odpf.dagger.core.processors;

import io.odpf.dagger.common.core.StencilClientOrchestrator;
import io.odpf.dagger.core.processors.types.PostProcessor;
import io.odpf.dagger.core.processors.longbow.LongbowFactory;
import io.odpf.dagger.core.processors.longbow.LongbowSchema;
import io.odpf.dagger.core.processors.telemetry.TelemetryProcessor;
import io.odpf.dagger.core.processors.telemetry.processor.MetricsTelemetryExporter;
import io.odpf.dagger.core.utils.Constants;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The factory class for Post processor.
 */
public class PostProcessorFactory {

    /**
     * Gets post processors.
     *
     * @param parameter             the configuration
     * @param stencilClientOrchestrator the stencil client orchestrator
     * @param columnNames               the column names
     * @param metricsTelemetryExporter  the metrics telemetry exporter
     * @return the post processors
     */
    public static List<PostProcessor> getPostProcessors(ParameterTool parameter, StencilClientOrchestrator stencilClientOrchestrator, String[] columnNames, MetricsTelemetryExporter metricsTelemetryExporter) {
        List<PostProcessor> postProcessors = new ArrayList<>();

        if (Arrays.stream(columnNames).anyMatch(s -> Pattern.compile(".*\\blongbow.*key\\b.*").matcher(s).find())) {
            postProcessors.add(getLongBowProcessor(columnNames, parameter, metricsTelemetryExporter, stencilClientOrchestrator));
        }
        if (parameter.getBoolean(Constants.PROCESSOR_POSTPROCESSOR_ENABLE_KEY, Constants.PROCESSOR_POSTPROCESSOR_ENABLE_DEFAULT)) {
            postProcessors.add(new ParentPostProcessor(parsePostProcessorConfig(parameter), parameter, stencilClientOrchestrator, metricsTelemetryExporter));
        }
        if (parameter.getBoolean(Constants.METRIC_TELEMETRY_ENABLE_KEY, Constants.METRIC_TELEMETRY_ENABLE_VALUE_DEFAULT)) {
            postProcessors.add(new TelemetryProcessor(metricsTelemetryExporter));
        }
        return postProcessors;
    }

    private static PostProcessor getLongBowProcessor(String[] columnNames, ParameterTool parameter, MetricsTelemetryExporter metricsTelemetryExporter, StencilClientOrchestrator stencilClientOrchestrator) {
        final LongbowSchema longbowSchema = new LongbowSchema(columnNames);
        LongbowFactory longbowFactory = new LongbowFactory(longbowSchema, parameter, stencilClientOrchestrator, metricsTelemetryExporter);

        return longbowFactory.getLongbowProcessor();
    }

    private static PostProcessorConfig parsePostProcessorConfig(ParameterTool parameter) {
        String postProcessorConfigString = parameter.get(Constants.PROCESSOR_POSTPROCESSOR_CONFIG_KEY, "");
        return PostProcessorConfig.parse(postProcessorConfigString);
    }
}
