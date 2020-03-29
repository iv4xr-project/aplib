package eu.iv4xr.framework.extensions.spatial;

import java.util.ArrayList;
import eu.iv4xr.framework.extensions.spatial.meshes.*;

/**
 * A simple container to store meshdata.
 * 
 * @author Naraenda
 */
public class Mesh {
    public ArrayList<Vec3> vertices;
    public ArrayList<Edge> edges;
    public ArrayList<Face> faces;
}
