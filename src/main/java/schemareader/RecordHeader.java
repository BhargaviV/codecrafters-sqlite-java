package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import utils.Parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;


@ToString
@Getter
@Setter
public class RecordHeader {
    long size;
    List<SerialTypeRecord> serialTypeCodes;

    public static RecordHeader parse(FileInputStream databaseFile) throws IOException {
        RecordHeader recordHeader = new RecordHeader();
        recordHeader.setSize(Parser.readSQLiteVarint(databaseFile));
        int totalSize = Math.toIntExact(recordHeader.getSize());
        totalSize -= 1; // because header size is with size
        List<SerialTypeRecord> serialTypes = new ArrayList<>();
        while (totalSize > 0) {
            SerialTypeRecord typeRecord = SerialTypeRecord.parse(databaseFile);
            serialTypes.add(typeRecord);
            totalSize -= typeRecord.getBytesRead();
        }
        recordHeader.setSerialTypeCodes(serialTypes);
        return recordHeader;
    }
}
