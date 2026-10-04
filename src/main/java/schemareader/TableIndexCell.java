package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import pagereader.PageType;
import utils.Parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;

@Setter
@ToString
@Getter
public class TableIndexCell implements BtreeCell {

    long leftChildPointer;
    long size;
    TableIndexRecord tableIndexRecord;

    public static TableIndexCell parse(FileInputStream databaseFile, BtreePageHeader pageHeader) throws IOException {
        TableIndexCell tableIndexCell = new TableIndexCell();
        if (pageHeader.getBtreePageType() == PageType.INDEX_INTERNAL) {
            int leftChildPage = ByteBuffer
                    .wrap(databaseFile.readNBytes(4))
                    .getInt();
            tableIndexCell.setLeftChildPointer(leftChildPage);
        }
        tableIndexCell.setSize(Parser.readSQLiteVarint(databaseFile));
        tableIndexCell.setTableIndexRecord(TableIndexRecord.parse(databaseFile));
        return tableIndexCell;
    }

}
