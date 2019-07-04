package model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import data.Factory;
import data.input.RoadMaxBlock;
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

public class RNMP {
	public static final int EASY = 0;
	public static final int MEDIUM = 1;
	public static final int HARD = 2;

	// INSTANCE
	Instance instance;
	int difficulty;
	
	// MODEL
	Model model;

	// VARIABLESt
	BoolVar[] isDone; // g[w]=1 iff w is done
	BoolVar[][] roadsPerturbation; // roadPertubation[roads][time]
	
	// OBJECTIF
	IntVar obj;

	// TASKS
	Task[][] tasks;
	

	public RNMP(Instance instance) {
		this.instance = instance;
		if(instance.name.contains("EASY")) {
			difficulty = EASY;
		} else if(instance.name.contains("MEDIUM")) {
			difficulty = MEDIUM;
		} else {
			difficulty = HARD;
		}
		model = new Model("RNMP");

		makeTasksAndIsDone();
		makePerturbation();

		makePrecedences();
		if(difficulty != EASY) {
			constraintWorkRessources();
		}

		makeObj();
		makeSearch();
	}
	
	public IntVar getStartWorksheet(int i) {
		return tasks[i][0].getStart();
	}
	
	public IntVar getEndWorksheet(int i) {
		return tasks[i][tasks[i].length - 1].getEnd();
	}

	public void makePerturbation() {
		roadsPerturbation = model.boolVarMatrix("roadsPerturbation", instance.roadsCost.length, instance.horizon);
		model.post(new Constraint("CHANNELING_CONSTRAINT", new PropChannelingRoadPerturbation(instance, tasks, isDone, roadsPerturbation)));

		if(difficulty == HARD) {
			BoolVar[] roadsBlockedMax;
			for(RoadMaxBlock rmb : instance.roadsBlocked) {
				roadsBlockedMax = new BoolVar[rmb.roadsID.length];
				for(int t = 0; t<instance.horizon; t++) {
					for(int k = 0; k<roadsBlockedMax.length; k++) {
						roadsBlockedMax[k] = roadsPerturbation[rmb.roadsID[k]][t];
					}
					model.sum(roadsBlockedMax, "<=", rmb.nbMaxBlocked).post();
				}
			}
		}
	}

	public void makeTasksAndIsDone() {
		tasks = new Task[instance.worksheets.length][];
		isDone = model.boolVarArray("isDone", instance.worksheets.length);
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = new Task[instance.worksheets[i].duration];
			for (int j = 0; j < tasks[i].length; j++) {
				IntVar start = model.intVar("start[" + i + "][" + j + "]", 0, instance.horizon);
				IntVar end = model.intVar("end[" + i + "][" + j + "]", 0, instance.horizon);
				tasks[i][j] = new Task(start, isDone[i], end);
				if (j > 0) {
					model.arithm(tasks[i][j - 1].getEnd(), "=", tasks[i][j].getStart()).post();
				}
			}
			model.arithm(getStartWorksheet(i), ">=", instance.worksheets[i].est).post(); // est
			model.arithm(getStartWorksheet(i), "<=", instance.worksheets[i].lst).post(); // lst
			model.arithm(isDone[i], ">=", instance.worksheets[i].mandatory).post(); // mandatory

			model.arithm(getStartWorksheet(i), "-", isDone[i].mul(instance.horizon).intVar(), "<=", instance.worksheets[i].est).post();
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

		IntVar sumUrgency = model.intVar("maxUrgency", 0, sum);
		int[] importances = Arrays.stream(instance.worksheets).mapToInt(ws -> ws.importance).toArray();
		model.scalar(isDone, importances, "=", sumUrgency).post();

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

		model.arithm(sumUrgency, "-", maxPerturbation, "=", obj).post();

		model.setObjective(true, obj);
	}

