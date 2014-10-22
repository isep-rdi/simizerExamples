    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example.policy.utils;

import java.util.Arrays;
import java.util.List;
import simizer.requests.Request;
import rcaller.*;
import simizer.Node;
import simizer.ServerNode;

/**
 *
 * @author rchiky
 */
public class Clustering {

    Request[] queries;
    String method; //kmeans,hclust (for hierarchical clustering)
    String distance;//euclidian,maximum,manhattan,pearson
    List<ServerNode> nodeList;
    // Clustering result with k request prototypes 
    double[][] clusters;
    double[][] costs;
    int nbParam;// number of parameters

    public Clustering(Request[] queries, List<ServerNode> nodeList) {
        this.queries = queries;
        this.method = "kmeans";
        this.distance = "euclidean";
        this.nodeList = nodeList;
        this.nbParam = 1;
        this.clusters = new double[nodeList.size()][nbParam];
        this.costs = new double[nodeList.size()][nodeList.size()];

    }

    public Clustering(Request[] queries, int nbParam, List<ServerNode> nodeList) {

        this.queries = queries;
        this.method = "kmeans";
        this.distance = "euclidean";
        this.nbParam = nbParam;
        this.nodeList = nodeList;
        this.clusters = new double[nodeList.size()][nbParam];
        this.costs = new double[nodeList.size()][nodeList.size()];

    }

    public Clustering(Request[] queries, String method, int nbParam, List<ServerNode> nodeList) {
        this.queries = queries;
        this.method = method;
        this.nodeList = nodeList;
        this.clusters = new double[nodeList.size()][nbParam];
        this.costs = new double[nodeList.size()][nodeList.size()];
        this.nbParam = nbParam;
    }

    public Clustering(Request[] queries, String method, String distance, int nbParam, List<ServerNode> nodeList) {
        this.queries = queries;
        this.method = method;
        this.nodeList = nodeList;
        this.clusters = new double[nodeList.size()][nbParam];
        this.costs = new double[nodeList.size()][nodeList.size()];
        this.distance = distance;
        this.nbParam = nbParam;
    }

