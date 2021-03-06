package com.sky.sns.barongreenback.crawler.executor;

import com.sky.sns.barongreenback.crawler.DataWriter;
import com.sky.sns.barongreenback.crawler.jobs.Job;
import com.googlecode.lazyrecords.Definition;
import com.googlecode.totallylazy.*;
import com.googlecode.utterlyidle.Application;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.sky.sns.barongreenback.crawler.executor.CrawlerConfigValues.*;
import static com.googlecode.totallylazy.Callables.value;
import static com.googlecode.totallylazy.Closeables.safeClose;
import static com.googlecode.totallylazy.Sequences.sequence;

public class CrawlerExecutors implements Closeable {

    private ConcurrentHashMap<String, Lazy<JobExecutor<PriorityJobRunnable>>> inputHandler = new ConcurrentHashMap<String, Lazy<JobExecutor<PriorityJobRunnable>>>();
    private ConcurrentHashMap<String, Lazy<JobExecutor<PriorityJobRunnable>>> processHandler = new ConcurrentHashMap<String, Lazy<JobExecutor<PriorityJobRunnable>>>();
    private ConcurrentHashMap<String, Lazy<DataWriter>> outputHandler = new ConcurrentHashMap<String, Lazy<DataWriter>>();
    private Map<CrawlerConfigValues, Integer> configValues = new HashMap<CrawlerConfigValues, Integer>();
    private final Application application;
    private final ExecutorFactory executorFactory;

    public CrawlerExecutors(Integer inputHandlerThreads, Integer inputHandlerCapacity, Integer processHandlerThreads, Integer processHandlerCapacity,
                            Integer outputHandlerThreads, Integer outputHandlerCapacity, Application application, ExecutorFactory executorFactory) {
        this.application = application;
        configValues.put(INPUT_HANDLER_THREADS, inputHandlerThreads);
        configValues.put(INPUT_HANDLER_CAPACITY, inputHandlerCapacity);
        configValues.put(PROCESS_HANDLER_THREADS, processHandlerThreads);
        configValues.put(PROCESS_HANDLER_CAPACITY, processHandlerCapacity);
        configValues.put(OUTPUT_HANDLER_THREADS, outputHandlerThreads);
        configValues.put(OUTPUT_HANDLER_CAPACITY, outputHandlerCapacity);
        this.executorFactory = executorFactory;
    }

    private DataWriter outputExecutor(String suffix) {
        return createDataWriter(configValues.get(OUTPUT_HANDLER_THREADS), configValues.get(OUTPUT_HANDLER_CAPACITY), 1, "Writing " + suffix);
    }

    private ThreadPoolJobExecutor<PriorityJobRunnable> processExecutor(String suffix) {
        return executorFactory.executor(configValues.get(PROCESS_HANDLER_THREADS), configValues.get(PROCESS_HANDLER_CAPACITY), "Processing " + suffix, BoundedPriorityBlockingQueue.class);
    }

    ThreadPoolJobExecutor<PriorityJobRunnable>  inputExecutor(String suffix) {
        return executorFactory.executor(configValues.get(INPUT_HANDLER_THREADS), configValues.get(INPUT_HANDLER_CAPACITY), "Reading " + suffix, BoundedPriorityBlockingQueue.class);
    }

    public void resetExecutors() {
        close();
        inputHandler.clear();
        processHandler.clear();
        outputHandler.clear();
    }

    public Integer inputHandlerThreads() {
        return configValues.get(INPUT_HANDLER_THREADS);
    }

    public Integer processHandlerThreads() {
        return configValues.get(PROCESS_HANDLER_THREADS);
    }

    public Integer outputHandlerThreads() {
        return configValues.get(OUTPUT_HANDLER_THREADS);
    }

    public Integer inputHandlerCapacity() {
        return configValues.get(INPUT_HANDLER_CAPACITY);
    }

    public Integer processHandlerCapacity() {
        return configValues.get(PROCESS_HANDLER_CAPACITY);
    }

    public Integer outputHandlerCapacity() {
        return configValues.get(OUTPUT_HANDLER_CAPACITY);
    }

    public JobExecutor<PriorityJobRunnable> inputHandler(Job job) {
        final String key = Option.option(job.dataSource().uri().authority()).getOrElse(job.dataSource().uri().path());
        inputHandler.putIfAbsent(key, new Lazy<JobExecutor<PriorityJobRunnable>>() {
            @Override
            protected JobExecutor get() throws Exception {
                return inputExecutor(key);
            }
        });
        return inputHandler.get(key).value();
    }

    public JobExecutor<PriorityJobRunnable> processHandler(Job job) {
        final Definition definition = job.destination();
        processHandler.putIfAbsent(definition.name(), new Lazy<JobExecutor<PriorityJobRunnable>>() {
            @Override
            protected JobExecutor get() throws Exception {
                return processExecutor(definition.name());
            }
        });
        return processHandler.get(definition.name()).value();
    }

    public DataWriter outputHandler(Job job) {
        final Definition definition = job.destination();
        outputHandler.putIfAbsent(definition.name(), new Lazy<DataWriter>() {
            @Override
            protected DataWriter get() throws Exception {
                return outputExecutor(definition.name());
            }
        });
        return outputHandler.get(definition.name()).value();
    }

    public Sequence<JobExecutor<PriorityJobRunnable>> statusMonitors() {
        Sequence<JobExecutor<PriorityJobRunnable>> inputHandlers = sequence(inputHandler.values()).map(Callables.<JobExecutor<PriorityJobRunnable>>value());
        Sequence<JobExecutor<PriorityJobRunnable>> processHandlers = sequence(processHandler.values()).map(Callables.<JobExecutor<PriorityJobRunnable>>value());
        Sequence<DataWriter> outputHandlers = sequence(outputHandler.values()).map(value(DataWriter.class));

        return Sequences.flatten(sequence(inputHandlers, processHandlers, outputHandlers));
    }

    public void handlerValues(Sequence<Pair<CrawlerConfigValues, Integer>> executorValues) {
        configValues.putAll(Maps.map(executorValues));
        resetExecutors();
    }

    @Override
    public void close() {
        statusMonitors().each(safeClose());
    }

    private DataWriter createDataWriter(int threads, int capacity, int seconds, String name) {
        return new DataWriter(application, threads, seconds, name, capacity);
    }


}

