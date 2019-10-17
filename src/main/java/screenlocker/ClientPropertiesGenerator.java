package screenlocker;

import java.io.*;
import java.util.Properties;

public class ClientPropertiesGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Not enough arguments!\n" +
                    "Expected arguments: <client.properties template> <id, name file>");
            return;
        }
        Properties template = Props.loadProperties(args[0]);
        BufferedReader br = new BufferedReader(new FileReader(args[1]));
        String s;
        new File("clients").mkdir();
        while ((s = br.readLine()) != null) {
            String[] parts = s.split("[\t;]");
            Props p = new Props(template);
            p.put("id", parts[0]);
            if (parts.length > 1)
                p.put("name", parts[1]);
            p.store(new FileWriter("clients/client-" + p.get("id") + ".properties"));
        }
    }
}
