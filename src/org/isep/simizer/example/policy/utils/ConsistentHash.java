 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.isep.simizer.example.policy.utils;

/**
 *
 * @author Tom White
 *  http://www.lexemetech.com/2007/11/consistent-hashing.html
 */
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {

  static private int SEED = (int) System.currentTimeMillis();
  
  private final int numberOfReplicas;
  private final SortedMap<Integer, T> circle = new TreeMap<>();
    private final int dataReplicas;
  
  private Integer hash(String key) {
      
    return MurmurHash3.murmurhash3_x86_32(key, 0, key.length(), SEED);
      
  }

  public ConsistentHash(int numberOfReplicas, Collection<T> nodes) {
     this(1, numberOfReplicas, nodes);
  }
  public ConsistentHash(int dataReplicas, int nodeReplicas, Collection<T> nodes) {
        this.dataReplicas = dataReplicas;
        this.numberOfReplicas = nodeReplicas;
        if(nodes != null) {
            for (T node : nodes) {
             add(node);
            }
        }
        
  }  
  
  public final void add(T node) {
   
    for (int i = 0; i < numberOfReplicas; i++) {
        
      circle.put(hash(node.toString() + ":" + i),
        node);
    }
  }

  public void remove(T node) {
    for (int i = 0; i < numberOfReplicas; i++) {
      circle.remove(hash(node.toString() + ":"+ i));
    }
  }

  public T get(Object key) {
    if (circle.isEmpty()) {
      return null;
    }
    int hash = hash(key.toString());
    //System.out.println("Hashed to: " + hash);
    if (!circle.containsKey(hash)) {
      SortedMap<Integer, T> tailMap =
        circle.tailMap(hash);
      
      hash = tailMap.isEmpty() ?
             circle.firstKey() : tailMap.firstKey();
    }
    return circle.get(hash);
  } 
  
  public List<T> getList(Object key) {
      List<T> resultList = new LinkedList<>();
      T tmp;
      for(int i=0;i<dataReplicas;i++) {
          tmp = get(key.toString().concat(":"+i));
          if(tmp != null)
             resultList.add(tmp);
       }
      
      return resultList;
      
  }

}
