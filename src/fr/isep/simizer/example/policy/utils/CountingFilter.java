// Aruth Perum Jothi, Aruth Perum Jothi, Thani Perum Karunai Aruth Perum Jothi
package fr.isep.simizer.example.policy.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 *  @author Sathiya
 *  @author Sylvain
 */
public class CountingFilter<E> implements Serializable {
    
    
    private final static int MAX_NB_HASH = 9;
    private final static double LN2 = Math.log(2);
        
    private byte[] buckets;
    private Queue<Integer> list;
    private int vectorSize=0;
      
    private int expectedNumberOfFilterElements; // expected (maximum) number of elements to be added
    private int numberOfAddedElements; // number of elements actually added to the Bloom filter
    private int k=0; // number of hash functions
    private int sliceSize;
    private int capacity;
    private double fpProb = 0.0;
    
    
    

    //static final Charset charset = Charset.forName("UTF-8"); // encoding used for storing hash values as strings
    
    
    
    public static double log2(double num)
    {
        return (Math.log(num)/LN2);
    }
   

    /**
      * Constructs an empty Bloom filter. the number of hash functions will set regarding 
      * the number of elements to store and the vector size.
      *  
      * @param capacity is the expected number of elements the filter will contain.
      * @param fpProb is the expected false positive probability
      * 
      */
  public CountingFilter(int capacity, double fpProb) {
      
      this.capacity = capacity;
      this.fpProb = fpProb;
      
      // most appropriate nb of hash function depending on fpProb
      // we will minimize the number of hash functions.
      this.k = (int) Math.floor(log2(1.0/fpProb) );
      
      if(k > MAX_NB_HASH)
          k= MAX_NB_HASH;
      
      this.sliceSize = (int) Math.ceil(capacity / LN2);
      System.out.println("Slice Size= " + sliceSize + " and k =" + k);
      
      this.vectorSize = sliceSize * k;
      this.buckets = new byte[vectorSize];
      byte b =0x0;
      Arrays.fill(buckets,b);
              
      this.list=new LinkedList<Integer>();
    
    
    }



   /**
     * Generates digests based on the contents of an array of bytes and splits the result into 4-byte integers and store them in an array. The
     * digest function is called until the required number of int's are produced. For each call to digest a salt
     * is prepended to the data. The salt is increased by 1 for each call. Provides 4 potential positions
     *
     * @param data specifies input data.
     * @param hashes number of hashes/int's to produce.
     * @return array of int-sized hashes
     */

    
// public static int[] md5hash(byte[] defaultBytes) {
//
//    int[] result= new int[4];
//
//    try{
//	MessageDigest algorithm = MessageDigest.getInstance("MD5");
//	algorithm.reset();
//        
//	algorithm.update(defaultBytes);
//	byte messageDigest[] = algorithm.digest();
//        //Using ByteBuffer seems appropriate to convert to int[4]
//        //http://stackoverflow.com/questions/2383265/convert-4-bytes-to-int
//               
//        ByteBuffer bb = ByteBuffer.wrap(messageDigest);
//        
//        for(int i=0;i<4;i++) {
//            result[i] = Math.abs(bb.getInt());
//            
//        }
//               
//              
//     }
//            
//    catch(NoSuchAlgorithmException nsae){ }
//        // System.out.println("");
//	return result;
//}
// // provides 5 positions between 0 and Integer.MAX_VALUE
//  public static int[] sha1hash(byte[] defaultBytes) {
//
//    int[] result= new int[5];
//
//    try{
//            MessageDigest algorithm = MessageDigest.getInstance("SHA1");
//            algorithm.reset();
//            algorithm.update(defaultBytes);
//            byte messageDigest[] = algorithm.digest();
//            
//            //Using ByteBuffer seems appropriate to convert to int[4]
//            //http://stackoverflow.com/questions/2383265/convert-4-bytes-to-int
//
//            ByteBuffer bb = ByteBuffer.wrap(messageDigest);
//
//            for(int i=0;i<5;i++) {
//                result[i] = Math.abs(bb.getInt());
//                //System.out.print(result[i] + " ");
//            }
//            //System.out.println();
//        }
//
//    catch(NoSuchAlgorithmException nsae){ }
//    
//    return result;
//}
    

