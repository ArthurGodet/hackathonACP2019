/*
@author Arthur Godet <arth.godet@gmail.com>
@since 04/07/2019
*/
package bench;

import data.Factory;
import data.input.Instance;
import model.RNMP;
import model.RNMPEasy;
import org.chocosolver.solver.exception.ContradictionException;

import java.io.IOException;

public class ReadBestValues {
    public static final String[] difficulties = new String[]{"EASY", "MEDIUM", "HARD"};
    public static final String[] values = new String[]{"5_3", "200_50", "1000_100", "2000_500", "2500_1000", "5000_1500"};

    public static void main(String[] args) throws IOException, ContradictionException {
        for(String diff : difficulties) {
            System.out.println(diff);
            StringBuilder sb = new StringBuilder();
            for(String v : values) {
                Instance instance = Factory.fromFile("data/"+diff+"_"+v+".json", Instance.class);
                int bestObjValue = ("EASY".equals(diff) ? RNMPEasy.computeObjectiveOfSolution(instance, "results/"+instance.name+".txt")
                        : RNMP.computeObjectiveOfSolution(instance, "results/"+instance.name+".txt"));
//                System.out.println(instance.name+" : "+bestObjValue);
                sb.append(bestObjValue).append(" ");
            }
            System.out.println(sb.toString());
        }
    }
}
