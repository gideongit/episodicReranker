package episodicReranker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class lcsgRule extends synUnit {

	//fields
    protected String rootPlusChildren;
    protected String rootSymbol;
    protected ArrayList<String> children;
    
    //class constructor.
	public lcsgRule(String rootSymbol, ArrayList<String> children, String entireProduction) {
  
	   this.rootSymbol = rootSymbol;
	   this.children = children;
	   this.rootPlusChildren = entireProduction; 
	   
	}

	public String getRootSymbol() {	
		return this.rootSymbol;
	}
	
	public String getUniqueSymbol() {	
		return this.rootPlusChildren;
	}
	
	public ArrayList<String> getChildren() {	
		return this.children;
	}
	
	public int getNrChildren() {
		return this.children.size();
	}
	
	
	//////////////////////////////////////////////
	// All following methods are dummies just to comply with parent Node
	//////////////////////////////////////////////
	
	public ArrayList<Double> getMeaning() {
		return null;	//comply with Node
	}
	
	
	public ArrayList<ArrayList<Double>> getSlots() {
		return null; //comply with Node
	}
	
	public void setStructureETNs(HashMap<String, HashSet<String>> ETNs) {
		//
	}
	
	
	public HashMap<String, HashSet<String>> getStructureETNs() {
		
		return null;
	}
	
		
	public int getETNTotals() {	
		return 0;
	}
	
	public ArrayList<Integer> getSlotIDs() {
		return new ArrayList<Integer>();
	}
	
	public void setETNTotalsOfRoot(int totalETNs) {
		//
	}
	
	public HashMap<Integer, ArrayList<Integer[]>> getRankedETNs() {
		return null;
	}
	
	 public int hashCode(){
	        return this.rootPlusChildren.hashCode(); 
	    }
		
	public boolean equals(Object obj){
        
        if(!(obj instanceof lcsgRule)){
           return false;
       }
       
        lcsgRule otherState = (lcsgRule) obj;

       //compare all fields 
       if(!(otherState.rootPlusChildren.equals(this.rootPlusChildren))) return false;
       //if(!(otherState.meaningRepresentation==this.meaningRepresentation)) return false;
       //if(!(otherState.slots==this.slots)) return false;
       
       return true;
       
   }
	 
	 public String toString() {
		 return this.rootPlusChildren;
	 }

}
