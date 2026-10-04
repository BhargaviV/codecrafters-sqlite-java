package parser;

import schemareader.DatabaseFile;
import schemareader.SchemaTable;
import schemareader.TableBtreeCell;
import schemareader.TableIndexCell;
import utils.IndexParser;
import utils.Parser;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryExecutor {

    DatabaseFile databaseFile;
    String filePath;
    List<SchemaTable> schemaTables;

    Set<String> constraintIdentifier = Set.of("PRIMARY", "FOREIGN", "CHECK", "UNIQUE");

    public QueryExecutor(DatabaseFile databaseFile, String filePath) {
        this.databaseFile = databaseFile;
        this.filePath = filePath;
        this.schemaTables = Parser.covertCellsToSchemaTable(databaseFile);
    }
    
    public List<String> getColumnNames(String sql) {
        System.err.println("sql" + sql);
        Pattern pattern = Pattern.compile("(?:\\G,?|\\()\\s*([^,()]*\\s+[^,()]*)\\s*(?=[^()]*\\))");
        Matcher matcher = pattern.matcher(sql);
        List<String> columnNames = new ArrayList<>();
        while (matcher.find()) {
            String columnName = matcher.group(1).trim().split(" ")[0];
            if (constraintIdentifier.contains(columnName.toUpperCase())) {
                break;
            }
            columnNames.add(columnName.toLowerCase());
        }
        return columnNames;
    }

    public String execute(String query) throws Exception {
        System.err.println("schemaTables" + schemaTables);
        Query parsedQuery = new Query(query).parse();
        String tableName = parsedQuery.getTables().getFirst();
        int pageSize = databaseFile.getDatabaseHeader().getPageSize();
        schemareader.SchemaTable table = Parser.findSchemaPage(schemaTables, tableName);

        int rootPageNumber = table.getRootPage();
        List<String> columns = getColumnNames(table.getSql());
        List<Integer> indexes = parsedQuery.getColumnIndexes(columns);
        Map<String, Integer> columnIndexMap = new HashMap<>();
        System.err.println("columns" + columns + "indexes" + indexes);
        for (int i = 0; i < columns.size(); i ++) {
            columnIndexMap.put(columns.get(i), i);
        }

        FileInputStream newDatabaseFilePtr = new FileInputStream(this.filePath);
        if (parsedQuery.isCountQuery()) {
            return String.valueOf(Parser.countRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize));
        } else {

            List<TableBtreeCell> cells;
            if (!parsedQuery.getConditionList().isEmpty()) {
                List<TableIndexCell> indexCells = IndexParser.findIndexPage(newDatabaseFilePtr,
                        schemaTables, parsedQuery,
                        pageSize);

                if (indexCells == null || indexCells.isEmpty()) {
                    cells = Parser.traverseAllRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize);
                    cells = parsedQuery.applyCondition(cells, columnIndexMap);
                } else {
                    cells = Parser.traverseRowsOnPage(newDatabaseFilePtr, indexCells, rootPageNumber, pageSize);
//                    System.err.println("cells" + cells);
                }
            } else {
                cells = Parser.traverseAllRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize);
                cells = parsedQuery.applyCondition(cells, columnIndexMap);
            }

            System.err.println("parsedQuery" + parsedQuery);
            StringBuilder result = new StringBuilder();

            for (TableBtreeCell cell: cells) {
                List<Object> retrivedColumns = cell.getRecord().getRecordBody().getBody();
                retrivedColumns.set(0, cell.getRowId());
                cell.getRecord().getRecordBody().setBody(retrivedColumns);
                for (int index: indexes) {
                    // because id is always part of columns
                    result.append(retrivedColumns.get(index));
                    result.append("|");
                }
                if (!result.isEmpty()) {
                    result.deleteCharAt(result.length() - 1);
                }
                result.append("\n");
            }
//            System.err.println("result" + result.toString());
            return result.toString();
        }
    }
}
