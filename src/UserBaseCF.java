import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/*
 * 基于用户的协同过滤推荐算法
 * 度量用户间相似性的方法选用：带修正的余弦相似性
 * 输入：UserID  ，     ItemID
 * 输出1：预测评分值
 * 输出2：RMSE（推荐质量）
 * */
class UserBaseCF{
	
	public static final int USERSIZE=943;
	public static final int ITEMSIZE=1682;
	public static final int UN=10;//某一user的最近邻居数
	//public static final int IN=10;//某一item的最近邻居数
	
	public int [] num=new int[USERSIZE+1];//每个用户为几部评了分
	public double[] average=new double[USERSIZE+1];//每个user的平均打分
	public double[][] rate=new double[USERSIZE+1][ITEMSIZE+1];//评分矩阵
	public double[][] DealedOfRate=new double[USERSIZE+1][ITEMSIZE+1];//针对稀疏问题处理后的评分矩阵
	
	Neighbor[][] NofUser =new Neighbor[USERSIZE+1][UN+1];//每个用户的最近的UN个邻居
	
	List<Double> x=new LinkedList<Double>();//LinkedList按照对象加入的顺序存储
	List<Double> y=new LinkedList<Double>();
	public static void main(String args[]) throws Exception{
		
		UserBaseCF cf=new UserBaseCF();
		if(cf.readFile("bin/ml-data_0/u1.base")){
			System.out.println("请等待，正在分析");
			cf.getAvr();//得到average[]
			cf.dealRate();//得到DealedOfRate
			
			cf.getNofUser();//得到NofUser
			/* test
			System.out.println(cf.rate[1][11]);
			System.out.println(cf.DealedOfRate[1][11]);
			System.out.println(cf.num[1]);
			System.out.println(cf.average[1]);
			System.out.println(cf.rate[1][10]);
			System.out.println(cf.DealedOfRate[1][10]);
		*/
			for(int i=1;i<=UN;i++){
				System.out.println(cf.NofUser[1][i].getID()+":"+cf.NofUser[1][i].getValue());
			}
			
			
			//测试
			//读文件
			File inputFile=new File("bin/ml-data_0/u1.test");
			BufferedReader reader=null;
	        if(!inputFile.exists()||inputFile.isDirectory())
					throw new FileNotFoundException();
	        reader=new BufferedReader(new FileReader(inputFile));
	        
	        //写文件
	        File outputFile=new File("bin/testResult.txt");
	        FileWriter writer=null;
	        if(!outputFile.exists())
	        	if(!outputFile.createNewFile())
	        		System.out.println("输出文件创建失败");
	        writer=new FileWriter(outputFile);
	        String title ="UserID"+"\t"+"ItemID"+"\t"+"OriginalRate"+"\t"+"PredictRate"+"\r\n";
	        writer.write(title);
	        writer.flush();
	        String[] part=new String[3];
	        String tmpToRead="";
	        String tmpToWrite="";
	        while((tmpToRead=reader.readLine())!=null){
	        	part=tmpToRead.split("\t");
	        	int userID=Integer.parseInt(part[0]);
	        	int itemID=Integer.parseInt(part[1]);
	        	double originalRate=Double.parseDouble(part[2]);
	        	double predictRate=cf.predict(userID, itemID);
	        	cf.x.add(originalRate);
	        	cf.y.add(predictRate);
	        	tmpToWrite=userID+"\t"+itemID+"\t"+originalRate+"\t"+predictRate+"\r\n";
	        	writer.write(tmpToWrite);
	        	writer.flush();
	        }
			System.out.println("分析完成，请打开工程目录下bin文件夹中的testResult.txt");
			System.out.println("利用RMSE分析结果为"+cf.analyse(cf.x, cf.y));
			
		}
		else 			
			System.out.println("失败");
		
	}
	
