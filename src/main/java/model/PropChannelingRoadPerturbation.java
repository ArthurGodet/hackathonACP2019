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

public class PropChannelingRoadPerturbation extends Propagator<IntVar> {
    private BoolVar[][] roadsPerturbation;
    private Task[][] tasks;
    private BoolVar[] isDone;
    private Instance instance;

    public PropChannelingRoadPerturbation(Instance instance, Task[][] tasks, BoolVar[] isDone, BoolVar[][] roadsPerturbation) {
        super(extractVars(tasks, isDone), PropagatorPriority.VERY_SLOW, false);
        this.instance = instance;
        this.tasks = tasks;
        this.isDone = isDone;
        this.roadsPerturbation = roadsPerturbation;
    }

    private static IntVar[] extractVars(Task[][] tasks, BoolVar[] isDone) {
        return ArrayUtils.append(Arrays.stream(tasks).map(array -> array[0].getStart()).toArray(IntVar[]::new),
                isDone);
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
        for(int i = 0; i<isDone.length; i++) {
            if(isDone[i].isInstantiated() && tasks[i][0].getStart().isInstantiated()) {
                nbWorksheetsFixed++;
            }
            if(isDone[i].isInstantiatedTo(1) && tasks[i][0].getStart().isInstantiated()) {
                int[] activities = instance.worksheets[i].roadsID;
                int startTime = tasks[i][0].getStart().getValue();
                for(int j = 0; j<activities.length; j++) {
                    roadsPerturbation[activities[j]][startTime+j].instantiateTo(1, this);
                }
            }
        }
        if(nbWorksheetsFixed == isDone.length) {
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
