package sys_parts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Класс представляет собой загрузчик файла, находящегося по определённому URL - адресу. Загружает файл в отдельном потоке.
 * @author Малякин Кирилл.
 */
public class URLDownloader extends Thread {

    private String filename;
    private String fileURL;
    private long timeOfDownload = -1;
    private long fileSize = -1;
    private boolean successful = true;

    /**
     * @param filename Путь и имя файла, в который будет осуществляться загрузка.
     * @param inpURL   Ссылка, с которой будет осуществляться загрузка.
     */
    public URLDownloader(String filename, String inpURL) {
        this.filename = filename;
        this.fileURL = inpURL;
    }

    public void run() {
        try {
            downloadUsingNIO(fileURL, filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запускает загрузку файла в отдельном потоке.
     */
    public void download() { //Небольшие обёртки, чтобы выглядело чуть - чуть солиднее.
        start();
    }

    /**
     * Возвращает состояние загрузки.
     * @return истина, если файл уже загружен, ложь во всех остальных случаях.
     */
    public boolean isDownloaded() { //Небольшие обёртки, чтобы выглядело чуть - чуть солиднее.
        return !this.isAlive();
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Загружает файл, используя NIO.
     * @param strUrl Ссылка, с которой будет осуществляться загрузка.
     * @param file Файл, в который будет осущствляться загрузка.
     * @throws IOException В случае проблем ввода - вывода в файл.
     */
    private void downloadUsingNIO(String strUrl, String file) throws IOException {
        try {
            long tempTimeCounter = System.currentTimeMillis();
            URL tmpurl = new URL(strUrl);
            try (ReadableByteChannel byteChannel = Channels.newChannel(tmpurl.openStream());
                 FileOutputStream stream = new FileOutputStream(file)) {
                stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
                fileSize = stream.getChannel().size();
            }
            timeOfDownload = System.currentTimeMillis() - tempTimeCounter;
        } catch (MalformedURLException e) {
            System.err.println("fileURL parsing error: \"" + strUrl + "\". Unknown protocol. Stopping download...");
            successful = false;
        } catch (Exception other) {
            // System.err.println("Error while downloading \"" + filename + "\". URL: \"" + strUrl + "\".");
            successful = false;
        }
    }

    public long getTimeOfDownload() {
        return timeOfDownload == 0 ? 1 : timeOfDownload;
    }

    public long getFileSize() {
        return fileSize == 0 ? 1 : fileSize;
    }

    public boolean isDownloadedSuccessful() {
        return successful && isDownloaded();
    }

    public String getURL() {
        return fileURL;
    }
}
