package model;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import data.input.RoadMaxBlock;
import data.input.Worksheet;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.util.tools.ArrayUtils;

import data.input.Instance;

public class RNMP {

	// INSTANCE
	Instance instance;
	
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
		model = new Model("RNMP");

		makeTasksAndIsDone();
		makePerturbation();

		makePrecedences();
		constraintWorkRessources();

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

	public void makeTasksAndIsDone() {
		tasks = new Task[instance.worksheets.length][];
		isDone = model.boolVarArray("isDone", instance.worksheets.length);
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = new Task[instance.worksheets[i].duration]; // TODO can be
																	// improved
																	// if all
																	// conso are
																	// identical
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
		IntVar[][] perturbations = new IntVar[instance.roadsCost.length][instance.horizon];
		for(int i = 0; i<instance.roadsCost.length; i++) {
			for(int j = 0; j<instance.roadsCost[i].length; j++) {
				perturbations[i][j] = model.intVar("perturbations["+i+"]["+j+"]", new int[]{0, instance.roadsCost[i][j]});
				model.times(roadsPerturbation[i][j], instance.roadsCost[i][j], perturbations[i][j]).post();
			}
		}
		IntVar[] sumPerturbation = model.intVarArray("sumPerturbation", instance.horizon, 0, -min);
		for(int t = 0; t<instance.horizon; t++) {
			IntVar[] tmp = new IntVar[instance.roadsCost.length];
			for(int i = 0; i<tmp.length; i++) {
				tmp[i] = perturbations[i][t];
			}
			model.sum(tmp, "=", sumPerturbation[t]).post();
		}
		model.max(maxPerturbation, sumPerturbation).post();

		model.arithm(sumUrgency, "-", maxPerturbation, "=", obj).post();

		model.setObjective(true, obj);
	}

	boolean isDoneVar = false;
	Integer idx = null;

	public void makeSearch() {
		IntVar[] decVars = new IntVar[2*isDone.length];
		for(int i = 0; i<decVars.length; i+=2) {
			decVars[i] = isDone[i/2];
			decVars[i+1] = getStartWorksheet(i/2);
		}
//		model.getSolver().setSearch(Search.inputOrderLBSearch(decVars));
		//*
		// TODO --> select first isDone for which importance is higher and instantiate to UB, then corresponding start and instantiate to LB
		model.getSolver().setSearch(Search.intVarSearch(new VariableSelector<IntVar>() {
			@Override
			public IntVar getVariable(IntVar[] variables) {
				if(idx != null) {
					return getStartWorksheet(idx);
				} else {
					isDoneVar = true;
					IntVar res = null;
					int bestImportance = Integer.MIN_VALUE;
					for(int i = 0; i<instance.worksheets.length; i++) {
						if(!isDone[i].isInstantiated() && (res==null || instance.worksheets[i].importance > bestImportance)) {
							bestImportance = instance.worksheets[i].importance;
							res = isDone[i];
							idx = i;
						}
					}
					if(idx == null) {
						isDoneVar = false;
						for(int i = 0; i<tasks.length; i++) {
							if(!getStartWorksheet(i).isInstantiated()) {
								return getStartWorksheet(i);
							}
						}
					}
					return res;
				}
			}
		}, new IntValueSelector() {
			@Override
			public int selectValue(IntVar var) {
				if (isDoneVar) {
					isDoneVar = false;
					return var.getUB();
				} else {
					idx = null;
					return var.getLB();
				}
			}
		}, decVars));
		//*/
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
					new PropPrecedences(isDone[i1], getEndWorksheet(i1), isDone[i2], getStartWorksheet(i2))));
		}
	}

	public void solve(String timeLimit) throws IOException {
		if(timeLimit != null) {
			model.getSolver().limitTime(timeLimit);
		}
		int[] best = null;

		while(model.getSolver().solve()) {
			best = Arrays.stream(tasks).map(array -> array[0].getStart()).mapToInt(IntVar::getValue).toArray();
			System.out.println(obj.getValue()+" : "+Arrays.toString(best));
		}

		FileWriter fw = new FileWriter("results/"+instance.name+".txt");
		if(best != null) {
			for(int i = 0; i<best.length; i++) {
				fw.write(i+" "+best[i]+"\n");
			}
		}
		fw.close();
	}
}
