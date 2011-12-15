package com.googlecode.barongreenback.search;

import com.googlecode.barongreenback.crawler.CrawlerTest;
import com.googlecode.barongreenback.crawler.CrawlerTests;
import com.googlecode.barongreenback.shared.ApplicationTests;
import com.googlecode.barongreenback.shared.ModelRepository;
import com.googlecode.barongreenback.views.Views;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.records.Keyword;
import com.googlecode.totallylazy.records.Record;
import com.googlecode.totallylazy.records.lucene.LuceneRecords;
import com.googlecode.utterlyidle.httpserver.RestServer;
import com.googlecode.waitrest.Waitrest;
import com.googlecode.yadic.Container;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static com.googlecode.totallylazy.Runnables.VOID;
import static com.googlecode.totallylazy.matchers.NumberMatcher.is;
import static com.googlecode.totallylazy.records.Keywords.keyword;
import static com.googlecode.totallylazy.records.Keywords.keywords;
import static org.hamcrest.MatcherAssert.assertThat;

public class SearchResourceTest extends ApplicationTests {
     @Test
    public void handlesInvalidQueriesInANiceWay() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "^&%$^%");
        assertThat(searchPage.queryMessage(), Matchers.is("Invalid Query"));
    }

     @Test
    public void supportsDelete() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "", true);
        assertThat(searchPage.numberOfResults(), is(2));
        searchPage = searchPage.delete();
        assertThat(searchPage.numberOfResults(), is(0));
    }

    @Test
    public void supportsQueryAll() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "");
        assertThat(searchPage.numberOfResults(), is(2));
    }

    @Test
    public void supportsQueryForAParticularEntry() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "users", "id:\"urn:uuid:c356d2c5-f975-4c4d-8e2a-a698158c6ef1\"");
        assertThat(searchPage.numberOfResults(), is(1));
    }

    @Test
    public void whenAnUnknownViewIsSpecifiedThenNoResultsShouldBeShown() throws Exception {
        SearchPage searchPage = new SearchPage(browser, "UNKNOWN", "");
        assertThat(searchPage.numberOfResults(), is(0));
    }

    @Before
    public void addSomeData() throws Exception {
        Waitrest waitrest = CrawlerTests.setupServerWithDataFeed();

        final Sequence<Record> recordSequence = CrawlerTest.crawlOnePageOnly().realise();

        application.usingRequestScope(new Callable1<Container, Void>() {
            public Void call(Container container) throws Exception {
                LuceneRecords luceneRecords = container.get(LuceneRecords.class);
                Keyword<Object> users = keyword("users");
                luceneRecords.define(users, keywords(recordSequence).toArray(Keyword.class));
                luceneRecords.add(users, recordSequence);
                ModelRepository views = container.get(ModelRepository.class);
                views.set(UUID.randomUUID(), Views.convertToViewModel(users, keywords(recordSequence)));
                return VOID;
            }
        });

        waitrest.close();
    }
}
