package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;

@Getter
@ToString
@Setter
public class Record {

    RecordHeader recordHeader;
    RecordBody recordBody;

    public static Record parse(FileInputStream databaseFile) throws IOException {
        Record record = new Record();
        record.setRecordHeader(RecordHeader.parse(databaseFile));
        record.setRecordBody(RecordBody.parse(databaseFile, record.getRecordHeader().getSerialTypeCodes()));
        return record;
    }
}
