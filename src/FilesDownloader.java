import sys_parts.BTRL.BTRLContainer;
import sys_parts.BTRL.exceptions.FieldNotExistException;
import sys_parts.BTRL.exceptions.ParsingException;
import sys_parts.BTRL.sys_parts.BTRLRecord;
import sys_parts.DownloadRecord;
import sys_parts.SiteResourcesParser;
import sys_parts.URLDownloader;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author доработка - Малякин Кирилл.
 */


public class FilesDownloader {

    private static final String URL_FILE = "src\\svc_files\\inFile.txt"; //Файл с записями
    private static final String DOWNLOAD_RECORDS_LIST_FILE = "src\\svc_files\\outFile.txt"; //Файл, в который сохраняются ссылки на музыку

    private static ArrayList<SiteResourcesParser> sites = new ArrayList<>(); //Сайты

    public static void main(String[] args) {
        readDownloadRecords();
        downloadFiles();
        System.out.println("Complete.");
    }

    private static void readDownloadRecords() {
        try (BufferedWriter inputRecordsFile = new BufferedWriter(new FileWriter(DOWNLOAD_RECORDS_LIST_FILE));
             BufferedReader inFileReader = new BufferedReader(new FileReader(URL_FILE))) {
            BTRLContainer sitesQuequeContainer = new BTRLContainer(inFileReader.lines().collect(Collectors.joining("\n")));
            for (BTRLRecord currentRecord : sitesQuequeContainer.getRecords()) {
                try {
                    DownloadRecord temporaryDownloadRecord = new DownloadRecord(currentRecord);
                    System.out.println("Processing \"" + temporaryDownloadRecord.site_url + "\"...");
                    sites.add(new SiteResourcesParser(temporaryDownloadRecord));
                    for (URLDownloader downloader : sites.get(sites.size() - 1).getDownloaders()) {
                        inputRecordsFile.write(downloader.getURL() + "\n");
                    }

                } catch (FieldNotExistException ignored) {
                    System.err.println("Error #14: Field not exist");
                }
            }
        } catch (IOException e) {
            System.err.println("Global IO error.");
            e.printStackTrace();
        } catch (ParsingException e) {
            System.err.println("Input file parsing error.");
            e.printStackTrace();
        }
    }

    /**
     * Метод скачивает музыку из сохранённых ссылок в заранее определённую папку.
     */
    private static void downloadFiles() {
        for (SiteResourcesParser site : sites) {
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

    private static ArrayList<URLDownloader> collectAllDownloadersFromSites(ArrayList<SiteResourcesParser> sites) {
        ArrayList<URLDownloader> downloaders = new ArrayList<>();
        for (SiteResourcesParser site : sites) {
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
