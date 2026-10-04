package schemareader;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
                objects.add(s);
            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_8)) {
                objects.add((int) byteBuffer.get());
            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_16)) {
                objects.add((int) byteBuffer.getShort());
            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_24)) {
                int value = ((byteBuffer.get() & 0xFF) << 16)
                        | ((byteBuffer.get() & 0xFF) << 8)
                        | (byteBuffer.get() & 0xFF);

                // Sign extension
                if ((value & 0x800000) != 0) {
                    value |= 0xFF000000;
                }

                objects.add(value);

            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_32)) {
                objects.add(byteBuffer.getInt());

            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_48)) {
                long value = 0;
                for (int i = 0; i < 6; i++) {
                    value = (value << 8) | (byteBuffer.get() & 0xFF);
                }
                objects.add(value);

            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_64)) {
                objects.add(byteBuffer.getLong());

            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.FLOAT_64)) {
                objects.add(byteBuffer.getDouble());

            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_ZERO)) {
                objects.add(0);

            } else if (serialTypeRecord.getSerialType().equals(SerialTypeRecord.SerialType.INT_ONE)) {
                objects.add(1);

            } else {
//                System.err.println("serialTypeRecord.getSerialType()" + serialTypeRecord.getSerialType());
                objects.add("");
            }
        }
        recordBody.setBody(objects);
//        System.err.println("recordBody" + recordBody);
        return recordBody;
    }
}