	public void makeSearch() {
		IntVar[] decVars = new IntVar[2*isDone.length];
		for(int i = 0; i<decVars.length; i+=2) {
			decVars[i] = isDone[i/2];
			decVars[i+1] = getStartWorksheet(i/2);
		}

		/*
		model.getSolver().setSearch(Search.inputOrderUBSearch(decVars));
		//*/
		/* TO ENSURE TO HAVE A SOLUTION
		model.getSolver().setSearch(Search.intVarSearch(new VariableSelector<IntVar>() {
			@Override
			public IntVar getVariable(IntVar[] variables) {
				for(int i = 0; i<isDone.length; i++) {
					if(isDone[i].isInstantiatedTo(1) && !getStartWorksheet(i).isInstantiated()) {
						return getStartWorksheet(i);
					}
				}
				for(int i = 0; i<isDone.length; i++) {
					if(!isDone[i].isInstantiated()) {
						return isDone[i];
					}
				}
				for(int i = 0; i<isDone.length; i++) {
					if(!getStartWorksheet(i).isInstantiated()) {
						return getStartWorksheet(i);
					}
				}
				return null;
			}
		}, new IntValueSelector() {
			@Override
			public int selectValue(IntVar var) {
				return var.getLB();
			}
		}, decVars));
		//*/
		//* HOME-MADE SEARCH
		model.getSolver().setSearch(Search.intVarSearch(new VariableSelector<IntVar>() {
			@Override
			public IntVar getVariable(IntVar[] variables) {
				for(int i = 0; i<isDone.length; i++) {
					if(isDone[i].isInstantiatedTo(1) && !getStartWorksheet(i).isInstantiated()) {
						return getStartWorksheet(i);
					}
				}
				Integer bestImportance = null;
				Integer idx = null;
				for(int i = 0; i<isDone.length; i++) {
					if(!isDone[i].isInstantiated() && (idx==null || bestImportance<instance.worksheets[i].importance)) {
						bestImportance = instance.worksheets[i].importance;
						idx = i;
					}
				}
				if(idx != null) {
					return isDone[idx];
				}
				for(int i = 0; i<isDone.length; i++) {
					if(!getStartWorksheet(i).isInstantiated()) {
						return getStartWorksheet(i);
					}
				}
				return null;
			}
		}, new IntValueSelector() {
			@Override
			public int selectValue(IntVar var) {
				return var.getUB();
			}
		}, decVars));
		//*/
		//* SEARCH FOR PRECEDENCES
		if(difficulty == EASY) {
			int[] nbPrecedences = new int[isDone.length];
			ArrayList<Integer>[] prec = new ArrayList[isDone.length];
			for(int k = 0; k<isDone.length; k++) {
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
					for(int i = 0; i<isDone.length; i++) {
						if(!getStartWorksheet(i).isInstantiated() && (best==-1 || best<nbPrecedences[i])) {
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
					return var.getUB();
				}
			}, decVars));
		}
		//*/
	}

	private static int computeNbPrec(ArrayList<Integer>[] prec, int i) {
		int sum = 0;
		for(int p : prec[i]) {
			sum += 1+computeNbPrec(prec, p);
		}
		return sum;
	}

	public void constraintWorkRessources() {
		// A cumulative for each work centers
		for (int center = 0; center < instance.workCenters.length; center++) {
			// We put every tasks of every concerned worksheets in one table
			int size = 0;
			for(int i = 0; i<tasks.length; i++) {
				if(instance.worksheets[i].workCenterID == center) {
					size += tasks[i].length;
				}
			}
			Task[] tasksCenter = new Task[size];
			// Idem for the heights
			IntVar[] heights = new IntVar[tasksCenter.length];
			int idx = 0;
			for (int w = 0; w < tasks.length; w++) {
				if (instance.worksheets[w].workCenterID == center) {
					for (int a = 0; a < tasks[w].length; a++) {
						// Amount of workers needed for tasks[w][a]
						tasksCenter[idx] = tasks[w][a];
						heights[idx] = model.intVar(instance.worksheets[w].amountOfWorkers[a]);
						idx++;
					}
				}
			}
			// Capacity of the work center
			IntVar cap = model.intVar(instance.workCenters[center]);
			model.cumulative(tasksCenter, heights, cap).post();
		}
	}

	public void makePrecedences() {
		for (int i = 0; i < instance.precedences.length; ++i) {
			int i1 = instance.precedences[i][0];
			int i2 = instance.precedences[i][1];
			model.post(new Constraint("PRECEDENCES_CONSTRAINT",
					new PropPrecedences(isDone[i1], getStartWorksheet(i1), getEndWorksheet(i1),
							isDone[i2], getStartWorksheet(i2), getEndWorksheet(i2))));
		}
	}

	public void solve(String timeLimit) throws IOException, ContradictionException {
		if(timeLimit != null) {
			model.getSolver().limitTime(timeLimit);
		}
		int[][] best = null;
		Integer bestObj = 0;

		while(model.getSolver().solve()) {
			int nbDone = (int) Arrays.stream(isDone).filter(b -> b.isInstantiatedTo(1)).count();
			bestObj = obj.getValue();
			best = new int[nbDone][2];
			int k = 0;
			for(int i = 0; i<isDone.length; i++) {
				if(isDone[i].isInstantiatedTo(1)) {
					best[k][0] = i;
					best[k][1] = getStartWorksheet(i).getValue();
					k++;
				}
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
