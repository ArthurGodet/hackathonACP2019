/*
@author Arthur Godet <arth.godet@gmail.com>
@since 03/07/2019
*/
package model;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

public class PropPrecedences extends Propagator<IntVar> {
    private BoolVar isDone1;
    private BoolVar isDone2;
    private IntVar start1, start2, end1, end2;

    public PropPrecedences(BoolVar isDone1, IntVar start1, IntVar end1, BoolVar isDone2, IntVar start2, IntVar end2) {
        super(new IntVar[]{isDone1, isDone2, end1, start2}, PropagatorPriority.BINARY, false);
        this.isDone1 = isDone1;
        this.isDone2 = isDone2;
        this.start1 = start1;
        this.start2 = start2;
        this.end1 = end1;
        this.end2 = end2;
    }

    @Override
    public ESat isEntailed() {
        // TODO ???
        return ESat.TRUE;
    }

    @Override
    public int getPropagationConditions(int idx) {
        return IntEventType.instantiation();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if(isDone1.getLB()+isDone2.getLB()>=2) {
            start2.updateLowerBound(end1.getLB(), this); // ect1 <= est2
            if(end1.isInstantiated()) {
                start2.updateLowerBound(end1.getValue(), this);
            } else if(start2.isInstantiated()) {
                end1.updateUpperBound(start2.getValue(), this);
            }
        } else if(start1.getLB()>end2.getUB()) {
            if(isDone1.isInstantiatedTo(1)) {
                isDone2.instantiateTo(0, this);
            } else if(isDone2.isInstantiatedTo(1)) {
                isDone1.instantiateTo(0, this);
            }
        }
    }
}
