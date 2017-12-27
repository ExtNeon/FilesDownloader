package sys_parts.BTRL.sys_parts;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Список дополнительных методов, которые используются сразу несколькими классами.
 * @author Малякин Кирилл. Гр. 15-20.
 */
public class AdditiveUtils {
    /**
     * Метод возвращает массив с результатами поиска фрагментов текста, подходящих под регулярное выражение @code{regexp}
     *
     * @param text   Исходный текст, который будет обработан.
     * @param regexp Регулярное выражение, по которому будет производиться поиск фрагментов.
     * @return Фрагменты текста, соответствующие регулярному выражению.
     */
    public static ArrayList<String> parseText(String text, String regexp) {
        Matcher fieldsExtractor = Pattern.compile(regexp).matcher(text);
        ArrayList<String> result = new ArrayList<>();
        while (fieldsExtractor.find()) {
            result.add(fieldsExtractor.group());
        }
        return result;
    }
}
