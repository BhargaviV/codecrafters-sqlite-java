import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;

@Setter
@Getter
@ToString
public class SerialTypeRecord {

    enum SerialType {
        NULL,
        INT_8,
        INT_16,
        INT_32,
        INT_48,
        INT_64,
        FLOAT_64,
        BLOB,
        STRING;
    }

    SerialType serialType;
    int size;
    int bytesRead;

    public static SerialTypeRecord parse(FileInputStream databaseFile) throws IOException {
        SerialTypeRecord serialTypeRecord = new SerialTypeRecord();
        long before = databaseFile.getChannel().position();
        long value = Parser.readSQLiteVarint(databaseFile);
        long after = databaseFile.getChannel().position();
        serialTypeRecord.setSize(getSize((int) (value)));
        serialTypeRecord.setSerialType(getSerialType((int) value));
        serialTypeRecord.setBytesRead((int) (after - before));

        return serialTypeRecord;
    }

    public static SerialType getSerialType(int serialType) {

        switch (serialType) {
            case 0 -> {
                return SerialType.NULL;
            }
            case 2 -> {
                return SerialType.INT_8;
            }
            case 3 -> {
                return SerialType.INT_16;
            }
            case 4 -> {
                return SerialType.INT_32;
            }
            case 5 -> {
                return SerialType.INT_64;
            }
            default -> {
                if (serialType >= 13 && serialType % 2 == 1) {
                    return SerialType.STRING;
                }
                if (serialType >= 12) {
                    return SerialType.BLOB;
                }
            }
        }

        return SerialType.NULL;
    }

    public static int getSize(int serialType) {
        switch (serialType) {
            case 0 -> {
                return 0;
            }
            case 2 -> {
                return 2;
            }
            case 3 -> {
                return 3;
            }
            case 4 -> {
                return 4;
            }
            case 5 -> {
                return 5;
            }
            default -> {
                if (serialType >= 13 && serialType % 2 == 1) {
                    return (serialType - 13) / 2;
                }
                else if (serialType >= 12) {
                    return (serialType - 12) / 2;
                }
                return 0;
            }
        }
    }

}
