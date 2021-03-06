package com.sky.sns.barongreenback.batch;

import com.sky.sns.barongreenback.crawler.CrawlerListPage;
import com.sky.sns.barongreenback.crawler.ImportCrawlerPage;
import com.sky.sns.barongreenback.shared.ApplicationTests;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.File;

import static com.googlecode.totallylazy.Files.*;
import static com.googlecode.totallylazy.matchers.NumberMatcher.is;
import static com.sky.sns.barongreenback.crawler.CrawlerTests.contentOf;
import static com.sky.sns.barongreenback.shared.messages.Category.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;

public class BatchResourceTest extends ApplicationTests {
    @Test
    public void deleteAllBackupsData() throws Exception {
        importOneCrawler();
        Message message = deleteTheIndex().message();
        assertThat(message.message(), message.category(), Matchers.is(SUCCESS));
        assertThat(numberOfCrawlers(), is(0));

        File file = extractFile(message);
        assertThat(file.exists(), Matchers.is(true));
        delete(file);
    }

    @Test
    public void canBackupAndRestore() throws Exception {
        File backupLocation = newBackupLocation();
        importOneCrawler();

        backupDataTo(backupLocation);
        deleteIndexAndRemoveAutomaticBackup();
        assertThat(numberOfCrawlers(), is(0));

        restoreFrom(backupLocation);
        assertThat(numberOfCrawlers(), is(1));

        delete(backupLocation);
    }

    private boolean deleteIndexAndRemoveAutomaticBackup() throws Exception {
        return delete(extractFile(deleteTheIndex().message()));
    }

    private File extractFile(Message message) {
        return new File(message.message().substring(message.message().indexOf(':') + 1).trim());
    }

    private BatchOperationsPage restoreFrom(File backupLocation) throws Exception {
        return verifySuccess(new BatchOperationsPage(browser).restore(backupLocation.getAbsolutePath()));
    }

    private File newBackupLocation() {
        return new File(temporaryDirectory(), randomFilename());
    }

    private BatchOperationsPage backupDataTo(File backupLocation) throws Exception {
        assertThat(backupLocation.exists(), Matchers.is(false));
        BatchOperationsPage page = verifySuccess(new BatchOperationsPage(browser).backup(backupLocation.getAbsolutePath()));
        assertThat(backupLocation.exists(), Matchers.is(true));
        return page;
    }

    private CrawlerListPage importOneCrawler() throws Exception {
        ImportCrawlerPage page = new ImportCrawlerPage(browser);
        page.model().value(contentOf("crawler.json"));
        CrawlerListPage crawlerListPage = page.importModel();
        assertThat(numberOfCrawlers(), is(1));
        return crawlerListPage;
    }

    private BatchOperationsPage deleteTheIndex() throws Exception {
        return verifySuccess(new BatchOperationsPage(browser).deleteAll());
    }

    private int numberOfCrawlers() throws Exception {
        return new CrawlerListPage(browser).numberOfCrawlers();
    }

    public static BatchOperationsPage verifySuccess(BatchOperationsPage page) {
        Message message = page.message();
        assertThat(message.message(), message.category(), Matchers.is(SUCCESS));
        return page;
    }
}
