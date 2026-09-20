package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import pagereader.PageType;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@ToString
@Getter
@Setter
public class BtreePageHeader {

    PageType btreePageType;
    int firstFreeBlock;
    int numberOfCells;
    int startOfCellContent;
    int fragmentedFreeCells;
    int rightMostPointer;

    public static BtreePageHeader parse(FileInputStream databaseFile) throws IOException {
        BtreePageHeader btreePageHeader = new BtreePageHeader();
        btreePageHeader.setBtreePageType(PageType.getPageType(databaseFile.read()));
        System.err.println("remaining" + databaseFile.available());
        btreePageHeader.setFirstFreeBlock(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setNumberOfCells(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setStartOfCellContent(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setFragmentedFreeCells(ByteBuffer.wrap(databaseFile.readNBytes(1)).get());
        if(btreePageHeader.getBtreePageType() == PageType.TABLE_INTERNAL) {
            btreePageHeader.setRightMostPointer(ByteBuffer.wrap(databaseFile.readNBytes(4)).getInt());
        }

        return btreePageHeader;
    }
}
