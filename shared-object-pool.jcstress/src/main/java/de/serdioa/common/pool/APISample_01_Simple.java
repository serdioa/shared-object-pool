package de.serdioa.common.pool;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

/*
    This is our first concurrency test. It is deliberately simplistic to show
    testing approaches, introduce JCStress APIs, etc.

    Suppose we want to see if the field increment is atomic. We can make test
    with two actors, both actors incrementing the field and recording what
    value they observed into the result object. As JCStress runs, it will
    invoke these methods on the objects holding the field once per each actor
    and instance, and record what results are coming from there.

    Done enough times, we will get the history of observed results, and that
    would tell us something about the concurrent behavior. For example, running
    this test would yield:

          [OK] o.o.j.t.JCStressSample_01_Simple
        (JVM args: [-server])
      Observed state   Occurrences   Expectation  Interpretation
                1, 1    54,734,140    ACCEPTABLE  Both threads came up with the same value: atomicity failure.
                1, 2    47,037,891    ACCEPTABLE  actor1 incremented, then actor2.
                2, 1    53,204,629    ACCEPTABLE  actor2 incremented, then actor1.

     How to run this test:
       $ java -jar jcstress-samples/target/jcstress.jar -t JCStress_APISample_01_Simple
 */

// Mark the class as JCStress test.
@JCStressTest

// These are the test outcomes.
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Both actors came up with the same value: atomicity failure.")
@Outcome(id = "1, 2", expect = Expect.ACCEPTABLE, desc = "actor1 incremented, then actor2.")
@Outcome(id = "2, 1", expect = Expect.ACCEPTABLE, desc = "actor2 incremented, then actor1.")

// This is a state object
@State
public class APISample_01_Simple {

    int v;

    @Actor
    public void actor1(II_Result r) {
        r.r1 = ++v; // record result from actor1 to field r1
    }

    @Actor
    public void actor2(II_Result r) {
        r.r2 = ++v; // record result from actor2 to field r2
    }

}