package org.isep.simizer.example.policy.utils;

import com.google.code.ekmeans.EKmeans;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import simizer.Node;
import simizer.requests.Request;
import weka.clusterers.Clusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.*;

public class ClusteringRequests {

    Request[] queries;
    List<Node> nodeList;
    double[][] clusters;
    double[][] costs;
    double[] machineToCost; // holds the averaged query cost on this machine
    int nbParam;
    Random random = new Random(System.currentTimeMillis());
    private Instances lastCenters=null;

    public ClusteringRequests(Request[] queries, List<Node> nodeList) {
        this.queries = queries;
        this.nodeList = nodeList;
        this.nbParam = 1;
        this.clusters = new double[nodeList.size()][nbParam];
        this.costs = new double[nodeList.size()][nodeList.size()];
        this.machineToCost = new double[nodeList.size()];
    }

    public ClusteringRequests(Request[] queries, int nbParam, List<Node> nodeList) {
        this.queries = queries;
        this.nbParam = nbParam;
        this.nodeList = nodeList;
        this.clusters = new double[nodeList.size()][nbParam];
        this.costs = new double[nodeList.size()][nodeList.size()];
        this.machineToCost = new double[nodeList.size()];
    }
    
    
    public double[][] getClusters() {
        return this.clusters;
    }

    public double[] getCluster(int i) {
        //System.out.println(i + " -> "+ Arrays.toString(this.clusters[i]));
        return this.clusters[i];
    }

    public double[][] getCosts() {
        return this.costs;
    }

    public double[][] computeRequestMatrix() {
        double[][] reqMat = new double[queries.length][nbParam];


        int[] nodeCount = new int[nodeList.size()];
        /**
         * Insert parameters of requests into matrix format
         */
        for (int i = 0; i < queries.length; i++) {
            String[] tmpQuery = queries[i].display().split(",");

            //System.out.println(Arrays.toString(tmpQuery));
            for (int j = 1; j <= (nbParam); j++) {
                //  System.out.println("J:" + (j*2));
                reqMat[i][j - 1] = Double.parseDouble(tmpQuery[ (j * 2)]);
            }
            //reqMat[i][nbParam] = (double)queries[i].getServerFinishTime()-queries[i].getClientStartTimestamp();
            int nodeId = queries[i].getNodeId();
            double curAvg = machineToCost[nodeId];
            machineToCost[nodeId] = ((curAvg * nodeCount[nodeId]) + queries[i].getCost()) / (nodeCount[nodeId] + 1);
            nodeCount[nodeId]++;
        }
        return reqMat;
    }
    
