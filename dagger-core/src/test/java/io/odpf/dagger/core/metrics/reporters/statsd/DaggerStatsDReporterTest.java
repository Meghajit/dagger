package io.odpf.dagger.core.metrics.reporters.statsd;

import org.apache.flink.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DaggerStatsDReporterTest {

    @Mock
    private Configuration flinkConfiguration;

    @Mock
    private io.odpf.dagger.common.configuration.Configuration daggerConfiguration;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldBeAbleToBuildAndMaintainSingletonCopyOfStatsDReporter() {
        when(daggerConfiguration.getString(anyString(), anyString())).thenReturn("some-tag");

        DaggerStatsDReporter daggerStatsDReporter = DaggerStatsDReporter.Provider.provide(flinkConfiguration, daggerConfiguration);

        assertEquals(daggerStatsDReporter.buildStatsDReporter(), daggerStatsDReporter.buildStatsDReporter());

    }

    @Test
    public void shouldBuildStatsDReporterWithGlobalTags() {
        when(daggerConfiguration.getString(anyString(), anyString())).thenReturn("some-tag");

        DaggerStatsDReporter.Provider
                .provide(flinkConfiguration, daggerConfiguration)
                .buildStatsDReporter();

        ArgumentCaptor<String> jobIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> defaultJobIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(daggerConfiguration, times(1)).getString(jobIdCaptor.capture(), defaultJobIdCaptor.capture());

        assertEquals("FLINK_JOB_ID", jobIdCaptor.getValue());
        assertEquals("SQL Flink job", defaultJobIdCaptor.getValue());
    }
}
