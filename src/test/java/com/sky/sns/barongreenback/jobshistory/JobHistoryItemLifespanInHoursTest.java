package com.sky.sns.barongreenback.jobshistory;

import org.junit.Test;

import java.util.Properties;

import static com.googlecode.totallylazy.matchers.Matchers.is;
import static com.sky.sns.barongreenback.jobshistory.JobHistoryItemLifespanInHours.DEFAULT;
import static com.sky.sns.barongreenback.jobshistory.JobHistoryItemLifespanInHours.PROPERTY_NAME;
import static org.junit.Assert.assertThat;

public class JobHistoryItemLifespanInHoursTest {

    @Test
    public void shouldInitialiseFromProperty() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTY_NAME, "5");

        assertThat(new JobHistoryItemLifespanInHours(properties).value(), is(5));
    }

    @Test
    public void shouldUseDefaultValueIfPropertyIsMissing() throws Exception {
        assertThat(new JobHistoryItemLifespanInHours(new Properties()).value(), is(DEFAULT));
    }
}