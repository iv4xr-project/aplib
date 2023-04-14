package eu.iv4xr.framework.spatial;



public class Matrix implements Cloneable {

	public float[][] matrix ; 
	public int numOfRows ;
	public int numOfColumns ;
	
	@Override
	public Object clone() {
		Matrix M = new Matrix() ;
		M.matrix = new float[this.numOfRows][this.numOfColumns] ;
		M.numOfColumns = this.numOfColumns ;
		M.numOfRows = this.numOfRows ;
		for (int r=0; r<numOfRows; r++) {
			for (int c=0; c<numOfColumns; c++) {
				M.matrix[r][c] = this.matrix[r][c] ;
			}
		}
		return M ;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer() ;
		buf.append("[ ") ;
		for (int r=0; r<numOfRows; r++) {
			if (r>0) buf.append("  ") ;
			for (int c=0; c<numOfColumns; c++) {
				if(c>0) buf.append(", ") ;
				buf.append("" + this.matrix[r][c]) ;
			}
			if (r<numOfRows-1)
				buf.append("\n") ;
			else
				buf.append(" ]") ;
		}
		return buf.toString() ;	
	}

	/**
	 * Add two matrices. The two matrices must have the same dimensions.
	 */
	public Matrix add(Matrix M2) {
		Matrix M1 = (Matrix) clone() ;
		for (int r=0; r<numOfRows; r++) {
			for (int c=0; c<numOfColumns; c++) {
				M1.matrix[r][c] += M2.matrix[r][c] ;
			}
		}
		return M1 ;
	}
	/**
	 * Multiply this matrix with a 3D vector. This matrix must be a 3x3 matrix.
	 */
	public Vec3 apply(Vec3 p) {
		Vec3 row0 = new Vec3(matrix[0][0],matrix[0][1],matrix[0][2]) ;
		Vec3 row1 = new Vec3(matrix[1][0],matrix[1][1],matrix[1][2]) ;
		Vec3 row2 = new Vec3(matrix[2][0],matrix[2][1],matrix[2][2]) ;
		Vec3 result = new Vec3(Vec3.dot(row0,p), 
							   Vec3.dot(row1,p),
							   Vec3.dot(row2,p)) ;
		return result ;
	}
	
	public static Matrix mkM2x2(
			float a1, float a2,
			float b1, float b2) {
		Matrix m = new Matrix() ;
		float[][] m_ = { {a1,a2}, {b1,b2} } ;
		m.matrix = m_ ;
		m.numOfRows = 2 ;
		m.numOfColumns = 2 ;
		return m ;		
	}
	
	public static Matrix mkM3x3(
			float a1, float a2, float a3,
			float b1, float b2, float b3,
			float c1, float c2, float c3) {
		Matrix m = new Matrix() ;
		float[][] m_ = { {a1,a2,a3}, {b1,b2,b3}, {c1,c2,c3} } ;
		m.matrix = m_ ;
		m.numOfRows = 3 ;
		m.numOfColumns = 3 ;
		return m ;		
	}
	
	/**
	 * A 3x3 matrix to do rotation +90-degree on the xz plane.
	 */
	public static final Matrix ROTxz90 = mkM3x3(
			    0, 0,-1,
			    0, 1, 0,
			    1, 0, 0
			) ;
	
	
	/**
	 * A 3x3 matrix to do rotation -90-degree on the xz plane.
	 */
	public static final Matrix ROTxz270 = mkM3x3(
		    0, 0, 1,
		    0, 1, 0,
		   -1, 0, 0
		) ;
	
	
	public static void main(String[] args) {
		Matrix M = mkM3x3(1,2,3,4,5,6,7,8,9) ;
		System.out.println(M.toString());
		System.out.println("M(1,0)=" + M.matrix[1][0]) ;
		System.out.println("M(0,2)=" + M.matrix[0][2]) ;
		
		Matrix T = mkM3x3(1,0,0,
				          0,1,0,
				          0,0,1) ;
		
		System.out.println(">> id: " + T.apply(new Vec3(1,2,3)));
		System.out.println(">> +90xz rotation " + ROTxz90.apply(new Vec3(1,2,3)));
		System.out.println(">> -90xz rotation: " + ROTxz270.apply(new Vec3(1,2,3)));
	}

	
	 
	
	

}
