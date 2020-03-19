package eu.iv4xr.framework.extensions.spatial.meshes;

import java.util.Arrays;
import java.util.Iterator;

public class Face implements Iterable<Integer> {
    public int[] vertices;

    public Face(int[] vertices) {
        this.vertices = vertices;
    }

	@Override
	public Iterator<Integer> iterator() {
		return Arrays.stream(vertices).boxed().iterator();
    }

    public static boolean isConnected(Face a, Face b) {
        int[] a_ = a.vertices.clone();
        int[] b_ = b.vertices.clone();
        Arrays.sort(a_);
        Arrays.sort(b_);

        int i = 0;
        int j = 0;
        int common = 0;

        while (i < a_.length && j < b_.length) {
            if (a_[i] == b_[j]) {
                common++;
                i++; j++;
            } else if (a_[i] < b_[j]) {
                i++;
            } else if (a_[i] > b_[j]) {
                j++;
            }
        }

        return common >= 2;
    }
}