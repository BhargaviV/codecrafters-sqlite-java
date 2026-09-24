package parser;

import schemareader.DatabaseFile;
import schemareader.SchemaTable;
import schemareader.TableBtreeCell;
import utils.Parser;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    List<Integer> getColumnIndexes(List<String> tableDefinedColumn, List<String> requestColumns) {
        List<Integer> indexes = new ArrayList<>();
        for (String requestColumn: requestColumns) {
            String tableName = requestColumn.trim().toLowerCase();
            int index = tableDefinedColumn.indexOf(tableName);
            if (index == -1) {
                continue;
            }
            indexes.add(index);
        }
        System.err.println("getColumnIndexes" + indexes);
        return indexes;
    }

    public String execute(String query) throws Exception {
        System.err.println("schemaTables" + schemaTables);
        Query parsedQuery = new Query(query).parse();
        String tableName = parsedQuery.getTables().getFirst();
        int pageSize = databaseFile.getDatabaseHeader().getPageSize();
        schemareader.SchemaTable table = schemaTables.stream().filter(schemaTable -> schemaTable.getName().equals(tableName))
                .toList()
                .getFirst();

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
            List<TableBtreeCell> cells = Parser.traverseRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize);

            System.err.println("indexes" + indexes + " " + parsedQuery.getColumnList());
            StringBuilder result = new StringBuilder();

            cells = parsedQuery.applyCondition(cells, columnIndexMap);

            for (TableBtreeCell cell: cells) {
                List<Object> retrivedColumns = cell.getRecord().getRecordBody().getBody();
                for (int index: indexes) {
                    // because id is always part of columns
                    result.append(retrivedColumns.get(index - 1));
                    result.append("|");
                }
                if (!result.isEmpty()) {
                    result.deleteCharAt(result.length() - 1);
                }
                result.append("\n");
            }
            System.err.println("result" + result.toString());
            return result.toString();
        }
    }
}
