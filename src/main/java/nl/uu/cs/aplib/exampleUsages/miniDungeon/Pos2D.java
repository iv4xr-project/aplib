package nl.uu.cs.aplib.exampleUsages.miniDungeon;

public class Pos2D {
	
	public int x ;
	public int y ;
	public Pos2D(int x, int y) {
		this.x = x ; this.y = y ;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Pos2D) {
			var o_ = (Pos2D) o ;
			return this.x == o_.x && this.y == o_.y ;
		}
		return false ;
	}

}
