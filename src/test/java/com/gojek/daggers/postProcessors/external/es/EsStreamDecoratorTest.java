package com.gojek.daggers.postProcessors.external.es;

import com.gojek.daggers.core.StencilClientOrchestrator;
import com.gojek.daggers.metrics.telemetry.TelemetrySubscriber;
import com.gojek.daggers.postProcessors.common.ColumnNameManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Mockito.mock;

public class EsStreamDecoratorTest {

    private StencilClientOrchestrator stencilClientOrchestrator;
    private EsSourceConfig esSourceConfig;
    private boolean telemetryEnabled;
    private long shutDownPeriod;

    @Mock
    private TelemetrySubscriber telemetrySubscriber;

    @Before
    public void setUp() {
        stencilClientOrchestrator = mock(StencilClientOrchestrator.class);
        telemetryEnabled = true;
        shutDownPeriod = 0L;
        esSourceConfig = new EsSourceConfig("localhost", "9200", "",
                "driver_id", "com.gojek.esb.fraud.DriverProfileFlattenLogMessage", "30",
                "5000", "5000", "5000", "5000", false, new HashMap<>());
    }

    @Test
    public void canDecorateStreamWhenConfigIsPresent() {
        EsStreamDecorator esStreamDecorator = new EsStreamDecorator(esSourceConfig, stencilClientOrchestrator, new ColumnNameManager(new String[4], new ArrayList<>()), telemetrySubscriber, telemetryEnabled, shutDownPeriod);

        Assert.assertTrue(esStreamDecorator.canDecorate());
    }

    @Test
    public void cannotDecorateStreamWhenConfigIsNull() {
        EsStreamDecorator esStreamDecorator = new EsStreamDecorator(null, stencilClientOrchestrator, new ColumnNameManager(new String[4], new ArrayList<>()), telemetrySubscriber, telemetryEnabled, shutDownPeriod);

        Assert.assertFalse(esStreamDecorator.canDecorate());
    }
}