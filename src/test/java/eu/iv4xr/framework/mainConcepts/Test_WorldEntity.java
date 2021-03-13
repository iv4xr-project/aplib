package eu.iv4xr.framework.mainConcepts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Test_WorldEntity {

    WorldEntity door(String id) {
        WorldEntity e = new WorldEntity(id, "door", true);
        e.properties.put("isOpen", false);
        return e;
    }

    WorldEntity sword(String id) {
        WorldEntity e = new WorldEntity(id, "sword", false);
        return e;
    }

    WorldEntity bag(String id) {
        WorldEntity e = new WorldEntity(id, "bag", true);
        return e;
    }

    @Test
    public void test_hasSameState() {
        var door1 = door("d1");
        assertTrue(door1.hasSameState(door("d1")));

        door1.properties.put("isOpen", true);
        assertFalse(door1.hasSameState(door("d1")));

        var bag1 = bag("bag1");
        bag1.elements.put("excalibur", sword("excalibur"));
        var bag2 = bag("bag1");
        bag2.elements.put("excalibur", sword("excalibur"));
        assertTrue(bag1.hasSameState(bag2));

        bag2.elements.put("sting", sword("sting"));
        assertFalse(bag1.hasSameState(bag2));

    }

    @Test
    public void test_getProperty() {
        var door = door("d1");
        door.properties.put("price", 100);
        door.properties.put("inscription", "goaway");

        assertTrue(door.getBooleanProperty("isOpen") == false);
        assertTrue(door.getIntProperty("price") == 100);
        assertTrue(door.getStringProperty("inscription").equals("goaway"));
        assertTrue(door.getProperty("inscription").equals("goaway"));

        // getting the value of non-existing properties
        assertTrue(door.getBooleanProperty("xxx") == false);
        assertTrue(door.getStringProperty("xxx") == null);
        assertThrows(IllegalArgumentException.class, () -> door.getIntProperty("xxx"));
    }

    @Test
    public void test_statechange() {
        var doorold = door("d1");
        var door = door("d1");
        door.linkPreviousState(doorold);

        assertFalse(door.hasChangedState());

        door.properties.put("isOpen", true);

        assertTrue(door.hasChangedState());

    }

}
