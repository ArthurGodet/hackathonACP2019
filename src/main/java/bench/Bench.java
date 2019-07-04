/*
@author Arthur Godet <arth.godet@gmail.com>
@since 07/03/2019
*/
package bench;

import data.Factory;
import data.input.Instance;
import model.RNMP;
import model.RNMPEasy;
import org.chocosolver.solver.exception.ContradictionException;

import java.io.File;
import java.io.IOException;

public class Bench {

    public static void main(String[] args) throws IOException, ContradictionException {
        if(args.length == 3) {
            if(args[0].equals("-lns")) {
                Instance instance = Factory.fromFile("data/"+args[2]+".json", Instance.class);
                System.out.println(instance.name);
                if(instance.name.contains("EASY")) {
                    RNMPEasy rnmpEasy = new RNMPEasy(instance);
                    rnmpEasy.lnsSolve(args[1]);
                } else {
                    RNMP rnmp = new RNMP(instance);
                    rnmp.lnsSolve(args[1]);
                }
            } else {
                throw new UnsupportedOperationException("if args has size 3, then args[0] should be -lns");
            }
        } else if(!"all".equals(args[1])) {
            Instance instance = Factory.fromFile("data/"+args[1]+".json", Instance.class);
            System.out.println(instance.name);
            if(instance.name.contains("EASY")) {
                RNMPEasy rnmpEasy = new RNMPEasy(instance);
                rnmpEasy.solve(args[0]);
            } else {
                RNMP rnmp = new RNMP(instance);
                rnmp.solve(args[0]);
            }
        } else {
            File folder = new File("data/");
            for(File f : folder.listFiles()) {
                Instance instance = Factory.fromFile(f.getPath(), Instance.class);
                System.out.println(instance.name);
                RNMP rnmp = new RNMP(instance);
                rnmp.solve(args[0]);
            }
        }
    }
}
