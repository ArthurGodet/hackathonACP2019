/*
@author Arthur Godet <arth.godet@gmail.com>
@since 03/07/2019
*/
package data;

import data.input.Instance;
import data.input.RoadMaxBlock;
import data.input.Worksheet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Reader {

    public static Instance readInstance(String path) throws IOException {
        Instance instance = new Instance();
        File file = new File(path);
        instance.name = file.getName().substring(0, file.getName().length()-4);
        Scanner scanner = new Scanner(new FileReader(path));

        // Read instance info
        String[] line = scanner.nextLine().split(" ");
        instance.horizon = Integer.parseInt(line[0]);
        instance.roadsCost = new int[Integer.parseInt(line[1])][instance.horizon];
        instance.workCenters = new int[Integer.parseInt(line[2])];
        instance.worksheets = new Worksheet[Integer.parseInt(line[3])];
        instance.nbActivities = Integer.parseInt(line[4]);

        // Read road info
        for(int i = 0; i<instance.roadsCost.length; i++) {
            line = scanner.nextLine().split(" ");
            for(int j = 1; j<line.length; j++) {
                String[] roadsInfo = line[j].split(":");
                int cost = Integer.parseInt(roadsInfo[2]);
                for(int k = Integer.parseInt(roadsInfo[0]); k<Integer.parseInt(roadsInfo[1]); k++) {
                    instance.roadsCost[i][k] = cost;
                }
            }
        }

        // Read work centers info
        for(int i = 0; i<instance.workCenters.length; i++) {
            instance.workCenters[i] = Integer.parseInt(scanner.nextLine().split(" ")[1]);
        }

        // Read worksheets info
        for(int i = 0; i<instance.worksheets.length; i++) {
            line = scanner.nextLine().split(" ");
            Worksheet worksheet = new Worksheet();
            instance.worksheets[i] = worksheet;
            worksheet.id = Integer.parseInt(line[0]);
            worksheet.workCenterID = Integer.parseInt(line[1]);
            worksheet.mandatory = Integer.parseInt(line[2]);
            worksheet.importance = Integer.parseInt(line[3]);
            worksheet.est = Integer.parseInt(line[4]);
            worksheet.lst = Integer.parseInt(line[5]);
            worksheet.duration = Integer.parseInt(line[6]);
            worksheet.roadsID = new int[worksheet.duration];
            for(int j = 0; j<worksheet.duration; j++) {
                worksheet.roadsID[j] = Integer.parseInt(line[7+j]);
            }
            worksheet.amountOfWorkers = new int[worksheet.duration];
            for(int j = 0; j<worksheet.duration; j++) {
                worksheet.amountOfWorkers[j] = Integer.parseInt(line[7+worksheet.duration+j]);
            }
        }

        // Read road block
        ArrayList<int[]> precedences = new ArrayList<>();
        ArrayList<RoadMaxBlock> roadsBlocked = new ArrayList<>();
        while(scanner.hasNextLine()) {
            line = scanner.nextLine().split(" ");
            if("M".equals(line[0])) {
                RoadMaxBlock rmb = new RoadMaxBlock();
                roadsBlocked.add(rmb);
                rmb.nbMaxBlocked = Integer.parseInt(line[1]);
                rmb.roadsID = new int[line.length-2];
                for(int k = 2; k<line.length; k++) {
                    rmb.roadsID[k-2] = Integer.parseInt(line[k]);
                }
            } else if("P".equals(line[0])) {
                int[] pred = new int[2];
                precedences.add(pred);
                pred[0] = Integer.parseInt(line[1]);
                pred[1] = Integer.parseInt(line[2]);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        instance.roadsBlocked = roadsBlocked.toArray(new RoadMaxBlock[0]);
        instance.precedences = new int[precedences.size()][];
        for(int i = 0; i<precedences.size(); i++) {
            instance.precedences[i] = precedences.get(i);
        }

        scanner.close();
        return instance;
    }

    public static void main(String[] args) throws IOException {
        File folder = new File("data/Instances");
        for(File f : folder.listFiles()) {
            System.out.println(f.getName());
            Instance inst = readInstance(f.getPath());
            Factory.toFile("data/"+inst.name+".json", inst);
        }
    }
}
