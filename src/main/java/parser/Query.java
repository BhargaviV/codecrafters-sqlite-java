package parser;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import schemareader.TableBtreeCell;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Setter
@Getter
@ToString
public class Query {

    List<String> tables;
    List<Column> columnList;
    List<Condition> conditionList;
    String query;
    boolean isCountQuery;

    Query(String query) {
        this.query = query;
    }

    public Query parse() {

        String regex =
                "(?i)select\\s+(?<columns>.*?)\\s+from\\s+(?<table>\\w+)(?:\\s+where\\s+(?<where>.*))?";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(query);

        if (matcher.matches()) {
            System.err.println(matcher.group("columns"));
            System.err.println(matcher.group("table"));
            System.err.println(matcher.group("where"));
        }

        List<String> tokens = Arrays.stream(query.toLowerCase().split(" ")).toList();
        List<Column> columns = new ArrayList<>();
        List<String> tables = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        if (matcher.matches()) {
           for (String columnName: matcher.group("columns").split(",")) {
                Column column = new Column();
                column.setName(columnName);
                columns.add(column);
            }

            if (!matcher.group("table").isEmpty()) {
               tables = Arrays.stream(matcher.group("table").split(",")).toList();
            }

            if (matcher.group("where") != null) {
                for (String conditionString : matcher.group("where").split("\\s*\\b(?:or|and)\\b\\s*")) {
                    HashMap<String, String> conditionMap = new HashMap<>();
                    String[] stringCondition = conditionString.split("=");
                    System.err.println("conditionString" + conditionString);
                    conditionMap.put(stringCondition[0].trim(), stringCondition[1].trim());
                    Condition condition = new Equal(conditionMap);
                    conditions.add(condition);
                }
            }
        }
        this.setColumnList(columns);
        this.setTables(tables);
        this.setConditionList(conditions);
        this.setCountQuery(tokens.get(1).toLowerCase().contains("count"));
        return this;
    }


    public List<TableBtreeCell> applyCondition(List<TableBtreeCell> cells, Map<String, Integer> columnMapping) {
        List<TableBtreeCell> filtered = new ArrayList<>();
        if (!conditionList.isEmpty()) {
           for (Condition condition: conditionList) {
               if (condition instanceof Equal) {
                   filtered.addAll(condition.apply(cells, columnMapping));
               }
           }
           System.err.println("applyCondition" + cells.size() + "after =" + filtered.size());
           return filtered;
        }
        return cells;
    }

    List<Integer> getColumnIndexes(List<String> tableDefinedColumn) {
        List<Integer> indexes = new ArrayList<>();
        for (Column requestColumn: this.getColumnList()) {
            String tableName = requestColumn.getName().trim().toLowerCase();
            int index = tableDefinedColumn.indexOf(tableName);
            if (index == -1) {
                continue;
            }
            indexes.add(index);
        }
        System.err.println("getColumnIndexes" + indexes);
        return indexes;
    }
}
