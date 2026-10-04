package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import utils.Parser;

import java.io.FileInputStream;
import java.io.IOException;

@Getter
@ToString
@Setter
public class TableIndexRecord {

    RecordHeader recordHeader;
    RecordBody recordBody;
    long rowId;

    public static TableIndexRecord parse(FileInputStream databaseFile) throws IOException {
        TableIndexRecord tableIndexRecord = new TableIndexRecord();
        tableIndexRecord.setRecordHeader(RecordHeader.parse(databaseFile));
        tableIndexRecord.setRecordBody(RecordBody.parse(databaseFile, tableIndexRecord.getRecordHeader().getSerialTypeCodes()));
        if (tableIndexRecord.getRecordBody().getBody().size() > 1) {
            tableIndexRecord.setRowId(Long.parseLong(tableIndexRecord.getRecordBody().getBody().get(1).toString()));
        }

        return tableIndexRecord;
    }
}
