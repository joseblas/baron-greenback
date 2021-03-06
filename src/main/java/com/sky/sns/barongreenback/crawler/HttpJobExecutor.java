package com.sky.sns.barongreenback.crawler;

import com.sky.sns.barongreenback.crawler.executor.CrawlerExecutors;
import com.sky.sns.barongreenback.crawler.executor.JobExecutor;
import com.sky.sns.barongreenback.crawler.executor.PriorityJobRunnable;
import com.sky.sns.barongreenback.crawler.jobs.Job;
import com.googlecode.lazyrecords.Record;
import com.googlecode.totallylazy.Block;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.utterlyidle.Response;
import com.googlecode.yadic.Container;

import java.io.PrintStream;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sky.sns.barongreenback.crawler.DataWriter.write;
import static com.sky.sns.barongreenback.crawler.HttpReader.getInput;

public class HttpJobExecutor {
    private final CrawlerExecutors executors;
    private final Container crawlerScope;
    private final Phaser phaser;

    public HttpJobExecutor(CrawlerExecutors executors, Container crawlerScope) {
        this.executors = executors;
        this.crawlerScope = crawlerScope;
        phaser =  crawlerScope.get(Phaser.class);
    }

    public int executeAndWait(Job job) throws InterruptedException {
        phaser.register();
        execute(job);
        phaser.awaitAdvance(phaser.arriveAndDeregister());
        return crawlerScope.get(AtomicInteger.class).get();
    }

    public void execute(Job job) throws InterruptedException {
        submit(job, executors.inputHandler(job), getInput(job, crawlerScope).then(
                submit(job, executors.processHandler(job), processJobs(job, crawlerScope).then(
                        submit(job, executors.outputHandler(job), write(job, crawlerScope))))));
    }

    private void submit(Job job, JobExecutor<PriorityJobRunnable> jobExecutor, final Runnable function) {
        phaser.register();
        PriorityJobRunnable priorityJobRunnable = new PriorityJobRunnable(job, logExceptions(countLatchDownAfter(function), crawlerScope.get(PrintStream.class)));
        jobExecutor.execute(priorityJobRunnable);
    }

    private <T> Block<T> submit(final Job job, final JobExecutor<PriorityJobRunnable> jobExecutor, final Function1<T, ?> runnable) {
        return new Block<T>() {
            @Override
            public void execute(T result) throws Exception {
                submit(job, jobExecutor, runnable.interruptable().deferApply(result));
            }
        };
    }

    private Runnable countLatchDownAfter(final Runnable function) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    function.run();
                } finally {
                    phaser.arriveAndDeregister();
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

    private Function1<Response, Sequence<Record>> processJobs(final Job job, final Container scope) {
        return new Function1<Response, Sequence<Record>>() {
            @Override
            public Sequence<Record> call(Response t) throws Exception {
                Pair<Sequence<Record>, Sequence<Job>> pair = job.process(scope, t);
                for (Job job : pair.second().interruptable()) {
                    execute(job);
                }
                return pair.first().interruptable();
            }
        };
    }
}
