package model;

import org.chocosolver.solver.Model;
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
	BoolVar[][] roadPerturbation; // roadPertubation[roads][time]
	
	// OBJECTIF
	IntVar obj;

	// TASKS
	Task[][] tasks;
	

	public RNMP() {

		model = new Model("RNMP");

		makeObj();

	}
	
	public IntVar getStartWorksheet(int i) {
		return tasks[i][0].getStart();
	}
	
	public IntVar getEndWorksheet(int i) {
		return tasks[i][tasks.length-1].getEnd();
	}
	
	public void makeObj() {
		// TODO
	} 

}
