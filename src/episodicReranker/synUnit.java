package episodicReranker;

import java.util.ArrayList;
/*
 * Constituent.java
 *
 * Created on 24 november 2005, 17:03
 *
 */
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author gideon
 * the children of this class are Terminal and nonTerminal
 */
public abstract class synUnit {
    
    protected String symbolString;
            
    /**
     * Creates a new instance of Constituent 
     */
    
    public synUnit(){
        
    }
    
   
    public abstract String getRootSymbol();
    
    public abstract String getUniqueSymbol();
    
    public abstract ArrayList<Double> getMeaning();
    
    public abstract ArrayList<ArrayList<Double>> getSlots();
    
    public abstract ArrayList<String> getChildren();
    
    public abstract int getNrChildren();
    
    public abstract ArrayList<Integer> getSlotIDs();
    
    public abstract HashMap<String, HashSet<String>> getStructureETNs();
    
    public abstract void setStructureETNs(HashMap<String, HashSet<String>> ETNs);
	
    //public abstract HashSet<String> getLabeledETNsOfSlotOrRoot(int slotOrRootNr, String label);
    
    public abstract int getETNTotals();
    
    public abstract HashMap<Integer, ArrayList<Integer[]>> getRankedETNs();
    
    //public abstract HashSet<Integer[]> getStructureETNs();
    
    public abstract void setETNTotalsOfRoot(int totalETNs);
    
    public abstract boolean equals(Object obj);
        
      
    
    //public abstract int hashCode();
    
    
}
