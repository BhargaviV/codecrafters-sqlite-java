import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Parser {

    public static DatabaseFile parseFile(String databaseFilePath) throws IOException {
        FileInputStream databaseFile = new FileInputStream(databaseFilePath);
        return DatabaseFile.parse(databaseFile);
    }

    public static long readSQLiteVarint(InputStream fileStream) throws IOException {
        long value = 0;

        for (int i = 0; i < 9; i++) {
            int b = fileStream.read() & 0xFF;

            if (i == 8) {
                // 9th byte uses all 8 bits
                value = (value << 8) | b;
                return value;
            }

            value = (value << 7) | (b & 0x7F);

            if ((b & 0x80) == 0) {
                return value;
            }
        }

        throw new IllegalStateException("Invalid SQLite varint");
    }
}
