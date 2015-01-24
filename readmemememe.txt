训练数据的使用:
	1.利用文件包中的u1.base，取出userID，itemID和分数，形成user-item打分表 rate
涉及函数public boolean readFile(String filePath)
	2.计算用户对已打分项的平均分，在后面计算相似度时，用来补全用户为打分的电影（该补全方法有待商榷）涉及函数public void getAvr()存在数组average中
	3.新建数组补全打分缺省值，想在各个用户的打分向量都准备好了。涉及函数public void dealRate()

二.用户相似度的计算方法
	1.采用前两次作业中的向量法和Pearson算法，计算当前用户与其他用户的向量相似度（这里比较费时间）。涉及函数
public double Pearson(double[] x,double[] y)
·	2.找出与每个用户最相近的10个用户，作为邻居，之后按照邻居的喜好，为电影打分。涉及函数
public void getNofUser()和public double predict(int userID, int itemID)

三.推荐过程
	1.以u1.test中的userID和itemID作为为输入数据，代入上面的预测打分predict函数中，重新打分，将打分后的数据输出到工程文件下“bin/testResult.txt”中，即可直观看见预测分与真实分的具体情况。
四.评价结果（RMSE）运用的公式
