package sys_parts;

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
 * Представляет собой обработчик сайта. Осуществляет подключение, загружает исходный код, обрабатывает его согласно регулярному выражению,
 * создаёт загрузчики ресурсов, основываясь на результатах обработки страниц. Предоставляет доступ к списку загрузчиков.
 * @author Малякин Кирилл. Гр. 15-20.
 */
public class SiteResourcesParser {
    private static final String SITE_FORMAT_PATTERN = "(?<=:\\/\\/).*?(?=\\/)";
    private static final String UNMATCHED_SITE_NAME = "unmatched_site";
    private ArrayList<URLDownloader> downloaders;

    /**
     * Создаёт объект. Загружает исходный код сайта, обрабатывает его согласно регулярному выражению, создаёт загрузчики.
     *
     * @param siteURL                          Сайт, с которого будет загружен исходный код и который будет обработан.
     * @param patternForFiles                  Регулярное выражение, по которому будет обработан код страницы.
     * @param extension                        Расширение загружаемых файлов.
     * @param downloadPath                     Путь до папки, в которую будет произведена загрузка.
     * @param downloadersLimit                 Лимит файлов, для которых будут созданы загрузчики.
     * @param forcedAbsoluteURLlinkToResources Добавочная абсолютная ссылка к ресурсам. Добавляется в начало ссылки к каждому найденному ресурсу.
     * @throws IOException В случае, если произошла ошибка подключения к сайту, либо ошибка во время загрузки исходного кода.
     */
    public SiteResourcesParser(String siteURL, String patternForFiles, String extension, String downloadPath, int downloadersLimit, String forcedAbsoluteURLlinkToResources) throws IOException {
        String downDir = preparePathForDownload(downloadPath, siteURL, extension); //Путь до всех файлов
        downloaders = createDownloaders(getMatchedLinksFromPage(siteURL, patternForFiles, forcedAbsoluteURLlinkToResources), downDir, extension, downloadersLimit);
    }

    /**
     * Создаёт объект. В качестве параметра принимает объект @code{DownloadRecord}, который является коллекцией полей.
     * @param inputRecord Коллекция полей, необходимых для создания объекта.
     * @throws IOException В случае, если произошла ошибка подключения к сайту, либо ошибка во время загрузки исходного кода.
     */
    public SiteResourcesParser(DownloadRecord inputRecord) throws IOException {
        this(inputRecord.site_url,
                inputRecord.matcher_pattern,
                inputRecord.file_extension,
                inputRecord.load_path,
                inputRecord.files_load_limit,
                inputRecord.absoluteURLlinkToResources);
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
            System.err.println("\"" + inUrl + "\" - Connection error. Maybe 403 forbidden or 404. Also, check internet connection.");
        }
        return "";
    }

    /**
     * Метод загружает исходный код сайта, обрабатывает его согласно регулярному выражению, возвращает результат обработки в виде
     * массива строк.
     * @param siteURL Сайт, исходный код которого будет обработан.
     * @param regExpPattern Регулярное выражение, согласно которому будет обработан код сайта.
     * @param forcedAbsoluteURLlinkToResources Добавочные абсолютные ссылки, которые будут добавлены в начало каждого фрагмента результата.
     * @return Массив найденных фрагментов, предствляющий собой массив строк.
     * @throws IOException В случае, если произошла ошибка доступа к сайту, либо ошибка ввода - вывода во время загрузки кода сайта.
     */
    private static ArrayList<String> getMatchedLinksFromPage(String siteURL, String regExpPattern, String forcedAbsoluteURLlinkToResources) throws IOException {
        String pageText = getPageCode(siteURL);
        ArrayList<String> resultLinks = new ArrayList<>();
        Matcher matcher = Pattern.compile(regExpPattern).matcher(pageText);
        while (matcher.find()) {
            resultLinks.add(forcedAbsoluteURLlinkToResources + matcher.group());
            // System.out.println("[" + resultLinks.size() + "] >> " + resultLinks.get(resultLinks.size() - 1) + " - founded."); //Надо бы тут экономить память.
        }
        System.out.println("\n\"" + siteURL + "\" - " + resultLinks.size() + (resultLinks.size()== 1 ? " link" : " links") + " founded on this page.\n-------------------------");
        return resultLinks;
    }

    /**
     * Метод читает список сохранённых ссылок и создаёт объект класса sys_parts.URLDownloader для каждой из них
     * Возвращает список всех загрузчиков.
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

    /**
     * Метод извлекает имя сайта из ссылки @code{siteUrl}, создаёт папку с расширением, а в ней папку с именем сайта.
     * Возвращает полученный путь до папки с именем сайта.
     * @param downloadPath Исходный путь до папки, где будут созданы вложенные папки.
     * @param siteURL Ссылка на сайт.
     * @param extension Расширение файлов, которые будут загружены в папку.
     * @return Полученный путь до папки с именем сайта.
     */
    private String preparePathForDownload(String downloadPath, String siteURL, String extension) {
        Matcher matcher = Pattern.compile(SITE_FORMAT_PATTERN).matcher(siteURL);
        String formattedURL = matcher.find() ? matcher.group() : UNMATCHED_SITE_NAME;
        createFolder(downloadPath);
        createFolder(downloadPath + "\\" + extension);
        createFolder(downloadPath + "\\" + extension + "\\" + formattedURL);
        return downloadPath + "\\" + extension + "\\" + formattedURL + "\\";
    }

    /**
     * Создаёт папку, если её не существует.
     *
     * @param path Путь к папке, которая должна быть создана.
     */
    private void createFolder(String path) {
        File downloadDir = new File(path);
        if (!downloadDir.exists()) {
            downloadDir.mkdir();
        }
    }

    /**
     * @return Список загрузчиков.
     */
    public ArrayList<URLDownloader> getDownloaders() {
        return downloaders;
    }
}
