package parser;

import schemareader.TableBtreeCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Equal implements Condition {

    Map<String, String> condition;
    Equal(HashMap<String, String> condition) {
        this.condition = condition;
    }

    @Override
    public List<TableBtreeCell> apply(List<TableBtreeCell> cells, Map<String, Integer> columnIndexes) {
        System.err.println("apply" + columnIndexes+ "\n" + "condition" + condition);
        List<TableBtreeCell> filtered = new ArrayList<>();
        for (TableBtreeCell cell: cells) {
            List<Object> retrievedColumns = cell.getRecord().getRecordBody().getBody();
            System.err.println("retrievedColumns" + retrievedColumns);

            boolean isTrue = true;
            for(Map.Entry<String, String> columnName: condition.entrySet()) {
                // Id is present in the row id column
                retrievedColumns.set(0, cell.getRowId());
                cell.getRecord().getRecordBody().setBody(retrievedColumns);
                int columnIndex = columnIndexes.get(columnName.getKey().trim());
                String actualValue = retrievedColumns.get(columnIndex).toString();
//                System.err.println("columnName" + columnName + columnIndexes.get(columnName.trim()) +
//                        "actualValue" + actualValue +
//                        "condition.get(columnName).replace(\"'\", \"\")" + condition.get(columnName).replace("'", "").trim());
                if (!actualValue.equals(columnName.getValue().replace("'", "").trim())) {
                    isTrue = false;
                    break;
                }
            }
            if (isTrue) {
                filtered.add(cell);
            }
        }
        System.err.println("filtered apply" + filtered);
        return filtered;
    }
}
