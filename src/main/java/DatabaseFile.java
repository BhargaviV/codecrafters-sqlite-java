import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;


@Setter
@Getter
@ToString
public class DatabaseFile {
    DatabaseHeader databaseHeader;
    BtreePageHeader btreePageHeader;

    public static DatabaseFile parse(FileInputStream databaseFileStream) throws IOException {
        DatabaseFile databaseFile = new DatabaseFile();
        DatabaseHeader databaseHeader = DatabaseHeader.parse(databaseFileStream);
        BtreePageHeader btreePageHeader = BtreePageHeader.parse(databaseFileStream);
        databaseFile.setDatabaseHeader(databaseHeader);
        databaseFile.setBtreePageHeader(btreePageHeader);
        return databaseFile;
    }
}
