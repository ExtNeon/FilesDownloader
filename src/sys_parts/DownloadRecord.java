package sys_parts;

import sys_parts.BTRL.exceptions.FieldNotExistException;
import sys_parts.BTRL.sys_parts.BTRLRecord;

/**
 * Шаблон,
 */
public class DownloadRecord {
    public String site_url = "";
    public String matcher_pattern = "";
    public String file_extension = "";
    public String absoluteURLlinkToResources = "";
    public String load_path = "";
    public int files_load_limit = 0;

    public DownloadRecord(BTRLRecord inputRecord) throws FieldNotExistException {
        this.site_url = inputRecord.getFieldByName("site-url").getValue();
        this.file_extension = inputRecord.getFieldByName("extension").getValue();
        this.matcher_pattern = inputRecord.getFieldByName("regexp").getValue();
        this.absoluteURLlinkToResources = inputRecord.getFieldByName("parent-link").getValue();
        this.files_load_limit = Integer.valueOf(inputRecord.getFieldByName("files-load-limit").getValue());
        this.load_path = inputRecord.getFieldByName("root-load-path").getValue();
    }
}
