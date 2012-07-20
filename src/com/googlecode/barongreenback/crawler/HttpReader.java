package com.googlecode.barongreenback.crawler;

import com.googlecode.totallylazy.Function;
import com.googlecode.totallylazy.Uri;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.handlers.HttpClient;
import com.googlecode.yadic.Container;

import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.handlers.Handlers.asFunction;

public class HttpReader {
    public static Function<Response> getInput(StagedJob job, Container crawlerScope) {
        Uri uri = job.datasource().uri();
        HttpClient httpClient = crawlerScope.get(HttpClient.class);
        FailureHandler failureHandler = crawlerScope.get(FailureHandler.class);
        return failureHandler.captureFailures(asFunction(httpClient), job).deferApply(get(uri).build());
    }
}