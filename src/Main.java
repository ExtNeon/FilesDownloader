import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    private static final String URL_FILE = "src\\svc_files\\inFile.txt";
    private static final String MUSIC_LIST_FILE = "src\\svc_files\\outFile.txt";
    private static final String PATH_FOR_LOAD = "src\\download\\";

    private static final String MP3_MUSIC_PATTERN = "(?<=data-url=\")[^\"]*(?=\")";
    private static final int PARALLEL_LOAD_LIMITER = 4;

    private static ArrayList<String> musicLinks = new ArrayList<>();

    public static void main(String[] args) {
        getMusicLinks();
        downloadMusic();
        System.out.println("Complete.");
    }

    private static void getMusicLinks() {
        try (BufferedWriter musicListFile = new BufferedWriter(new FileWriter(MUSIC_LIST_FILE))) {
            ArrayList<String> URLFromFile = readAllLinksInFile(URL_FILE);
            for (String currentURL : URLFromFile) {
                System.out.println("Processing \"" + currentURL + "\"...");
                collectAndSaveAllLinks(getPageCode(currentURL), musicListFile);
            }
        } catch (IOException e) {
            System.err.println("Global IO error.");
            e.printStackTrace();
        }
    }

    private static void downloadMusic() {
        ArrayList<URLDownloader> downloaders = createDownloaders();
        delay(1000);
        long lastMsCount = System.currentTimeMillis();
        waitWhileAllSongsWillBeDownloaded(downloaders);
        System.out.println("\n" + musicLinks.size() + " files was downloaded successful.\n" +
                "It spend: " + formatSecondsToNormalTime((int)((System.currentTimeMillis() - lastMsCount) / 1000)));
    }

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
     * Создаёт и возвращает загрузчики. ь
     * @return
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

    private static String formatSecondsToNormalTime(int seconds) {
        int s = seconds % 60;
        int m = (seconds / 60) % 60;
        int h = (seconds / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h, m, s);
    }

    private static String getPageCode(String inUrl) {
        try (BufferedReader pageReader = new BufferedReader(new InputStreamReader(new URL(inUrl).openStream()))) {
            return pageReader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            System.err.println("\"" + inUrl + "\" - IO error. Maybe 403 forbidden.");
        }
        return "";
    }

    private static void collectAndSaveAllLinks(String pageText, BufferedWriter linksContainer) throws IOException {
        Matcher musicMatcher = Pattern.compile(MP3_MUSIC_PATTERN).matcher(pageText);
        for (int i = 0; musicMatcher.find() && i < PARALLEL_LOAD_LIMITER; i++) {
            musicLinks.add(musicMatcher.group());
            linksContainer.write(musicMatcher.group() + "\r\n");
            System.out.println("[" + musicLinks.size() + "] >> " + musicLinks.get(musicLinks.size() - 1) + " - founded."); //Надо бы тут экономить память.
        }
    }

    private static ArrayList<String> readAllLinksInFile(String filename) {
        ArrayList list = new ArrayList();
        try (BufferedReader urlFile = new BufferedReader(new FileReader(URL_FILE))) {
            String readedUrl;
            while ((readedUrl = urlFile.readLine()) != null) {
                if (readedUrl.startsWith("//")) continue;
                list.add(readedUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

}
