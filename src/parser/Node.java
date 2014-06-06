/*
 * Node.java
 *
 * Created on 1 december 2005, 9:10
 *
 */

package parser;

/**
 *
 * @author gideon
 */

import java.util.*;

public class Node {
    
    private ArrayList<Node> childNodes;
    private String leftHandSide;
    private String productionName;
    private Node parentNode;
    private int nodeType;   //1 = TERMINAL; 2=NONTERMINAL
    private int nrSubtrees;
    private int depthOfNode;
     private int spanLeft;
    private int spanRight;
    private int rankNrInDerivation;
    static int newNodeIndex = 0;
    public int processedChildren =0;
    private String structureETN;
    
    /**
     * Creates a new instance of Node 
     */
    public Node(String myLHS, Node myParentNode) {
        this.leftHandSide = myLHS;
        this.childNodes = new ArrayList<Node>();
        this.parentNode = myParentNode;
    }
    
    public void addChildNode(Node myNode){
        this.childNodes.add(myNode);
    }
    
    public Node getParentNode(){
        return this.parentNode;
    }
    
    public String getName(){
        return this.leftHandSide;
    }
    
    
    public void setName(String newName){
        this.leftHandSide = newName;
    }
    
    public String getProductionName(){
        return this.productionName;
    }
    
    
    public void setProductionName(String newName){
        this.productionName = newName;
    }
    
    
    public ArrayList<Node> getChildNodes(){
        return this.childNodes;
    }
    
    public String getStructureETN(){
        return this.structureETN;
    }
    
    public  void setStructureETN(String ETN){
        this.structureETN=ETN;
    }
    
    public void removeAllChildNodes (){
        this.childNodes.clear();
    }
    
    public void setType(int myType){
       this.nodeType = myType; 
    }
    
    public int getType(){
       return this.nodeType; 
    }
    
    public void replaceChildNode(int childNodeIndex, Node newNode) {

        //Integer oldInteger = new Integer(oldIndex);
        //Integer newInteger = new Integer(newIndex);
        //for ( Node myNode : this.childNodes) {
        //    if (oldNode.getName().equals(myNode.getName())) {
        //        int k = this.childNodes.indexOf(myNode);
                this.childNodes.set(childNodeIndex,  newNode);
        //    }
        //}
    }
    
    public void replaceParentNode(Node myparentNode) {
        this.parentNode = myparentNode;
    }
    
    public void setDepth(int myDepth){
        this.depthOfNode = myDepth;
    }
    
     public int getDepth(){
        return this.depthOfNode;
    }
     
         
     public void replaceLHS(String myLHS){
         this.leftHandSide = myLHS;
     }
     
     public void setnrSubtrees(int mynrSubtrees){
        this.nrSubtrees = mynrSubtrees;
    }
    
     public int getnrSubtrees(){
        return this.nrSubtrees;
    }
      
     public int getLeftSpan(){
        return this.spanLeft;
    }
     
     public void setLeftSpan(int myLeftSpan){
        this.spanLeft = myLeftSpan;
    }
    
     public int getRightSpan(){
        return this.spanRight;
    }
     
     public void setRightSpan(int myRightSpan){
        this.spanRight = myRightSpan;
    }
     
     public int getRankNrInDerivation(){
    	 return this.rankNrInDerivation;
     }
     
     public void setRankNrInDerivation(int rankNr) {
    	 this.rankNrInDerivation = rankNr;
     }
     
     public boolean equals(Object obj){ //only for sake of comparing parseTrees (arrays of nodes) after CYK parse
         //so you are not interested in children
        if(!(obj instanceof Node)){
            return false;
        }
        
        Node other = (Node) obj;
        if(!(other.leftHandSide.equals(this.leftHandSide))) {
            return false;
        }
        if(!(other.getLeftSpan() == this.getLeftSpan())){
            return false;
        }
        if(!(other.getRightSpan() == this.getRightSpan())){
            return false;
        }
        if(!(other.childNodes.equals(this.childNodes))){
            return false;
        }
        return true;
    }
}
