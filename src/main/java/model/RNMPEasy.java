package model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import data.Factory;
import data.input.Worksheet;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.util.tools.ArrayUtils;

import data.input.Instance;

public class RNMPEasy {
    // INSTANCE
    Instance instance;

    // MODEL
    Model model;

    // VARIABLES
    BoolVar[][] roadsPerturbation; // roadPertubation[roads][time]

    // OBJECTIF
    IntVar obj;

    // TASKS
    Task[] tasks;


    public RNMPEasy(Instance instance) {
        this.instance = instance;
        model = new Model("RNMPEasy");

        makeTasksAndIsDone();
        makePerturbation();

        makePrecedences();

        makeObj();
        makeSearch();
    }

    public IntVar getStartWorksheet(int i) {
        return tasks[i].getStart();
    }

    public IntVar getEndWorksheet(int i) {
        return tasks[i].getEnd();
    }

    public void makePerturbation() {
        roadsPerturbation = model.boolVarMatrix("roadsPerturbation", instance.roadsCost.length, instance.horizon);
        model.post(new Constraint("CHANNELING_CONSTRAINT", new PropChannelingRoadPerturbationEasy(instance, tasks, roadsPerturbation)));
    }

    public void makeTasksAndIsDone() {
        tasks = new Task[instance.worksheets.length];
        IntVar[] starts = model.intVarArray("start", tasks.length, 0, instance.horizon);
        for (int i = 0; i < tasks.length; i++) {
            model.arithm(starts[i], ">=", instance.worksheets[i].est).post(); // est
            model.arithm(starts[i], "<=", instance.worksheets[i].lst).post(); // lst
            tasks[i] = new Task(starts[i], model.intVar(instance.worksheets[i].duration),
                    starts[i].add(instance.worksheets[i].duration).intVar());
        }

    }

    public void makeObj() {
        int min = 0;
        int sum = 0;
        for(Worksheet ws : instance.worksheets) {
            sum += ws.importance;
        }
        for(int t = 0; t<instance.horizon; t++) {
            int a = 0;
            for(int i = 0; i<instance.roadsCost.length; i++) {
                a -= instance.roadsCost[i][t];
            }
            min = Math.min(min, a);
        }

        obj = model.intVar("obj", min, sum);

        IntVar maxPerturbation = model.intVar("maxPerturbation", 0, -min);
        IntVar[] sumPerturbation = model.intVarArray("sumPerturbation", instance.horizon, 0, -min);
        for(int j = 0; j<sumPerturbation.length; j++) {
            int[] coefs = new int[roadsPerturbation.length];
            IntVar[] tmp = new IntVar[roadsPerturbation.length];
            for(int i = 0; i<roadsPerturbation.length; i++) {
                coefs[i] = instance.roadsCost[i][j];
                tmp[i] = roadsPerturbation[i][j];
            }
            model.scalar(tmp, coefs, "=", sumPerturbation[j]).post();
        }
        model.max(maxPerturbation, sumPerturbation).post();

        model.arithm(obj, "+", maxPerturbation, "=", sum).post();

        model.setObjective(true, obj);
    }

