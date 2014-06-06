
package episodicReranker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;


/**
 * Implementation of an episodic syntactic unit corresponding to a parent node 
 * plus its children nodes, or a terminal node (in which case children is empty) 
 * EpisodicConstituents contain episodic trace numbers (rankedETNs), which are lists of
 * pointers referring to derivations of exemplars in which this unit has been used
 * 
 * <p> 
 * 
 */
public class episodicConstituent  extends synUnit {

	//fields
    protected String rootSymbol;
    protected ArrayList<String> children;
    
    /**
     * Name of the parent node followed by children, and separated by *
     */
    protected String rootPlusChildren;
    //Integer[3] is episodeNr plus wordNumber + recursionNr (although recursionNr is always=1)
    /**
     * Not in use
     */
    protected HashMap<String, HashSet<String>> structureETNs = new HashMap<String, HashSet<String>>();
    //protected ArrayList<ArrayList<Double>> slots = new ArrayList<ArrayList<Double>>();
    /**
     * Not in use
     */
    protected int totalETNs = 0;
    //format: sentence_nr + ordinal nr
    
    /**
     * An ArrayList of pointers referring to the sequence number in derivations
 	 * of the train set where this unit has been used. (ETNs = episodic trace number)
 	 * Every pointer has the format {SequenceNr, HashCodeOfBoundEpsidodicUnit}
 	 * SequenceNr = sequence (rank) nr of the current unit in the derivation
 	 * HashCodeOfBoundEpsidodicUnit = pointer to the (hashcode of the) successor unit in the derivation
 	 * 
     */
    //DIT MOET ZIJN: HM<Double, Integer[]> waar Double een identifier voor het fragments is: sentenceNr.#fragment
    //protected ArrayList<Integer[]> rankedETNs = new ArrayList<Integer[]>();
    
    protected HashMap<Integer, ArrayList<Integer[]>> rankedETNs = new HashMap<Integer, ArrayList<Integer[]>>();


    //String = rankedETN[0] + "_" + rankedETN[1]
    /**
	 * Stores activation (in fact, pathlengths) of the ETNs of a unit in the episodicGrammar, 
	 * such that when the unit is revisited the activation (path) can be reactivated after it has been lost.
	 * since the constituent (node) can be visited multiple times during the same test derivation
	 * activation values must be kept for every distinct visit (state), 
	 * so you must use a fifo stack (ArrayList<Integer>) of activation values
	 * Only used for discontiguous fragments, if INCLUDE_DISCONTIGUITIES=true
	 * 
	 * @param String identifier for the ETN, in format SentenceNr_SequenceNr (=rankedETN[0] + "_" + rankedETN[1])
	 * @param Integer activation (=pathLength)
	 */
    protected HashMap<Integer, TreeMap<Integer, ArrayList<Integer>>> activationValuesOfRankedETNs = new HashMap<Integer, TreeMap<Integer, ArrayList<Integer>>>();
    //protected HashMap<Double, ArrayList<Integer>> activationValuesOfRankedETNs = new HashMap<Double, ArrayList<Integer>>();
    
