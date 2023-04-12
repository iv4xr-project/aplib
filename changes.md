# Changes

### 1.3.0 (with respect to 1.2.9)

* Class **Environment** The APIs for `Environment` (in `uu.cs.aplib.mainConcepts`) are simplified. These are dropped:

    * `refresh()` and `refreshWorker()`

  This is added: `observe(agentId)`. Implementors must now implement this method as well for their own concrete implementation of `Environment`. See the class' documentation.

* Class **SimpleState**  (in `uu.cs.aplib.mainConcepts`). The signature of the method `updateState()` is changed to `updateState(agentId)`. This affects all sub-classes of `SimpleState`. 
