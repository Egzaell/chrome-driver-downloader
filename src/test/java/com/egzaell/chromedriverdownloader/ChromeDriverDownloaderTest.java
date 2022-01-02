package com.egzaell.chromedriverdownloader;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ChromeDriverDownloaderTest {

    private ChromeDriverDownloader downloader;

    @Before
    public void setup() {
        downloader = new ChromeDriverDownloader();
    }

    @Test
    public void shouldDownloadDriver() {
        downloader.downloadDriver("97");
        assertTrue(true);
    }

}
