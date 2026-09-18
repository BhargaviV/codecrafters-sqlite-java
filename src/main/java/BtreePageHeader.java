import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@ToString
@Getter
@Setter
public class BtreePageHeader {

    int btreePageType;
    int firstFreeBlock;
    int numberOfCells;
    int startOfCellContent;
    int fragmentedFreeCells;
    // int rightMostPointer;


    public static BtreePageHeader parse(FileInputStream databaseFile) throws IOException {
        BtreePageHeader btreePageHeader = new BtreePageHeader();
        btreePageHeader.setBtreePageType(databaseFile.read());
        System.out.println("remaining" + databaseFile.available());
        btreePageHeader.setFirstFreeBlock(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setNumberOfCells(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setStartOfCellContent(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setFragmentedFreeCells(ByteBuffer.wrap(databaseFile.readNBytes(1)).get());
//        btreePageHeader.setRightMostPointer(ByteBuffer.wrap(databaseFile.readNBytes(4)).getInt());

        return btreePageHeader;
    }
}
