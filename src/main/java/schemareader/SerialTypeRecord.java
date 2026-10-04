package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import utils.Parser;

import java.io.FileInputStream;
import java.io.IOException;

@Setter
@Getter
@ToString
public class SerialTypeRecord {

    enum SerialType {
        NULL,
        INT_ZERO,
        INT_ONE,
        RESERVED,
        INT_8,
        INT_16,
        INT_24,
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
            case 1 -> {
                return SerialType.INT_8;
            }
            case 2 -> {
                return SerialType.INT_16;
            }
            case 3 -> {
                return SerialType.INT_24;
            }
            case 4 -> {
                return SerialType.INT_32;
            }
            case 5 -> {
                return SerialType.INT_48;
            }
            case 6 -> {
                return SerialType.INT_64;
            }
            case 7 -> {
                return SerialType.FLOAT_64;
            }
            case 8 -> {
                return SerialType.INT_ZERO;
            }
            case 9 -> {
                return SerialType.INT_ONE;
            }
            default -> {
                if (serialType >= 12 && serialType % 2 == 0) {
                    return SerialType.BLOB;
                }
                if (serialType >= 13 && serialType % 2 == 1) {
                    return SerialType.STRING;
                }
                return SerialType.RESERVED;
            }
        }

    }

    public static int getSize(int serialType) {
        switch (serialType) {
            case 0 -> {
                return 0; // NULL
            }
            case 1 -> {
                return 1; // INTEGER
            }
            case 2 -> {
                return 2; // INTEGER
            }
            case 3 -> {
                return 3; // INTEGER
            }
            case 4 -> {
                return 4; // INTEGER
            }
            case 5 -> {
                return 6; // INTEGER
            }
            case 6 -> {
                return 8; // INTEGER
            }
            case 7 -> {
                return 8; // FLOAT
            }
            case 8, 9 -> {
                return 0; // constants 0 and 1
            }
            default -> {

                if (serialType >= 13 && serialType % 2 == 1) {
                    return (serialType - 13) / 2; // TEXT
                }
                if (serialType >= 12 && serialType % 2 == 0) {
                    return (serialType - 12) / 2; // BLOB
                }

                return 0; // reserved 10, 11
            }
        }
    }

}
