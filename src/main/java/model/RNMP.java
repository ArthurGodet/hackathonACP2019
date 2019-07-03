package model;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

import data.input.Instance;

public class RNMP {

	// INSTANCE
	Instance instance;
	
	// MODEL
	Model model;

	// VARIABLESt
	BoolVar[] isDone; // g[w]=1 iff w is done
	BoolVar[][] roadsPerturbation; // roadsPertubation[roads][time]
	
	// OBJECTIF
	IntVar obj;

	// TASKS
	Task[][] tasks;
	

	public RNMP() {
		model = new Model("RNMP");

		makeTasksAndIsDone();
		makePerturbation();

		makeObj();

	}
	
	public IntVar getStartWorksheet(int i) {
		return tasks[i][0].getStart();
	}
	
	public IntVar getEndWorksheet(int i) {
		return tasks[i][tasks.length-1].getEnd();
	}

	public void makePerturbation() {
		roadsPerturbation = model.boolVarMatrix("roadsPerturbation", instance.roadsCost.length, instance.horizon);
		model.post(new Constraint("CHANNELING_CONSTRAINT", new PropChannelingRoadPerturbation(instance, tasks, isDone, roadsPerturbation)));
	}

	public void makeTasksAndIsDone() {
		tasks = new Task[instance.worksheets.length][];
		isDone = model.boolVarArray("isDone", instance.worksheets.length);
		for(int i = 0; i<tasks.length; i++) {
			tasks[i] = new Task[instance.worksheets[i].duration]; // TODO can be improved if all conso are identical
			for(int j = 0; j<tasks[i].length; j++) {
				IntVar start = model.intVar("start["+i+"]["+j+"]", 0, instance.horizon);
				IntVar end = model.intVar("end["+i+"]["+j+"]", 0, instance.horizon);
				tasks[i][j] = new Task(start, isDone[i], end);
				if(j>0) {
					model.arithm(tasks[i][j-1].getEnd(), "=", tasks[i][j].getStart()).post();
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

}
