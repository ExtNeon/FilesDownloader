package sys_parts.BTRL.sys_parts;

import sys_parts.BTRL.exceptions.FieldParsingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BTRLField {
    private static final String FIELD_DIVIDER_REGEXP = "([^\\s;}].*?(?=:))(?:[:\\s].+?)(.*)";
    private String name = "";
    private String value = "";

    public BTRLField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    BTRLField(String field) throws FieldParsingException {
        Matcher recordParser = Pattern.compile(FIELD_DIVIDER_REGEXP).matcher(field);
        boolean b = recordParser.find();
        if (b && recordParser.groupCount() > 1) {
            name = recordParser.group(1);
            value = recordParser.group(2);
        } else {
            throw new FieldParsingException();
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
