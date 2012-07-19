package com.googlecode.barongreenback.crawler;

import com.googlecode.barongreenback.persistence.BaronGreenbackRecords;
import com.googlecode.lazyrecords.Definition;
import com.googlecode.lazyrecords.Keyword;
import com.googlecode.lazyrecords.Record;
import com.googlecode.lazyrecords.Records;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Callables;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.utterlyidle.Application;
import com.googlecode.yadic.Container;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import static com.googlecode.barongreenback.shared.RecordDefinition.uniqueFields;
import static com.googlecode.lazyrecords.Using.using;
import static com.googlecode.totallylazy.Predicates.in;

public class DataWriter {
    private final Records records;
    private final PrintStream log;

    public DataWriter(BaronGreenbackRecords records, PrintStream log) {
        this(records.value(), log);
    }

    public DataWriter(Records records, PrintStream log) {
        this.records = records;
        this.log = log;
    }

    public Number writeUnique(final Definition destination, final Sequence<Record> newRecords) {
        if (newRecords.isEmpty()) return 0;

        try {
            Sequence<Keyword<?>> unique = uniqueFields(destination);
            if (newRecords.head().fields().map(Callables.<Keyword<?>>first()).exists(in(unique))) {
                return records.put(destination, Record.methods.update(using(unique), newRecords));
            }
        } catch (Exception e) {
            e.printStackTrace(log);
            throw new RuntimeException(e);
        }

        return 0;
    }

    public static Function1<Sequence<Record>, Number> write(final Application application, final StagedJob job, final Container crawlerScope) {
        return new Function1<Sequence<Record>, Number>() {
            @Override
            public Number call(final Sequence<Record> newData) throws Exception {
                return application.usingRequestScope(new Callable1<Container, Number>() {
                    @Override
                    public Number call(Container container) throws Exception {
                        try {
                            Number updated = new DataWriter(container.get(BaronGreenbackRecords.class).value(), container.get(PrintStream.class)).writeUnique(job.destination(), newData);
                            crawlerScope.get(AtomicInteger.class).addAndGet(updated.intValue());
                            return updated;
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }
}
