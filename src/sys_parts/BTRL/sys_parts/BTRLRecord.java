package sys_parts.BTRL.sys_parts;

import sys_parts.BTRL.exceptions.FieldNotExistException;
import sys_parts.BTRL.exceptions.FieldParsingException;
import sys_parts.BTRL.exceptions.ParsingException;
import sys_parts.BTRL.exceptions.RecordTypeParsingException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Кирилл on 22.12.2017.
 */
public class BTRLRecord {
    private static final String EXTRACT_RECORDS_FROM_BLOCK_REGEXP = "[^\\s;].*?(?=;)";
    private static final String GET_BLOCK_TYPE_REGEXP = "[^\\s;}].*?(?=.\\{)";
    private static final String CONST_MATCH_REGEXP = "([$][^\\s;}].*?)(?:[:][\\s]*)(.*)(?=;)";
    private static final String GET_BLOCK_CONTENT_REGEXP = "(?<=\\{)[\\s\\S]*?(?=\\})";

    private ArrayList<BTRLField> fields;
    private String recordType;

    public BTRLRecord(String blockText, ArrayList<BTRLField> constants, ArrayList<BTRLField> parentFields) throws ParsingException {
        recordType = parseRecordType(blockText);
        fields = parseFields(collectAllSTR(AdditiveUtils.parseText(blockText, GET_BLOCK_CONTENT_REGEXP)), constants, parentFields);
        if (recordType.equals("init")) {
            readAndAddConstantsIntoConstList(blockText, constants, fields);
            parentFields.addAll(fields);
        }
    }

    private static ArrayList<BTRLField> parseFields(String block, ArrayList<BTRLField> constants, ArrayList<BTRLField> parentFields) throws FieldParsingException {
        ArrayList<String> readedFields = AdditiveUtils.parseText(block, EXTRACT_RECORDS_FROM_BLOCK_REGEXP);
        ArrayList<BTRLField> result = new ArrayList<>(readedFields.size());
        for (BTRLField parentField : parentFields) {
            result.add(new BTRLField(parentField.getName(), parentField.getValue()));
        }
        for (int i = 0; i < readedFields.size(); i++) {
            BTRLField readedField = new BTRLField(readedFields.get(i));
            if ("".equals(readedField.getName())) { //Если это инит и нам попалась константа.
                continue;
            }
            processValueForConstReplace(readedField, constants);
            if (i >= result.size()) {
                result.add(readedField);
            } else {
                for (BTRLField currentField : result) {
                    if (currentField.getName().equals(readedField.getName())) {
                        currentField.setValue(readedField.getValue());
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static void processValueForConstReplace(BTRLField targetField, ArrayList<BTRLField> constantContainer) {
        for (BTRLField currentConstant : constantContainer) {
            if (targetField.getValue().equals(currentConstant.getName())) {
                targetField.setValue(currentConstant.getValue());
                break;
            }
        }
    }

    private static void readAndAddConstantsIntoConstList(String block, ArrayList<BTRLField> constants, ArrayList<BTRLField> fields) {
        Matcher constMatcher = Pattern.compile(CONST_MATCH_REGEXP).matcher(block);
        while (constMatcher.find()) {
            if (constMatcher.groupCount() > 1) {
                constants.add(new BTRLField(constMatcher.group(1), constMatcher.group(2)));
            }
        }
        for (int i = 0; i < fields.size(); ) {
            if (fields.get(i).getName().startsWith("$")) {
                fields.remove(i);
                i = 0;
            } else {
                i++;
            }
        }
    }

    private static String parseRecordType(String block) throws RecordTypeParsingException {
        ArrayList<String> gettedResult = AdditiveUtils.parseText(block, GET_BLOCK_TYPE_REGEXP);
        if (gettedResult.size() == 0) {
            throw new RecordTypeParsingException("");
        }
        return gettedResult.get(0);
    }

    private static String collectAllSTR(ArrayList<String> arlst) {
        StringBuilder builder = new StringBuilder();
        for (String al : arlst) {
            builder.append(al);
        }
        return builder.toString();
    }

    public ArrayList<BTRLField> getAllFields() {
        return fields;
    }

    /**
     * Возвращает поле в записи по его имени. Если такого поля не существует, то выбрасывает исключение @code{FieldNotExistException}
     *
     * @param fieldName
     * @return
     * @throws FieldNotExistException
     */
    public BTRLField getFieldByName(String fieldName) throws FieldNotExistException {
        for (BTRLField currentField : fields) {
            if (currentField.getName().equals(fieldName)) {
                return currentField;
            }
        }
        throw new FieldNotExistException("Field with name \"" + fieldName + "\" does not exist.");
    }

    public String getRecordType() {
        return recordType;
    }

    @Override
    public String toString() {
        StringBuilder tempBuilder = new StringBuilder("Record type = ");
        tempBuilder.append(recordType);
        tempBuilder.append("; contains ");
        tempBuilder.append(fields.size());
        tempBuilder.append(" fields:\n");
        for (BTRLField currentField : fields) {
            tempBuilder.append(currentField);
            tempBuilder.append('\n');
        }
        return tempBuilder.toString();
    }
}
