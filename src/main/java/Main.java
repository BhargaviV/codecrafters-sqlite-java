import parser.QueryParser;
import schemareader.SchemaTable;
import utils.Parser;
import schemareader.DatabaseFile;

void main(String[] args) throws Exception {
    if (args.length < 2) {
        IO.println("Missing <database path> and <command>");
        return;
    }

    String databaseFilePath = args[0];
    String command = args[1];
    DatabaseFile databaseFile = null;

    try {
        FileInputStream databaseFileStream = new FileInputStream(databaseFilePath);
        databaseFile = DatabaseFile.parse(databaseFileStream);
    } catch (IOException e) {
        IO.println("Error reading file: " + e.getMessage());
        return;
    }
    List<SchemaTable> tables = Parser.covertCellsToSchemaTable(databaseFile);
    switch (command) {
        case ".dbinfo" -> {
            System.err.println("Logs from your program will appear here!");
            IO.println("database page size: " + databaseFile.getDatabaseHeader().getPageSize());
            IO.println("number of tables: " + databaseFile.getBtreePageHeader().getNumberOfCells());
        }
        case ".tables" -> {
            for (SchemaTable table: tables) {
                System.out.print(table.getName() + " ");
            }
        }
        default -> {
            if (command.toLowerCase().contains("select")) {
                QueryParser parser = new QueryParser(databaseFile, databaseFilePath);
                IO.println(parser.parse(command));
            } else {
                IO.println("Missing or invalid command passed: " + command);
            }
        }
    }
}
