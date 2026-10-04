package utils;

import pagereader.PageType;
import schemareader.BtreePageHeader;
import schemareader.TableBtreeCell;
import schemareader.TableIndexCell;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BtreeParser extends Parser {

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
        List<Long> rowIds = indexCells.stream().map(tableIndexCell -> tableIndexCell.getTableIndexRecord().getRowId()).toList();
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
                + "size " + cells.size()
        + " indexCells = " + indexCells.size());

        // for each rowId find the row in btree
        return cells;
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
//            System.err.println("traverseAllRowsOnPage totalsize" + cells.size());
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
}
