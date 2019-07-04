/*
@author Arthur Godet <arth.godet@gmail.com>
@since 03/07/2019
*/
package model;

import data.input.Instance;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

public class PropChannelingRoadPerturbationEasy extends Propagator<IntVar> {
    private BoolVar[][] roadsPerturbation;
    private Task[] tasks;
    private Instance instance;

    public PropChannelingRoadPerturbationEasy(Instance instance, Task[] tasks, BoolVar[][] roadsPerturbation) {
        super(extractVars(tasks), PropagatorPriority.VERY_SLOW, false);
        this.instance = instance;
        this.tasks = tasks;
        this.roadsPerturbation = roadsPerturbation;
    }

    private static IntVar[] extractVars(Task[] tasks) {
        return Arrays.stream(tasks).map(Task::getStart).toArray(IntVar[]::new);
    }

    @Override
    public int getPropagationConditions(int idx) {
        return IntEventType.instantiation();
    }

    @Override
    public ESat isEntailed() {
        // TODO ???
        return ESat.TRUE;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int nbWorksheetsFixed = 0;
        for(int i = 0; i<tasks.length; i++) {
            if(tasks[i].getStart().isInstantiated()) {
                nbWorksheetsFixed++;
                int[] activities = instance.worksheets[i].roadsID;
                int startTime = tasks[i].getStart().getValue();
                for(int j = 0; j<activities.length; j++) {
                    roadsPerturbation[activities[j]][startTime+j].instantiateTo(1, this);
                }
            }
        }
        if(nbWorksheetsFixed == tasks.length) {
            for(int i = 0; i<roadsPerturbation.length; i++) {
                for(int j = 0; j<roadsPerturbation[i].length; j++) {
                    if(!roadsPerturbation[i][j].isInstantiated()) {
                        roadsPerturbation[i][j].instantiateTo(0, this);
                    }
                }
            }
        }
    }
}