    public void makeSearch() {
        IntVar[] decVars = Arrays.stream(tasks).map(t -> t.getStart()).toArray(IntVar[]::new);

		/*
		model.getSolver().setSearch(Search.inputOrderUBSearch(decVars));
		//*/
        int[] nbPrecedences = new int[tasks.length];
        ArrayList<Integer>[] prec = new ArrayList[tasks.length];
        for(int k = 0; k<tasks.length; k++) {
            prec[k] = new ArrayList<>();
        }
        for(int i = 0; i<instance.precedences.length; i++) {
            prec[instance.precedences[i][1]].add(instance.precedences[i][0]);
        }
        for(int i = 0; i<nbPrecedences.length; i++) {
            nbPrecedences[i] = computeNbPrec(prec, i);
        }
        model.getSolver().setSearch(Search.intVarSearch(new VariableSelector<IntVar>() {
            @Override
            public IntVar getVariable(IntVar[] variables) {
                int best = -1;
                for(int i = 0; i<tasks.length; i++) {
                    if(!getStartWorksheet(i).isInstantiated() && (best==-1 || nbPrecedences[best]<nbPrecedences[i])) {
                        best = i;
                    }
                }
                if(best == -1) {
                    return null;
                } else {
                    return getStartWorksheet(best);
                }
            }
        }, new IntValueSelector() {
            @Override
            public int selectValue(IntVar var) {
                int id = -1;
                for(int i = 0; i<nbPrecedences.length; i++) {
                    if(getStartWorksheet(i).equals(var)) {
                        id = i;
                        break;
                    }
                }
//                if(nbPrecedences[id] > 0) {
//                    return var.getUB();
//                } else {
                    int bestStart = -1;
                    int lessInc = Integer.MAX_VALUE;
                    for(int t = var.getLB(); t<=var.getUB(); t++) {
                        int inc = computeIncreasePerturbation(id, t);
                        if(inc < lessInc) {
                            lessInc = inc;
                            bestStart = t;
                        }
                    }
                    return bestStart;
//                }
            }
        }, decVars));
    }

    private int computeIncreasePerturbation(int id, int start) {
        int inc = 0;
        for(int k = 0; k<instance.worksheets[id].duration; k++) {
            int idRoad = instance.worksheets[id].roadsID[k];
            if(!roadsPerturbation[idRoad][start+k].isInstantiated()) {
                inc += instance.roadsCost[idRoad][start+k];
            }
        }
        return inc;
    }

    private static int computeNbPrec(ArrayList<Integer>[] prec, int i) {
        int sum = 0;
        for(int p : prec[i]) {
            sum += 1+computeNbPrec(prec, p);
        }
        return sum;
    }

    public void makePrecedences() {
        for (int i = 0; i < instance.precedences.length; ++i) {
            int i1 = instance.precedences[i][0];
            int i2 = instance.precedences[i][1];
            model.arithm(getEndWorksheet(i1), "<=", getStartWorksheet(i2)).post();
        }
    }

    public void solve(String timeLimit) throws IOException, ContradictionException {
        if(timeLimit != null) {
            model.getSolver().limitTime(timeLimit);
        }
        int[][] best = null;
        Integer bestObj = 0;

        while(model.getSolver().solve()) {
            bestObj = obj.getValue();
            best = new int[tasks.length][2];
            for(int i = 0; i<best.length; i++) {
                best[i][0] = i;
                best[i][1] = getStartWorksheet(i).getValue();
            }
            System.out.println(instance.name+" -> "+obj.getValue()+" : "+Arrays.deepToString(best));
            if(computeObjectiveOfSolution(instance, "results/"+instance.name+".txt")<bestObj) {
                FileWriter fw = new FileWriter("results/"+instance.name+".txt");
                for(int i = 0; i<best.length; i++) {
                    fw.write(best[i][0]+" "+best[i][1]+" "+"\n");
                }
                fw.close();
            }
        }

        model.getSolver().printStatistics();
    }


    public static int computeObjectiveOfSolution(Instance instance, String path) throws IOException, ContradictionException {
        RNMP rnmp = new RNMP(instance);
        Scanner scanner = new Scanner(new FileReader(path));
        while(scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split(" ");
            int id = Integer.parseInt(line[0]);
            int start = Integer.parseInt(line[1]);
            rnmp.isDone[id].instantiateTo(1, Cause.Null);
            rnmp.getStartWorksheet(id).instantiateTo(start, Cause.Null);
        }
        scanner.close();
        for(int i = 0; i<rnmp.isDone.length; i++) {
            if(!rnmp.isDone[i].isInstantiated()) {
                rnmp.isDone[i].instantiateTo(0, Cause.Null);
            }
        }
        rnmp.model.getSolver().propagate();
        return rnmp.obj.getValue();
    }

    public static void main(String[] args) throws IOException, ContradictionException{
        String instanceName = "HARD_5000_1500";
        Instance instance = Factory.fromFile("data/"+instanceName+".json", Instance.class);
        int obj = computeObjectiveOfSolution(instance, "results/"+instanceName+".txt");
        System.out.println(obj);
    }
}
