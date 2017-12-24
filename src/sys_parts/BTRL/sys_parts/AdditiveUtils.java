package sys_parts.BTRL.sys_parts;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Кирилл on 22.12.2017.
 */
public class AdditiveUtils {
    public static ArrayList<String> parseText(String text, String regexp) {
        Matcher fieldsExtractor = Pattern.compile(regexp).matcher(text);
        ArrayList<String> result = new ArrayList<>();
        while (fieldsExtractor.find()) {
            result.add(fieldsExtractor.group());
        }
        return result;
    }
}
