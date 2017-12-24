package sys_parts.BTRL;

import sys_parts.BTRL.exceptions.ParsingException;
import sys_parts.BTRL.sys_parts.AdditiveUtils;
import sys_parts.BTRL.sys_parts.BTRLField;
import sys_parts.BTRL.sys_parts.BTRLRecord;

import java.util.ArrayList;

/**
 * Created by Кирилл on 22.12.2017.
 */
//BLOCK TEMPLATE RECORDS LANGUAGE CONTAINER
public class BTRLContainer {
    private static final String BLOCK_EXTRACTION_REGEXP = "(?<=:)[\\s\\S]*?(?<=[^\\\\][}])";
    private static final String COMMENT_EXTRACTION_REGEXP = "(?>\\/[*][*])[\\s\\S]*?(?>\\*\\*\\/)";
    private ArrayList<BTRLRecord> records;
    private ArrayList<BTRLField> constants;
    private ArrayList<BTRLField> parentFields;

    public BTRLContainer(String text) throws ParsingException {
        text = excludeCommentsFromText(text);
        records = new ArrayList<>();
        constants = new ArrayList<>();
        fillDefaultConstantList();
        parentFields = new ArrayList<>();
        ArrayList<String> blocks = getBlocksFromText(text);
        for (String block : blocks) {
            BTRLRecord tempRecord = new BTRLRecord(block, constants, parentFields);
            if (!"init".equals(tempRecord.getRecordType())) { //Блоки init не должны оказаться в общем списке записей.
                records.add(tempRecord);
            }
        }
    }

    private static ArrayList<String> getBlocksFromText(String text) {
        return AdditiveUtils.parseText(text, BLOCK_EXTRACTION_REGEXP);
    }

    private static String excludeCommentsFromText(String text) {
        StringBuilder temp = new StringBuilder(text);
        ArrayList<String> comments = AdditiveUtils.parseText(text, COMMENT_EXTRACTION_REGEXP);
        for (String comment : comments) {
            int i = temp.indexOf(comment);
            int j = comment.length();
            temp.delete(i, i + j);
        }
        //temp.index
        return temp.toString();
    }

    public ArrayList<BTRLRecord> getRecords() {
        return records;
    }

    private void fillDefaultConstantList() {
        constants.add(new BTRLField("$EMPTY", ""));
    }

    @Override
    public String toString() {
        StringBuilder tempBuilder = new StringBuilder("Contains ");
        tempBuilder.append(constants.size());
        tempBuilder.append(" constants, ");
        tempBuilder.append(parentFields.size());
        tempBuilder.append(" base fields and ");
        tempBuilder.append(records.size());
        tempBuilder.append(" records.\n");

        for (BTRLRecord currentRecord : records) {
            tempBuilder.append(currentRecord);
            tempBuilder.append("----------\n\n");
        }

        return tempBuilder.toString();
    }
}
