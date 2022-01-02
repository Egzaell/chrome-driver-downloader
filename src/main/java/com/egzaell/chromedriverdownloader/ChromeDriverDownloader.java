package com.egzaell.chromedriverdownloader;

import com.egzaell.chromedriverdownloader.exception.VersionNotFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ChromeDriverDownloader {

    private static final String DOWNLOAD_PAGE_URL = "https://chromedriver.chromium.org/downloads";
    private static final String A_TAG = "<a ";
    private static final String LINK_PARTIAL_CONTENT = "ChromeDriver ";
    private static final String HREF_PROPERTY = "href=";
    private static final String PARENTHESIS = "\"";
    private static final String DOWNLOAD_URL = "https://chromedriver.storage.googleapis.com/%s/%s";
    private static final String FILENAME = "chromedriver_";
    private static final String ZIP_EXTENSION = ".zip";

    public void downloadDriver(String version) {
        try {
            List<String> lines = getHtml(DOWNLOAD_PAGE_URL);
            String downloadAnchor = findRightAnchor(version, lines);
            String driverVersion = getFullVersion(downloadAnchor, version);
            String filename = getFilename();
            String downloadLink = getDownloadLink(driverVersion, filename);
            downloadDriver(downloadLink, filename);
            unzipDriver(filename);
            clean(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<String> getHtml(String address) throws IOException {
        List<String> lines = new ArrayList<>();
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                lines.add(line);
            }
        }

        return lines;
    }

    private String findRightAnchor(String version, List<String> lines) {
        List<String> anchorList = new ArrayList<>();
        lines.stream()
            .filter(l -> l.contains(A_TAG))
            .map(l -> Arrays.asList(l.split(A_TAG)))
            .collect(Collectors.toList())
            .forEach(anchorList::addAll);

        Optional<String> anchorOptional = anchorList.stream()
            .filter(a -> a.contains(LINK_PARTIAL_CONTENT + version))
            .findFirst();

        if (anchorOptional.isPresent()) {
            return anchorOptional.get();
        } else {
            throw new VersionNotFoundException("Couldn't find a download link for specified version: " + version);
        }
    }

    private String getFullVersion(String anchor, String version) {
        int hrefIndex = anchor.indexOf(HREF_PROPERTY);
        String link = anchor.substring(hrefIndex + HREF_PROPERTY.length() + 1);
        int parenthesisIndex = link.indexOf(PARENTHESIS);
        int versionIndex = link.indexOf(version);
        return link.substring(versionIndex, parenthesisIndex - 1);
    }

    private String getDownloadLink(String version, String filename) {
        return String.format(DOWNLOAD_URL, version, filename);
    }

    private String getFilename() {
        return FILENAME + getSystemName() + ZIP_EXTENSION;
    }

    private String getSystemName() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            return "win32";
        } else {
            return "linux64";
        }
    }

    private void downloadDriver(String downloadLink, String filename)
        throws IOException {
        URL url = new URL(downloadLink);
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

    private void unzipDriver(String filename) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filename))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            File driverFile = new File(zipEntry.getName());
            FileOutputStream fileOutputStream = new FileOutputStream(driverFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zipInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, len);
            }
            fileOutputStream.close();
            zipInputStream.closeEntry();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clean(String filename) {
        File zip = new File(filename);
        zip.delete();
    }

}
