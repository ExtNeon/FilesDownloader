import sys_parts.URLDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 */
public class DownloadSite {
    private static final String SITE_FORMAT_PATTERN = "";
    private static final String UNMATCHED_SITE_NAME = "unmatched_site";
    private ArrayList<URLDownloader> downloaders;

    public DownloadSite(String siteURL, String patternForFiles, String extension, String downloadPath, int downloadersLimit, String forcedAbsoluteURLlinkToResources) throws IOException {
        String downDir = formatFilePath(downloadPath, siteURL, extension); //Путь до всех файлов
        File downloadDir = new File(downDir);
        if (!downloadDir.exists()) {
            downloadDir.mkdir();
        }
        downloaders = createDownloaders(getMatchedLinksFromPage(siteURL, patternForFiles, forcedAbsoluteURLlinkToResources), downDir, extension, downloadersLimit);
    }

    /**
     * Метод загружает веб - страницу, находящуюся по ссылке @code{inUrl} и возвращает её код.
     *
     * @param inUrl Целевой адрес http - сервера с которого необходимо загрузить код.
     * @return Код страницы, находящейся по ссылке @code{inUrl}
     */
    private static String getPageCode(String inUrl) {
        try (BufferedReader pageReader = new BufferedReader(new InputStreamReader(new URL(inUrl).openStream()))) {
            return pageReader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            System.err.println("\"" + inUrl + "\" - IO error. Maybe 403 forbidden.");
        }
        return "";
    }

    private static ArrayList<String> getMatchedLinksFromPage(String siteURL, String regExpPattern, String forcedAbsoluteURLlinkToResources) throws IOException {
        String pageText = getPageCode(siteURL);
        ArrayList<String> resultLinks = new ArrayList<>();
        Matcher matcher = Pattern.compile(regExpPattern).matcher(pageText);
        while (matcher.find()) {
            resultLinks.add(forcedAbsoluteURLlinkToResources + matcher.group());
            // System.out.println("[" + resultLinks.size() + "] >> " + resultLinks.get(resultLinks.size() - 1) + " - founded."); //Надо бы тут экономить память.
        }
        System.out.println("\n\"" + siteURL + "\" - " + resultLinks.size() + " links founded on this page.\n-------------------------");
        return resultLinks;
    }

    /**
     * Метод читает список сохранённых ссылок и создаёт объект класса sys_parts.URLDownloader для каждой из них
     * Инициирует загрузку для каждого загрузчика и возвращает список всех загрузчиков.
     *
     * @return ArrayList c объектами класса sys_parts.URLDownloader, являющихся загрузчиками.
     */
    private static ArrayList<URLDownloader> createDownloaders(ArrayList<String> fileLinks, String pathToDownload, String extension, int maxDownloaders) {
        maxDownloaders = fileLinks.size() > maxDownloaders ? maxDownloaders : fileLinks.size();
        ArrayList<URLDownloader> downloaders = new ArrayList<>();
        for (int count = 0; count < maxDownloaders; count++) {
            downloaders.add(new URLDownloader(pathToDownload + count + "." + extension, fileLinks.get(count)));
        }
        return downloaders;
    }

    private String formatFilePath(String downloadPath, String siteURL, String extension) {
        Matcher matcher = Pattern.compile(SITE_FORMAT_PATTERN).matcher(siteURL);
        String formattedURL = matcher.find() ? matcher.group() : UNMATCHED_SITE_NAME;
        return downloadPath + "\\" + extension + "\\" + formattedURL + "\\";
    }

    public ArrayList<URLDownloader> getDownloaders() {
        return downloaders;
    }
}
