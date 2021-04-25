# CS448 - Project 3

## What we have implemented
- Changed the original `LockTable` to abstract, this will serve as the base class for all the other implementations
- Changed the implementation of `LockTable`
    - Instead of a `HashMap<BlockId, Integer> locks`, we changed it to a `HashMap<BlockId, List<Transaction>> locks`
    - For a particular graph, `locks.get(blk)` returns a list of transactions that are currently holding a lock on the block
    - We keep count of the lock type on a particular block by storing it in another hash map `HashMap<BlockId, Integer>`
        - `0`: no locks are on this block
        - `1`: one or more S locks on this block
        - `-1`: one X lock on this block
    
## Tests
- all tests are under `simpledb/tx/`
### `WaitDieTest`
- This test is for testing the correctness of the different implementations
- Changes made
    - Made the transaction rollback if an `InterruptedException` is thrown, this is to accomodate for wound wait, as that interrupts a thread instead of throwing a custom exception
    - Made the test delete the "waitdietest" folder before running
- How to run this test
    - change line 38 to the implementation that needs to be tested
    

### `DeadlockQueryTest`
- This test is designed to break the timeout based implementation
- What it does
    - It creates a deadlock, which for a timeout, would take a lot of time to resolve
    - This also automatically delete the folder it created at the beginning of the program
- How to run this test
    - change line 36 to the implementation that needs to be tested
    
### `DeadlockQueryComparison`
- this is for generating the test output of option 1
- how to run
    - call test1()~test7() in the main function to generate the output files

### `GeneralComparison`
- this is for generating the test output of task 5
- how to run
    - call test1()~test4() in tthe main function