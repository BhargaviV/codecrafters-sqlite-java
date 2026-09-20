package utils;

import pagereader.PageType;
import schemareader.BtreePageHeader;
import schemareader.DatabaseFile;
import schemareader.SchemaTable;
import schemareader.TableBtreeCell;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    public static List<SchemaTable> covertCellsToSchemaTable(DatabaseFile databaseFile) {
        List<SchemaTable> tables = new ArrayList<>();
        for (TableBtreeCell cell: databaseFile.getCells()) {
            List<Object> objectList = cell.getRecord().getRecordBody().getBody();
            SchemaTable schemaTable = new SchemaTable();
            schemaTable.setType((String) objectList.get(0));
            schemaTable.setName((String) objectList.get(1));
            schemaTable.setTblName((String) objectList.get(2));
            schemaTable.setRootPage((int) objectList.get(3));
            schemaTable.setSql((String) objectList.get(4));
            tables.add(schemaTable);
        }
        return tables;
    }

    static int readUnsignedShort(FileInputStream databaseFile) throws IOException {
        return ByteBuffer
                .wrap(databaseFile.readNBytes(2))
                .getShort() & 0xFFFF;
    }

    static long countInteriorPage(FileInputStream databaseFile, BtreePageHeader header,
                                  int pageNumber, int pageSize) throws IOException {
        long count = 0;
        for (int i = 0; i < header.getNumberOfCells(); i++) {
            int cellOffset = readUnsignedShort(databaseFile);
            long cellAbsoluteOffset = (pageNumber - 1L) * pageSize + cellOffset;
            databaseFile.getChannel().position(cellAbsoluteOffset);
            int leftChildPage = databaseFile.read() & 0xFF;
            Parser.readSQLiteVarint(databaseFile); // separator rowid
            count += countRowsOnPage(databaseFile, leftChildPage, pageSize);
        }
        count += countRowsOnPage(databaseFile, header.getRightMostPointer(), pageSize);
        return count;
    }

    public static long countRowsOnPage(FileInputStream databaseFile, int pageNumber, int pageSize) throws IOException {
        long pageOffset = (pageNumber - 1L) * pageSize;
        databaseFile.getChannel().position(pageOffset);
        BtreePageHeader header = BtreePageHeader.parse(databaseFile);
        if (header.getBtreePageType() == PageType.TABLE_LEAF) {
            return header.getNumberOfCells();
        }

        if (header.getBtreePageType() == PageType.TABLE_INTERNAL) {
            return countInteriorPage(databaseFile, header, pageNumber, pageSize);
        }

        throw new IllegalStateException(
                "Unexpected table B-tree page type: " + header.getBtreePageType()
        );
    }
}
