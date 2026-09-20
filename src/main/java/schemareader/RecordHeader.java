package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import utils.Parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
        System.err.println("schemareader.RecordHeader.parse " +  recordHeader);

        System.err.println("schemareader.RecordHeader.parse totalSize" +  totalSize);
        List<SerialTypeRecord> serialTypes = new ArrayList<>();
        while (totalSize > 0) {
            SerialTypeRecord typeRecord = SerialTypeRecord.parse(databaseFile);
            serialTypes.add(typeRecord);
            totalSize -= typeRecord.getBytesRead();
        }
        recordHeader.setSerialTypeCodes(serialTypes);
        System.err.println("schemareader.RecordHeader.parse after " +  recordHeader);
        return recordHeader;
    }
}
