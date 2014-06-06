package episodicReranker;

import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

public class treeletState {

	protected episodicConstituent treelet;
	
	/**
	 * The updated trace sequences associated with the current treeletState (episodicConstituent) in the derivation.
	 * that is the constituent associated with the leftSide of the current binding. 
	 * For every sentenceNr in HM currentNodeTraceActivations contains an TreeMap of pairs
	 * {rankNr, pathLength}; multiple pairs are possible if the stored exemplar sentence has visited 
	 * this same constituent multiple times. 
	 * Updates from the trace sequences in previousNodeTraceActivations.
	 */
	protected HashMap<Integer, TreeMap<Integer, traceFields>> traceActivations = null;
	
	/**
	 * constructs an episodicConstituent (treelet) with activated traces 
	 * @param myTreelet
	 */
	public treeletState(episodicConstituent myTreelet){
		
		this.treelet = myTreelet;
		
		traceActivations = new HashMap<Integer, TreeMap<Integer, traceFields>>();
		
	}
	
	//not used
	public TreeMap<Integer, traceFields> addTraceActivationsOfOneExemplar(int sentenceNrOfEexemplar) {
		
		/**
		 * Stores TreeMap<rankNrOfETN, pathLength> for a certain sentenceNr;
		 * possibly multiple pairs if exemplar sentence has visited the node multiple times.
		 */
		TreeMap<Integer, traceFields> traceActivationsOfOneExemplar = new TreeMap(Collections.reverseOrder());	//new TreeMap<Integer, Integer>();
		this.traceActivations.put(sentenceNrOfEexemplar, traceActivationsOfOneExemplar);
		return traceActivationsOfOneExemplar;
	}

	/*
	public traceState makeTraceState(int sentenceNrOfExemplar, int rankNr) {
		TreeMap<Integer, traceState> traceActivationsOfOneExemplar1 = traceActivations.get(sentenceNrOfExemplar);
		traceState myTrace = new traceState();
		traceActivationsOfOneExemplar1.put(rankNr, myTrace);
		return myTrace;
	}
*/
	
	HashMap<Integer, TreeMap<Integer, traceFields>> getTraceActivations() {
		return this.traceActivations;
	}
	
	public episodicConstituent getTreelet() {
		return this.treelet;
	}
}
