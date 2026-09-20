package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

class TableBtreeInteriorCell {
    int leftChildPage;
    long rowId;
}


@Getter
@Setter
@ToString
public class TableBtreeInteriorPage {

    BtreePageHeader header;
    int rightMostPointer;
    List<TableBtreeInteriorCell> cells;

}
