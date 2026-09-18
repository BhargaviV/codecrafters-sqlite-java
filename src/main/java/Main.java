void main(String[] args) {
    if (args.length < 2) {
        IO.println("Missing <database path> and <command>");
        return;
    }

    String databaseFilePath = args[0];
    String command = args[1];

    switch (command) {
        case ".dbinfo" -> {
            try {
                DatabaseFile databaseFile = Parser.parseFile(databaseFilePath);
                System.err.println("Logs from your program will appear here!");
                IO.println("database page size: " + databaseFile.getDatabaseHeader().getPageSize());
                IO.println("number of tables: " + databaseFile.getBtreePageHeader().getNumberOfCells());
                for (TableBtreeCell cell: databaseFile.getCells()) {
                    for (Object object : cell.getRecord().getRecordBody().getBody()) {
                        if (object instanceof String) {
                            System.out.print(object);
                        }
                    }
                }
            } catch (IOException e) {
                IO.println("Error reading file: " + e.getMessage());
            }
        }
        case ".tables" -> {
            try {
                DatabaseFile databaseFile = Parser.parseFile(databaseFilePath);
                for (TableBtreeCell cell: databaseFile.getCells()) {
                    Object object = cell.getRecord().getRecordBody().getBody().get(1);
                    if (object instanceof String) {
                        System.out.print(object + " ");
                    }
                }
            } catch (IOException e) {
                IO.println("Error reading file: " + e.getMessage());
            }
        }
        default -> IO.println("Missing or invalid command passed: " + command);
    }
}