    /**
     * Calculates the expected probability of false positives based on
     * the number of expected filter elements and the size of the Bloom filter.
     * <br /><br />
     * The value returned by this method is the <i>expected</i> rate of false
     * positives, assuming the number of inserted elements equals the number of
     * expected elements. If the number of elements in the Bloom filter is less
     * than the expected value, the true probability of false positives will be lower.
     *
     * @return expected probability of false positives.
     */
    public double expectedFalsePositiveProbability() {
        return getFalsePositiveProbability(expectedNumberOfFilterElements);
    }

    /**
     * Calculate the probability of a false positive given the specified
     * number of inserted elements.
     *
     * @param numberOfElements number of inserted elements.
     * @return probability of a false positive.
     */
    public double getFalsePositiveProbability(double numberOfElements) {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow((1 - Math.exp(-k * (double) numberOfElements
                        / (double) vectorSize)), k);

    }

    /**
     * Get the current probability of a false positive. The probability is calculated from
     * the size of the Bloom filter and the current number of elements added to it.
     *
     * @return probability of false positives.
     */
    public double getFalsePositiveProbability() {
        return getFalsePositiveProbability(numberOfAddedElements);
    }


    /**
     * 
     */
    public void add(E element) {
       add(element.toString());
    }
    
    /**
     *delete an element in the set 
     * 
     * @param element 
     */
   

    /**
     * Adds an array of bytes to the Bloom filter.
     *
     * @param bytes array of bytes to add to the Bloom filter.
     */
    private void add(String str) {
           int [] hash = fastHash(str, k);
          //SHA1 Hash
             
             for(int i=0; i<k;i++) {
          
                addTo(i, hash[i]);
            }


         numberOfAddedElements ++;
        
        
    }
    
    // calculates position and inserts
    private void addTo(int i, int h) {
         int position= (i*sliceSize) + (h % sliceSize); 
         //System.out.print("P" + i + " = " + position);
         buckets[position]++;
         addqueue(position);
    }

    
    public void addqueue(int hash)  {
      if(list.size() >= (capacity*k)) {
      
          int remove= list.poll();
          buckets[remove]--;
          numberOfAddedElements --;
      }
       list.add(new Integer(hash));  

  }
        
    /**
     * Returns true if the element could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param element element to check.
     * @return true if the element could have been inserted into the Bloom filter.
     */
    public boolean contains(E element) {
        //System.out.println(element);
        return contains(element.toString());
    }

    /**
     * Returns true if the array of bytes could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param bytes array of bytes to check.
     * @return true if the array could have been inserted into the Bloom filter.
     */
    private boolean contains(String str) {
        return contains(fastHash(str,k));
    }
    public static int[] fastHash(String dt, int k) {
        int[] result = new int[k];
        //String dt;
        for(int i=0; i<k;i++) {
            dt = dt.concat(":"+1);
            result[i] =  (int) Math.abs( 
                     MurmurHash3.murmurhash3_x86_32(dt, 0, dt.length(), 0x12345)
                    );
        }
        return result;
    }
    
    public boolean contains(int[] hash) {
        int i =0;
        boolean res;
        do{
             int position= (i*sliceSize) + (hash[i] % sliceSize); 
             res = (buckets[position] > 0);
             if(!res) return res;
             i++;
             
          } while(i < k && i < hash.length && res);
        
        return true;
    }

    /**
     * Returns the number of bits in the Bloom filter. Use count() to retrieve
     * the number of inserted elements.
     *
     * @return the size of the bitset used by the Bloom filter.
     */
    public int size() {
        return this.vectorSize;
    }

    /**
     * Returns the number of elements added to the Bloom filter after it
     * was constructed or after clear() was called.
     * @TODO: update this to reflect memory occupation
     * @return number of elements added to the Bloom filter.
     */
    public int count() {
        return this.numberOfAddedElements;
    }

    /**
     * Returns the expected number of elements to be inserted into the filter.
     * This value is the same value as the one passed to the constructor.
     *
     * @return expected number of elements.
     */
    public int getExpectedNumberOfElements() {
        return expectedNumberOfFilterElements;
    }
    
    public static int[] concat(int[] first, int[] second) {
        int[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

}