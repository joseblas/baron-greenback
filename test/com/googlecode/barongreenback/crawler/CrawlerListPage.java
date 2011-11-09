package com.googlecode.barongreenback.crawler;

import com.googlecode.barongreenback.jobs.JobsListPage;
import com.googlecode.utterlyidle.HttpHandler;
import com.googlecode.utterlyidle.Request;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.html.Html;
import com.googlecode.utterlyidle.html.Link;

import static com.googlecode.totallylazy.proxy.Call.method;
import static com.googlecode.totallylazy.proxy.Call.on;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.annotations.AnnotatedBindings.relativeUriOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CrawlerListPage {
    private final HttpHandler httpHandler;
    private final Html html;

    public CrawlerListPage(HttpHandler httpHandler) throws Exception {
        this(httpHandler, httpHandler.handle(get("/" + relativeUriOf(method(on(CrawlerResource.class).list()))).build()));
    }

    public CrawlerListPage(HttpHandler httpHandler, Response response) throws Exception {
        this.httpHandler = httpHandler;
        this.html = Html.html(response);
        assertThat(html.title(), containsString("Crawlers"));
    }

    public boolean contains(String value) {
        return html.selectContent("//a[contains(@class, 'update')]/text()").equals(value);
    }

    public CrawlerPage edit(String value) throws Exception {
        Request request = linkFor(value).click();
        return new CrawlerPage(httpHandler, httpHandler.handle(request));
    }

    public Link linkFor(String value) {
        return html.link(linkTo(value));
    }

    private String linkTo(String value) {
        return "//a[contains(@class, 'update') and text() = '" + value + "']";
    }

    public JobsListPage crawl(String value) throws Exception {
        Request request = html.form("//tr[" + linkTo(value) + "]/descendant::form[contains(@class, 'crawl')]").submit("descendant::input[@type='submit' and @class='crawl']");
        Response response = httpHandler.handle(request);
        return new JobsListPage(httpHandler, response);
    }
}