    @Deprecated
    public void computeClustersEKmeans() {

        int numberOfRequests = queries.length;
        int clusterCount = nodeList.size();
        int numberOfPara = nbParam;
        int[] cluster = new int[nodeList.size()];

        double[][] eachRequest = computeRequestMatrix();
        /**
         * Init center value for each cluster with initial requests OR by Random
         * value
         */
        double[][] centroids = new double[clusterCount][numberOfPara];
        double[] tmpCenter = new double[numberOfPara];
        for (int i = 0; i < clusterCount; i++) {
            // RANDOM 
//            for (int j = 0; j < (numberOfPara); j++) {
//                centroids[i][j] = Math.abs(random.nextDouble());
//                //System.out.println("centroids => "+ Arrays.toString(centroids[i]));
//            }
             boolean found =true;
             while(found) {
                 tmpCenter = eachRequest[random.nextInt(numberOfRequests)];
                 found=false;
                 for(int k=(i-1);k>=0;k--) {
                     found=Arrays.equals(tmpCenter,centroids[k]);
                     if(found) break;
                 }
             }
             centroids[i] = tmpCenter;
//             for (int j = 1; j < (numberOfPara); j++) { 
//                 centroids[i][j] =eachRequest[i][j]; 
//             }
             
            cluster[i] = 0;
        }
       
        /**
         * Insert parameters of requests into matrix format
         */
    
        
        EKmeans eKmeans = new EKmeans(centroids, eachRequest);
        //eKmeans.setIteration(64);
        eKmeans.setEqual(false); // equals true -> experimental & buggy ! cf. (https://code.google.com/p/ekmeans/)

        eKmeans.setDistanceFunction(EKmeans.EUCLIDEAN_DISTANCE_FUNCTION);
        eKmeans.run();

        /**
         * Assignment of which cluster has which requests, by k-means
         * optimization.
         */
        
        int[] assignments = eKmeans.getAssignments();
        int[][] clusterToRequest = new int[clusterCount][];
        int [] counts = eKmeans.getCounts();
        for(int i=0;i<counts.length;i++)
            clusterToRequest[i] = new int[counts[i]];
        
        for (int i = 0; i < numberOfRequests; i++) {
            clusterToRequest[ assignments[i]][ cluster[ assignments[i]]] = i;
            cluster[ assignments[i]]++;
        }

        double center[][] = eKmeans.getCentroids();
        for (int i = 0; i < center.length; i++) {
            clusters[i] = Arrays.copyOf(center[i], center[i].length);
            /*
             * for (int j = 0; j < center[i].length; j++) { clusters[i][j] =
             * center[i][j]; }
             */
        }

        computeCostMatrix(clusterToRequest);
    }
    /**
     * Creates a Weka Datatset
     * @param reqMat
     * @return 
     */
    private Instances getWekaDataset(double[][] reqMat) {
        FastVector attrVec = new FastVector();
        int nbPar = reqMat[0].length;
        for (int p = 0; p < nbPar; p++) {
            attrVec.addElement(new Attribute(Integer.toString(p)));
        }

        Instances dataset = new Instances("queries", attrVec, 0);
       
        for (int i = 0; i < reqMat.length; i++) {
            Instance tmp = new Instance(1.0, reqMat[i]);
            dataset.add(tmp);
        }
        return dataset;
    }
    /**
     * Uses an iterative K-Means : it is possible to pass the centers 
     * 
     */
    
    public void computeClustersIKmeans() {
        
        IterativeSimpleKMeans isk = new IterativeSimpleKMeans();
                
       
        try {
            isk.setNumClusters(nodeList.size());
            if(this.lastCenters==null) {
                
                isk.setSeed(random.nextInt());
                System.out.println("NO CENTER");
            } else {
                isk.setClusterCentroids(this.lastCenters);
                System.out.println("PREVIOUS " + lastCenters.toString());
            }
        } catch (Exception ex) {
            Logger.getLogger(ClusteringRequests.class.getName()).log(Level.SEVERE, null, ex);
        }
        computeClusters(isk);
        
        this.lastCenters = isk.getClusterCentroids();
        System.out.println("NEW" + lastCenters.toString());
            
    }
 
     /**
     * Uses Weka's SimpleKmeans to compute requests models
     */
    public void computeClustersWekaMeans() {
        SimpleKMeans kmeans = new SimpleKMeans();
        kmeans.setSeed(random.nextInt());
        
        try {
            kmeans.setNumClusters(nodeList.size());
        } catch (Exception ex) {
            Logger.getLogger(ClusteringRequests.class.getName()).log(Level.SEVERE, null, ex);
        }
        computeClusters(kmeans);
       
       //System.out.println(kmeans.getClusterCentroids().toString());
       
    }
    /**
     * Computes a hierarchical clustering of the requests
     */
    public void computeClustersHMeans() {
        // Actual Clustering
        HierarchicalClusterer hc = new HierarchicalClusterer();
        hc.setNumClusters(nodeList.size());
        hc.setLinkType(new SelectedTag("CENTROID", HierarchicalClusterer.TAGS_LINK_TYPE));
        computeClusters(hc);
    
       
    }
    
