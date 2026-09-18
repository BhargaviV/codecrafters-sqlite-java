import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
public class RecordBody {

    List<Object> body;

    public static RecordBody parse(FileInputStream databaseFile, List<SerialTypeRecord> typeRecords) throws IOException {

        RecordBody recordBody = new RecordBody();
        List<Object> objects = new ArrayList<>();
        for (SerialTypeRecord serialTypeRecord : typeRecords) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(databaseFile.readNBytes(serialTypeRecord.getSize()));
            // Later add other types
            if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.STRING)) {
                String s = StandardCharsets.UTF_8.decode(byteBuffer).toString();
                System.out.println("Table names " + s);
                objects.add(s);
            }
        }
        recordBody.setBody(objects);
        return recordBody;
    }
}
