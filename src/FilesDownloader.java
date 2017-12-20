import sys_parts.URLDownloader;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * @author доработка - Малякин Кирилл.
 */


public class FilesDownloader {
    private static final String URL_FILE = "src\\svc_files\\inFile.txt"; //Файл со ссылками на сайты
    private static final String DOWNLOAD_RECORDS_LIST_FILE = "src\\svc_files\\outFile.txt"; //Файл, в который сохраняются ссылки на музыку

    private static final String INFILE_LINE_DELIMITER = ";";

    private static final String PATH_FOR_LOAD = "src\\download\\"; //Папка, в которую загружается музыка.

    private static final int DOWNLOAD_LIMIT_FOR_SITE = 2; //Максимальное количество ссылок с одного сайта.

    private static ArrayList<DownloadSite> sites = new ArrayList<>();

    public static void main(String[] args) {
        readDownloadRecords();
        downloadFiles();
        System.out.println("Complete.");
    }

    private static void readDownloadRecords() {
        try (BufferedWriter inputRecordsFile = new BufferedWriter(new FileWriter(DOWNLOAD_RECORDS_LIST_FILE))) {
            ArrayList<String> linesFromFile = readAllLinesInFile(URL_FILE);
            for (String currentLine : linesFromFile) {
                DownloadRecord downloadRecord = getParsedLineInfo(currentLine);
                System.out.println("Processing \"" + downloadRecord.site_url + "\"...");
                sites.add(new DownloadSite(downloadRecord.site_url, downloadRecord.matcher_pattern, downloadRecord.file_extension, PATH_FOR_LOAD, DOWNLOAD_LIMIT_FOR_SITE, downloadRecord.absoluteURLlinkToResources));
                for (URLDownloader downloader : sites.get(sites.size() - 1).getDownloaders()) {
                    inputRecordsFile.write(downloader.getURL() + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Global IO error.");
            e.printStackTrace();
        }
    }

    /**
     * Метод скачивает музыку из сохранённых ссылок в заранее определённую папку.
     */
    private static void downloadFiles() {
        for (DownloadSite site : sites) {
            for (URLDownloader downloader : site.getDownloaders()) {
                downloader.download();
                System.out.println(downloader.getFilename() + " - starting download...");
            }
        }
        ArrayList<URLDownloader> downloaders = collectAllDownloadersFromSites(sites);
        int downloadersCount = downloaders.size();
        delay(1000);
        long lastMsCount = System.currentTimeMillis();
        waitWhileAllFilesWillBeDownloaded(downloaders);
        System.out.println("\n" + downloadersCount + " files was downloaded successful.\n" +
                "It spend: " + formatSecondsToNormalTime((int) ((System.currentTimeMillis() - lastMsCount) / 1000)));
    }

    /**
     * Метод ожидает, пока все загрузчики не завершат работу. Дополнительно выводит прогресс загрузок, исходя из количества
     * загруженных файлов.
     *
     * @param downloaders ArrayList c объектами класса sys_parts.URLDownloader, завершения работы которых необходимо ждать.
     */
    private static void waitWhileAllFilesWillBeDownloaded(ArrayList<URLDownloader> downloaders) {
        int downloadedPercent, i = 0;
        int downloadersCount = downloaders.size();
        System.out.print(getPercentLine(0, 60) + " done");
        while (downloaders.size() > 0) {
            if (i >= downloaders.size()) {
                i = 0;
            } else {
                if (downloaders.get(i).isDownloaded()) {
                    downloadedPercent = 100 - (((downloaders.size() - 1) * 100) / downloadersCount);
                    System.out.print("\r" + downloaders.get(i).getFilename() + " [" + formatFileSize(downloaders.get(i).getFileSize()) + "] - done (~" + (formatFileSize((long) (downloaders.get(i).getFileSize() / ((downloaders.get(i).getTimeOfDownload() / 1000.))))) + "/s)" +
                            "\n" + getPercentLine(downloadedPercent, 60) + " done");
                    downloaders.remove(i);
                    i = 0;
                } else i++;
            }
        }
    }


    /**
     * Возвращает строку ASCII - графики, представляющую собой progressBar.
     *
     * @param percents процентное соотношение заполненной части шкалы к пустой.
     * @return ASCII - строка, представляющая собой progressBar.
     */
    private static String getPercentLine(int percents, int length) {
        StringBuilder temp = new StringBuilder("[");
        percents /= 100. / length;
        for (int i = 1; i <= length; i++) {
            if (i <= percents) {
                temp.append('=');
            } else {
                temp.append(' ');
            }
        }
        temp.append("] | ");
        temp.append(new DecimalFormat("#0.00").format(percents * 100. / length));
        temp.append('%');
        return temp.toString();
    }

    /**
     * Метод форматирует секунды в привычное время и возвращает отформатированную строку формата HH:MM:SS
     *
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
     * Метод читает файл @code{filename} и возвращает все строки данного файла, за исключением закомментированных (// в начале)
     *
     * @param filename файл, из которого необходимо произвести чтение.
     * @return ArrayList cо строками.
     */
    private static ArrayList<String> readAllLinesInFile(String filename) throws IOException {
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
     *
     * @param ms Количество миллисекунд, на которое необходимо задержать выполнение потока.
     */
    private static void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private static DownloadRecord getParsedLineInfo(String inputLine) {
        DownloadRecord result = new DownloadRecord();
        StringBuilder builder = new StringBuilder(inputLine);
        result.site_url = builder.substring(0, builder.indexOf(INFILE_LINE_DELIMITER));
        builder.delete(0, builder.indexOf(INFILE_LINE_DELIMITER) + 1);
        result.matcher_pattern = builder.substring(0, builder.indexOf(INFILE_LINE_DELIMITER));
        builder.delete(0, builder.indexOf(INFILE_LINE_DELIMITER) + 1);
        result.file_extension = builder.substring(0, builder.indexOf(INFILE_LINE_DELIMITER));
        builder.delete(0, builder.indexOf(INFILE_LINE_DELIMITER) + 1);
        result.absoluteURLlinkToResources = builder.toString();
        return result;
    }

    private static ArrayList<URLDownloader> collectAllDownloadersFromSites(ArrayList<DownloadSite> sites) {
        ArrayList<URLDownloader> downloaders = new ArrayList<>();
        for (DownloadSite site : sites) {
            downloaders.addAll(site.getDownloaders());
        }
        return downloaders;
    }

    private static String formatFileSize(long size) {
        if (size >= 1024 * 1024 * 1024) {
            return new DecimalFormat("#0.00").format(size / (1024 * 1024 * 1024)) + " GiB";
        } else if (size >= 1024 * 1024) {
            return new DecimalFormat("#0.00").format(size / (1024 * 1024)) + " MiB";
        } else if (size >= 1024) {
            return new DecimalFormat("#0.00").format(size / (1024)) + " KiB";
        } else {
            return size + " b";
        }
    }

}
