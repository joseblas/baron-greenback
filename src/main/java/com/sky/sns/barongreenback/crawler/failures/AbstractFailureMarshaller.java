package com.sky.sns.barongreenback.crawler.failures;

import com.googlecode.funclate.Model;
import com.googlecode.lazyrecords.Definition;
import com.googlecode.lazyrecords.Keyword;
import com.googlecode.lazyrecords.Record;
import com.googlecode.lazyrecords.mappings.StringMapping;
import com.googlecode.lazyrecords.mappings.StringMappings;
import com.googlecode.totallylazy.Function2;
import com.googlecode.totallylazy.LazyException;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.time.Clock;
import com.sky.sns.barongreenback.crawler.CheckpointHandler;
import com.sky.sns.barongreenback.crawler.Crawler;
import com.sky.sns.barongreenback.crawler.CrawlerRepository;
import com.sky.sns.barongreenback.crawler.HttpVisitedFactory;
import com.sky.sns.barongreenback.crawler.datasources.HttpDataSource;
import com.sky.sns.barongreenback.persistence.BaronGreenbackStringMappings;
import com.sky.sns.barongreenback.shared.RecordDefinition;

import java.util.List;
import java.util.UUID;

import static com.googlecode.funclate.Model.mutable.model;
import static com.googlecode.funclate.Model.mutable.parse;
import static com.googlecode.lazyrecords.Keyword.constructors.keyword;
import static com.googlecode.lazyrecords.Record.constructors.record;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.sky.sns.barongreenback.crawler.datasources.HttpDataSource.httpDataSource;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.CRAWLER_ID;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.DURATION;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.JOB_TYPE;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.REASON;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.RECORD;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.REQUEST_TIME;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.SOURCE;
import static com.sky.sns.barongreenback.crawler.failures.FailureRepository.URI;
import static com.sky.sns.barongreenback.shared.RecordDefinition.convert;

abstract public class AbstractFailureMarshaller implements FailureMarshaller {
    private final CrawlerRepository crawlerRepository;
    private final CheckpointHandler checkpointHandler;
    protected final HttpVisitedFactory visited;
    protected final Clock clock;
    private final StringMappings stringMappings;

    public AbstractFailureMarshaller(CrawlerRepository crawlerRepository, CheckpointHandler checkpointHandler, HttpVisitedFactory visited, BaronGreenbackStringMappings stringMappings, Clock clock) {
        this.crawlerRepository = crawlerRepository;
        this.checkpointHandler = checkpointHandler;
        this.visited = visited;
        this.stringMappings = stringMappings.value();
        this.clock = clock;
    }

    public Definition destination(Record record) {
        return Crawler.methods.destinationDefinition(crawlerIdFor(record));
    }

    public HttpDataSource dataSource(Record record) {
        return httpDataSource(record.get(URI), convert(parse(record.get(SOURCE))).definition());
    }

    @Override
    public Record marshal(Failure failure) {
        return record().
                set(JOB_TYPE, FailureMarshallers.forJob(failure.job()).name()).
                set(REASON, failure.reason()).
                set(URI, failure.job().dataSource().uri()).
                set(REQUEST_TIME, failure.job().createdDate()).
                set(DURATION, failure.duration()).
                set(SOURCE, RecordDefinition.toModel(failure.job().dataSource().source()).toString()).
                set(RECORD, toJson(failure.job().record())).
                set(CRAWLER_ID, failure.job().crawlerId());
    }

    protected Object lastCheckpointFor(Record record) {
        try {
            return checkpointHandler.lastCheckpointFor(crawlerIdFor(record));
        } catch (Exception e) {
            throw LazyException.lazyException(e);
        }
    }

    protected String moreUri(Record record) {
        return Crawler.methods.more(crawlerIdFor(record));
    }

    private Record toRecord(String json) {
        Model model = parse(json);
        List<Model> records = model.getValues("record", Model.class);
        return sequence(records).fold(record(), toRecord());
    }

    private Function2<Record, Model, Record> toRecord() {
        return new Function2<Record, Model, Record>() {
            @Override
            public Record call(Record record, Model model) throws Exception {
                String name = model.get("name", String.class);
                Class<?> type = Class.forName(model.get("type", String.class));
                Keyword<Object> keyword = keyword(name, type);
                final Object value = model.get("value");
                return record.set(keyword, value == null ? null : stringMappings.get(type).toValue(value.toString()));
            }
        };
    }

    protected String toJson(Record record) {
        return record.fields().fold(model(), recordToModel()).toString();
    }

    private Function2<Model, Pair<Keyword<?>, Object>, Model> recordToModel() {
        return new Function2<Model, Pair<Keyword<?>, Object>, Model>() {
            @Override
            public Model call(Model model, Pair<Keyword<?>, Object> field) throws Exception {
                return model.add("record", model().
                        add("name", field.first().name()).
                        add("type", field.first().forClass().getName()).
                        add("value", field.second()));
            }
        };
    }

    private Model crawlerIdFor(Record record) {
        UUID crawlerId = record.get(CRAWLER_ID);
        return crawlerRepository.crawlerFor(crawlerId);
    }

    protected UUID crawlerId(Record record) {
        return record.get(CRAWLER_ID);
    }

    protected Record crawledRecord(Record record) {
        return toRecord(record.get(RECORD));
    }
}