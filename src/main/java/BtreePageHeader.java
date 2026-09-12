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

    public static BtreePageHeader parse(FileInputStream databaseFile) throws IOException {
        BtreePageHeader btreePageHeader = new BtreePageHeader();
        btreePageHeader.setBtreePageType(databaseFile.read());
        System.out.println("remaining" + databaseFile.available());
        btreePageHeader.setFirstFreeBlock(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        btreePageHeader.setNumberOfCells(ByteBuffer.wrap(databaseFile.readNBytes(2)).getShort());
        return btreePageHeader;
    }
}
