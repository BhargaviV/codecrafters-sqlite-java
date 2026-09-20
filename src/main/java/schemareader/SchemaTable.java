package schemareader;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Setter
@Getter
@ToString
public class SchemaTable {

//    enum SchemaType {
//        TABLE,
//        VIEW,
//        INDEX,
//        TRIGGER
//    }
    List<SchemaTable> tables;
    String type;
    String name;
    String tblName;
    int rootPage;
    String sql;
}
