package nl.uu.cs.aplib.utils;

public class Pair<T, U> {

    public T fst;
    public U snd;

    public Pair(T x, U y) {
        fst = x;
        snd = y;
    }

    /**
     * Override the equals method. Equals should only be dependent on the variables
     * that are stored in the Pair.
     *
     * @param o The other object.
     * @return Boolean whether the Pairs are equal.
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof Pair<?, ?>) // o is a Tuple
                && ((Pair<?, ?>) o).fst.equals(this.fst) // first object is equal
                && ((Pair<?, ?>) o).snd.equals(this.snd); // second object is equal
    }

    /**
     * Override the hash function hash(pair(1,2)) should not be equal to
     * hash(pair(2,1)), so the second object is multiplied by 2.
     *
     * @return hashCode of the pair.
     */
    @Override
    public int hashCode() {
        // use doubles so we do not cause an overflow
        return (int) ((double) fst.hashCode() + 2.0 * (double) snd.hashCode());
    }

    /**
     * @return string representation
     */
    @Override
    public String toString() {
        // use doubles so we do not cause an overflow
        return String.format("<%s,%s>", fst, snd);
    }

}