    public void computeClusters() {

        int i = 0, j = 0;
        int k = nodeList.size();

        //Creating an instance of class RCaller
        RCaller caller = new RCaller();
        RCode code = new RCode();
        //Where is the Rscript
        caller.setRscriptExecutable("/usr/bin/Rscript");
        caller.setRExecutable("/usr/bin/R");
        // caller.setRscriptExecutable("C:/Program Files/R/R-2.15.2/bin/x64/Rscript.exe");
        //caller.setRExecutable("C:/Program Files/R/R-2.15.2/bin/x64/R.exe");

        code.addRCode("queries<-NULL");
        code.addRCode("library(amap)");

        /* 
         for(i=0;i<queries.length;i++)
         {
         //System.out.println(" display " + queries[i].display());
         Map<String, String> tmpVal = new HashMap<String,String>();
         String[] tmpTab = queries[i].display().split(",");
           
         tmpVal.put("id",tmpTab[0]);
           
         for(int x=1;x<tmpTab.length-2;x+=2) {
         tmpVal.put(tmpTab[i], tmpTab[i+1]);
         }
         tmpVal.put("nodeId", tmpTab[tmpTab.length-2]);
         tmpVal.put("cost", tmpTab[tmpTab.length-1]);
         try{
         //           code.addStringArray("query",queries[i].display().split(","));
         code.addStringArray("query",tmpVal.values().toArray(new String[tmpVal.size()]));
         code.addRCode("queries<-rbind(queries,as.numeric(query))");
         } catch(Exception e) {
         System.out.println(e.getMessage());
         }
         }
         */
//        int qSize = nbParam + 3;
//        Map positionMap = new HashMap<String, Integer>();
//        String [] first = queries[0].display().split(",");
//        for()
        for (i = 0; i < queries.length; i++) {
            //System.out.println(" display " + queries[i].display()+ "Query Length :"+queries.length);

            String[] tmpQuery = queries[i].display().split(",");


            //Plz generate this dynamically
            // String[] tmpQueryParse= new String[] {tmpQuery[0],tmpQuery[2],tmpQuery[4],tmpQuery[6],tmpQuery[7],tmpQuery[8]};
            // String[] tmpQueryParse = new String[qSize];
            // System.out.println(tmpQuery[0]+","+tmpQuery[2]+","+tmpQuery[4]+","+tmpQuery[6]+","+tmpQuery[7]+","+tmpQuery[8]);
            // code.addStringArray("query",queries[i].display().split(","));
            code.addStringArray("query", tmpQuery);
            code.addRCode("queries<-rbind(queries,as.numeric(query))");


        }


        code.addRCode("costs<-matrix(0," + k + "," + k + ")");
        if (method.equalsIgnoreCase("hclust")) {
            //code.addRCode("hc<-hcluster(queries[,2:"+(nbParam+1)+"],method=\""+distance+"\")");
            code.addRCode("hc<-hcluster(queries[,2:" + (nbParam + 1) + "],method=\"" + distance + "\")");
            code.addRCode("cluster<-cutree(hc,k=" + k + ")");
            code.addRCode("centers<-NULL");


            for (i = 1; i <= k; i++) {
                code.addRCode("centersC<-queries[cluster==" + i + ",]");
                code.addRCode("if (!is.vector(centersC)) centers<-rbind(centers,apply(centersC,2,mean)) "
                        + "else {centers<-rbind(centers,centersC);centersC<-rbind(centersC,centersC) }");

                for (Node n : nodeList) {

                    code.addRCode("costs[" + i + "," + (n.getId() + 1) + "]"
                            + "<-mean(centersC[centersC[,ncol(centersC)-1]==" + n.getId() + ",ncol(centersC)])");
                }

            }
            code.addRCode("centers<-centers[,2:" + (nbParam + 1) + "]");
            code.addRCode("costs[is.na(costs)]<-30");
            code.addRCode("res<-list(clusters=cluster,centers=t(centers),costs=t(costs))");


            /////Kmeans
        } else if (method.equalsIgnoreCase("kmeans")) {
            code.addRCode("hc<-Kmeans(queries[,2:" + (nbParam + 2) + "],centers=" + k + ",method=\"" + distance + "\")");
            code.addRCode("centers<-NULL");


            for (i = 1; i <= k; i++) {
                code.addRCode("centersC<-queries[hc$cluster==" + i + ",]");
                code.addRCode("if (!is.vector(centersC)) centers<-rbind(centers,apply(centersC,2,mean)) "
                        + "else {centers<-rbind(centers,centersC);centersC<-rbind(centersC,centersC) }");

                for (Node n : nodeList) {

                    code.addRCode("costs[" + i + "," + (n.getId() + 1) + "]"
                            + "<-mean(centersC[centersC[,ncol(centersC)-1]==" + n.getId() + ",ncol(centersC)])");
                }

            }

            code.addRCode("costs[is.na(costs)]<-30");

            code.addRCode("res<-list(centers=t(hc$centers),clusters=hc$cluster,costs=t(costs))");
        } else {
            System.out.println("méthode non implémentée");

        }
        //run the R code and return the result to Java


        caller.setRCode(code);

        //  System.out.println("entered launchClustering method");
        try {
            System.out.println(caller.getRCode());
            caller.runAndReturnResult("res");
            System.out.println(Arrays.toString(caller.getParser().getAsStringArray("centers")));
            // System.out.println("clustering..."); 
            this.clusters = caller.getParser().getAsDoubleMatrix("centers", k, nbParam);

            //System.out.println("clustering2..."); 
            this.costs = caller.getParser().getAsDoubleMatrix("costs", k, k);// row: clusters and col: nodes
            //System.out.println("clustering3..."); 


        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Problem in Maincaller.runAndReturnResult");
        }




    }

    public double[][] getClusters() {
        return this.clusters;
    }

    public double[] getCluster(int i) {
        return this.clusters[i];
    }

    public double[][] getCosts() {
        return this.costs;
    }
    /* public static void main(String[] args) {

     long[][] A = {
     {1,1,0,7,9},
     {1,4,0,8,9},
     {2,1,0,7,9 },
     {3,4,0,9,10},
     {1,1,0,7,9},
     {1,4,0,8,9},
     {2,1,0,7,9 },
     {3,4,0,9,10},
     {1,1,0,7,9},
     {1,4,0,8,9},
     };
     Request[] queries=new Request[10];
        
     for(int i = 0; i < 10 ;i++ ) {
             
     //System.out.println(requests[i]);
     Request r = new Request(
     A[i][0], //id
     (int)A[i][1], //param
     A[i][2], //artime
     A[i][3], //chtime
     A[i][4] //cmtime
     );
     queries[i]=r;
             
     }
        
     Clustering c=new Clustering(queries);
     c.computeClusters();

     }*/
}
