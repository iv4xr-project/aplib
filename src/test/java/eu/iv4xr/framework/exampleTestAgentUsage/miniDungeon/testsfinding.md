### Bugs found by Unit Testing

* class ``Smeagol``, line ``"name = "Smaegol"``

* class `Monster`, line  ``hpMax = 20 ;
  hp = 3 ;``

* class `Maze`, the constructor, line ``w = new Wall(size - 1, 0, "")``

### Bugs found by System Testing

* class ``MiniDungeon``, method ``visibleTiles()``. Line ``else if (config.enableSmeagol && isVisible .. etc)``  should be ``if`` without ``else``. Nasty bug. Not found by unit test.

* class ``MiniDungeon``, method ``seedMaze(maze)``. The method does not reserve free-squares for placing players, or for teleporting.
The fix involves adding some bits of code for reserving this.
Without this, it is possible that an 'item' is overwritten by placing
a player ontop of it. Obviously, if this happens to be a scroll,
the it is not good. Hard to find bug, as it depends on randomness.

### Effort to write

* `UnitTestEntity` : 1h
* `UnitTestMaze` : 1h
* `UnitTestDungeon` : 1h
