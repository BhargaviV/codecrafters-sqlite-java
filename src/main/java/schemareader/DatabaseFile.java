package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@ToString
public class DatabaseFile {
    DatabaseHeader databaseHeader;
    BtreePageHeader btreePageHeader;
    List<Short> cellPointerArray;
    List<TableBtreeCell> cells;

    public static DatabaseFile parse(FileInputStream databaseFileStream) throws IOException {
        DatabaseFile databaseFile = new DatabaseFile();
        DatabaseHeader databaseHeader = DatabaseHeader.parse(databaseFileStream);
        BtreePageHeader btreePageHeader = BtreePageHeader.parse(databaseFileStream);
        databaseFile.setDatabaseHeader(databaseHeader);
        databaseFile.setBtreePageHeader(btreePageHeader);

        /*
        Cell content is stored in the cell content region of the b-tree page.
        SQLite strives to place cells as far toward the end of the b-tree page as it can, in order to leave space for
        future growth of the cell pointer array. The area in between the last cell pointer array entry and
        the beginning of the first cell is the unallocated region.
         */

        List<Integer> cellOffsets = new ArrayList<>();
        for (int i = 0; i < btreePageHeader.getNumberOfCells(); i ++) {
            int cellOffset = ByteBuffer.wrap(databaseFileStream.readNBytes(2)).getShort() & 0xFFFF;
            cellOffsets.add(cellOffset);
        }

        long pageStart = 0;
        List<TableBtreeCell> tableBtreeCells = new ArrayList<>();
        for (Integer cellOffset: cellOffsets) {
            long absoluteCellOffset = pageStart + cellOffset;
            System.err.println(
                    "cellOffset=" + cellOffset +
                            ", absolute=" + absoluteCellOffset
            );
            databaseFileStream.getChannel().position(absoluteCellOffset);
            tableBtreeCells.add(TableBtreeCell.parse(databaseFileStream));
        }
        databaseFile.setCells(tableBtreeCells);
        return databaseFile;
    }
}
