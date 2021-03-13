package eu.iv4xr.framework.mainConcepts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Test_WorldModel {

    WorldEntity door(String id) {
        WorldEntity e = new WorldEntity(id, "door", true);
        e.properties.put("isOpen", false);
        return e;
    }

    WorldEntity sword(String id) {
        WorldEntity e = new WorldEntity(id, "sword", false);
        return e;
    }

    WorldModel world(long timestamp) {
        WorldModel wom = new WorldModel();
        wom.elements.put("d1", door("d1"));
        wom.elements.put("d2", door("d2"));
        wom.elements.put("excalibur", sword("excalibur"));
        wom.timestamp = timestamp;
        for (var e : wom.elements.values())
            e.assignTimeStamp(timestamp);
        return wom;
    }

    @Test
    public void test_mergeNewObservation() {
        // create a world model wom
        WorldModel wom = world(0);
        assertTrue(wom.getElement("d2").id.equals("d2"));
        assertTrue(wom.getElement("d2").type.equals("door"));

        // performs a merge with a new World Model with the same state;
        // no state change should be inferred:
        var changed = wom.mergeNewObservation(world(10));
        assertTrue(changed.isEmpty());

        // the time-stamp should however be updated to the latest one:
        assertTrue(wom.timestamp == 10);
        assertTrue(wom.getElement("d1").getBooleanProperty("isOpen") == false);
        assertTrue(wom.getElement("d1").timestamp == 10);

        // now merge a new World Model; in this one the door d2 has a different state,
        // and moreover, a new entity (sword) is also added:
        WorldModel wom2 = world(20);
        wom2.getElement("d2").properties.put("isOpen", true);
        wom2.elements.remove("excalibur");
        var sting = sword("sting");
        sting.assignTimeStamp(20);
        wom2.elements.put("sting", sting);

        // the merge should report two entities who state changes: the door d2 and the
        // newly
        // added sword:
        changed = wom.mergeNewObservation(wom2);
        assertTrue(changed.size() == 2);

        assertTrue(wom.timestamp == 20);

        // check the entities states and timestamp:
        assertTrue(wom.getElement("d1").getBooleanProperty("isOpen") == false);
        assertTrue(wom.getElement("d1").timestamp == 20);
        assertTrue(wom.getElement("d2").getBooleanProperty("isOpen") == true);
        assertTrue(wom.getElement("d2").timestamp == 20);
        assertTrue(wom.getElement("excalibur").timestamp == 10);
        assertTrue(wom.getElement("sting").type.equals("sword"));
        assertTrue(wom.getElement("sting").timestamp == 20);

        // check that "previous state" of entities are correctly added:
        assertTrue(wom.getElement("d1").getPreviousState() == null);
        assertTrue(wom.getElement("d2").getPreviousState().getBooleanProperty("isOpen") == false);

        // trying to merge with an older World Model should fail:
        assertThrows(IllegalArgumentException.class, () -> wom.mergeNewObservation(world(10)));
    }

    @Test
    public void test_mergeOldObservation() {
        // create a world model wom
        WorldModel wom = world(20);
        // trying to merge a newer observation should fail:
        assertThrows(IllegalArgumentException.class, () -> wom.mergeOldObservation(world(30)));

        // now merge a old observation; in this one the door d2 has a different state,
        // and moreover, a new entity (sword) is also added:
        WorldModel wom2 = world(10);
        wom2.getElement("d2").properties.put("isOpen", true);
        wom2.elements.remove("excalibur");
        var sting = sword("sting");
        sting.assignTimeStamp(10);
        wom2.elements.put("sting", sting);

        // the merge should report two entities who state changes: the door d2 and the
        // newly
        // added sword:
        wom.mergeOldObservation(wom2);

        for (var e : wom.elements.values()) {
            System.out.println("" + e.id + " t=" + e.timestamp);
        }

        assertTrue(wom.timestamp == 20);

        // check the entities states and timestamp:
        assertTrue(wom.getElement("d1").getBooleanProperty("isOpen") == false);
        assertTrue(wom.getElement("d1").timestamp == 20);
        assertTrue(wom.getElement("d2").getBooleanProperty("isOpen") == false);
        assertTrue(wom.getElement("d2").timestamp == 20);
        assertTrue(wom.getElement("excalibur").timestamp == 20);
        assertTrue(wom.getElement("sting").type.equals("sword"));
        assertTrue(wom.getElement("sting").timestamp == 10);

        // check that "previous state" of entities are correctly added:
        assertTrue(wom.getElement("d1").getPreviousState() == null);
        assertTrue(wom.getElement("d2").getPreviousState().getBooleanProperty("isOpen") == true);

    }

}
