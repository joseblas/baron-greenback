package com.googlecode.barongreenback.crawler;

import com.googlecode.lazyrecords.Record;
import com.googlecode.totallylazy.CountLatch;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.utterlyidle.Application;
import com.googlecode.yadic.Container;

import java.io.PrintStream;
import java.util.concurrent.Future;

public class StagedJobExecutor {
    private final InputHandler inputHandler;
    private final ProcessHandler processHandler;
    private final OutputHandler outputHandler;
    private final Application application;

    public StagedJobExecutor(CrawlerExecutors executors, Application application) {
        this.inputHandler = executors.inputHandler();
        this.processHandler = executors.processHandler();
        this.outputHandler = executors.outputHandler();
        this.application = application;
    }

    public Future<?> crawl(StagedJob job) throws InterruptedException {
        return submit(inputHandler, HttpReader.getInput(job).then(
                submit(processHandler, processJobs(job.process()).then(
                        submit(outputHandler, DataWriter.write(application, job), job.container())), job.container())), job.container());
    }

    private Future<?> submit(JobExecutor jobExecutor, final Runnable function, final Container container) {
        container.get(CountLatch.class).countUp();
        return jobExecutor.executor.submit(logExceptions(countLatchDownAfter(function, container.get(CountLatch.class)), container.get(PrintStream.class)));
    }

    private <T> Function1<T, Future<?>> submit(final JobExecutor jobExecutor, final Function1<T, ?> runnable, final Container container) {
        return new Function1<T, Future<?>>() {
            @Override
            public Future<?> call(T result) throws Exception {
                return submit(jobExecutor, runnable.deferApply(result), container);
            }
        };
    }

    private Runnable countLatchDownAfter(final Runnable function, final CountLatch countLatch) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    function.run();
                } finally {
                    countLatch.countDown();
                }
            }
        };
    }

    private Runnable logExceptions(final Runnable function, final PrintStream printStream) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    function.run();
                } catch (RuntimeException e) {
                    e.printStackTrace(printStream);
                    throw e;
                }
            }
        };
    }

    private <T> Function1<T, Sequence<Record>> processJobs(final Function1<T, Pair<Sequence<Record>, Sequence<StagedJob>>> function) {
        return new Function1<T, Sequence<Record>>() {
            @Override
            public Sequence<Record> call(T t) throws Exception {
                Pair<Sequence<Record>, Sequence<StagedJob>> pair = function.call(t);
                for (StagedJob job : pair.second()) {
                    crawl(job);
                }
                return pair.first();
            }
        };
    }
}