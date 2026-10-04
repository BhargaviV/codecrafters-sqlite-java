package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import utils.Parser;

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

        databaseFile.setCells(Parser.parseBtreeLeaf(databaseFileStream, btreePageHeader, 0, 0));
//        System.err.println("database header cells" + databaseFile.getCells());
        return databaseFile;
    }
}
