package parser;

import schemareader.DatabaseFile;
import schemareader.SchemaTable;
import schemareader.TableBtreeCell;
import utils.Parser;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {

    DatabaseFile databaseFile;
    String filePath;

    Set<String> constraintIdentifier = Set.of("PRIMARY", "FOREIGN", "CHECK", "UNIQUE");

    public QueryParser(DatabaseFile databaseFile, String filePath) {
        this.databaseFile = databaseFile;
        this.filePath = filePath;
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

    public String parse(String query) throws Exception {
        List<SchemaTable> schemaTables = Parser.covertCellsToSchemaTable(databaseFile);
        System.err.println("schemaTables" + schemaTables);
        List<String> tokens = Arrays.stream(query.split(" ")).toList();
        String tableName = tokens.getLast();
        int pageSize = databaseFile.getDatabaseHeader().getPageSize();
        schemareader.SchemaTable table = schemaTables.stream().filter(schemaTable -> schemaTable.getName().equals(tableName))
                .toList()
                .getFirst();

        Integer rootPageNumber = table.getRootPage();
        List<String> columns = getColumnNames(table.getSql());

        System.err.println("Page number for table " + tableName + "=" + rootPageNumber);
        System.err.println("columns" + columns);
        FileInputStream newDatabaseFilePtr = new FileInputStream(this.filePath);
        if (tokens.get(1).toLowerCase().contains("count")) {
            return String.valueOf(Parser.countRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize));
        } else if (columns.contains(tokens.get(1).toLowerCase())) {
            List<TableBtreeCell> cells = Parser.traverseRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize);
            List<Integer> indexes = new ArrayList<>();
            for (String requestColumn: tokens.get(1).split(",")) {
                int index = columns.indexOf(requestColumn.toLowerCase());
                indexes.add(index);
            }
            String result = "";
            for (TableBtreeCell cell: cells) {
                List<Object> retrivedColumns = cell.getRecord().getRecordBody().getBody();
                for (int index: indexes) {
                    // because id is always part of columns
                    result = result + retrivedColumns.get(index - 1) + "|";
                }
                result = result.replaceAll("|$", "") + "\n";
            }
            System.err.println("result" + result);
            return result;
        }
        return null;
    }
}
