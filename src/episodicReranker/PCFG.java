package episodicReranker;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import parser.Node;
import parser.parseTree;

public class PCFG extends grammar {


	/**
	 * HashMap<String lefthandside, HashMap<String righthandside, Double frequency or prob.>> 
	 */
	protected HashMap<String, HashMap<String, Double>> rulesOfPCFG = new HashMap<String, HashMap<String, Double>>();
	
	// HPNNet class constructor.
	public PCFG(ArrayList<parseTree> preprocessedParseTrees) {

		//add special START production; the following will also take care of adding a START.1 nonterminal to lexicon
		if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) nonTerminalProductions.put("START*-*TOP", null);
		
		for (parseTree myParseTree : preprocessedParseTrees) {
	
			myParseTree.assignProductionNamesToNodes();
			addToLexicons(myParseTree);
		}
		
		if (Main.SMOOTH_FOR_UNKNOWN_PRODUCTIONS && Main.DO_HORIZONTAL_MARKOVIZATION)
			createVirtualProductions();
		
		for (parseTree myParseTree : preprocessedParseTrees) {
			for (Node myNode : myParseTree.getNodes()) {
				if (myNode.getChildNodes().size()>0) {
					String leftHandSide = myNode.getName();
					HashMap<String, Double> rightHandSides = null;
					if (rulesOfPCFG.get(leftHandSide)==null) {
						rightHandSides = new HashMap<String, Double>();
						rulesOfPCFG.put(leftHandSide, rightHandSides);
					}
					else rightHandSides = rulesOfPCFG.get(leftHandSide);
					
					String rightHandSide = "", childLabel="";
					//for (Node childNode : myNode.getChildNodes()) rightHandSide += childNode.getName() + "~";
					for (Node childNode : myNode.getChildNodes()) {
						childLabel = childNode.getName();	//get rid of postag
						if (childNode.getType()==parser.parameters.TERMINAL && !parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) childLabel = childLabel.split(" ")[1].trim();
						
						rightHandSide += childLabel + "~";
					}
					
					if (rightHandSides.get(rightHandSide)==null) rightHandSides.put(rightHandSide, 1.);
					else rightHandSides.put(rightHandSide, rightHandSides.get(rightHandSide)+1.);
				}
			}
		}	//for (parseTree myParseTree : reducedParseTreesFromTreebank)
		
		//normalize
		for (String leftHandSide : rulesOfPCFG.keySet()) {
			double totalCount = 0.;
			for (String rightHandSide : rulesOfPCFG.get(leftHandSide).keySet()) {
				totalCount += rulesOfPCFG.get(leftHandSide).get(rightHandSide);
			}
			//divide frequencies of RHS by totalcounts
			for (String rightHandSide : rulesOfPCFG.get(leftHandSide).keySet()) {
				rulesOfPCFG.get(leftHandSide).put(rightHandSide, rulesOfPCFG.get(leftHandSide).get(rightHandSide)/totalCount);
			}
		}
		
		//combine:
		HashMap<String, Integer> virtualProductions = this.getUniformSmoothLevelCounts();
		
	}
	
	public double computePCFGSentenceProbability(int sentenceCounter, parseTree myTestParseTree) {
		
		double sentenceProbability = 1.;
		for (Node myNode : myTestParseTree.getNodes()) {
			if (myNode.getChildNodes().size()>0) {
				String leftHandSide = myNode.getName();
				
				String rightHandSide = "", childLabel="";
				 
				for (Node childNode : myNode.getChildNodes()) {
					childLabel = childNode.getName();	//get rid of postag
					if (childNode.getType()==parser.parameters.TERMINAL && !parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) childLabel = childLabel.split(" ")[1].trim();
					rightHandSide += childLabel + "~";
				}
				
				//get probability
				if (!(rulesOfPCFG.get(leftHandSide)==null) && !(rulesOfPCFG.get(leftHandSide).get(rightHandSide)==null)) {
					double ruleProbability = rulesOfPCFG.get(leftHandSide).get(rightHandSide);
					sentenceProbability *= ruleProbability;
				}
				else {
					if (rulesOfPCFG.get(leftHandSide)==null) {
						System.out.println("Sentence #" + sentenceCounter + ": rulesOfPCFG.get(lhs)==null for lhs=" + leftHandSide);
						sentenceProbability=0.;
					}
					else {
						if (rulesOfPCFG.get(leftHandSide).get(rightHandSide)==null) {
							System.out.println("Sentence #" + sentenceCounter + ": rulesOfPCFG.get(lhs).get(rhs)==null for lhs=" + leftHandSide + " and rhs=" + rightHandSide);
							sentenceProbability=0.;
						}
					}
				}
			}
		}
		return sentenceProbability;
	}
	
	public HashMap<String, synUnit> getNonTerminalUnits() {
		return this.nonTerminalProductions;
	}
	
	public HashMap<String, synUnit> getTerminalUnits() {		
		return this.terminalUnits;
	}
	
	public HashSet<String> getTerminals() {
		HashSet<String> myTerminals = new HashSet<String>();
		for (String t : this.terminalUnits.keySet()) myTerminals.add(t);
		return myTerminals;
	}
	
	public HashMap<String, HashMap<String, Double>> getProjectAndAttachProbabilities() {
		return null;
	}

	public HashMap<String, HashMap<String, Double>> getShiftProbabilities() {
		return null;
	}
}
