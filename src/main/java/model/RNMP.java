package model;

import java.util.Arrays;

import org.chocosolver.solver.Model;
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
	BoolVar[][] roadPerturbation; // roadPertubation[roads][time]

	// OBJECTIF
	IntVar obj;

	// TASKS
	Task[][] tasks;

	public RNMP() {
		model = new Model("RNMP");

		makeTasksAndIsDone();

		makeObj();
		makePrecedences();
		constraintWorkRessources();
		

	}

	public IntVar getStartWorksheet(int i) {
		return tasks[i][0].getStart();
	}

	public IntVar getEndWorksheet(int i) {
		return tasks[i][tasks.length - 1].getEnd();
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
			model.arithm(isDone[i], "=", instance.worksheets[i].mandatory).post(); // mandatory

			model.arithm(getStartWorksheet(i), "<=", isDone[i].mul(instance.horizon).intVar()).post();
		}

	}

	public void makeObj() {
		// TODO

	}

	public void constraintWorkRessources() {

		// A cumulative for each work centers
		for (int center = 0; center < instance.workCenters.length; center++) {

			// We put every tasks of every concerned worksheets in one table
			Task[] tasksCenter = new Task[instance.nbActivities];
			// Idem for the heights
			IntVar[] heights = new IntVar[tasksCenter.length];
			int idx = 0;
			for (int w = 0; w < tasks.length; w++) {
				if (instance.worksheets[w].workCenterID == center) {
					tasksCenter = ArrayUtils.append(tasksCenter, tasks[w]);
					for (int a = 0; a < tasks[w].length; a++) {

						// Amount of workers needed for tasks[w][a]
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
			model.arithm(getEndWorksheet(instance.precedences[i][0]), "<=",
					getStartWorksheet(instance.precedences[i][1])).post();
		}
	}

}
