package eu.monnetproject.translation.sources.common;




/**
 *  
 * @author kasooja 
 */

public class Pair<X,Y> { 
	private X x;
	private Y y;
	
	public Pair(X x, Y y) {
		this.x = x;
		this.y = y;
	}
	
	public X getFirst() {
		return x;
	}

	public Y getSecond() {
		return y;
	}
	

	@Override
	public int hashCode() {
		if(x instanceof String && y instanceof String) 
			return ((String)x + (String)y).hashCode();		
		return super.hashCode();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {		
		if(x instanceof String && y instanceof String) {
			if(obj instanceof Pair) {
				Object first = ((Pair) obj).getFirst();
				Object second = ((Pair) obj).getSecond();
				if(first instanceof String && second instanceof String) {
					if(((String) x + (String)y).equals((String)first + (String)second))
						return true;					 
					else 
						return false;				 
				} else {
					return false;
				}			
			}
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		if(x instanceof String && y instanceof String) 
			return (String)x+"->"+(String)y;
		return super.toString();
	}
	
}