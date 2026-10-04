package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import utils.Parser;

import java.io.FileInputStream;
import java.io.IOException;

@Setter
@Getter
@ToString
public class TableBtreeCell implements BtreeCell {

    /*
    A varint which is the total number of bytes of payload, including any overflow
    A varint which is the integer key, a.k.a. "rowid"
    The initial portion of the payload that does not spill to overflow pages.
    A 4-byte big-endian integer page number for the first page of the overflow page list - omitted if all payload fits on the b-tree page.
     */

    long recordSize;
    long rowId;
    Record record;

    public static TableBtreeCell parse(FileInputStream databaseFile) throws IOException {
        TableBtreeCell tableBtreeCell = new TableBtreeCell();
        tableBtreeCell.setRecordSize(Parser.readSQLiteVarint(databaseFile));
        tableBtreeCell.setRowId(Parser.readSQLiteVarint(databaseFile));
        tableBtreeCell.setRecord(Record.parse(databaseFile));
        return tableBtreeCell;
    }
}
