package sys_parts.BTRL.sys_parts;

import sys_parts.BTRL.exceptions.BlockTypeParsingException;
import sys_parts.BTRL.exceptions.FieldNotExistException;
import sys_parts.BTRL.exceptions.FieldParsingException;
import sys_parts.BTRL.exceptions.ParsingException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Представляет собой целую запись с полями типа @code{BTRLField}.
 * Обрабатывает строку с кодом блока, находит поля и константы.
 * Позволяет получить поле по его имени.
 * @author Малякин Кирилл. Гр. 15-20.
 */
public class BTRLRecord {
    private static final String EXTRACT_RECORDS_FROM_BLOCK_REGEXP = "[^\\s;].*?(?=;)";
    private static final String GET_BLOCK_TYPE_REGEXP = "[^\\s;}].*?(?=.\\{)";
    private static final String CONST_MATCH_REGEXP = "([$][^\\s;}].*?)(?:[:][\\s]*)(.*)(?=;)";
    private static final String GET_BLOCK_CONTENT_REGEXP = "(?<=\\{)[\\s\\S]*?(?=\\})";

    private ArrayList<BTRLField> fields;
    private String blockType;

    /**
     * Создаёт объект, при этом обрабатывает код блока, извлекает из него поля, константы; заменяет вызовы констант их значением,
     * прописывает поля по умолчанию.
     *
     * @param blockText    Исходный код блока, который будет обработан.
     * @param constants    Массив констант, каждая из которых представлена объектом @code{BTRLField}.
     * @param parentFields Массив полей по умолчанию, которые будут обязательно добавлены в результат.
     * @throws ParsingException В случае, если блок не соответствует шаблону оформления кода.
     */
    public BTRLRecord(String blockText, ArrayList<BTRLField> constants, ArrayList<BTRLField> parentFields) throws ParsingException {
        blockType = getBlockType(blockText);
        fields = parseFields(collectAllSTR(AdditiveUtils.parseText(blockText, GET_BLOCK_CONTENT_REGEXP)), constants, parentFields);
        if (blockType.equals("init")) {
            readConstantsIntoConstList(blockText, constants, fields);
            parentFields.addAll(fields);
        }
    }

    /**
     * Метод обрабатывает код блока, извлекает из него поля, константы; заменяет вызовы констант их значением,
     * прописывает поля по умолчанию. Возвращает массив объектов @code{BTRLField}, представляющий собой список обработанных полей.
     * @param block Исходный код блока, который будет обработан.
     * @param constants Массив констант, каждая из которых представлена объектом @code{BTRLField}.
     * @param parentFields Массив полей по умолчанию, которые будут обязательно добавлены в результат.
     * @return Массив объектов @code{BTRLField}, представляющий собой список обработанных полей.
     * @throws FieldParsingException В случае, если определённое поле не соответствует правилам оформления.
     */
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

    /**
     * Обрабатывыет полученное в качестве параметра поле блока типа @code{BTRLField}, заменяя вызовы констант на их значения.
     * @param targetField Целевое поле типа @code{BTRLField}, в котором будут заменены вызовы констант.
     * @param constantContainer Массив с объектами @code{BTRLField}, представляющим собой список констант.
     */
    private static void processValueForConstReplace(BTRLField targetField, ArrayList<BTRLField> constantContainer) {
        for (BTRLField currentConstant : constantContainer) {
            if (targetField.getValue().equals(currentConstant.getName())) {
                targetField.setValue(currentConstant.getValue());
                break;
            }
        }
    }

    /**
     * Метод парсит код блока, находит в нём константы, добавляет их в массив @code{constants}.
     * Заодно удаляет некорректно прочитанные константы из полей @code{fields}
     * @param block Код блока, который будет обработан.
     * @param constants Массив констант типа @code{BTRLField}, в который будут добавлены прочитанные константы.
     * @param fields Поля блока, из которого будут удалены ошибочно прочитанные в качестве полей константы.
     */
    private static void readConstantsIntoConstList(String block, ArrayList<BTRLField> constants, ArrayList<BTRLField> fields) {
        Matcher constMatcher = Pattern.compile(CONST_MATCH_REGEXP).matcher(block);
        while (constMatcher.find()) {
            if (constMatcher.groupCount() > 1) {
                constants.add(new BTRLField(constMatcher.group(1), constMatcher.group(2)));
            }
        }
        for (int i = 0; i < fields.size(); ) {
            if (fields.get(i).getName().startsWith("$")) {
                fields.remove(i);
            } else {
                i++;
            }
        }
    }

    /**
     * Извлекает и возвращает из строки с кодом блока его тип.
     * Тип блока - участок между сигнатурой объявления блока ":" и началом его тела "{"
     *
     * @param block Код блока, из которого будет извлечён его тип.
     * @return Тип блока в виде строки.
     * @throws BlockTypeParsingException В случае, если блок не соответствует шаблону оформления.
     */
    private static String getBlockType(String block) throws BlockTypeParsingException {
        ArrayList<String> gettedResult = AdditiveUtils.parseText(block, GET_BLOCK_TYPE_REGEXP);
        if (gettedResult.size() == 0) {
            throw new BlockTypeParsingException("Block \"" + block + "\" type parsing error.");
        }
        return gettedResult.get(0);
    }

    /**
     * Небольшой велосипед. Берёт в качестве параметра ArrayList со строковыми объектами, возвращает строку с текстом,
     * представляющим собой коллекцию строк, разделённых символами переноса.
     * @param arlst Входящий массив со строковыми объектами.
     * @return Текст, содержащий строки из массива, разделённые символом переноса строки.
     */
    private static String collectAllSTR(ArrayList<String> arlst) {
        StringBuilder builder = new StringBuilder();
        for (String al : arlst) {
            builder.append(al);
            builder.append('\n');
        }
        return builder.toString();
    }

    /**
     * Возвращает все поля в данной записи (блоке).
     * @return Все поля этого блока в виде объектов типа @code{BTRLField}
     */
    public ArrayList<BTRLField> getAllFields() {
        return fields;
    }

    /**
     * Возвращает поле в записи по его имени. Если такого поля не существует, то выбрасывает исключение @code{FieldNotExistException}
     *
     * @param fieldName Имя поля, по которому будет произведён поиск и возврат значения.
     * @return Значение поля с именем @{fieldName}
     * @throws FieldNotExistException В случае, если поля с таким именем не существует
     */
    public BTRLField getFieldByName(String fieldName) throws FieldNotExistException {
        for (BTRLField currentField : fields) {
            if (currentField.getName().equals(fieldName)) {
                return currentField;
            }
        }
        throw new FieldNotExistException("Field with name \"" + fieldName + "\" does not exist.");
    }

    /**
     * Возвращает тип блока, которому соответствует этот объект.
     * Тип блока - участок между сигнатурой объявления блока ":" и началом его тела "{"
     *
     * @return Тип блока в строковом формате.
     */
    public String getBlockType() {
        return blockType;
    }

    @Override
    public String toString() {
        StringBuilder tempBuilder = new StringBuilder("Record type = ");
        tempBuilder.append(blockType);
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