	//Chapter1:准备工作
		//1-1:读取文件内容，得到评分矩阵     1:读取成功       -1：读取失败
	public boolean readFile(String filePath){
		File inputFile=new File(filePath);
		BufferedReader reader=null;
        try {
			reader=new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("文件不存在"+e.getMessage());
			return false;
		}
		
        String sentence="";
        String[] part=new String[3];
        try {
			while((sentence=reader.readLine())!=null){
				part=sentence.split("\t");
				int userID=Integer.parseInt(part[0]);
				int itemID=Integer.parseInt(part[1]);
				double Rate=Double.parseDouble(part[2]);
				//构造矩阵
				rate[userID][itemID]=Rate;
			}
		} catch (NumberFormatException|IOException e) {
			System.out.println("读文件发生错误"+e.getMessage());
			return false;
		}
        return true;	
	}
		//1-2计算每个用户的平均分
	public void getLen(){//计算每个用户为几部电影打分
		for(int i=1;i<=USERSIZE;i++){
			int n=0;
			for(int j=1;j<=ITEMSIZE;j++){
				if(rate[i][j]!=0)
					n++;
			}
			num[i]=n;
		}
	
	}
	public void getAvr(){
		getLen();
		int i,j;
		for(i=1;i<=USERSIZE;i++){
			double sum=0.0;
			for(j=1;j<rate[i].length;j++){//每个length都是ITEMSIZE=1682
				sum+=rate[i][j];
			}
			average[i]=sum/num[i];
		}
	}
		//1-3处理评分矩阵的稀疏问题（重要事项！！！）
		//重点处理该user对没有被评分的item，会打几分
		//暂时用1-2中计算出的平均分	
	public void dealRate(){
		int  i,j;
		for(i=1;i<=USERSIZE;i++){
			for(j=1;j<=ITEMSIZE;j++){
				if(rate[i][j]==0)
					DealedOfRate[i][j]=average[i];
				else
					DealedOfRate[i][j]=rate[i][j];
			}
		}
	}
	//Chapter2：聚类，找和某一用户有相同喜好的一类用户
		//2-1：:Pearson计算向量的相似度
	public double Sum(double[] arr){
		double total=(double)0.0;
		for(double ele:arr)
			total+=ele;
		return total;
	}
	public double Mutipl(double[] arr1,double[] arr2,int len){
		double total=(double)0.0;
		for(int i=0;i<len;i++)
			total+=arr1[i]*arr2[i];
		return total;
	}
	public double Pearson(double[] x,double[] y){
		int lenx=x.length;
		int leny=y.length;
		int len=lenx;//小容错
		if(lenx<leny) len=lenx;
		else len=leny;	
		double sumX=Sum(x);
		double sumY=Sum(y);
		double sumXX=Mutipl(x,x,len);
		double sumYY=Mutipl(y,y,len);
		double sumXY=Mutipl(x,y,len);
		double upside=sumXY-sumX*sumY/len;
		//double downside=(double) Math.sqrt((sumXX-(Math.pow(sumX, 2))/len)*(sumYY-(Math.pow(sumY, 2))/len));
		double downside=(double) Math.sqrt((sumXX-Math.pow(sumX, 2)/len)*(sumYY-Math.pow(sumY, 2)/len));
		
		//System.out.println(len+" "+sumX+" "+sumY+" "+sumXX+" "+sumYY+" "+sumXY);
		return upside/downside;
	}
	
		//2-2将Pearson算法用在求user的近邻上，求NofUser数组
	public void getNofUser(){
		int  id,userID;
		for(userID=1;userID<=USERSIZE;userID++){
			Set<Neighbor> neighborList=new TreeSet();//会将压入的Neighbor排好序存放
			Neighbor[] tmpNeighbor=new Neighbor[USERSIZE+1];
			for(id=1;id<=USERSIZE;id++){
				if(id!=userID){
					double sim=Pearson(DealedOfRate[userID],DealedOfRate[id]);
					tmpNeighbor[id]=new Neighbor(id,sim);
					neighborList.add(tmpNeighbor[id]);
				}
			}
			
			int k=1;
			Iterator it=neighborList.iterator();
			while(k<=UN&&it.hasNext()){
				Neighbor tmp=(Neighbor) it.next();
				NofUser[userID][k]=tmp;
				k++;
			}
		}
	}
	
	//Chapter3:根据最近邻居给出预测评分
	public double predict(int userID, int itemID){//这里的userID为用户输入，减1后为数组下标！
		double sum1=0;
	    double sum2=0;
	    for(int i=1;i<=UN;i++){//对最近的UN个邻居进行处理
	        int neighborID=NofUser[userID][i].getID();
	        double neib_sim=NofUser[userID][i].getValue();
	        sum1+=neib_sim*(DealedOfRate[neighborID][itemID]-average[neighborID]);
	        sum2+=Math.abs(neib_sim);
	    }
	    return average[userID]+sum1/sum2;
	}
	
	//Chapter4:测试
	//以u1.test的userID，itemID为输入，用以上运算再给出一组打分，与u1.test中进行比较
	//部分测试已在main函数中做好，这里实现均方差公式RMSE
	//它是观测值与真值偏差的平方和 与 观测次数n比值的平方根
	public double RMSE(double[] x, double[] y){
		double rmse=0;
		int lenx=x.length;
		int leny=y.length;
		int len=lenx;//小容错
		if(lenx<leny) len=lenx;
		else len=leny;
		
		double diffSum=0;
		double diffMutipl;
		for(int i=0;i<len;i++){
			diffMutipl=Math.pow((x[i]-y[i]), 2);
			diffSum+=diffMutipl;
		}
		rmse=Math.sqrt(diffSum/len);
		System.out.println(len);
		//System.out.println(diff);
		return rmse;
	}
	public double analyse(List<Double>x,List<Double>y){
		int lenx=x.size();
		int leny=y.size();
		int len=lenx;//小容错
		if(lenx<leny) len=lenx;
		else len=leny;
		//System.out.println(len);
		double[] tmpX=new double[len];
		double[] tmpY=new double[len];
		for(int i=0;i<len;i++){
			tmpX[i]=x.get(i);
			tmpY[i]=y.get(i);
		}
		return RMSE(tmpX,tmpY);
		//System.out.println(tmpY[1]);
	}
}