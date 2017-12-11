import sun.misc.Regexp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by Кирилл on 11.12.2017.
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

    public URLDownloader(String saveAs, String URL) {
        this.filename = saveAs;
        this.URL = URL;
    }

    public void download() { //Небольшие обёртки, чтобы выглядело чуть - чуть солиднее.
        start();
    }

    public boolean isDownloaded() { //Небольшие обёртки, чтобы выглядело чуть - чуть солиднее.
        return !this.isAlive();
    }

    public String getFilename() {
        return filename;
    }

    public String getURL() {
        return URL;
    }

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