    /**
     * Generic method for cluster computation using WEKA
     * @param algo Clustering algorithm implementing the specified interfaces
     */
    private void computeClusters(Clusterer algo) {
        int numberOfRequests = queries.length,
                clusterCount = nodeList.size();
          double[][] reqMat = computeRequestMatrix();
          
        Instances dataset = getWekaDataset(reqMat);
        ArrayList[] clusterToRequest = new ArrayList[clusterCount];
         
        try {
          
            algo.buildClusterer(dataset);
            
             for (int i = 0; i < numberOfRequests; i++) {
                int clustId = algo.clusterInstance(dataset.instance(i));
                
                if(clusterToRequest[clustId]==null) 
                    clusterToRequest[clustId] = new ArrayList<Integer>();
                
                clusterToRequest[clustId].add(i);
             }
             
        } catch (Exception ex) {
            Logger.getLogger(ClusteringRequests.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int[][] cluster2 = new int[clusterCount][];
        
        for(int i=0;i<clusterToRequest.length;i++) {
         //  System.out.println(clusterToRequest[i].size());
           cluster2[i] = new int[clusterToRequest[i].size()];
           for(int j=0;j<cluster2[i].length;j++)
               cluster2[i][j] = (int) clusterToRequest[i].get(j);
           
        }
        /**
         * Centers computation
         * @TODO put in another method
         */
         for (int i = 0; i < cluster2.length; i++) {
            double[] reqProto = new double[nbParam];
            
            for(int j=0; j < cluster2[i].length; j++) {
                
                for(int k=0;k<nbParam;k++) {
                    double weight = reqProto[k] * j;
                    double val = reqMat[ cluster2[i][j] ][k];
                    reqProto[k] = (weight + val) / (j+1);
                }
            }
            clusters[i] = reqProto;
        }
        
        computeCostMatrix(cluster2);

        
    }
     /**
     * For every cluster, to every node, we get its average cost for every
     * requests, form the cost matrix.
     */
    public void computeCostMatrix(int[][] clusterToRequest) {
        double totalCost = 0, avgCost;
        //double defaultValue = 30;
        int counter = 0;
        int clusterCount = clusterToRequest.length;

        for (int c = 0; c < clusterCount; c++) {

            //System.out.print("Mat:" +Arrays.toString(getCluster(c)) + ":");

            for (int m = 0; m < clusterCount; m++) {
                counter = 0;
                for (int r = 0; r < clusterToRequest[c].length; r++) {
                    totalCost = 0;

                    if (queries[ clusterToRequest[c][r] ].getNodeId() == m) {
                        totalCost += queries[ clusterToRequest[c][r]].getCost();
                    } else {
                        totalCost += machineToCost[m]; 
                    }
                    counter++;
                }
                
                if(counter==0)
                    avgCost = machineToCost[m];
                else
                    avgCost = totalCost / counter;
                
                //System.out.print(avgCost+",");
                costs[m][c] = avgCost;
                
            }
            //System.out.print("\n");
        }
    }

    public static void main(String[] args) {
        Random ran = new Random(System.currentTimeMillis());
        double[] names = new double[300];
        for (int i = 0; i < 300; i++) {
            names[i] = ran.nextDouble();
        }
        FastVector attrVec = new FastVector();
        attrVec.addElement(new Attribute("0"));

        Instances dataset = new Instances("queries", attrVec, 0);


        for (int i = 0; i < names.length; i++) {

            Instance tmp = new Instance(1.0, new double[]{names[i]});
            tmp.setDataset(dataset);
            // tmp.setValue(0, names[i]);

            dataset.add(tmp);
        }

        // Actual Clustering
        HierarchicalClusterer hc = new HierarchicalClusterer();
        hc.setNumClusters(3);
        hc.setLinkType(new SelectedTag("CENTROID", HierarchicalClusterer.TAGS_LINK_TYPE));
        try {


            hc.buildClusterer(dataset);
            System.out.println(hc.getNumClusters() + " clusters");
            System.out.println(dataset.classIndex());
            for (int i = 0; i < names.length; i++) {
                System.out.println(names[i] + " => " + hc.clusterInstance(dataset.instance(i)));
            }
        } catch (Exception ex) {
            Logger.getLogger(ClusteringRequests.class.getName()).log(Level.SEVERE, null, ex);
        }
        
       

    }

    public void setRequests(Request[] queries) {
         this.queries = queries;    }

    public void setNbParams(int nbParams) {
        this.nbParam = nbParams;
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }
}
