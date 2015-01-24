
 public class Neighbor implements Comparable{
	private int id;//邻居的编号
	private double value;//与邻居的相似度
	public Neighbor(int id,double value) {
		this.id=id;
		this.value=value;
	}
	public int getID(){
		return id;
	}
	public double getValue(){
		return value;
	}
	
	
	public int compareTo(Object o) {//覆写方法，是对象按照value降序排列
		// TODO Auto-generated method stub
		if(o instanceof Neighbor){
			Integer ID=((Neighbor) o).id;
			Double VALUE=((Neighbor) o).value;
			return VALUE.compareTo(value);
		}
		else{
			return 2;
		}
	}
}

