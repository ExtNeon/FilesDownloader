package sys_parts.BTRL.sys_parts;

import sys_parts.BTRL.exceptions.FieldParsingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс представляет собой поле блока. Поле имеет имя и значение.
 * Может принимать в качестве параметров как явное определение имени и значения, так и строку, соотвествующую шаблону оформления.
 *
 * @author Малякин Кирилл. Гр. 15-20.
 */

public class BTRLField {
    private static final String FIELD_DIVIDER_REGEXP = "([^\\s;}].*?(?=:))(?:[:\\s].+?)(.*)";
    private String name = "";
    private String value = "";

    public BTRLField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Создаёт объект, принимая в качестве параметра для инициализации строку, оформленную согласно шаблону имя_поля: значение;
     * @param field Строка, оформленная согласно шаблону имя_поля: значение;
     * @throws FieldParsingException В случае, если принятая строка не соотвествует шаблону оформления.
     */
    BTRLField(String field) throws FieldParsingException {
        Matcher recordParser = Pattern.compile(FIELD_DIVIDER_REGEXP).matcher(field);
        boolean b = recordParser.find();
        if (b && recordParser.groupCount() > 1) {
            name = recordParser.group(1);
            value = recordParser.group(2);
        } else {
            throw new FieldParsingException("Field \"" + field + "\" parsing error: unable to split the expression.");
        }
    }

    String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "name = \'" + name + "\'; value = \'" + value + "\';";

    }
}
