/*
@author Arthur Godet <arth.godet@gmail.com>
@since 07/03/2019
*/
package bench;

import data.Factory;
import data.input.Instance;
import model.RNMP;

import java.io.File;
import java.io.IOException;

public class Bench {

    public static void main(String[] args) throws IOException {
        if(!"all".equals(args[1])) {
            Instance instance = Factory.fromFile("data/"+args[1]+".json", Instance.class);
            System.out.println(instance.name);
            if(!new File("results/"+instance.name+".txt").exists()) {
                RNMP rnmp = new RNMP(instance);
                rnmp.solve(args[0]);
                System.in.read();
            }
        } else {
            File folder = new File("data/");
            for(File f : folder.listFiles()) {
                Instance instance = Factory.fromFile(f.getPath(), Instance.class);
                System.out.println(instance.name);
                if(!new File("results/"+instance.name+".txt").exists()) {
                    RNMP rnmp = new RNMP(instance);
                    rnmp.solve(args[0]);
                }
            }
        }
    }
}
