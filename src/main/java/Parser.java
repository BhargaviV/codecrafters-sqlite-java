import java.io.FileInputStream;
import java.io.IOException;

public class Parser {

    public static DatabaseFile parseFile(String databaseFilePath) throws IOException {
        FileInputStream databaseFile = new FileInputStream(databaseFilePath);
        return DatabaseFile.parse(databaseFile);
    }
}
