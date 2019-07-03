/*
@author Arthur Godet <arth.godet@gmail.com>
@since 07/03/2019
*/
package data;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Factory {

    public static <T> T fromFile(String path, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(path), valueType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void toFile(String path, Object toWrite) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File f = new File(path);
            if(!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toWrite);
            FileWriter fw = new FileWriter(path);
            fw.write(s);
            fw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
