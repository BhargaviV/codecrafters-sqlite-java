package utils;

import pagereader.PageType;
import parser.Condition;
import parser.Equal;
import parser.Query;
import schemareader.BtreePageHeader;
import schemareader.SchemaTable;
import schemareader.TableIndexCell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class IndexParser extends Parser {

    public static List<TableIndexCell> findIndexPage(FileInputStream databaseFile, List<SchemaTable> schemaTables, Query query,
                                            int pageSize) throws IOException {

        for (Condition condition: query.getConditionList()) {
            if (condition instanceof Equal) {

                for (String key: ((Equal) condition).getCondition().keySet()) {
                    List<SchemaTable> table = schemaTables.stream().filter(schemaTable -> schemaTable.getType().equals("index") && schemaTable.getName().contains("idx_"))
                            .toList();
                    if (table.isEmpty()) {
                        return null;
                    }
                    String cellValue = ((Equal) condition).getCondition().get(key);
                    String searchKey = cellValue;
                    if (cellValue.startsWith("'") && cellValue.endsWith("'")) {
                        searchKey = cellValue.substring(1, cellValue.length() - 1).trim();
                    }

                    return traverseInteriorIndexPage(databaseFile, table.getFirst().getRootPage(), pageSize, searchKey);
                }
            }
        }
        return null;
    }

    static List<TableIndexCell> traverseInteriorIndexPage(FileInputStream databaseFile,
                                                          int pageNumber, int pageSize, String searchKey) throws IOException {

        long pageOffset = (pageNumber - 1L) * pageSize;
        databaseFile.getChannel().position(pageOffset);
        BtreePageHeader header = BtreePageHeader.parse(databaseFile);
        if (header.getBtreePageType() == PageType.INDEX_LEAF) {
            return new ArrayList<>(traverseIndexPage(databaseFile, header, pageNumber, pageSize,
                    searchKey));
        }

        List<Integer> cellOffsets = new ArrayList<>();
        for (int i = 0; i < header.getNumberOfCells(); i++) {
            int cellOffset = readUnsignedShort(databaseFile);
            cellOffsets.add(cellOffset);
        }

//        System.err.println(
//                "page=" + pageNumber +
//                        " type=" + header.getBtreePageType() +
//                        " cells=" + header.getNumberOfCells() +
//                        " cellContent=" + header.getStartOfCellContent() +
//                        " rightMost=" + header.getRightMostPointer() +
//                        " pageOffset=" + pageOffset
//        );

        List<TableIndexCell> tableIndexCells = new ArrayList<>();
        List<Object> keys = new ArrayList<>();
        for (long cellOffset: cellOffsets) {
//            System.err.print("cellOffset=" + cellOffset + " " + "absoluteOffset=" + (pageOffset + cellOffset) + " ");
            databaseFile.getChannel().position(pageOffset + cellOffset);
            TableIndexCell indexCell = TableIndexCell.parse(databaseFile, header);
            tableIndexCells.add(indexCell);
            String key = indexCell.getTableIndexRecord().getRecordBody().getBody().get(0).toString();
            keys.add(key);
        }


        int matchIndex = SqliteBTreeSearch.findLessThanOrEqualTo(keys, searchKey);
        int targetChildPage;
        if (matchIndex == -1) {
            // Everything on this page is strictly greater than the target.
            // Go down the Left Child Pointer of the very first cell.
            targetChildPage = Math.toIntExact(tableIndexCells.getFirst().getLeftChildPointer());
        } else if ((matchIndex + 1 < keys.size())) {
            targetChildPage = Math.toIntExact(tableIndexCells.get(matchIndex + 1).getLeftChildPointer());
        } else {
            // Look at the element at matchIndex
            Object matchedKey = keys.get(matchIndex);

            if (SqliteBTreeSearch.compareSqliteValues(matchedKey, searchKey) == 0) {
                // EXACT MATCH FOUND on this interior page!
                // In SQLite interior index pages, items equal to this key reside
                // down this cell's own Left Child Pointer.
                targetChildPage = Math.toIntExact(tableIndexCells.get(matchIndex).getLeftChildPointer());
            } else {
                // NO EXACT MATCH (e.g. search key 'republic of the congo' is greater than 'qatar')
                // We must check if the target is also greater than the NEXT available cell's boundary.
                if (matchIndex + 1 < keys.size()) {
                    // Target is greater than keys.get(matchIndex) but less than or equal to keys.get(matchIndex + 1)
                    targetChildPage = Math.toIntExact(tableIndexCells.get(matchIndex + 1).getLeftChildPointer());
                } else {
                    // Target is greater than the absolute last element on the page (e.g. greater than 'russia')
                    // Drop cleanly into the page's Rightmost Pointer fallback.
                    targetChildPage = header.getRightMostPointer();
                }
            }
        }

//        System.err.println("traverseInteriorIndexPage indexCell=" + keys + " matchIndex=" + tableIndexCells.get(Math.min(keys.size() - 1, matchIndex + 1)).getTableIndexRecord() +
//                " targetPage=" + targetChildPage +
//                " keySize=" + keys.size() + " searchTarget=" + searchKey + " matchIndex index=" + matchIndex);
        return traverseInteriorIndexPage(databaseFile, targetChildPage, pageSize, searchKey);
    }


    static List<TableIndexCell> traverseIndexPage(FileInputStream databaseFileStream, BtreePageHeader btreePageHeader,
                                                      int pageNumber, int pageSize,
                                                      String searchKey) throws IOException {

        List<Integer> cellOffsets = new ArrayList<>();
        for (int i = 0; i < btreePageHeader.getNumberOfCells(); i ++) {
            int cellOffset = ByteBuffer.wrap(databaseFileStream.readNBytes(2)).getShort() & 0xFFFF;
            cellOffsets.add(cellOffset);
        }

        long pageOffset = (pageNumber - 1L) * pageSize;
        List<TableIndexCell> tableIndexCells = new ArrayList<>();
        for (Integer cellOffset: cellOffsets) {
            long absoluteCellOffset = pageOffset + cellOffset;
            databaseFileStream.getChannel().position(absoluteCellOffset);
            TableIndexCell cell = TableIndexCell.parse(databaseFileStream, btreePageHeader);
            if (cell.getTableIndexRecord().getRecordBody().getBody().getFirst().equals(searchKey)) {
                tableIndexCells.add(cell);
            }
        }
        return tableIndexCells;
    }
}
