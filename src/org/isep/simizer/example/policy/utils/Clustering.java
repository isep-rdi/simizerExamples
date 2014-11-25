    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example.policy.utils;

import java.util.Arrays;
import java.util.List;
import rcaller.RCaller;
import rcaller.RCode;
import simizer.Node;
import simizer.VM;
import simizer.requests.Request;

/**
 *
 * @author rchiky
 */
public class Clustering {

  Request[] queries;
  String method; //kmeans,hclust (for hierarchical clustering)
  String distance;//euclidian,maximum,manhattan,pearson
  List<VM> nodeList;
  // Clustering result with k request prototypes
  double[][] clusters;
  double[][] costs;
  int nbParam;// number of parameters

  public Clustering(Request[] queries, List<VM> nodeList) {
    this.queries = queries;
    this.method = "kmeans";
    this.distance = "euclidean";
    this.nodeList = nodeList;
    this.nbParam = 1;
    this.clusters = new double[nodeList.size()][nbParam];
    this.costs = new double[nodeList.size()][nodeList.size()];

  }

  public Clustering(Request[] queries, int nbParam, List<VM> nodeList) {

    this.queries = queries;
    this.method = "kmeans";
    this.distance = "euclidean";
    this.nbParam = nbParam;
    this.nodeList = nodeList;
    this.clusters = new double[nodeList.size()][nbParam];
    this.costs = new double[nodeList.size()][nodeList.size()];

  }

  public Clustering(Request[] queries, String method, int nbParam, List<VM> nodeList) {
    this.queries = queries;
    this.method = method;
    this.nodeList = nodeList;
    this.clusters = new double[nodeList.size()][nbParam];
    this.costs = new double[nodeList.size()][nodeList.size()];
    this.nbParam = nbParam;
  }

  public Clustering(Request[] queries, String method, String distance, int nbParam, List<VM> nodeList) {
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

    code.addRCode("queries<-NULL");
    code.addRCode("library(amap)");

    for (i = 0; i < queries.length; i++) {

      String[] tmpQuery = queries[i].display().split(",");

            //Plz generate this dynamically
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

    try {
      System.out.println(caller.getRCode());
      caller.runAndReturnResult("res");
      System.out.println(Arrays.toString(caller.getParser().getAsStringArray("centers")));
      this.clusters = caller.getParser().getAsDoubleMatrix("centers", k, nbParam);

      this.costs = caller.getParser().getAsDoubleMatrix("costs", k, k);// row: clusters and col: nodes

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

}
