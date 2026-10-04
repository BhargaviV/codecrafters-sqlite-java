package utils;

import pagereader.PageType;
import parser.Condition;
import parser.Equal;
import parser.Query;
import schemareader.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Parser {


    static int upperBoundLong(List<Long> values, Long target) {
        int left = 0;
        int right = values.size();

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (values.get(mid).compareTo(target) > 0) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    static int upperBound(List<String> values, String target) {

        int left = 0;
        int right = values.size();

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (values.get(mid).compareTo(target) > 0) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return right;
    }

    public static SchemaTable findSchemaPage(List<SchemaTable> schemaTables, String searchNAme) {
        return schemaTables.stream().filter(schemaTable -> schemaTable.getName().equals(searchNAme))
                .toList()
                .getFirst();
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

    public static List<TableBtreeCell> parseBtreeLeaf(FileInputStream databaseFileStream, BtreePageHeader btreePageHeader,
                                                      int pageNumber,
                                                      int pageSize) throws IOException {
        List<Integer> cellOffsets = new ArrayList<>();
        for (int i = 0; i < btreePageHeader.getNumberOfCells(); i ++) {
            int cellOffset = ByteBuffer.wrap(databaseFileStream.readNBytes(2)).getShort() & 0xFFFF;
            cellOffsets.add(cellOffset);
        }

        long pageOffset = (pageNumber - 1L) * pageSize;
        List<TableBtreeCell> tableBtreeCells = new ArrayList<>();
        for (Integer cellOffset: cellOffsets) {
            long absoluteCellOffset = pageOffset + cellOffset;
            databaseFileStream.getChannel().position(absoluteCellOffset);
            tableBtreeCells.add(TableBtreeCell.parse(databaseFileStream));
        }
        return tableBtreeCells;
    }

    public static List<SchemaTable> covertCellsToSchemaTable(DatabaseFile databaseFile) {
        List<SchemaTable> tables = new ArrayList<>();
//        System.err.println("covertCellsToSchemaTable" + databaseFile.getCells());
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

    static List<TableBtreeCell> traverseInteriorPage(FileInputStream databaseFile, BtreePageHeader header,
                                                     int pageNumber, int pageSize) throws IOException {
        List<TableBtreeCell> cells = new ArrayList<>();
        List<Integer> cellOffsets = new ArrayList<>();
        for (int i = 0; i < header.getNumberOfCells(); i++) {
            int cellOffset = readUnsignedShort(databaseFile);
            cellOffsets.add(cellOffset);
        }

        for (int cellOffset: cellOffsets) {
            long cellAbsoluteOffset = (pageNumber - 1L) * pageSize + cellOffset;
            databaseFile.getChannel().position(cellAbsoluteOffset);
            int leftChildPage = ByteBuffer
                    .wrap(databaseFile.readNBytes(4))
                    .getInt();
            Parser.readSQLiteVarint(databaseFile); // separator rowid
            cells.addAll(traverseAllRowsOnPage(databaseFile, leftChildPage, pageSize));
        }
        cells.addAll(traverseAllRowsOnPage(databaseFile, header.getRightMostPointer(), pageSize));
//        System.err.println("cells traverseInteriorPage" + cells);
        return cells;
    }

    public static List<TableBtreeCell> traverseAllRowsOnPage(FileInputStream databaseFile, int pageNumber, int pageSize) throws IOException {
        long pageOffset = Math.max(0, (pageNumber - 1L) * pageSize);
//        System.err.println("pageSize" + pageSize + "pageNumber" + pageNumber + " ");
        databaseFile.getChannel().position(pageOffset);
        BtreePageHeader header = BtreePageHeader.parse(databaseFile);

        if (header.getBtreePageType() == PageType.TABLE_LEAF) {
            List<TableBtreeCell> cells = parseBtreeLeaf(databaseFile, header, pageNumber, pageSize);
            System.err.println("traverseAllRowsOnPage totalsize" + cells.size());
            return cells;
        }

        if (header.getBtreePageType() == PageType.TABLE_INTERNAL) {
            List<TableBtreeCell> tableBtreeCells = new ArrayList<>(traverseInteriorPage(databaseFile, header, pageNumber, pageSize));
            return tableBtreeCells;
        }

        if (header.getBtreePageType() == null) {
            return List.of();
        }

        throw new IllegalStateException(
                "Unexpected table B-tree page type: " + header.getBtreePageType()
        );
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

    public static TableBtreeCell getBtreeCells(Long targetRowId,
                                                     int pageNumber,
                                                     int pageSize,
                                                     FileInputStream databaseFile) throws IOException {
        long pageOffset = Math.max(0, (pageNumber - 1L) * pageSize);
        databaseFile.getChannel().position(pageOffset);
        BtreePageHeader header = BtreePageHeader.parse(databaseFile);

        if (header.getBtreePageType() == PageType.TABLE_LEAF) {
            List<TableBtreeCell> cells = parseBtreeLeaf(databaseFile, header, pageNumber, pageSize);
            for (TableBtreeCell cell : cells) {
                if (cell.getRowId() == targetRowId) {
                    return cell;
                }
            }
            return null;
        }

        if (header.getBtreePageType() == PageType.TABLE_INTERNAL) {
            List<Object> rowIds = new ArrayList<>();
            List<Integer> pages = new ArrayList<>();
            List<Integer> cellOffsets = new ArrayList<>();
            for (int i = 0; i < header.getNumberOfCells(); i++) {
                int cellOffset = readUnsignedShort(databaseFile);
                cellOffsets.add(cellOffset);
            }

            for (int cellOffset: cellOffsets) {
                long cellAbsoluteOffset = (pageNumber - 1L) * pageSize + cellOffset;
                databaseFile.getChannel().position(cellAbsoluteOffset);
                int leftChildPage = ByteBuffer
                        .wrap(databaseFile.readNBytes(4))
                        .getInt();
                rowIds.add(Parser.readSQLiteVarint(databaseFile)); // separator rowid
                pages.add(leftChildPage);
            }


            int index = SqliteBTreeSearch.findLessThanOrEqualTo(rowIds, targetRowId);
//            System.err.println("rowIds=" + rowIds +
//                    "targetRowId=" + targetRowId +
//                    "index=" + index);
            int nextPage;
            if (index == -1) {
                nextPage = pages.getFirst();
            } else if (index + 1 < rowIds.size()) {
                nextPage = pages.get(index + 1);
            } else {
                nextPage = header.getRightMostPointer();
            }

            return getBtreeCells(targetRowId, nextPage, pageSize, databaseFile);
        }
        return null;
    }


    public static List<TableBtreeCell> traverseRowsOnPage(FileInputStream databaseFile,
                                                          List<TableIndexCell> indexCells,
                                                          int pageNumber,
                                                          int pageSize) throws IOException {
        Set<Long> rowIds = indexCells.stream().map(tableIndexCell -> tableIndexCell.getTableIndexRecord().getRowId()).collect(Collectors.toSet());
        List<TableBtreeCell> cells = new ArrayList<>();
        for (Long rowId: rowIds) {
            TableBtreeCell cell = getBtreeCells(rowId, pageNumber, pageSize, databaseFile);
            if (cell != null && cell.getRowId() == rowId) {
                cells.add(cell);
            }
        }
//        System.err.println("cekks" + cells);
        System.err.println("getBtreeCells" +
                cells.stream().map(cell -> cell.getRecord().getRecordBody().getBody().get(7)).collect(Collectors.toSet())
        + "size " + cells.size());

        // for each rowId find the row in btree
        return cells;
    }
}
