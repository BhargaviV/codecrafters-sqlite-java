package parser;

import schemareader.DatabaseFile;
import schemareader.SchemaTable;
import utils.Parser;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

public class QueryParser {

    DatabaseFile databaseFile;
    String filePath;

    public QueryParser(DatabaseFile databaseFile, String filePath) {
        this.databaseFile = databaseFile;
        this.filePath = filePath;
    }

    public String parse(String query) throws Exception {
        List<SchemaTable> schemaTables = Parser.covertCellsToSchemaTable(databaseFile);
        System.err.println("schemaTables" + schemaTables);
        List<String> tokens = Arrays.stream(query.split(" ")).toList();
        String tableName = tokens.getLast();
        int pageSize = databaseFile.getDatabaseHeader().getPageSize();
        Integer rootPageNumber = schemaTables.stream().filter(schemaTable -> schemaTable.getName().equals(tableName))
                .map(SchemaTable::getRootPage)
                .toList()
                .getFirst();
        System.err.println("Page number for table " + tableName + "=" + rootPageNumber);
        FileInputStream newDatabaseFilePtr = new FileInputStream(this.filePath);
        if (tokens.get(1).toLowerCase().contains("count")) {
            return String.valueOf(Parser.countRowsOnPage(newDatabaseFilePtr, rootPageNumber, pageSize));
        }
        return null;
    }
}
