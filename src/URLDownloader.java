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
    private String URL;

    public void run() {
        try {
            downloadUsingNIO(URL, filename);
        } catch (IOException e) {
            try {
                throw e; //Горождение огорода для того, чтобы перебросить исключение. Обожаю.
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    /**
     * @param saveAs Путь и имя файла, в который будет осуществляться загрузка.
     * @param URL Ссылка, с которой будет осуществляться загрузка.
     */
    URLDownloader(String saveAs, String URL) {
        this.filename = saveAs;
        this.URL = URL;
    }

    /**
     * Запускает загрузку файла в отдельном потоке.
     */
    void download() { //Небольшие обёртки, чтобы выглядело чуть - чуть солиднее.
        start();
    }

    /**
     * Возвращает состояние загрузки.
     * @return истина, если файл уже загружен, ложь во всех остальных случаях.
     */
    boolean isDownloaded() { //Небольшие обёртки, чтобы выглядело чуть - чуть солиднее.
        return !this.isAlive();
    }

    String getFilename() {
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
            URL tmpurl = new URL(strUrl);
            ReadableByteChannel byteChannel = Channels.newChannel(tmpurl.openStream());
            FileOutputStream stream = new FileOutputStream(file);
            stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
            stream.close();
            byteChannel.close();
        } catch (MalformedURLException e) {
            System.err.println("URL parsing error: \"" + strUrl + "\". Unknown protocol. Stopping download...");
        }
    }

}
