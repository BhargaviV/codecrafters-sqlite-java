import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
@Setter
@ToString
public class DatabaseHeader {

    String headerString;
    int pageSize;

    public static DatabaseHeader parse(FileInputStream databaseFile) throws IOException {
        DatabaseHeader databaseHeader = new DatabaseHeader();
        databaseFile.skip(16); // Skip the first 16 bytes of the header
        byte[] pageSizeBytes = new byte[2]; // The following 2 bytes are the page size
        databaseFile.read(pageSizeBytes);
        int pageSize = Short.toUnsignedInt(ByteBuffer.wrap(pageSizeBytes).getShort());
        databaseHeader.setPageSize(pageSize);
        databaseFile.skipNBytes(100 - 18);
        return databaseHeader;
    }
}
