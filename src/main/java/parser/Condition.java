package parser;

import schemareader.TableBtreeCell;

import java.util.List;
import java.util.Map;

public interface Condition {

    List<TableBtreeCell> apply(List<TableBtreeCell> cells, Map<String, Integer> columns);
}
