package schemareader;

import java.io.FileInputStream;
import java.io.IOException;

public interface BtreeCell {

    static TableBtreeCell parse(FileInputStream databaseFile) throws IOException {
        return new TableBtreeCell();
    }
}
