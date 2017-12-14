import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author доработка - Малякин Кирилл.
 */

public class MusicDownloader {
    private static final String URL_FILE = "src\\svc_files\\inFile.txt"; //Файл со ссылками на сайты
    private static final String MUSIC_LIST_FILE = "src\\svc_files\\outFile.txt"; //Файл, в который сохраняются ссылки на музыку
    private static final String PATH_FOR_LOAD = "src\\download\\"; //Папка, в которую загружается музыка.

    private static final String MP3_MUSIC_PATTERN = "(?<=data-url=\")[^\"]*(?=\")"; //Регулярное выражение, по которому обрабатываются страницы.
    private static final int MAX_LINKS_FROM_SITE = 4; //Максимальное количество ссылок с одного сайта.

    private static ArrayList<String> musicLinks = new ArrayList<>();

    public static void main(String[] args) {
        collectAndSaveMusicLinks();
        downloadMusic();
        System.out.println("Complete.");
    }

    /**
     * Метод читает сайты из списка, собирает и сохраняет все ссылки на MP3 - файлы, находящиеся на каждой странице.
     * Количество сохранённых ссылок ограничено значением константы MAX_LINKS_FROM_SITE.
     */
    private static void collectAndSaveMusicLinks() {
        try (BufferedWriter musicListFile = new BufferedWriter(new FileWriter(MUSIC_LIST_FILE))) {
            ArrayList<String> URLFromFile = readAllLinksInFile(URL_FILE);
            for (String currentURL : URLFromFile) {
                System.out.println("Processing \"" + currentURL + "\"...");
                collectAndSaveLinksFromPage(getPageCode(currentURL), musicListFile);
            }
        } catch (IOException e) {
            System.err.println("Global IO error.");
            e.printStackTrace();
        }
    }

    /**
     * Метод скачивает музыку из сохранённых ссылок в заранее определённую папку.
     */
    private static void downloadMusic() {
        ArrayList<URLDownloader> downloaders = createDownloaders();
        delay(1000);
        long lastMsCount = System.currentTimeMillis();
        waitWhileAllSongsWillBeDownloaded(downloaders);
        System.out.println("\n" + musicLinks.size() + " files was downloaded successful.\n" +
                "It spend: " + formatSecondsToNormalTime((int)((System.currentTimeMillis() - lastMsCount) / 1000)));
    }

    /**
     * Метод ожидает, пока все загрузчики не завершат работу. Дополнительно выводит прогресс загрузок, исходя из количества
     * загруженных файлов.
     * @param downloaders ArrayList c объектами класса URLDownloader, завершения работы которых необходимо ждать.
     */
    private static void waitWhileAllSongsWillBeDownloaded(ArrayList<URLDownloader> downloaders) {
        int downloadedPercent, i = 0;
        while (downloaders.size() > 0) {
            if (i >= downloaders.size()) {
                i = 0;
            } else {
                if (downloaders.get(i).isDownloaded()) {
                    downloadedPercent = 100 - (((downloaders.size() - 1) * 100) / musicLinks.size());
                    System.out.print("\r" + downloaders.get(i).getFilename() + " - done\n" + getPercentLine(downloadedPercent) + " done");
                    downloaders.remove(i);
                    i = 0;
                } else i++;
            }
        }
    }

    /**
     * Метод читает список сохранённых ссылок и создаёт объект класса URLDownloader для каждой из них
     * Инициирует загрузку для каждого загрузчика и возвращает список всех загрузчиков.
     * @return ArrayList c объектами класса URLDownloader, являющихся загрузчиками.
     */
    private static ArrayList<URLDownloader> createDownloaders() {
        ArrayList<URLDownloader> downloaders = new ArrayList<>(musicLinks.size());
        for (int count = 0; count < musicLinks.size(); count++) {
            downloaders.add(new URLDownloader(PATH_FOR_LOAD + count + ".mp3", musicLinks.get(count)));
            downloaders.get(count).download();
            System.out.println(downloaders.get(count).getFilename() + " - starting download...");
        }
        return downloaders;
    }

    /**
     * Возвращает строку ASCII - графики, представляющую собой progressBar.
     * @param percents процентное соотношение заполненной части шкалы к пустой.
     * @return ASCII - строка, представляющая собой progressBar.
     */
    private static String getPercentLine(int percents) {
        StringBuilder temp = new StringBuilder("[");
        percents /= 2;
        for (int i = 1; i <= 50; i++) {
            if (i <= percents) {
                temp.append('=');
            } else {
                temp.append(' ');
            }
        }
        temp.append("] | ");
        temp.append(percents * 2);
        temp.append('%');
        return temp.toString();
    }

    /**
     * Метод форматирует секунды в привычное время и возвращает отформатированную строку формата HH:MM:SS
     * @param seconds Секунды
     * @return Отформатированная строка формата HH:MM:SS
     */
    private static String formatSecondsToNormalTime(int seconds) {
        int s = seconds % 60;
        int m = (seconds / 60) % 60;
        int h = (seconds / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h, m, s);
    }

    /**
     * Метод загружает веб - страницу, находящуюся по ссылке @code{inUrl} и возвращает её код.
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

    /**
     * Метод обрабатывает код страницы @code{pageText} в соответствии с регулярным выражением, хранящимся
     * в константе MP3_MUSIC_PATTERN, сохраняет результаты в файл, представленный объектом BufferedReader - linksContainer
     * и в ArrayList musicLinks.
     * @param pageText Исходный код страницы, которую необходимо обработать.
     * @param linksContainer файл, представленный объектом BufferedReader, в который необходимо сохранить найденные ссылки.
     * @throws IOException В случае, если произошла ошибка ввода/вывода в файл linksContainer.
     */
    private static void collectAndSaveLinksFromPage(String pageText, BufferedWriter linksContainer) throws IOException {
        Matcher musicMatcher = Pattern.compile(MP3_MUSIC_PATTERN).matcher(pageText);
        for (int i = 0; musicMatcher.find() && i < MAX_LINKS_FROM_SITE; i++) {
            musicLinks.add(musicMatcher.group());
            linksContainer.write(musicMatcher.group() + "\r\n");
            System.out.println("[" + musicLinks.size() + "] >> " + musicLinks.get(musicLinks.size() - 1) + " - founded."); //Надо бы тут экономить память.
        }
    }

    /**
     * Метод читает файл @code{filename} и возвращает все строки данного файла, за исключением закомментированных (// в начале)
     * @param filename файл, из которого необходимо произвести чтение.
     * @return ArrayList cо строками.
     */
    private static ArrayList<String> readAllLinksInFile(String filename) throws IOException {
        ArrayList<String> list = new ArrayList<>();
        try (BufferedReader urlFile = new BufferedReader(new FileReader(URL_FILE))) {
            String readedUrl;
            while ((readedUrl = urlFile.readLine()) != null) {
                if (readedUrl.startsWith("//")) continue;
                list.add(readedUrl);
            }
        }
        return list;
    }

    /**
     * Задерживает выполнение текущего потока на определённое количество миллисекунд.
     * @param ms Количество миллисекунд, на которое необходимо задержать выполнение потока.
     */
    private static void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

}
