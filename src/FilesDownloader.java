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
 * Программа, загружающая ресурсы с сайтов.
 * Все данные берутся из конфигурационного файла собственной разработки.
 * @author Малякин Кирилл. Гр 15-20.
 */


public class FilesDownloader {

    private static final String URL_FILE = "src\\downloadConfig.txt"; //Файл с записями
    private static final String DOWNLOAD_RECORDS_LIST_FILE = "src\\svc_files\\outFile.txt"; //Файл, в который сохраняются ссылки на музыку
    private static final boolean DELETE_CORRUPTED_FILES = true; //Если загрузка не завершилась успешно, повреждённые файлы удаляются.


    private static ArrayList<SiteResourcesParser> sites = new ArrayList<>(); //Сайты


    public static void main(String[] args) {
        readDownloadRecords(URL_FILE, DOWNLOAD_RECORDS_LIST_FILE, sites);
        downloadFiles();
        System.out.println("Complete.");
    }

    /**
     * Метод читает конфигурационный файл, создаёт сайты, собирает ссылки на ресурсы, сохраняя их в файл @code{linksListFile}
     * и в массив объектов SiteResourceParser @code{sitelist}
     *
     * @param configFilename Путь и название файла конфигурации
     * @param linksListFile  Путь и название файла, в который будут сохранены ссылки на ресурсы
     * @param sitelist       Массив объектов SiteResourceParser, в который будут собраны результаты
     */
    private static void readDownloadRecords(String configFilename, String linksListFile, ArrayList<SiteResourcesParser> sitelist) {
        try (BufferedWriter inputRecordsFile = new BufferedWriter(new FileWriter(linksListFile));
             BufferedReader inFileReader = new BufferedReader(new FileReader(configFilename))) {
            BTRLContainer sitesQuequeContainer = new BTRLContainer(inFileReader.lines().collect(Collectors.joining("\n")));
            for (BTRLRecord currentRecord : sitesQuequeContainer.getRecords()) {
                try {
                    DownloadRecord temporaryDownloadRecord = new DownloadRecord(currentRecord);
                    System.out.println("Processing \"" + temporaryDownloadRecord.site_url + "\"...");
                    sitelist.add(new SiteResourcesParser(temporaryDownloadRecord));
                    for (URLDownloader downloader : sitelist.get(sitelist.size() - 1).getDownloaders()) {
                        inputRecordsFile.write(downloader.getURL() + "\n");
                    }

                } catch (FieldNotExistException ignored) {
                    System.err.println("Error #14: Field doesn\'t exist");
                }
            }
        } catch (IOException e) {
            System.err.println("Global IO error.");
            e.printStackTrace();
        } catch (ParsingException e) {
            System.err.println("Config file parsing error.");
            e.printStackTrace();
        }
    }

    /**
     * Метод запускает загрузку файлов по ранее собранным ссылкам, ждёт окончания загрузки и в процессе отображает её прогресс.
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
        int downloadedCorrectly = getCountOfSuccessfulDownloadedFiles(downloaders);
        System.out.println("\n" + downloadedCorrectly + (downloadedCorrectly == 1 ? " file" : " files") + " was downloaded successful.\n" +
                (downloadedCorrectly == downloadersCount ? "" : (downloadersCount - downloadedCorrectly) + " files wasn\'t downloaded correctly\n") +
                "It spend: " + formatSecondsToNormalTime((int) ((System.currentTimeMillis() - lastMsCount) / 1000)));
    }

    /**
     * Метод ожидает, пока все загрузчики не завершат работу. Дополнительно выводит прогресс загрузок, исходя из количества
     * загруженных файлов. Возвращает число успешно загруженных файлов.
     *
     * @param downloaders ArrayList c объектами класса sys_parts.URLDownloader, завершения работы которых необходимо ждать.
     * @return Количество успешно загруженных файлов.
     */
    private static int getCountOfSuccessfulDownloadedFiles(ArrayList<URLDownloader> downloaders) {
        int downloadedPercent, i = 0;
        int downloadersCount = downloaders.size();
        int downloadedCorrectly = downloaders.size();
        System.out.print(getPercentLine(0, 60) + " done");
        while (downloaders.size() > 0) {
            if (i >= downloaders.size()) {
                i = 0;
            } else {
                if (downloaders.get(i).isDownloaded()) {
                    downloadedPercent = 100 - (((downloaders.size() - 1) * 100) / downloadersCount);
                    if (!downloaders.get(i).isDownloadedSuccessful()) {
                        downloadedCorrectly--;
                        if (DELETE_CORRUPTED_FILES) {
                            new File(downloaders.get(i).getFilename()).delete();
                        }
                    }
                    System.out.print("\r" + downloaders.get(i).getFilename() + " [" + formatFileSize(downloaders.get(i).getFileSize()) + "] - " + (downloaders.get(i).isDownloadedSuccessful() ? "done (~" + (formatFileSize((long) (downloaders.get(i).getFileSize() / ((downloaders.get(i).getTimeOfDownload() / 1000.))))) + "/s)" : "failed. This file will be deleted") +
                            "\n" + getPercentLine(downloadedPercent, 60) + " done");
                    downloaders.remove(i);
                    i = 0;
                } else i++;
            }
        }
        return downloadedCorrectly;
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

    /**
     * Собирает все загрузчики с объектов SiteResourceParser в один массив.
     * @param sites Объекты типа SiteResourcesParser, из которых будут собраны загрузчики
     * @return Массив объектов URLDownloader
     */
    private static ArrayList<URLDownloader> collectAllDownloadersFromSites(ArrayList<SiteResourcesParser> sites) {
        ArrayList<URLDownloader> downloaders = new ArrayList<>();
        for (SiteResourcesParser site : sites) {
            downloaders.addAll(site.getDownloaders());
        }
        return downloaders;
    }

    /**
     * Возвращает отформатированный размер информации, исходя из количества байт.
     * @param size Исходный размер массива информации (файла, ...)
     * @return Отформатированная строка
     */
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
