package com.sky.sns.barongreenback.shared;

import com.sky.sns.barongreenback.crawler.CrawlerImportPage;
import com.sky.sns.barongreenback.crawler.CrawlerListPage;
import com.sky.sns.barongreenback.schedules.ScheduleListPage;
import com.googlecode.lazyrecords.lucene.Persistence;
import com.googlecode.totallylazy.*;
import com.googlecode.utterlyidle.Application;
import com.googlecode.utterlyidle.BasePath;
import com.googlecode.utterlyidle.SmartHttpClient;
import com.googlecode.utterlyidle.handlers.ClientHttpHandler;
import com.googlecode.utterlyidle.handlers.HttpClient;
import com.googlecode.utterlyidle.handlers.InternalHttpHandler;
import com.googlecode.utterlyidle.handlers.RoutingClient;
import com.googlecode.utterlyidle.html.Browser;
import com.googlecode.utterlyidle.jobs.Completer;
import com.googlecode.utterlyidle.jobs.CountDownCompleter;
import com.googlecode.utterlyidle.modules.Module;
import com.googlecode.utterlyidle.modules.RequestScopedModule;
import com.googlecode.utterlyidle.schedules.CountDownScheduler;
import com.googlecode.utterlyidle.schedules.Scheduler;
import com.googlecode.waitrest.Waitrest;
import com.googlecode.yadic.Container;
import com.sky.sns.barongreenback.WebApplication;
import com.sky.sns.barongreenback.crawler.CrawlerConnectTimeout;
import com.sky.sns.barongreenback.crawler.CrawlerHttpClient;
import com.sky.sns.barongreenback.crawler.CrawlerReadTimeout;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static com.googlecode.utterlyidle.handlers.RoutingClient.allTrafficTo;
import static com.googlecode.utterlyidle.html.Browser.browser;
import static com.sky.sns.barongreenback.crawler.CrawlerTests.serverWithDataFeed;

public abstract class ApplicationTests {
    protected Application application;
    protected Browser browser;
    protected Waitrest waitrest;

    @After
    public void closeApplication() throws IOException {
        application.close();
        waitrest.close();
    }

    @Before
    public void deleteIndex() throws Exception {
        waitrest = serverWithDataFeed();
        application = new WebApplication(BasePath.basePath("/"), getProperties());
        extraModules().each(new Block<Module>() {
            @Override
            protected void execute(Module module) throws Exception {
                application.add(module);
            }
        });
        application.add(new RequestScopedModule() {
            @Override
            public Container addPerRequestObjects(Container container) throws Exception {
                container.removeOption(CrawlerHttpClient.class);
                container.addInstance(Waitrest.class, waitrest);
                return container.addActivator(CrawlerHttpClient.class, RoutingCrawlerHttpClientActivator.class);
            }
        });
        application.usingRequestScope(new Callable1<Container, Object>() {
            @Override
            public Object call(Container container) throws Exception {
                container.get(Persistence.class).deleteAll();
                return null;
            }
        });

        application.start();
        browser = browser(application);
    }

    protected Sequence<? extends Module> extraModules(){
        return Sequences.empty();
    }

    protected Properties getProperties() {
        return new Properties();
    }

    public com.sky.sns.barongreenback.crawler.CrawlerListPage importCrawler(InputStream stream) throws Exception {
        CrawlerImportPage crawlerImportPage = new CrawlerImportPage(browser);
        return crawlerImportPage.importCrawler(Strings.toString(stream), Option.<UUID>none());
    }

    public ScheduleListPage crawlSampleData(CrawlerListPage listPage, String name) throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        application.applicationScope().addInstance(CountDownLatch.class, latch).
                decorate(Scheduler.class, CountDownScheduler.class).
                decorate(Completer.class, CountDownCompleter.class);

        ScheduleListPage jobs = listPage.crawlAndCreateView(name);
        latch.await();
        return jobs;
    }

    protected Uri feed() {
        return feed(waitrest);
    }

    protected static Uri feed(Waitrest waitrest) {
        return Uri.uri(waitrest.getURL().toString()).mergePath("data");
    }

    protected HttpClient feedClient() {
        return allTrafficTo(new ClientHttpHandler(), feed().authority());
    }

    public static class RoutingCrawlerHttpClientActivator implements Callable<CrawlerHttpClient> {
        private final BaronGreenbackProperties properties;
        private final InternalHttpHandler internalHttpHandler;
        private final Waitrest waitrest;


        public RoutingCrawlerHttpClientActivator(BaronGreenbackProperties properties, InternalHttpHandler internalHttpHandler, Waitrest waitrest) {
            this.properties = properties;
            this.internalHttpHandler = internalHttpHandler;
            this.waitrest = waitrest;
        }

        @Override
        public CrawlerHttpClient call() throws Exception {
            ClientHttpHandler httpHandler = new ClientHttpHandler(new CrawlerConnectTimeout(properties).value(), new CrawlerReadTimeout(properties).value());
            final RoutingClient routingClient = allTrafficTo(httpHandler, feed(waitrest).authority());
            return new CrawlerHttpClient(new SmartHttpClient(internalHttpHandler, routingClient));
        }
    }
}