    /**
     * Constructs an episodic syntactic unit corresponding to a parent nonterminal node 
     * plus its children nodes, or a terminal node (in which case children is empty)
     * Note that starred children are included in ArrayList children, but not in entireProduction
     * 
     * @param rootSymbol name of the parent node
     * @param children names of the daughter nodes
     * @param entireProduction name of parent followed by children, and separated by *
     */
	public episodicConstituent(String rootSymbol, ArrayList<String> children, String entireProduction) {
			  
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
	
	public int getNrChildren() {
		return this.children.size();
	}
	
	/**
	 * Not in use; included for compatibility with synUnit
	 */
	public ArrayList<Double> getMeaning() {
		return null;	//comply with Node
	}
	
	/**
	 * Not in use; included for compatibility with synUnit
	 */
	public ArrayList<String> getChildren() {	
		return null;
	}
	
	/**
	 * Not in use; included for compatibility with synUnit
	 */
	public ArrayList<ArrayList<Double>> getSlots() {
		return null; //comply with Node
	}
	
	/**
	 * Not in use
	 */
	public void setStructureETNs(HashMap<String, HashSet<String>> ETNs) {
		this.structureETNs = ETNs;
	}
	
	/**
	 * Not in use
	 */
	public HashMap<String, HashSet<String>> getStructureETNs() {
		//there are no slots, so ignore i
		return this.structureETNs;
	}
	
	/**
	 * Not in use
	 */	
	public int getETNTotals() {
		//ArrayList<Integer> temp = new ArrayList<Integer>();
		//temp.add(this.totalETNs);
		return this.totalETNs;
	}
	
	
	public int hashCode(){
        return this.rootPlusChildren.hashCode(); 
    }
	
	/**
	 * Not in use; included for compatibility with synUnit
	 */
	public ArrayList<Integer> getSlotIDs() {
		return new ArrayList<Integer>();
	}
	
	/**
	 * Not in use
	 */
	public void setETNTotalsOfRoot(int totalETNs) {
		this.totalETNs = totalETNs;
	}
	
	/**
	 * Returns ArrayList of rankedETNs associated with this unit, 
	 * each of which is in the format {SentenceNr, SequenceNr, HashCodeOfBoundEpsidodicUnit}
	 * 
	 */
	public HashMap<Integer, ArrayList<Integer[]>> getRankedETNs() {
		return this.rankedETNs;
	}
	
	/**
     * Adds an ETN to the episodicConstituent, that is a pointer 
     * to the rank (sequence) number in the derivation where this unit has been used. 
	 *
 	 * @param ETN has the format {SequenceNr, HashCodeOfBoundEpsidodicUnit}
 	 * @param SequenceNr sequence (rank) nr of the current unit in the derivation
 	 * @param HashCodeOfBoundEpsidodicUnit pointer to the (hashcode of the) successor unit in the derivation
 	 * 
     */
	public void addRankedETN(Integer[] ETN, int sentenceNr) {
		
		//check if there is already an ETN in this episodicConstituent with the same sentenceNr
		//HashMap<Double, Integer[]> rankedETNs
		//if the same fragment has already passed through this node you need to start a new fragment
		//but also add another ETN for the existing fragment
		ArrayList<Integer[]> rankedETNsFromSameSentence = null;
		if (this.rankedETNs.get(sentenceNr)==null) {
			rankedETNsFromSameSentence = new ArrayList<Integer[]>();
			this.rankedETNs.put(sentenceNr, rankedETNsFromSameSentence);
		}
		else rankedETNsFromSameSentence = this.rankedETNs.get(sentenceNr);
		
		rankedETNsFromSameSentence.add(ETN);
	}
	

	/*
	public int addRankedETN_forShortestDerivation(Integer[] ETN, int sentenceNr, int fragmentNr) {
		
		double fragmentId = java.lang.Double.parseDouble("" + sentenceNr + "." + fragmentNr);
		//check if there is already an ETN in this episodicConstituent with the same fragmentNr
		//HashMap<Double, Integer[]> rankedETNs
		//if the same fragment has already passed through this node you need to start a new fragment
		//but also add another ETN for the existing fragment
		while (!(this.rankedETNs.get(fragmentId)==null)) {
			fragmentNr +=1; 
			fragmentId = java.lang.Double.parseDouble("" + sentenceNr + "." + fragmentNr);
		}
		
		//create entries for all fragments up to the last one
			//ETN ={rankNr, nextProductionHashCode}
		//protected HashMap<Integer, ArrayList<Integer[]>> rankedETNs
			this.rankedETNs.put(fragmentId, ETN);
		
		return fragmentNr;
	}
*/
	
public void removeETNs(int sentenceCounter) {
	/*
	ArrayList<Double> fragmentIDsForRemoval = new ArrayList<Double>();
	for (Double fragID : this.rankedETNs.keySet()) {
		int sentenceNrOfFragment = (int) Math.floor(fragID);
		//don't remove sentence itself
		if (sentenceNrOfFragment==sentenceCounter) fragmentIDsForRemoval.add(fragID);
	}
	for (Double myFragment : fragmentIDsForRemoval) {
		this.rankedETNs.remove(myFragment);
	}
	*/
	for (int fragmentNr : Main.fragmentNrsCoupledToSentenceNr.get(sentenceCounter)) {
		this.rankedETNs.remove(fragmentNr);
	}
}
	/**
	 * NOT USED
	 * Stores activation (in fact, pathlength) of a trace of a unit in the episodicGrammar, 
	 * such that when the unit is revisited the activation (path) can be reactivated after it has been lost.  
	 * Only used for discontiguous fragments, if INCLUDE_DISCONTIGUITIES=true
	 * 
	 * @param myETN identifier for the ETN, in format {SentenceNr, SequenceNr}
	 * @param myActivation activation (=pathLength)
	 */
/*
	public void setActivationOfRankedETN(Double fragmentID, int myActivation) {
		//String strETN = "" + myETN[0] + "_" + myETN[1]; //"" + sentenceNrOfETN + "_" + rankNrOfETN
		//push on the activation stack
		ArrayList<Integer> activationStack=null;
		if (activationValuesOfRankedETNs.get(fragmentID)==null) {	//empty stack
			activationStack = new ArrayList<Integer>();
			activationValuesOfRankedETNs.put(fragmentID, activationStack);
		}
		else activationStack = activationValuesOfRankedETNs.get(fragmentID);	//non-empty stack	
		activationStack.add(myActivation);
	}
	*/
public void setActivationOfRankedETN(int sentenceNr, int rankNr, int myActivation) {
	//String strETN = "" + myETN[0] + "_" + myETN[1]; //"" + sentenceNrOfETN + "_" + rankNrOfETN
	//push on the activation stack
	ArrayList<Integer> activationStack=null;
	//TreeMap<rankNr, ArrayList<activations>>
	TreeMap<Integer, ArrayList<Integer>> activationStacksOfSentence = null;
	
	if (activationValuesOfRankedETNs.get(sentenceNr)==null) {	//muliple activationStacks possible per sentence, if visited multiple times
		activationStacksOfSentence =  new TreeMap<Integer, ArrayList<Integer>>();
		activationValuesOfRankedETNs.put(sentenceNr, activationStacksOfSentence);
	}
	else activationStacksOfSentence = activationValuesOfRankedETNs.get(sentenceNr);
	
	
	if (activationStacksOfSentence.get(rankNr)==null) {	//empty stack
		activationStack = new ArrayList<Integer>();
		activationStacksOfSentence.put(rankNr, activationStack);
	}
	else activationStack = activationStacksOfSentence.get(rankNr);	//non-empty stack	
	activationStack.add(myActivation);
}

	/**
	 * NOT IN USE
	 * Returns activation (in fact, pathlength) of a trace of a starred nonterminal
	 * unit in the episodicGrammar, that is left sister of the unit to which derivation attaches  
	 *  pop from the activation stack (remove last entry from ArrayList<Integer>)
	 * Only used for discontiguous fragments, if INCLUDE_DISCONTIGUITIES=true
	 * 
	 * @param fragmentID identifier for the ETN, in format {SentenceNr, SequenceNr}
	 */
	public Integer getActivationOfRankedETN(int sentenceNr, int rankNr) {
		
		//System.out.println("the ETNs stored in node " + this.rootSymbol + " have activations:" + activationValuesOfRankedETNs + "; strETN=" + strETN);
		//System.out.println("whereas number of ETNs stored in node :" + this.getRankedETNs().size());
		
		//find the highest rankNr that is lower than current rankNr
		if (activationValuesOfRankedETNs.get(sentenceNr)==null) return null;
		
		ArrayList<Integer> activationStack = null;
		for (Integer myRankNr : activationValuesOfRankedETNs.get(sentenceNr).keySet()) {
			if (myRankNr> rankNr) break;
			activationStack = activationValuesOfRankedETNs.get(sentenceNr).get(myRankNr);
		}
		
		//pop from the activation stack
		
		//ArrayList<Integer> activationStack = activationValuesOfRankedETNs.get(strETN);
		if (activationStack==null) return null;
		int activationOnTopOfStack = activationStack.remove(activationStack.size()-1);
				
		return activationOnTopOfStack;

	}
	
	public void clearActivationValuesOfRankedETNs() {
		activationValuesOfRankedETNs.clear();
	}
	

	public boolean equals(Object obj){
        
        if(!(obj instanceof episodicConstituent)){
           return false;
       }
       
        episodicConstituent otherState = (episodicConstituent) obj;

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
