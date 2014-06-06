/*
 * Utils.java
 *
 * Created on 7 maart 2006, 13:34
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package parser;

/**
 *
 * @author gideon
 */
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Utils {
    
    public String[][] randomSentences;
    
    /** Creates a new instance of Utils */
    public Utils() {
    }
    
    
    public static int computeUniqueBrackets(ArrayList<Node> listOfNodes) {
       //int nrConstituentsOfParse = 0;
       HashSet<String> listOfUniqueBrackets = new HashSet<String>();
       for (Node myNode : listOfNodes) {
            if (myNode.getType()==parameters.NONTERMINAL && (myNode.getRightSpan()-myNode.getLeftSpan()>1)) listOfUniqueBrackets.add(myNode.getLeftSpan() + "-" + myNode.getRightSpan());
        }
       return listOfUniqueBrackets.size();  //nrConstituentsOfParse;
   }
    
    /** Converts array of words to a printable sentence.
     */
    public static String arrayToSentence(String[] sentence, int iStart){
        StringBuffer buff = new StringBuffer();
        //for(String word : sentence){
        
        for (int i= iStart; i < sentence.length; i++){
            //buff.append(word).append(' ');
            buff.append(sentence[i]).append(' ');
        }
        
        return buff.toString();
    }
    
    /** Converts arrayList of words to a printable sentence.
     */
    public static String arrayListToSentence(ArrayList<String> sentence, int iStart){
        StringBuffer buff = new StringBuffer();
        //for(String word : sentence){
        
        for (int i= iStart; i < sentence.size(); i++){
            //buff.append(word).append(' ');
            buff.append(sentence.get(i)).append(' ');
        }
        
        return buff.toString().trim();
    }
    public static ArrayList<String> getListOfWSJFiles() throws Exception {

       ArrayList<String> listOfFiles = new ArrayList<String>();
       
       
       File myDir = new File("C:/wsj/train");
       File mySubDir;
       for (String myDirName : myDir.list()) {
           mySubDir = new File("C:/wsj/train/" + myDirName);
           for (String myFile : mySubDir.list()) {
               //System.out.println(myFile );
               listOfFiles.add("C:/wsj/train/" + myDirName + "/" + myFile);
          }  
       }
       /*
       File myDir = new File("C:/wsj/test");
       File mySubDir;
       for (String myDirName : myDir.list()) {
           mySubDir = new File("C:/wsj/test/" + myDirName);
           for (String myFile : mySubDir.list()) {
               //System.out.println(myFile );
               listOfFiles.add("C:/wsj/test/" + myDirName + "/" + myFile);
          }  
       }
        */
       //temp
       //if (!Main.READ_WSJ_DIRECTORY && Main.WSJ_TRAINING_FILE != null) {
       // listOfFiles.clear();
        
       // listOfFiles.add(Main.WSJ_TRAINING_FILE);
       //}
       return listOfFiles;
    }

}
