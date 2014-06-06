package episodicReranker;


import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.TreeMap;

import parser.Node;
import parser.parseTree;


/**
 * Implementation of a grammar whose primitive units are episodicConstituents.
 * The units of an episodicGrammar contain episodic trace numbers (rankedETNs), which are lists of
 * pointers referring to derivations of exemplars in which this unit has been used
 * 
 * <p> 
 * 
 */
public class episodicGrammar extends grammar {

	/**
	 * String is productionName. Initialized in constructor of episodicGrammar.
	 */
	public HashMap<String, episodicConstituent> episodicNonTerminalProductions;


	/**
	 * Groups episodicConstituent according to their parent label (rootLabel)
	 */
	//protected HashMap<String, HashSet<episodicConstituent>> nodesWithSameRootLabel = new HashMap<String, HashSet<episodicConstituent>>();

	
	/**
	 * contextFreeProbabilities are either M&C left corner probabilities (project, attach and shift) 
	 * conditioned on goalCateg@LeftCorner (thus not in fact history free), or pCFG probabilities in case of topdown model.
	 *  XXX* Counts of context free (zero order) productions estimated from treebank
	 * and also left corner projections / attachments and shifts
	 * <conditioning label, HM<production, frequency>>
	 * the conditioning label is the root in case of top-down parser, and left corner/starNonT in case of lcp
	 
	 */
	protected HashMap<String, HashMap<String, Double>> historyFreeProbabilities = new HashMap<String, HashMap<String, Double>>();
	
	protected HashMap<String, HashMap<String, Double>> backOffProbabilities = new HashMap<String, HashMap<String, Double>>();
	
	/**
	 * Not in use.
	 * every ETN of an input node (or cNode) has exactly one goal slot, 
	 * which is the only non-left slot with the same (first 2 components of the) ETN
	 * this is goalSlot where highest recursive projection from current node is attached
	 */
	protected HashMap<String, String> leftCornerGoalSlots = new HashMap<String, String>();

	/**
	 * The original parseTrees from the treebank from which the episodicGrammar is constructed
	 * Only used for printing.
	 */
	protected ArrayList<parseTree> parseTreesWithETNs = new ArrayList<parseTree>();

	/**
	 * longestCommonPathFragment = "" + sentenceNrOfETN + "_" + rankNrOfETN + "_" + pathLength + "_" + currentBindingOfTestSentence;
	 */
	protected static  ArrayList<ArrayList<String>> shortestDerivations = new ArrayList<ArrayList<String>>();

	/**
	 * For every sentence (Integer) ArrayList of the fragments in shortest derivation. Every fragment in 
	 * shortest derivation is encoded as ordered sequence of episodicConstituent. From this ETNs can
	 * be reconstructed (add sequenceNr plus pointer to the next episodicConstituent). 
	 */
	protected static TreeMap<Integer, HashSet<ArrayList<String>>> shortestDerivationFragments = new TreeMap<Integer, HashSet<ArrayList<String>>>();
	
	/**
	 * For every sentenceNr there is a stack of states (ArrayList), with last visited state on top of stack, where
	 * every state has one (or more) Integer[]={highest rankNr, pathLength} (no need to keep pathLength=1) 
	 */
	protected static HashMap<Integer, ArrayList<Double[]>> discontiguousFragments  = new HashMap<Integer, ArrayList<Double[]>>();
	
	public static String nrUnknownProductionsFeedback="";
	
	//protected static int rankNr=0;
	
	/**
	 * Constructs an episodic grammar by creating episodicConstituents 
	 * (HashMaps of episodicInputNodes and episodicNonTerminalProductions)
	 * corresponding to nonterminal productions and terminals in the treebank.
	 * invokes addToLexicons to fill (non-)terminal lexicons of the grammar.
	 * 
	 * inserts starred nonterminals between the children of a production.
	 * if this is an episodic left corner grammar adds special START node, and
	 * creates shift productions (X* --> w)  

	 * creates createVirtualProductions and doBackOffStatistics
	 * @param preprocessedParseTrees treebank from which episodicGrammar is constructed
	 * @throws Exception 
	 */
	public episodicGrammar(ArrayList<parseTree> preprocessedParseTrees) throws Exception {

		//store original parse trees from which episodicGrammar is constructed. Only used for printing.
		this.parseTreesWithETNs = preprocessedParseTrees;	

		//add special START production; the following will also take care of adding a START^1 nonterminal to lexicon
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL ) nonTerminalProductions.put("START*-*TOP", null);
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL ) nonTerminalProductions.put("START*TOP", null);

		for (parseTree myParseTree : preprocessedParseTrees) addToLexicons(myParseTree);	

		if (Main.SMOOTH_FOR_UNKNOWN_PRODUCTIONS && Main.DO_HORIZONTAL_MARKOVIZATION)
			//generates all possible virtualProductions, and virtual nonTerminals (via binarization)
			//but doesn't put these in lexicon, or in nonterminalProductions
			createVirtualProductions();

		//counts frequencies of context free rules conditioned on label, for zero order back-off 
		doBackOffStatistics(preprocessedParseTrees);

		//create references to episodicConstituent
		//this.episodicInputNodes = new HashMap<String, episodicConstituent>();
		this.episodicNonTerminalProductions = new HashMap<String, episodicConstituent>();

		//create input nodes          
		for (String nodeName : terminalUnits.keySet()) {          

			ArrayList<String> children = new ArrayList<String>();

			episodicReranker.synUnit iNode = new episodicConstituent(nodeName, children, nodeName);
			//this.episodicInputNodes.put(nodeName, (episodicConstituent) iNode);

			//again, for compliance with grammar:
			//terminalUnits were created before in addLexicons, but with iNode=null
			this.terminalUnits.put(nodeName, iNode);	
		}

		//check:
		//if (nonterminalProductions.contains("PRN**``*S*,*''*NP*")) System.out.println(" nonterminalProductions contains PRN**``*S*,*''*NP*");
		//else  System.out.println(" NOT nonterminalProductions contains PRN**``*S*,*''*NP*");

		//create episodicNonTerminalProductions including children for starred nonT
		for (String nodeName : nonTerminalProductions.keySet()) {          

			String[] production = nodeName.split("\\*");
			String rootSymbol = production[0];
			ArrayList<String> children = new ArrayList<String>();

			for (int i=1; i<production.length; i++) {
				children.add(production[i]);
				//if it is not the last child put starred nonT in between the children 
				if ((Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) && i< (production.length-1)) {
					//add number of dots equal to i
					String dottedNonT =production[0];
					children.add(dottedNonT + "^" + i);
				}
			}

			// call constructor of episodicConstituent
			//Note that starred children are included in children, but not in nodeName
			episodicReranker.synUnit cNode = new episodicConstituent(rootSymbol, children, nodeName);
			this.episodicNonTerminalProductions.put(nodeName, (episodicConstituent) cNode);

			//once more, for compliance with grammar:
			this.nonTerminalProductions.put(nodeName, cNode);

			//create shift productions from parseTrees, and add to episodicNonTerminalProductions
			if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) {

				//create extra episodicConstituents for the register positions between the slots: these must receive their own ETNs
				String[] splitProduction = nodeName.split("\\*");

				//number of children = splitProduction.length-1; number of intermediate slotpositions = splitProduction.length-1
				if (splitProduction.length >=3) {	//skip unary productions: they have no associated shift production
					ArrayList<String> childrenOfStar = new ArrayList<String>();
					String rhs ="";
					for (int i =1; i< (splitProduction.length); i++) {
						rhs += "*" + splitProduction[i];
						childrenOfStar.add(splitProduction[i]);
					}
					for (int i =1; i<= (splitProduction.length-2); i++) {
						String nodeStateName = splitProduction[0] + "^" + i + rhs;

						cNode = new episodicConstituent(splitProduction[0] + "^" + i, childrenOfStar, nodeStateName);
						this.episodicNonTerminalProductions.put(nodeStateName, (episodicConstituent) cNode);
						//again for compliance with grammar:
						//this.nonTerminalUnits.put(nodeName, cNode);

						//System.out.println("added new node(state) with label " + nodeStateName);
					}
				}
			}	//if (experiments.PROBABILITY_MODEL == experiments.EPISODIC_LEFTCORNER_MODEL || experiments.PROBABILITY_MODEL == experiments.Manning_CarpenterLCP) {	//if(experiments.INCLUDE_SHIFT_PRODUCTIONS)
			
			if (Main.PROBABILITY_MODEL == Main.EPISODIC_TOPDOWN_MODEL && Main.EXTRA_ATTACHMENT_IN_TD_DERIVATION) {	//if(experiments.INCLUDE_SHIFT_PRODUCTIONS)
				
				String[] splitProduction = nodeName.split("\\*");
				
				//create extra episodicConstituents for all register positions
				for (int i =0; i<children.size(); i++) {
					String nodeStateName = nodeName + "_" + i;
					cNode = new episodicConstituent(splitProduction[0] + "^" + i, children, nodeStateName);
					this.episodicNonTerminalProductions.put(nodeStateName, (episodicConstituent) cNode);
				
				}

			}
		}	//for (String nodeName : nonTerminalProductions.keySet())
	}

	/**
	 * Fills HashMap<String, HashMap<String, Integer>> contextFreeStatistics and
	 * contextFreeRootFrequencies
	 * @param preprocessedParseTrees
	 * @throws Exception 
	 */
	public HashMap<String, HashMap<String, Double>> doBackOffStatistics(ArrayList<parseTree> preprocessedParseTrees) throws Exception {

		/**
		 * Counts of context free (zero order) productions estimated from treebank
		 * and also left corner projections / attachments and shifts
		 * <conditioning label, HM<production, frequency>>
		 * the conditioning label is the root in case of top-down parser, and left corner/starNonT in case of lcp
		 */
		HashMap<String, HashMap<String, Integer>> pcfgCounts = new HashMap<String, HashMap<String, Integer>>();

		/**
		 * For denominator: total counts of all productions with the same root.
		 * Is also used for lcp, where it adds counts of all projections with the same left corner
		 */
		HashMap<String, Integer> pcfgSameRootCounts = new HashMap<String, Integer>();


		/**
		 * given the binary rule <X...Z> --> <X...Y> Z
		 * then you want back-off probabilities P(<X...Y>|X) for which Y occurs in <X...Y> ???Z
		 * and id. given the unary rule <X...Z> --> Z 
		 * you want back-off probabilities P(*|X) for which X occurs in <X...Z>
		 * A is weggemarginaliseerd, dus dit is in feite kans op unaire expansie.
		 * 
		 * for left corner parser: 
		 * given the binary rule <X...Z> --> <X...Y> Z, 
		 * then estimate P(<X...Z>|X) (first part of l.c.)
		 * voor willekeurig Y. (dwz voor elke Y is deze kans hetzelfde/Y is weggemarginaliseerd)
		 * 
		 * given the unary rule X --> <X...Y> (other way round than top-down unary)
		 * then estimate back-off prob. P<unary|X> where X is the left component of the left corner
		 * 
		 * and given the shift rule <X...Y>* --> w
		 * then you want back-off probabilities P(w|X*) where X occurs in <X...Y>*
		 * 
		 */
		HashMap<String, HashMap<String, Integer>> backoffCounts = new HashMap<String, HashMap<String, Integer>>();
		
			
		
		/**
		 * For denominator: total counts of all productions with the same root.
		 * Is also used for lcp, where it adds counts of all projections with the same left corner
		 */
		HashMap<String, Integer> backoffSameRootCounts = new HashMap<String, Integer>();

		//combine counts from the real treebank with virtualProductions/counts (if smooth_productions)
		//this replaces the use of nodesWithSameRootLabel

		//read (context free) frequencies out of treebank: zie onder PCFG en onder lcsGrammar
		//LET OP: reading off treebank anders voor top-down dan voor left-corner!
		//moet je bij left corner grammar conditioneren of lc en gc, of alleen op lc???
		//ik denk het laatste, omdat gc al in episodic gr zit, en dit is alleen back-off!

		/*
		//combine treebank frequencies with virtual productions; first add the virtual productions
		if (experiments.SMOOTH_FOR_UNKNOWN_PRODUCTIONS) {
			HashMap<String, HashSet<String>> virtualProductions= this.getVirtualProductions();

			//First, create frequencies of 1 for all possible productions and put them in
			//HashMap<String, HashMap<String, Integer>> contextFreeStatistics
			for (String lhs : virtualProductions.keySet()) {
				HashMap<String, Integer> productionsWithFrequencyOne = new HashMap<String, Integer>();
				treeBankStatistics.put(lhs, productionsWithFrequencyOne);

				for (String production : virtualProductions.get(lhs)) {
					productionsWithFrequencyOne.put(production, 1);
				}
				contextFreeRootFrequencies.put(lhs, 0);
			}
		}
		else {	//no smoothing
			*/
		//create empty HashMaps for all possible left hand sides
		for (String lhs : this.nonTerminals) {	//includes preterminals, and all intermediate nonterminals <X...Y> from the treebank
			HashMap<String, Integer> productionsWithFrequencies = new HashMap<String, Integer>();
			pcfgCounts.put(lhs, productionsWithFrequencies);
			pcfgSameRootCounts.put(lhs, 0);
			if (!(lhs.contains(">"))) {
				HashMap<String, Integer> productionsWithFrequencies2 = new HashMap<String, Integer>();
				backoffCounts.put(lhs, productionsWithFrequencies2);
				backoffSameRootCounts.put(lhs, 0);
			}
		}
		//}
		
		//read other base frequency counts from the treebank
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL ) {

			//count frequencies of context free productions in treebank, and put them in contextFreeStatistics
			//om PCFG base probabilities uit te rekenen moet je virtual rule counts optellen bij
			//de echte frequency counts vd treebank

			HashMap<String, Integer> productionsWithFrequencies = new HashMap<String, Integer>();
			productionsWithFrequencies.put("START*TOP", 1);
			pcfgCounts.put("START", productionsWithFrequencies);
			pcfgSameRootCounts.put("START", 1);
			HashMap<String, Integer> leftcornerCondOnSTART = new HashMap<String, Integer>();
			leftcornerCondOnSTART.put("TOP", 1); //"START*TOP"
			backoffCounts.put("START", leftcornerCondOnSTART);
			backoffSameRootCounts.put("START", 1);
			
			//System.out.println("before: pcfgCounts.get(TOP).size()=" + pcfgCounts.get("TOP").size());
			String childLabel="", production="";
			HashSet<String> topProductions = new HashSet<String>();
			
			for (parseTree myParseTree : preprocessedParseTrees) {
				for (Node myNode : myParseTree.getNodes()) {
					if (myNode.getChildNodes().size()>0) {	//all nonterminal and preterminal rules
						String leftHandSide = myNode.getName();
						//should never be null, because you have added HM for every possible lhs
						HashMap<String, Integer> productionsCondONLHS = pcfgCounts.get(leftHandSide);

						//productions are in the form lhs*rhs1*rhs2*etc.
						production = myNode.getProductionName();
						//if (leftHandSide.equals("TOP")) {
							//System.out.println("###myNode.getProductionName()=" + myNode.getProductionName());
						//	topProductions.add(production);
						//}
						/*
						//leftHandSide; 

						for (Node childNode : myNode.getChildNodes()) {
							childLabel = childNode.getName();
							//get rid of postag:
							if (childNode.getType()==dopparser.Main.TERMINAL && !dopparser.Main.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) childLabel = childLabel.split(" ")[1].trim();

							production += "*" + childLabel;
						}
						 */
						if (productionsCondONLHS.get(production)==null) productionsCondONLHS.put(production, 1);
						else productionsCondONLHS.put(production, productionsCondONLHS.get(production)+1);
						
						//backoffCounts
						/*
						if (myNode.getChildNodes().size()==1 && leftHandSide.contains(">")) {	//unary rules with compound label
							// given the unary rule <X...Z> --> Z 
							// then you want back-off probabilities P(*|X) for which X occurs in <X...Z>
							// A is weggemarginaliseerd, dus dit is in feite kans op unaire expansie.String X = leftHandSide.split(">")[0];
							String X = leftHandSide.split(">")[0];
							
							HashMap<String, Integer> YCondOnX = backoffCounts.get(X);
							if (YCondOnX.get("unary")==null) YCondOnX.put("unary", 1);
							else YCondOnX.put("unary", YCondOnX.get("unary")+1);				
						}
						
						if (myNode.getChildNodes().size()==2) {	//binary rules
							//given the binary rule <X...Z> --> <X...Y> Z
							//then you want back-off probabilities P(<X...Y>|X) for which Y occurs in <X...Y>
							String X = leftHandSide.split(">")[0];
							String leftCorner = myNode.getChildNodes().get(0).getName();
							//String Y = leftCorner.split(">")[0];
							
							HashMap<String, Integer> leftcornerCondOnX = backoffCounts.get(X);
							if (leftcornerCondOnX.get(leftCorner)==null) leftcornerCondOnX.put(leftCorner, 1);
							else leftcornerCondOnX.put(leftCorner, leftcornerCondOnX.get(leftCorner)+1);							
						}
						*/
						
						String conditioningLabel = leftHandSide;	//not compound label
						if (leftHandSide.contains(">")) conditioningLabel = leftHandSide.split(">")[0];
						String leftCorner = myNode.getChildNodes().get(0).getName();
						//String Y = leftCorner.split(">")[0];
						
						HashMap<String, Integer> leftcornerCondOnX = backoffCounts.get(conditioningLabel);
						if (leftcornerCondOnX.get(leftCorner)==null) leftcornerCondOnX.put(leftCorner, 1);
						else leftcornerCondOnX.put(leftCorner, leftcornerCondOnX.get(leftCorner)+1);							
					
					}
				}
			}	//for (parseTree myParseTree : reducedParseTreesFromTreebank)
			//System.out.println("after: pcfgCounts.get(TOP).size()=" + pcfgCounts.get("TOP").size() + "; topProductions.s=" + topProductions.size());
			//for (String topp : topProductions) System.out.println(topp);
			//compute the total aggregates (unigram statistics) and put in HashMap<String, Integer> contextFreeLabels

			/*
			System.out.println("Base context free counts, nonterminals:");
			System.out.println("***************************************");
			System.out.println("nonterminals without >:");
			TreeSet<String> orderedNonT = new TreeSet<String>();
			for (String nonT : virtualProductions.keySet()) {
				orderedNonT.add(nonT);
				if (!(nonT.contains(">"))) System.out.println(nonT);
			}
			System.out.println("***************************************");
			System.out.println("all nonterminals in binarized treebank:");

			for (String nonT : orderedNonT) {
				System.out.println(nonT);
			}
			System.out.println("***************************************");
			System.out.println("Base context free counts, nonterminals:");	
			 */

		}	//if (experiments.PROBABILITY_MODEL.equals("topdown_with_path") )

		
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL  ) {
	
			//also computes backOffProbabilities
				historyFreeProbabilities =  estimateSymbolicLCProbabilitiesAndBackoff(preprocessedParseTrees);

		}	


		int totalRules=0;
		for (String lhs : pcfgCounts.keySet()) {	//includes preterminals, and includes ">"
			//HashMap<String, Integer> productionsWithFrequencies = contextFreeStatistics.get(lhs);
			totalRules += pcfgCounts.get(lhs).size();	//check
			
			if (pcfgSameRootCounts.get(lhs) ==null)	pcfgSameRootCounts.put(lhs, 0);
			
			for (String production2 : pcfgCounts.get(lhs).keySet()) {
				pcfgSameRootCounts.put(lhs, pcfgSameRootCounts.get(lhs) + pcfgCounts.get(lhs).get(production2));
				//Print check  SBAR*SBAR>VP
				//if (production2.startsWith("SBAR*SBAR")) 
				if (Main.FEEDBACK) System.out.println("l.c.: " + lhs + ";  " + production2 + "  " + pcfgCounts.get(lhs).get(production2));
			}
		}
		//System.out.println("@@@@@@@@ totalRules=" + totalRules);
		
		//normalize:
		for (String lhs : pcfgCounts.keySet()) {	//includes preterminals, and includes ">"
			
			HashMap<String, Double> conditionalProbs = new HashMap<String, Double>();
			historyFreeProbabilities.put(lhs, conditionalProbs);
			
			for (String production2 : pcfgCounts.get(lhs).keySet()) {
				double Pr = ((double) pcfgCounts.get(lhs).get(production2))/((double) pcfgSameRootCounts.get(lhs));
				conditionalProbs.put(production2, Pr);
			}
		}
		
		
		//totals summed over conditioningLabels w/o ">"
		for (String lhs : backoffCounts.keySet()) {		
			if (backoffSameRootCounts.get(lhs) ==null)	backoffSameRootCounts.put(lhs, 0);
			
			for (String production2 : backoffCounts.get(lhs).keySet()) {
				backoffSameRootCounts.put(lhs, backoffSameRootCounts.get(lhs) + backoffCounts.get(lhs).get(production2));
			}
		}
			
		//normalize:
		for (String lhs : backoffCounts.keySet()) {	
			
			HashMap<String, Double> conditionalBackOffProbs = new HashMap<String, Double>();
			backOffProbabilities.put(lhs, conditionalBackOffProbs);
			
			for (String production2 : backoffCounts.get(lhs).keySet()) {
				//int enumerator = backoffCounts.get(backOffLabel).get(leftCornerOfTD);
				//int denominator = backoffSameRootCounts.get(backOffLabel);
				double Pr = ((double) backoffCounts.get(lhs).get(production2))/((double) backoffSameRootCounts.get(lhs));
				conditionalBackOffProbs.put(production2, Pr);
			}
		}
		
		/*check
		NumberFormat numberFormatter = new DecimalFormat("#.######");
		double totalP=0d;
		for (String rhs : pcfgCounts.get("TOP").keySet()) {
			double P1 = ((double) pcfgCounts.get("TOP").get(rhs))/((double) pcfgSameRootCounts.get("TOP"));
			System.out.println("$$$$$$ TOP-->" + rhs + ": P=" + numberFormatter.format(P1));
			totalP+=P1;
		}
		System.out.println("$$$$$$ TotalP=" + numberFormatter.format(totalP));
		*/
		return historyFreeProbabilities;
	}

	//results in updated attachment and projection rules, also prints leftCornerProbabilities to ./treebanks/left_corner_probabilities
	public HashMap<String, HashMap<String, Double>>  estimateSymbolicLCProbabilitiesAndBackoff(ArrayList<parseTree> reducedParseTreesFromTreebank) throws Exception {

		
		HashMap<String, HashMap<String, Double>> lcProbabilities = new HashMap<String, HashMap<String, Double>>();

		HashMap<String, HashMap<String, Integer>> backoffCounts = new HashMap<String, HashMap<String, Integer>>();
		
		
		if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) {	//if(experiments.INCLUDE_SHIFT_PRODUCTIONS)
			HashMap<String, Double> shiftsFromSTART = new HashMap<String, Double>();
			lcProbabilities.put("TOP@START^1", shiftsFromSTART);	//gc=TOP, lc=START^1 (shift rule)
			backOffProbabilities.put("TOP@START^1", shiftsFromSTART);
		}
		//enumerate the parsetrees, and count the rules
		for (parseTree myParseTree : reducedParseTreesFromTreebank) {			

			
			// enumerate nodes in dopparser.parseTree:	
			for (parser.Node treeNode : myParseTree.getNodes()) {

					String leftCornerLabel = treeNode.getName();	//treeNode.getProductionName();

					parser.Node parentNode = treeNode.getParentNode();
					int childIndex = 0;

					//check
					//if (!(parentNode==null))
					//	System.out.println("leftCornerLabel=" + leftCornerLabel + "; parentNode=" + parentNode.getHPNNodeName() + "; childIndex=" + childIndex);
					//else System.out.println("leftCornerLabel=" + leftCornerLabel + "; parentNode=null");

					String goalCategPlusLC = "", projectedRuleOrAttach="", goalCategory="";
					
					if (!(parentNode==null)) {	
						
						childIndex = parentNode.getChildNodes().indexOf(treeNode);
						////////////////////////////////
						////////    projection    //////
						if (childIndex==0) {	//projection
							//unique name for category labels only if they have same number of children
							//projectedNode is the first non-unary parent in the chain (of which treeNode is left descendant
							projectedRuleOrAttach = parentNode.getProductionName();
							//productionName() is reserved for completed rules; 
							//if not complete, then the next production is one with starred nonT
							if (parentNode.getChildNodes().size()>1) { // if more than 1 child this means it is not complete after projection 
								String[] splitProduction = parentNode.getProductionName().split("\\*");
								projectedRuleOrAttach = splitProduction[0] + "^1";
								for (int i =1; i< (splitProduction.length); i++) projectedRuleOrAttach += "*" + splitProduction[i];
							}
							
							//continue looking up for goalslot, until you find a parent that it is not left child
							boolean leftChild = true;
							//find parent node of projectedNode: skip parent nodes for which child is left child, and continue up until you find paren with more than one child
							while (!(parentNode==null) && leftChild ) {
								//find out which one of the children this is
								if (!(parentNode.getParentNode()==null)) childIndex = parentNode.getParentNode().getChildNodes().indexOf(parentNode);
								parentNode = parentNode.getParentNode();
								if (childIndex>0) leftChild = false; //breaks from while loop
							}
							
							if (!(parentNode==null)) {	//you found a parent for which the current branch is not attached to left child (goalslot is not left slot) 
								goalCategory = parentNode.getChildNodes().get(childIndex).getName();	//.getProductionName();
								//goalSlotPlusLC = goalNodeLabel + "@" + leftCornerLabel;
								goalCategPlusLC = goalCategory + "@" + leftCornerLabel;
							}
							
							else {	//no parent for which not left child: left chaining, goalslot is START_slot2
								//in this case the goal category is simply the TOP node production	
								//goalSlotPlusLC = topNode.getProductionName() + leftCornerLabel;	
								goalCategory = "TOP";
								goalCategPlusLC = "TOP@" + leftCornerLabel;
							}
							
							//System.out.println("projection; projectedNodeLabel=" + projectedNodeLabel + "; nodeSlotCombi=" + nodeSlotCombi);
							//HashMap<String, HashMap<String, Double>> projections
							
						}
						else {	//non-left child: b-u attachment
							//keep the frequency counts, probabilities are stored after normalization
							// nodeSlotCombi = goalNodeLabel + "@" + leftCornerLabel;
							goalCategory = parentNode.getChildNodes().get(childIndex).getName();	//.getProductionName();
							//treeNode.goalCategory = goalNodeLabel;
							
							goalCategPlusLC = goalCategory + "@" + leftCornerLabel;	// + (childIndex +1) + "@"
							projectedRuleOrAttach = "attach";
							//System.out.println("attachment;  nodeSlotCombi=" + nodeSlotCombi);
							
						}
						
						//first key is left corner plus goalslot, value is HM of projected nodelabels plus their frequency, given l.c. and goalslot
						HashMap<String, Double> projAndattachFreqOfLCGoal;
						if (lcProbabilities.get(goalCategPlusLC)==null) {
							projAndattachFreqOfLCGoal = new HashMap<String, Double>();
							lcProbabilities.put(goalCategPlusLC, projAndattachFreqOfLCGoal);
						}
						else projAndattachFreqOfLCGoal = lcProbabilities.get(goalCategPlusLC);
						
						if (projAndattachFreqOfLCGoal.get(projectedRuleOrAttach)==null) 
							projAndattachFreqOfLCGoal.put(projectedRuleOrAttach, 1.);
						else projAndattachFreqOfLCGoal.put(projectedRuleOrAttach, projAndattachFreqOfLCGoal.get(projectedRuleOrAttach) + 1.);
						
												
						////////////////////////////////
						// back-off from partial left corner: given the binary rule <X...Z> --> <X...Y> Z, with left corner <X...Y>,
						// then estimate P(<X...Z>|X) (conditioned on partial l.c.) for arbitrary Y. 
						// or, given the unary rule X --> <X...Y> 
						// then estimate back-off prob. P<unary|X> where X is the left component of the left corner
						// (other way round than top-down unary: <X...Y> --> X :nothing to backoff, but include with backed-off X...also if leftCornerLabel does not contain ">")
						// so you must reduce nextProduction/projectedRuleOrAttach to its leftHandSide: <X...Z>, 
						//or in case of unary rule X --> <X...Y>: leftHandSide=X, because rhs is arbitrary and backed off!
						// or in case projectedRuleOrAttach = "attach" then leave it.
						
						//dit betekent dat unary regels vd vorm <X...Y> --> X worden samengevoegd met <X...Z> --> <X...Y> Z,
						//want ze krijgen allebei dezelfde backed off leftcorner
						
						String backedOffGoalCategPlusLC = goalCategPlusLC;
						if (leftCornerLabel.contains(">")) 
							backedOffGoalCategPlusLC = goalCategory + "@" + leftCornerLabel.split(">")[0];
								
						String backedOffRule = projectedRuleOrAttach;	//for attach
						if (backedOffRule.contains("*"))	// backedOffRule = leftHandSide
							backedOffRule = backedOffRule.split("\\*")[0];
						
						HashMap<String, Integer> backedOffProjectOrAttachRules = null;
						if (!( backoffCounts.get(backedOffGoalCategPlusLC)==null)) backedOffProjectOrAttachRules = backoffCounts.get(backedOffGoalCategPlusLC);
						else {
							backedOffProjectOrAttachRules = new HashMap<String, Integer>();
							backoffCounts.put(backedOffGoalCategPlusLC, backedOffProjectOrAttachRules);
						}
						//leftHandSide = <X...Z>, or in case of unary rule X --> <X...Y>: leftHandSide=X
						if (backedOffProjectOrAttachRules.get(backedOffRule)==null) backedOffProjectOrAttachRules.put(backedOffRule, 1);
						else backedOffProjectOrAttachRules.put(backedOffRule, backedOffProjectOrAttachRules.get(backedOffRule)+1);
							
						//////////////////////////////////////
						
						//shift probabilities
						//if (experiments.INCLUDE_SHIFT_PRODUCTIONS) {
							if (treeNode.getType()==parser.parameters.TERMINAL) {
								String shiftTerminalLabel = treeNode.getProductionName();
								//find starred nonterminal from which shift happened
								String dottedNonTerminal = "", goalCat="", goalCatPlusLC="", shiftRule="";
								
								if (treeNode.getLeftSpan()>0) {
				
									String shiftProduction = lcsGrammar.findShiftProductionAssociatedWithTerminal(myParseTree, treeNode);
									dottedNonTerminal = shiftProduction.split("~")[0];
									goalCat = shiftProduction.split("~")[1];
									goalCatPlusLC = goalCat + "@" + dottedNonTerminal;
									shiftRule = shiftTerminalLabel + "*" + dottedNonTerminal;
									
									//treeNode.goalCategory = shiftProduction.split("~")[1];
									//System.out.println("shiftProduction=" + shiftProduction);
											
									HashMap<String, Double> shiftTerminalCounts = null;
									if (lcProbabilities.get(goalCatPlusLC)==null) {	
										//new HM
										shiftTerminalCounts = new HashMap<String, Double>();
										lcProbabilities.put(goalCatPlusLC, shiftTerminalCounts);
										shiftTerminalCounts.put(shiftRule, 1.);
									}
									else {
										shiftTerminalCounts = lcProbabilities.get(goalCatPlusLC);
										if (shiftTerminalCounts.get(shiftRule)==null) 
											shiftTerminalCounts.put(shiftRule, 1.);
										else shiftTerminalCounts.put(shiftRule, shiftTerminalCounts.get(shiftRule) + 1.);
									}
									//shiftProductions.add(shiftProduction);
									
								}
								else {	//leftSpan=0: shift from START^1 "START^1~"
									goalCatPlusLC = "TOP@START^1";
									dottedNonTerminal = "START^1";
									HashMap<String, Double> shiftTerminalCounts = lcProbabilities.get(goalCatPlusLC);
									//treeNode.goalCategory = "TOP";
									shiftRule = shiftTerminalLabel + "*START^1";	//word*starredNonT
									if (shiftTerminalCounts.get(shiftRule)==null) 
										shiftTerminalCounts.put(shiftRule, 1.);
									else shiftTerminalCounts.put(shiftRule, shiftTerminalCounts.get(shiftRule) + 1.);
								}
						
								// back-off: given the shift rule <X...Y>* --> w (given lc <X...Y>* and gc)
								// then you want back-off probabilities P(w|X*, g.c.) where X occurs in <X...Y>*
								backedOffGoalCategPlusLC = goalCatPlusLC;	//if simple left corner it stays the same
								
								if (dottedNonTerminal.contains(">")) 	//compound node, not START^1
									backedOffGoalCategPlusLC = goalCat + "@" + dottedNonTerminal.split(">")[0] + "^1";
									
								String backedOffShiftRule = shiftTerminalLabel;
								
								HashMap<String, Integer> conditionalShiftRule = null;
								if (!(backoffCounts.get(backedOffGoalCategPlusLC)==null)) conditionalShiftRule=backoffCounts.get(backedOffGoalCategPlusLC);
								else {
									conditionalShiftRule = new HashMap<String, Integer>();
									backoffCounts.put(backedOffGoalCategPlusLC, conditionalShiftRule);
								}
								if (conditionalShiftRule.get(backedOffShiftRule)==null) conditionalShiftRule.put(backedOffShiftRule, 1);
								else conditionalShiftRule.put(backedOffShiftRule, conditionalShiftRule.get(backedOffShiftRule)+1);
							
								
							}
						//}	//if (INCLUDE_SHIFT_PROBABILITIES)
						
					}	//	if (!(parentNode==null)) {
			}	//for (dopparser.Node treeNode : myParseTree.getNodes())
		}	//for (String simpleSentence : completeSentences)
		
		//normalizeSymbolicLeftCornerProbabilities(lcProbabilities);
		for (String gcPluslc : lcProbabilities.keySet()) {
			HashMap<String, Double> conditionalShiftProbs = lcProbabilities.get(gcPluslc);
			double totalProbability = 0.;
			
			//compute totals, sum over all projectedNodeLabel for a certain goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
			for (String shiftedTerminalLabel : conditionalShiftProbs.keySet()) {			
				totalProbability += conditionalShiftProbs.get(shiftedTerminalLabel);	
			}
		
			//normalize
			if (totalProbability==0.) totalProbability=1.;
			for (String shiftedTerminalLabel : conditionalShiftProbs.keySet()) {
				conditionalShiftProbs.put(shiftedTerminalLabel, conditionalShiftProbs.get(shiftedTerminalLabel)/totalProbability);
			}
		}
		
		//normalize back-off probabilities: HashMap<String, HashMap<String, Integer>> backoffCounts
		//HashMap<String, HashMap<String, Double>> backOffProbabilities
		for (String gcPluslc : backoffCounts.keySet()) {
			HashMap<String, Integer> conditionalBackOffCounts = backoffCounts.get(gcPluslc);
			HashMap<String, Double> conditionalBackOffProbabilities = new HashMap<String, Double>();
			backOffProbabilities.put(gcPluslc, conditionalBackOffProbabilities);
			int totalCounts = 0;
			
			//compute totals, sum over all projectedNodeLabel for a certain goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
			for (String myRule : conditionalBackOffCounts.keySet()) {			
				totalCounts += conditionalBackOffCounts.get(myRule);	
			}
		
			//normalize
			if (totalCounts==0) totalCounts=1;
			for (String myRule : conditionalBackOffCounts.keySet()) {
				
				conditionalBackOffProbabilities.put(myRule, ((double) conditionalBackOffCounts.get(myRule))/((double) totalCounts));
			}
		}
		
		
		return lcProbabilities;
	}
	/*
	 * Groups all episodicConstituents with the same left hand side
	 * in a single HashSet, indexed by lhs in nodesWithSameRootLabel

	public HashMap<String, HashSet<episodicConstituent>> createSetOfNodesWithSameRootLabel() {

		for (String nodeName : episodicNonTerminalProductions.keySet()) {  
			//HashMap<String, HashSet<hpndop.Node>> nodesWithSameRootLabel
			String rootLabel = nodeName.split("\\*")[0];
			HashSet<episodicConstituent> nodesWithThisLabel;
			if (nodesWithSameRootLabel.get(rootLabel)==null) {
				nodesWithThisLabel = new HashSet<episodicConstituent>();
				nodesWithSameRootLabel.put(rootLabel, nodesWithThisLabel);
			}
			else nodesWithThisLabel = nodesWithSameRootLabel.get(rootLabel);
			nodesWithThisLabel.add(episodicNonTerminalProductions.get(nodeName));
		}
		for (String nodeName : episodicInputNodes.keySet()) { 
			//create HashSet with single member (for compatibility with nonterminals)
			HashSet<episodicConstituent> nodesWithThisLabel  = new HashSet<episodicConstituent>();
			nodesWithThisLabel.add(episodicInputNodes.get(nodeName));
			nodesWithSameRootLabel.put(nodeName, nodesWithThisLabel);
		}

		return nodesWithSameRootLabel;
	}
	 */

	/** 
	 * Fills episodicConstituent with rankedETNs from parseTrees, which it finds by
	 * reconstructing left corner resp. left most topdown derivation
	 * at the same time puts temporary ETNs in dopparser.Node's of the parseTree -> parseTreesWithETNs
	 */
	public void fillEpisodicNodeswithTraces(ArrayList<parseTree> parseTrees) throws Exception {

		int myCounter = 0;

		for (parseTree myParseTree : parseTrees) {

			//Note: these must be parseTrees without unary nodes! not sure
			if (myCounter % 500 ==0) System.out.println("############    " + myCounter + " out of " + parseTrees.size() + "  sentences processed for episodic traces   #############");
			ArrayList<String> bindingsOfDerivation = new ArrayList<String>();

			if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL ) bindingsOfDerivation=rankBindingsForLeftCornerDerivation(myParseTree, myCounter);
			else {
				if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL ) bindingsOfDerivation=rankBindingsForTopDownDerivation(myParseTree, myCounter);
				
			}

			/////////////////////////////////////////////////////////
			/////////////////          CHECK         ////////////////
			/////////////////////////////////////////////////////////
			
			if (myCounter<5) {
				//System.out.println("#####  myParseTree: " + myParseTree.printWSJFormat() + "  ####");
				for (String bnd : bindingsOfDerivation) {
					//System.out.println(bnd.split("~")[0] + "   NEXT:" + bnd.split("~")[1] + "   GOALC+LC:" + bnd.split("~")[2]);	// + "   SISTER:" + bnd.split("~")[3]
				
					
					//check if it matches protected HashMap<String, HashMap<String, Double>> leftCornerProbabilities
					/*
					System.out.println("left corner prob of " + bnd.split("~")[1] + " given " + bnd.split("~")[2] +  "=" + historyFreeProbabilities.get(bnd.split("~")[2]).get(bnd.split("~")[1]));
					double totalP=0d;
					for (String rule : historyFreeProbabilities.get(bnd.split("~")[2]).keySet()) {
						System.out.println("rule=" + rule + "; P=" + historyFreeProbabilities.get(bnd.split("~")[2]).get(rule));
						totalP +=historyFreeProbabilities.get(bnd.split("~")[2]).get(rule);
					}
					System.out.println("totalP=" + totalP);
					*/
					/*
					//backOffProbabilities
					String backedOffGoalCategPlusLC = bnd.split("~")[2];
					//if shift rule, then left corner ends with ^1
					//goalCPlusHalfLeftCorner = goalCat + "@" + dottedNonTerminal.split(">")[0] + "^1";
					String shiftExtension ="";
					if (backedOffGoalCategPlusLC.split("@")[1].endsWith("^1")) shiftExtension = "^1";
					if (backedOffGoalCategPlusLC.contains(">")) 
						backedOffGoalCategPlusLC = backedOffGoalCategPlusLC.split(">")[0] + shiftExtension;
					
					//reduce nextProduction to its lhs
					String backedOffNextProduction = bnd.split("~")[1];
					if (backedOffNextProduction.contains("*")) 
						backedOffNextProduction = backedOffNextProduction.split("\\*")[0];
					
					//ECHTER indien shift rule (X*-->word) dan is juist backedOffNextProduction = backedOffNextProduction.split("\\*")[1];
					//if ((backedOffNextProduction.contains("^")) )
					//	backedOffNextProduction = bnd.split("~")[1].split("\\*")[1];
					
					System.out.println("nextProduction=" + bnd.split("~")[1] + "; backedOffNextProd=" + backedOffNextProduction + "; conditionCtxt=" + bnd.split("~")[2] + "; backoffContext=" + backedOffGoalCategPlusLC + "; P_boff=" + backOffProbabilities.get(backedOffGoalCategPlusLC).get(backedOffNextProduction));
					double totalP=0d;
					for (String backedOffRule : backOffProbabilities.get(backedOffGoalCategPlusLC).keySet()) {
						//System.out.println("rule=" + backedOffRule + "; P=" + backOffProbabilities.get(backedOffGoalCategPlusLC).get(backedOffRule));
						totalP +=backOffProbabilities.get(backedOffGoalCategPlusLC).get(backedOffRule);
					}
					System.out.println("totalP=" + totalP);
					*/
									/*
					//uniform probs
					//xxx think about attachments: more than 1/uniform
					String conditioningContext = bnd.split("~")[2].split("@")[1];	//only the left corner (no splitting at >)
					double P3 = 1./((double) this.getUniformSmoothLevelCounts().get(conditioningContext));	//uniform
					
					if (bnd.split("~")[1].equals("attach")) {
						if (conditioningContext.equals(bnd.split("~")[2].split("@")[0])) {	//only non-zero if lc=gc
							P3 = ((double) this.getUniformSmoothLevelAttachCounts().get(conditioningContext))/((double) this.getUniformSmoothLevelCounts().get(conditioningContext));	//uniform
						System.out.println("getUniformSmoothLevelAttachCounts=" + this.getUniformSmoothLevelAttachCounts().get(conditioningContext) + "; getUniformSmoothLevelCounts=" + this.getUniformSmoothLevelCounts().get(conditioningContext)); }
						else P3=0.;
					}
					NumberFormat numberFormatter = new DecimalFormat("#.######");
					System.out.println("uniform lc probs for " + bnd.split("~")[1] + " conditioned on lc=" + conditioningContext + " and gc=" + bnd.split("~")[2].split("@")[0] + "; P3=" + numberFormatter.format(P3));
					//double P3 = 1./((double) this.getUniformSmoothLevelCounts().get(conditioningContext));	//uniform
					*/	
				}	//for (String bnd : bindingsOfDerivation)
			}	//if (myCounter<5)
			
			myCounter++;
		}

	}

	
	/**
	 * Computes the log likelihood of a sentence according to the episodic probability model.
	 * First computes ordered sequence of bindings of myTestParseTree according to either 
	 * left corner or topdown derivation. Then calls 
	 * computeBindingProbabilitiesGivenPartialPathAndRankedETNs
	 * with the computed bindings as argument.
	 * 
	 * @param sentenceCounter for printing feedback
	 * @param totalNrTestSentences for printing feedback
	 * @param myTestParseTree parseTree of test sentences for which you want to compute the likelihood
	 * @return logSentenceProbabilities log of sentenceProbabilities as a function of parametrized history
	 */
	public double[] computeLikelihoodOfSentenceWHistory(int sentenceCounter, int totalNrTestSentences, parseTree myTestParseTree, int nrCandidate, ArrayList<Integer> nrOfFragmentsInShortestDerivation) {

		ArrayList<String> bindingsOfDerivation = null;
		
		//remove identical sentences and duplicates
		/**
		 * For every episodicConstituent stores the sentenceNrs (Integer) of duplicates, 
		 * and corresponding ArrayList of ETNs, to be put back after you are done with the shortest derivation
		 */
		HashMap<episodicConstituent, HashMap<Integer, ArrayList<Integer[]>>> ETNsRemovedFromEpisodicCst = new HashMap<episodicConstituent, HashMap<Integer, ArrayList<Integer[]>>>();
		
		if (Main.COMPUTE_SHORTEST_DERIVATION) {
			//get the sentenceNrs of the duplicates: HashMap<String, HashSet<Integer>> duplicateSentenceNrsInTrainSet;
			String myParseString = myTestParseTree.printWSJFormat();
			HashSet<Integer> sentenceNrsOfDuplicates = null;
			if (!(sentencePreprocessor.duplicateSentenceNrsInTrainSet.get(myParseString)==null)) {
				sentenceNrsOfDuplicates = sentencePreprocessor.duplicateSentenceNrsInTrainSet.get(myParseString);
				
				//System.out.println("This sentenceNr=" + sentenceCounter + "; duplicate sentenceNrs=" + sentenceNrsOfDuplicates);
				//loop over episodicConstituents and remove (and store) ETNs of duplicates
				for (String prod : this.episodicNonTerminalProductions.keySet()) {
					episodicConstituent thisConstituent = this.episodicNonTerminalProductions.get(prod);
					HashMap<Integer, ArrayList<Integer[]>> removedSentencesAndETNsFromEpiCst = new HashMap<Integer, ArrayList<Integer[]>>();
					//for (Integer sentenceNr : thisConstituent.getRankedETNs().keySet()) {
					//for (Iterator<Integer> it = thisConstituent.getRankedETNs().Iterator(); it.hasNext();) {
					//	Integer sentenceNr = (Integer) it.next();
					//	if (sentenceNrsOfDuplicates.contains(sentenceNr)) {
					for (Integer sentenceNr : sentenceNrsOfDuplicates) {
							ArrayList<Integer[]> removedETNs = thisConstituent.getRankedETNs().remove(sentenceNr);
							if (!(removedETNs==null)) removedSentencesAndETNsFromEpiCst.put(sentenceNr, removedETNs);
						//}
					}
					ETNsRemovedFromEpisodicCst.put(thisConstituent, removedSentencesAndETNsFromEpiCst);
				}	//for (String prod : this.episodicNonTerminalProductions.keySet())
				
			}
			else {
				if (Main.FEEDBACK) System.out.println("Sentence " + sentenceCounter + " does not occur in train set: " + myParseString);
			}
		//remove them from all episodicConstituents
		/*
		 * voor elke episodicConstit:
			ArrayList<Integer[]> = this.rankedETNs.remove(sentenceNr); + alle duplicates.
			die moet je onthouden: 
			HashMap<episodicConstituent, HashMap<sentenceNr, ArrayList<Integer[]>>>),
			 en vervolgens terugzetten in de juiste episodicConstit.

		 */
		
		}
		
		if (Main.INCLUDE_RECENT_SHORTEST_DERIVATIONs) {
			boolean bln_print=false;
			if (sentenceCounter==100) bln_print=true;
			
			//check fragmentIDs in epCst
			//if (sentenceCounter==100) checkFragmentETNs();
			
			for (int precedingSentenceNr = sentenceCounter-Main.RECENCY; precedingSentenceNr <= sentenceCounter-Main.RECENCY; precedingSentenceNr++) {
				if (precedingSentenceNr>=0)
					Main.putETNsFromOneShortestDerivationInEpisodicConstituents(precedingSentenceNr, this, bln_print);
			}
			//check fragmentIDs in epCst
			//if (sentenceCounter==100) checkFragmentETNs();
			
		}
		
		//System.out.println("xxxxx " + myTestParseTree.printWSJFormat());
		//parameter -1 indicates that the rankedETN will not be stored in episodicConstituent
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL) bindingsOfDerivation = rankBindingsForLeftCornerDerivation(myTestParseTree, -1);
		else {
			if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL) bindingsOfDerivation = rankBindingsForTopDownDerivation(myTestParseTree, -1);		
		}


		//#### check
		if (Main.FEEDBACK) {
			System.out.println("test sentence #" + sentenceCounter + " out of " + totalNrTestSentences + "; ordered bindings of derivation for " + myTestParseTree.printWSJFormat() + ":");
			for (String binding : bindingsOfDerivation) {
				System.out.println(binding);				
			}
		}

		
		//clear all activation values left over in the traces from the previous sentence
		if (Main.INCLUDE_DISCONTIGUITIES_OLD) {
			//for (String nodeLabel : this.episodicInputNodes.keySet()) {
			//	this.episodicInputNodes.get(nodeLabel).clearActivationValuesOfRankedETNs();
			//}
			for (String nodeLabel : this.episodicNonTerminalProductions.keySet()) {
				this.episodicNonTerminalProductions.get(nodeLabel).clearActivationValuesOfRankedETNs();
			}
		}

		/**
		 * log of sentenceProbabilities as a function of parametrized history
		 */
		double[] logSentenceProbabilities = null;

		ArrayList<String> longestCommonPathFragments = new ArrayList<String>();

		//////////////////////////////////////////////////
		////      COMPUTE LIKELIHOOD OF SENTENCE      ////
		logSentenceProbabilities = computeBindingProbabilitiesGivenPartialPathAndRankedETNs(bindingsOfDerivation, longestCommonPathFragments, sentenceCounter, nrCandidate);

		
		if (Main.COMPUTE_SHORTEST_DERIVATION) {
			
			//Array of length 1: only for returning the size/#fragments
			nrOfFragmentsInShortestDerivation.add(longestCommonPathFragments.size());
			
			if (Main.COMPUTE_GREEDY_SHORTEST_DERIVATION) {
				shortestDerivations.add(longestCommonPathFragments);
			}
			
			//put the ETNs for the duplicates back again	
			//HashMap<episodicConstituent, HashMap<Integer, ArrayList<Integer[]>>> ETNsRemovedFromEpisodicCst = new HashMap<episodicConstituent, HashMap<Integer, ArrayList<Integer[]>>>();
			for (episodicConstituent thisConstituent : ETNsRemovedFromEpisodicCst.keySet()) {
				for (Integer sentenceNr : ETNsRemovedFromEpisodicCst.get(thisConstituent).keySet()) {
					for (Integer[] myETN : ETNsRemovedFromEpisodicCst.get(thisConstituent).get(sentenceNr))
						thisConstituent.addRankedETN(myETN, sentenceNr);
				}
			}
		}
		
		if (Main.INCLUDE_RECENT_SHORTEST_DERIVATIONs) {
			//remove the ETNs corresponding to the fragments of the shortest derivation again
			for (int precedingSentenceNr = sentenceCounter-Main.RECENCY; precedingSentenceNr <= sentenceCounter-Main.RECENCY; precedingSentenceNr++) {
				if (precedingSentenceNr>=0)
			//for (int precedingSentenceNr = sentenceCounter+1; precedingSentenceNr<=sentenceCounter+10; precedingSentenceNr++) {
			//	if (precedingSentenceNr<totalNrTestSentences)
					Main.removeETNsFromOneShortestDerivationInEpisodicConstituents(precedingSentenceNr, this);
			}
			
			//check fragmentIDs in epCst
			//if (sentenceCounter==100) checkFragmentETNs();
		}
		
		return logSentenceProbabilities;

	}

	
	
	/* 
	 * starting from first word, along derivation determine parser operation at that point, that is binding from node to slot determine prob of particular binding of given LC derivation (attach or project to slot) conditioned on history 1, 2, 3 etc.
	// given a certain history, find out if any of the ETN's along the path of the entire history
	 are shared: this is generalization of computeLCProbabilitiesFromETNs
	 */


	/**
	 * Given sequence of bindings of a test sentence, this method computes the binding probabilities
	 * (that is project, attach or shift probabilities for lcp, or predict prob. for topdown parser) 
	 * for every binding according to the episodic prob. model, by weighing traces of all exemplars 
	 * that are stored in visited nodes along the test derivation with their common histories (pathLength).
	 * Returns the product of the binding probabilities, parametrized by max. considered history.
	 * 
	 * @param bindingsOfTestSentence ordered sequence of bindings in format
	 * leftCornerConstituentProductionName + "~" + boundConstituentProductionName + "~" + attachInfo, 
	 * (and computed in rankBindingsForLeftCornerDerivation or rankBindingsForTopDownDerivation)
	 * @param longestFragmentsOfShortestDerivation
	 * @param sentenceCounter
	 * @return sentenceProbabilities as a function of parametrized history
	 */
	public double[] computeBindingProbabilitiesGivenPartialPathAndRankedETNs(ArrayList<String> bindingsOfTestSentence, ArrayList<String> longestFragmentsOfShortestDerivation, int sentenceCounter, int nrCandidate) {

		//timing
		long currentTime = System.currentTimeMillis();
		long startTime = currentTime;
		
		NumberFormat numberFormatter = new DecimalFormat("#.######");
		//along the derivation of the test sentence, keep track of all episodic paths
		//length of conditioning history 


		/**
		 * HashMap&lt;Integer: sentenceNr, TreeMap&lt;Integer: rankNr, traceFields: CH, SD etc.&gt;&gt;
		 * multiple traces (ETN) per sentenceNr possible within an episodicConstit
		 */
		HashMap<Integer, TreeMap<Integer, traceFields>> previousNodeTraceActivations = new HashMap<Integer, TreeMap<Integer, traceFields>>();
		
			
		/**
		 * For every sentenceNr there is a stack of states (ArrayList), with last visited state on top of stack, where
		 * every state has one (or more) Integer[]={highest rankNr, pathLength} (no need to keep pathLength=1) 
		 */
		if (Main.INCLUDE_DISCONTIGUITIES && !Main.SYNTACTIC_PRIMING) discontiguousFragments.clear();
		/*
		else { //check only:
			for (Integer sentenceNrOfETN : discontiguousFragments.keySet()) {
			ArrayList<Double[]> discontiguousFragmentOfSentenceNr = discontiguousFragments.get(sentenceNrOfETN);
			if (!(discontiguousFragmentOfSentenceNr==null) && discontiguousFragmentOfSentenceNr.size()>0) System.out.println("discontiguousFragmentOfSentenceNr.size()=" + discontiguousFragmentOfSentenceNr.size());
			}}
		*/
		
		/**
		 * sentenceProbabilities as a function of parametrized history
		 */
		double[] sentenceProbabilities = {1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.};	//new double[12] separate probability for every max history 
		long time1=0, time2=0, time3=0, time4=0, time5=0, timeDISCTG=0, timeDISCTG2=0;
		int longestPathLength = 0, bindingCounter=0, treeletCounter=0;
		String longestCommonPathFragment = "";
		int nrIterationsCheck = 0, nrUnknownProductions=0;

		treeletState[] treeletStates = new treeletState[bindingsOfTestSentence.size()];
		
		for (String binding : bindingsOfTestSentence) {


			//binding = this.production~next.production~conditioningInfo~leftSisterOfThis 
			//for top-down: conditioningInfo=rootLabel; for leftcorner: conditioningInfo=gc+ "@" + lc; back-off info can be extracted from labels of conditioningInfo.
			//next.production.hashCode() (in testderivation) must correspond with rankedETN[2] van de trace in this.production.

			/**
			 * productionName of the bound constituent (if nonterminal then it is in format parent*chi1*chi2*etc.
			 */
			String thisProduction = binding.split("~")[0];
			//remove the postag for inputNodes: nodeLabel = postag@word, but in HPN postags have been removed from names
			if (thisProduction.contains("@")) thisProduction = thisProduction.split("@")[1];	

			/**
			 * productionName of the recipient of the binding (episodicConstituent)
			 */
			String nextProduction = binding.split("~")[1];
			int nextProductionHC = nextProduction.hashCode();

			//if (experiments.FEEDBACK) System.out.println("rightSideOfBinding=" + rightSideOfBinding + "; rightSideOfBindingHC=" + rightSideOfBindingHC);
			if (Main.FEEDBACK)
				System.out.println("binding=" + binding + "; thisProduction=" + thisProduction + "; nextProduction=" + nextProduction + "; nextProductionHC=" + nextProductionHC);	// + "; boundNode=" + boundNode

			boolean hasLeftSister = false;

			
			if (Main.INCLUDE_DISCONTIGUITIES_OLD) {
				currentTime = System.currentTimeMillis();
				episodicConstituent leftSister = null;
				
				//retrieve activations (pathlengths) of all rankedETNs of the left sister, for use inside the rankedETN loop
				if ((Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL && nextProduction.equals("attach")) ||
						(Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL && !(binding.split("~")[3].equals("-")))) {

					//retrieve episodicConstituent corresponding to leftSisterLabel (if starred nonT this includes full productionName, e.g., S1.*NP*VP)
					//binding.split("~")[3] is the leftSisterLabel
					leftSister = this.episodicNonTerminalProductions.get(binding.split("~")[3]);
	
					if (!(leftSister==null)) hasLeftSister = true;	
				}
				timeDISCTG += System.currentTimeMillis() - currentTime;
			}
			
			
			currentTime = System.currentTimeMillis();

			//look at all rankedETNs in bound node, and decide whether they are continuation of episodic path or not
			//ArrayList because the same node can be visited more than once by an exemplar; Integer[] = rankNr, pathlength

			
			int rankNrOfETN = -1;
			/**
			 * Stores TreeMap<rankNrOfETN, traceState> of a trace with a certain sentenceNr;
			 * possibly multiple pairs if exemplar sentence has visited the node multiple times.
			 * traceState is data structure for storing `state' properties of a trace: commonHistory, derivationLength and viterbiPointer
			 */
			TreeMap<Integer, traceFields> currentNodeTraceActivationsOfSentenceNr = new TreeMap<Integer, traceFields>(); 


			/**
			 * Frequencies of common pathLengths for all rankedETNs combined
			 */
			int[] pathLengthFrequencies = new int[24];	
			/**
			 * Frequencies of pathLengths of rankedETNs that 
			 * continue with same binding as test derivation
			 */
			int[] pathLengthFrequenciesWithCorrectBinding = new int[24];

			Integer pathLength=0;
			int cutOffPathLen=0, nextProductionStoredInETN = 0;	
			double contribution=0d;

			/**
			 * for finding fragments of shortest derivations
			 */
			boolean pathOfLongestTraceExtended=false;

			/////////////////////////////////////////////////////////////////////////////////////////
			//////     LOOP OVER rankedETNs leftConstituent    //////
			/////////////////////////////////////////////////////////////////////////////////////////
			//was: for(episodicConstituent nodeWithSameRootLabel : this.nodesWithSameRootLabel.get(rootLabel)) {
			//System.out.println("nodeWithSameRootLabel.getRankedETNs().size=" + nodeWithSameRootLabel.getRankedETNs().size());

			/**
			 * a constituent filled with traces: look it up by the production, use it only find traces
			 */
			episodicConstituent thisConstituent = this.episodicNonTerminalProductions.get(thisProduction);
			
			//episodicConstituent exist only for productions that occur in train treebank
			if (!(thisConstituent==null)) {
				nrIterationsCheck +=  thisConstituent.getRankedETNs().size();

				time1 += System.currentTimeMillis() - currentTime;

				//was: HashMap<Integer, TreeMap<Integer, Integer>> currentNodeTraceActivations = new HashMap<Integer, TreeMap<Integer, Integer>>();
				
				/**
				 * keep for every iteration through bindings the trace activations of the current treelet
				 */
				treeletStates[treeletCounter]  = new treeletState(thisConstituent);
				
				//stores trace activations (traceState) of all traces in previous treelet
				/**
				 * traceActivations = HM<Integer: sentenceNr, TreeMap<Integer: rankNr, traceFields: properties of trace>>
				 */
				if (treeletCounter>0) previousNodeTraceActivations = treeletStates[treeletCounter-1].getTraceActivations();
				
				//pre-filter: select the traces with the shortest derivations only
				int[] shortestDerivationViterbiPointer = new int[2]; //{shortestDerivationSentenceNr, shortestDerivationRankNr, shortestDerivationLength}
				double previousShortestDerivationLength = 1000d;
				double[] shortestDerivationTraceOfCurrentTreelet = new double[3];
				//double[] shortestDerivationTraceOfPredecessor;
				
				if (Main.COMPUTE_SHORTEST_DERIVATION && treeletCounter>0) {	//selectedTraceShortestDerivations
					double[] shortestDerivationTraceOfPreviousTreelet = prefilterTracesWithShortestDerivations(previousNodeTraceActivations);
					shortestDerivationViterbiPointer[0] = (int) shortestDerivationTraceOfPreviousTreelet[0];
					shortestDerivationViterbiPointer[1] = (int) shortestDerivationTraceOfPreviousTreelet[1];
					previousShortestDerivationLength = shortestDerivationTraceOfPreviousTreelet[2] + 1.;	//increment by 1 assuming that this derivation is not continued with the same exemplar
				}	
				
				/////////////////////////////////////////
				//////     LOOP OVER rankedETNs    //////
				/////////////////////////////////////////
				//possibly empty if this production doesn't occur in the train set!
				//System.out.println("#traces=" + thisConstituent.getRankedETNs().size());
				
				for (int sentenceNrOfETN : thisConstituent.getRankedETNs().keySet()) {

					if (Main.FEEDBACK) System.out.println(">>>>> sentenceNrOfETN=" + sentenceNrOfETN);
					// if computeShortestDerivation then don't allow to use the same sentence in the shortest derivation as the current one
					//if (!experiments.COMPUTE_SHORTEST_DERIVATION || (experiments.COMPUTE_SHORTEST_DERIVATION && !(sentenceNrOfETN==sentenceCounter))) {

					//put empty treemap of activations in current treeletState
					//xxx VEEL TE INGEWIKKELD! tracefields moeten gewoon samen met traces in een enkel object
					TreeMap<Integer, traceFields> traceActivationsOfOneSentence = new TreeMap(Collections.reverseOrder());	//new TreeMap<Integer, Integer>();
					treeletStates[treeletCounter].getTraceActivations().put(sentenceNrOfETN, traceActivationsOfOneSentence);

					TreeMap<Integer, traceFields> previousNodeTraceActivationsOfSentenceNr = previousNodeTraceActivations.get(sentenceNrOfETN);
					
					
					for (Integer[] rankedETN : thisConstituent.getRankedETNs().get(sentenceNrOfETN)) {
						
						currentTime = System.currentTimeMillis();
						rankNrOfETN = rankedETN[0];
						if (Main.FEEDBACK) System.out.println(">>>>> >>>>> rankNrOfETN=" + rankNrOfETN);
						/**
						 * storedBinding is HashCode of the recipient of the binding (episodicConstituent production)
						 */
						nextProductionStoredInETN = rankedETN[1];	//thisConstituent.getRankedETNs().get(fragmentID)[1];

						//test
						//if (sentenceNrOfETN==54) testRankNrsOfSentence54.add(rankNrOfETN);
						
						//////////////////////////////////////////////////////////////
						///////////////    UPDATE TRACE SEQUENCES      ///////////////
						// The pathLength is increased by 1 when in subsequent constituent 
						// rankedETN from same exemplar with rankNr+1 is encountered.
						//////////////////////////////////////////////////////////////

						//in any case you need to create a new entry for the current rankedETN
						//only if another fragment of the same sentence has occurred then keep its array

						time2 += System.currentTimeMillis() - currentTime;
						currentTime = System.currentTimeMillis();

						// 2 cases: - if sentence nr of ranked ETN is contained in oldTraceNumberSequences 
						// AND if rankNr(t) = rank(t-1) + 1: replace rankNr in oldTraceNumberSequences and higher pathLength in oldTraceNumberSequences

						boolean blnFoundEpisode = false;
						
						if (!(previousNodeTraceActivationsOfSentenceNr==null) ) { //ETN incremented by 1 on the path

							//for (Integer rankNr : previousNodeTraceActivations.get(sentenceNrOfETN).keySet()) {
							/*
							if (experiments.COMPUTE_SHORTEST_DERIVATION){
								//select the one with the shortest derivation among those that have same sentenceNr and later rankNr...
								for (Integer rankNr : previousNodeTraceActivationsOfSentenceNr.keySet()) {
									if ((rankNr > rankNrOfETN) && previousNodeTraceActivationsOfSentenceNr.get(rankNr).getDerivationLength() + 0.5 < shortestDerivationLength) {
										shortestDerivationTraceOfCurrentTreelet[0]=sentenceNrOfETN;
										shortestDerivationTraceOfCurrentTreelet[1]=rankNr;
										shortestDerivationTraceOfCurrentTreelet[2]=previousNodeTraceActivationsOfSentenceNr.get(rankNr).getDerivationLength() + 0.5;	//increment with 0.5 because same exemplar	
									}
								}
							}
							*/
							
							traceFields myTrace = previousNodeTraceActivationsOfSentenceNr.get(rankNrOfETN -1);
							if (!(myTrace==null)) {	// (rankNr==(rankNrOfETN -1)): continuation of the episodic path
								
								pathLength = myTrace.getCH() + 1;	//continue path and higher length
									//remove it from previousNodeTraceActivations
									//previousNodeTraceActivationsOfSentenceNr.remove(rankNrOfETN -1);
									blnFoundEpisode = true;
									
									if (Main.COMPUTE_SHORTEST_DERIVATION) {
										//only change pointer to either trace in previous treelet with same sentenceNr (and earlier rankNr), or real predecessor
										//and only do this if that yields an aggregate derivLength that is less than shortestDerivationLength+1
										if (myTrace.getDerivationLength() <= previousShortestDerivationLength) {	//{shortestDerivationSentenceNr, shortestDerivationRankNr, shortestDerivationLength}
											shortestDerivationTraceOfCurrentTreelet[0]=sentenceNrOfETN;
											shortestDerivationTraceOfCurrentTreelet[1]=rankNrOfETN -1;
											shortestDerivationTraceOfCurrentTreelet[2]=myTrace.getDerivationLength();	//no increment with 1 because continuation
										}	
									}
							}
						}	//if (!(previousNodeTraceActivations.get(sentenceNrOfETN)==null) )
						
								
						if (!blnFoundEpisode) {	//either you find a discontiguous fragment, or pathLength is set to 1.
							if (Main.INCLUDE_DISCONTIGUITIES) {
								
								//try to find if there exists in discontiguousFragments same sentenceNr
								//find lowest rankNr in previousNodeTraceActivations and check that it is lower than rankedETN
								if (!(previousNodeTraceActivationsOfSentenceNr==null) && previousNodeTraceActivationsOfSentenceNr.size()>0) {
									//Integer previousRankNr = previousNodeTraceActivationsOfSentenceNr.firstKey();
									//note, that previousNodeTraceActivationsOfSentenceNr is TreeMap in reverseOrder, so you find the highest rankNr<rankNrOfETN
									for (Integer previousRankNr : previousNodeTraceActivationsOfSentenceNr.keySet()) {
											
										if (previousRankNr < rankNrOfETN) {
											//pathLength = Math.max(previousNodeTraceActivationsOfSentenceNr.get(previousRankNr) -experiments.DISCTG_DISCOUNT, 1);
											int previousPathLength;
											//if (experiments.LINEAR_DECAY) previousPathLength = previousNodeTraceActivationsOfSentenceNr.get(previousRankNr)-1; else
											 previousPathLength = (int) Main.DISCTG_DECAY*previousNodeTraceActivationsOfSentenceNr.get(previousRankNr).getCH();
											if (Main.DISCONTIGUITIES_AVERAGED)
												pathLength = Math.max((int) ((previousPathLength+1)*Main.DISCONTIGUITIES_REDUCTION), 1);
											else pathLength = Math.max(previousPathLength, 1);
											//remove it from previousNodeTraceActivationsOfSentenceNr
											previousNodeTraceActivationsOfSentenceNr.remove(previousRankNr);
											if (Main.FEEDBACK) System.out.println("used episode from previous; sentenceNr=" + sentenceNrOfETN + "; previousRankNr=" + previousRankNr + "; rankNrOfETN=" + rankNrOfETN + "; pathLength=" + pathLength);
											blnFoundEpisode = true;
											break;
										}
									}
								}	//if (!(previousNodeTraceActivations.get(sentenceNrOfETN)==null) )
								if (!blnFoundEpisode) {
									//try stored discontiguities
									ArrayList<Double[]> discontiguousFragmentOfSentenceNr = discontiguousFragments.get(sentenceNrOfETN);
									if (!(discontiguousFragmentOfSentenceNr==null) && discontiguousFragmentOfSentenceNr.size()>0) {
										//for certain sentenceNrOfETN there is a stack of states, each state is Integer[] {rankNr, pathLength}
										//find the highest state in the stack for which rankNr<rankNrOfETN
										//was: for (int i= discontiguousFragmentOfSentenceNr.size()-1 ; i>=0; i--) {
										for (ListIterator<Double[]> it = discontiguousFragmentOfSentenceNr.listIterator(discontiguousFragmentOfSentenceNr.size()); it.hasPrevious();) {
											Double[] rankNrPlusPathLength = (Double[]) it.previous();
										
											//rounds pathlength to integer below
											//Integer[] rankNrAndPathLengthOfState = {(int) (discontiguousFragmentOfSentenceNr.get(i)[0]*1), (int) (discontiguousFragmentOfSentenceNr.get(i)[1]*1)};
											//Integer[] rankNrAndPathLengthOfState = {(int) (rankNrPlusPathLength[0]*1), (int) (rankNrPlusPathLength[1]*1)};
											int rankNrOfDisctg = (int) (rankNrPlusPathLength[0]*1);
											int pathLengthOfDisctg = (int) (rankNrPlusPathLength[1]*1);
											
											if (rankNrOfDisctg < rankNrOfETN) {
												if (Main.DISCONTIGUITIES_AVERAGED)
													pathLength = Math.max((int) ((pathLengthOfDisctg+1)*Main.DISCONTIGUITIES_REDUCTION), 1);
												else pathLength = Math.max(pathLengthOfDisctg, 1);
												
												//was: pathLength = Math.max((pathLengthOfDisctg-experiments.DISCTG_DECAY)/2, 1);	// -experiments.DISCTG_DISCOUNT;
												//remove it from discontiguousFragmentOfSentenceNr
												//discontiguousFragmentOfSentenceNr.remove(i);	//previousRankNr
												it.remove();
												if (Main.FEEDBACK) System.out.println("used episode from discontiguousFragments; sentenceNr=" + sentenceNrOfETN + "; previousRankNr=" + rankNrOfDisctg + "; rankNrOfETN=" + rankNrOfETN + "; pathLength=" + pathLength);
												blnFoundEpisode = true;
												break;
											}
										}
									}	//if (!(discontiguousFragments.get(sentenceNrOfETN)==null))
									if (!blnFoundEpisode) {
										//no discontiguous fragments found: set pathLength to 1
										if (Main.FEEDBACK) System.out.println("not found any discontiguous");
										pathLength = 1;
									}
								}	//no episode found in previousNodeTraceActivationsOfSentenceNr
								
							}	//if (experiments.INCLUDE_DISCONTIGUITIES) {
							else { 
								pathLength = 1;
								shortestDerivationTraceOfCurrentTreelet[2] = previousShortestDerivationLength;
								for (int i=0; i<2; i++) shortestDerivationTraceOfCurrentTreelet[i] = shortestDerivationViterbiPointer[i];
							}
							
						}	//if (!blnFoundEpisode) {
								
						//in all cases (discontiguities or not):
						traceFields myTrace = new traceFields();
						myTrace.setCH(pathLength);
						if (Main.COMPUTE_SHORTEST_DERIVATION) {
							myTrace.setDerivationLength(shortestDerivationTraceOfCurrentTreelet[2]);	//either it has already been incremented by 1, or it is successor
							myTrace.setViterbiPointer((int) shortestDerivationTraceOfCurrentTreelet[0], (int) shortestDerivationTraceOfCurrentTreelet[1]);
						}
						currentNodeTraceActivationsOfSentenceNr.put(rankNrOfETN, myTrace);
						
						//currentNodeTraceActivationsOfSentenceNr.get(key)
						//was: currentNodeTraceActivationsOfSentenceNr.get(rankNrOfETN).setCH(pathLength);
											
						if (Main.COMPUTE_GREEDY_SHORTEST_DERIVATION && pathLength>1) {	

							if (pathLength > longestPathLength) {
								pathOfLongestTraceExtended = true;
								longestPathLength = pathLength;
								//keep a reference to the fragment
								longestCommonPathFragment = "" + sentenceNrOfETN + "_" + rankNrOfETN + "_" + pathLength + "_" + bindingCounter;
							}
						}
						
						time4 += System.currentTimeMillis() - currentTime;

						////////////////////////////////////////////////////
						////////    UPDATE pathLengthFrequencies    ////////
						////////////////////////////////////////////////////
						if (!Main.COMPUTE_SHORTEST_DERIVATION) {	//all this unnecessary if you are only interested in shortest derivation
							int maxPathLength = Math.min(Main.MAX_HISTORY, pathLength);

							pathLengthFrequencies[maxPathLength] += 1;
							//if ((storedBinding).equals(boundSlot))
							if(nextProductionStoredInETN==nextProductionHC) {
								pathLengthFrequenciesWithCorrectBinding[maxPathLength] += 1;
							}
						}	//if (!computeShortestDerivation)


					}	//for (Integer[] rankedETN : thisConstituent.getRankedETNs().get(sentenceNrOfETN)) {
					
					//check:
					/*
					if (bindingCounter==bindingsOfTestSentence.size()-1) {
							if (currentNodeTraceActivationsOfSentenceNr.size()>1) 
								System.out.println("currentNodeTraceActivations tralala=" + currentNodeTraceActivationsOfSentenceNr.size());	
					}
					*/
					
				}	//for (int sentenceNrOfETN : thisConstituent.getRankedETNs().keySet()) {
				
				// END OF FRAGMENT (because not extended): remove all entries in episodicPathPlusHighestRank and in episodicPathPlusLength that are not continued in the new node
				//dwz vervang episodicPathPlusHighestRankAndLength door newEpisodicPathPlusHighestRankAndLength
				if (Main.COMPUTE_GREEDY_SHORTEST_DERIVATION && (!pathOfLongestTraceExtended || (bindingCounter==bindingsOfTestSentence.size()-1)) && (bindingCounter>0)) {
					//if the path is not extended it means you are at the end of a fragment: save the longestCommonPathFragment (doesn't matter from which sentence) corresponding to previous longestPathlength
					longestFragmentsOfShortestDerivation.add(longestCommonPathFragment);
					//check: longestCommonPathFragment = "" + sentenceNrOfETN + "_" + rankNrOfETN + "_" + pathLength + "_" + nrBindingOfTestSentence;

					//only for printing fragment trees of shortest derivation
					if (!Main.COMPUTE_SHORTEST_DERIVATION_RERANKER) recordShortestFragments(bindingsOfTestSentence, sentenceCounter,
							bindingCounter, longestCommonPathFragment);

					//set pathLengths of all traces back to 1
					
					//AAA for (Integer sentenceNR : currentNodeTraceActivations.keySet()) {
					for (Integer sentenceNR : treeletStates[treeletCounter].getTraceActivations().keySet()) {
						//System.out.println("tailAndLengthOfCommonPath2 not null; size=" + tailAndLengthOfCommonPath2.size());
						//AAA for (Integer myRankNr : currentNodeTraceActivations.get(sentenceNR).keySet()) 
						for (Integer myRankNr : treeletStates[treeletCounter].getTraceActivations().get(sentenceNR).keySet())
							//myRankedETNPlusLength[1]= 1;
							//AAA currentNodeTraceActivations.get(sentenceNR).put(myRankNr, 1);
							treeletStates[treeletCounter].getTraceActivations().get(sentenceNR).get(myRankNr).setCH(1);
						}
					longestPathLength=1;
					longestCommonPathFragment="";
				}	//if (experiments.COMPUTE_GREEDY_SHORTEST_DERIVATION && (!pathOfLongestTraceExtended || (bindingCounter==bindingsOfTestSentence.size()-1)) && (bindingCounter>0)) {

				treeletCounter++;
			}	//if (!(thisConstituent==null))
			else {
				nrUnknownProductions++;
				if (Main.FEEDBACK) 
					System.out.println("The treelet " + thisProduction + " does not occur in train treebank.");
			}
			////////////////////////////////////////////////////////////////////////////////
			//////////////////// END LOOP OVER RANKEDETNS /////////////////////////////
			///////////////////////////////////////////////////////////////////////////////
			
			//transfer all remaining previousNodeTraceActivations to discontiguousFragmentsOfSentence
			if (Main.INCLUDE_DISCONTIGUITIES) {
				//if there are any previousNodeTraceActivationsifSentenceNr left UNUSED, then transfer them to discontiguousFragments 
				
				for (Integer sentenceNrOfETN : previousNodeTraceActivations.keySet()) {
					//if previousNTActivations not emptied
					if ( previousNodeTraceActivations.get(sentenceNrOfETN).size() >0) {
					
						ArrayList<Double[]> discontiguousFragmentsOfSentence = discontiguousFragments.get(sentenceNrOfETN);
						
						if (discontiguousFragmentsOfSentence==null) {	//--> previousNodeTraceActivations.get(sentenceNrOfETN).size() >0
								discontiguousFragmentsOfSentence = new ArrayList<Double[]>();
								discontiguousFragments.put(sentenceNrOfETN, discontiguousFragmentsOfSentence);
						}
	
						//put rankNr and pathLength from previousNodeTraceActivations that have not been used in discontiguousFragments
						//but only if pathLength>1
						for (Integer previousRankNr : previousNodeTraceActivations.get(sentenceNrOfETN).keySet()) {	//TreeMap
							Integer previousPathLength = previousNodeTraceActivations.get(sentenceNrOfETN).get(previousRankNr).getCH();
							if (previousPathLength>2) {	//because useless to keep discontiguous fragments with pathlength <=2: after decay it will be truncated to 1.
								discontiguousFragmentsOfSentence.add(new Double[] {(double) previousRankNr, (double) previousPathLength});	//the leftovers having this sentenceNr
								//System.out.println("disctg fragment added of length " + previousPathLength);
							}
						}
						
					}	//if ( previousNodeTraceActivations.get(sentenceNrOfETN).size() >0)
				}
				
				//decay all discontiguous fragments (after every binding)
				//HashSet<Integer> removeSmallFragments = null;
				for (Integer sentenceNrOfETN : discontiguousFragments.keySet()) {
					//ArrayList<Double[]> discontiguousFragmentsOfSentence = discontiguousFragments.get(sentenceNrOfETN);
					int counter=0;
					//removeSmallFragments= new HashSet<Integer>();
					for (Double[] rankNrPlusPathLength : discontiguousFragments.get(sentenceNrOfETN)) {
						//if (experiments.LINEAR_DECAY) rankNrPlusPathLength[1] = rankNrPlusPathLength[1] - experiments.DISCTG_DECAY; else
						 rankNrPlusPathLength[1] = Main.DISCTG_DECAY*rankNrPlusPathLength[1];
						//if (rankNrPlusPathLength[1]<2.) removeSmallFragments.add(counter);
						counter++;
					}
					//remove SmallFragments
					//for (Integer myIndex : removeSmallFragments) {
					//	discontiguousFragments.get(sentenceNrOfETN).remove(myIndex);
					//}
				}
				
				
			}	//if (experiments.INCLUDE_DISCONTIGUITIES)
			
			//####################################################################################################
			
				
			currentTime = System.currentTimeMillis();

			////////////////////////////////////////////////////////////////////////////////
			//////      COMPUTE binding probabilities from pathLengthFrequencies      //////
			////////////////////////////////////////////////////////////////////////////////
			if (!Main.COMPUTE_SHORTEST_DERIVATION) {

				//print and compute probability:
				int[] totalWeightedCounts = new int[24];	//one integer for every max history
				int[] totalCorrectCounts = new int[24];

				//add zero-order base context free counts (with or without smoothing unknown productions)

				/**
				 * Conditioning label (instead of rootLabel) 
				 * in case of top-down parser conditioningLabel is the root (left hand side) of a production
				 * in case of left-corner parser conditioningLabel is either the left corner of a production, 
				 * or starred nonterminal (if it is unary production then conditioningLabel is the only daughter) 
				 */
				String conditioningContext = binding.split("~")[2];
				/**
				 * reduce the compound conditioning label (i.e., <X...Y>) to its simple counterpart (i.e., X)
				 */
				String backOffContext = conditioningContext;

				double P1=0d, P2=0d, P3=0d, P_Backoff1=0d, P_Backoff2=0d, P_Backoff3=0d;
				/*
				String goalCategory="", leftCorner1="";
				if (experiments.PROBABILITY_MODEL.equals("leftcorner_with_path")) {
					//conditioningContext = goalCategory@leftCorner
					goalCategory= conditioningContext.split("@")[0];
					leftCorner1= conditioningContext.split("@")[1];
				}
				 */

				//given the conditioningLabel you have 3 back-off levels
				//the first is given by pcfgCounts, and conditioned on conditioningLabel
				//the second, if conditioningLabel is compound, is given by backoffCounts, 
				//and conditioned on left part of conditioningLabel (e.g. X in <X...Y>)
				//or on X* if shift rule where conditioningLabel = <X...Y>*
				//third level is given by uniformSmoothLevelCounts, and is uniform distribution
				//over all possible virtual rules


				//uniform: uniformSmoothLevelCounts: find lhs that equals conditioning label
				//find zero order frequency of rightSideOfBindingHC
				//System.out.println("conditioningContext=" + conditioningContext);// + "; rightSideOfBinding=" + rightSideOfBinding);
				if (Main.SMOOTH_FOR_UNKNOWN_PRODUCTIONS) {

					String lc = conditioningContext;	//no back-off to label. produce all unary and binary rules goalcat@leftcorner
					if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL) lc =conditioningContext.split("@")[1];
					
					if (!(nextProduction.equals("attach"))) {
						if (this.getUniformSmoothLevelCounts().get(lc)==null) System.out.println("null; lc=" + lc + "; binding=" + binding);
						P3 = 1./((double) this.getUniformSmoothLevelCounts().get(lc));	//uniform
					}
					else {	//nextProduction.equals("attach")
						//attachments: more than 1/uniform
						if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL) {
							if ( lc.equals(conditioningContext.split("@")[0])) 	//only non-zero if lc=gc
								P3 = ((double) this.getUniformSmoothLevelAttachCounts().get(lc))/((double) this.getUniformSmoothLevelCounts().get(lc));	//uniform
							else P3=0.;
						}
						if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL) P3 =1.;

					}

					//if shift rule, then left corner ends with ^1
					//goalCPlusHalfLeftCorner = goalCat + "@" + dottedNonTerminal.split(">")[0] + "^1";
					String shiftExtension ="";
					if (backOffContext.endsWith("^1")) shiftExtension = "^1";
					if (backOffContext.contains(">")) 
						backOffContext = backOffContext.split(">")[0] + shiftExtension;


					//determine what is correct outcome from rightSideOfBinding: : which part are you interested in?
					//given the binary rule <X...Z> --> <X...Y> Z
					//then you want the back-off probabilities P(<X...Y>|X) for which Y occurs in <X...Y> and arbitrary Z
					//given unary rule <X...Z> --> Z then P("unary"|X)

					//for left corner: look at root of rightSideOfBinding
					// back-off from partial left corner: given the binary rule <X...Z> --> <X...Y> Z with left corner <X...Y>, 
					// then estimate P(<X...Z>|X) (conditioned on partial l.c.) for arbitrary Y. 
					// or, given the unary rule X --> <X...Y> 
					// then estimate back-off prob. P<unary|X> where X is the left component of the left corner
					// (other way round than top-down unary: <X...Y> --> X stays the same...also if leftCornerLabel does not contain ">")
					// so you must reduce nextProduction/projectedRuleOrAttach to its leftHandSide: <X...Z>, 
					//or in case of unary rule X --> <X...Y>: leftHandSide=X, because rhs is arbitrary and backed off!
					// or in case projectedRuleOrAttach = "attach" then leave it.	

					/**
					 * in case of top down model you are only interested left corner (first symbol of rhs) of nextPoduction
					 * in case of left corner model you are only interested in lhs of nextProduction
					 */
					String backedOffNextProduction = nextProduction;	//in case top-down and terminal, or in case left-corner and attach
					if (nextProduction.contains("*")) {	//nonterminal
						if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL)
							backedOffNextProduction = backedOffNextProduction.split("\\*")[1];	// productionName is in format parent*chi1*chi2*etc.)
						if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL) {
							backedOffNextProduction = backedOffNextProduction.split("\\*")[0];	//only the left hand side
							//ECHTER indien shift rule (X*-->word) dan is juist backedOffNextProduction = backedOffNextProduction.split("\\*")[1];
							//if ((backedOffNextProduction.contains("^")) )
							//	backedOffNextProduction = nextProduction.split("\\*")[1];
						}
					}



					if (!(backOffProbabilities.get(backOffContext)==null) && !(backOffProbabilities.get(backOffContext).get(backedOffNextProduction)==null)) 
						P2 = backOffProbabilities.get(backOffContext).get(backedOffNextProduction);


					P_Backoff2 = (1 - Main.LAMBDA_3)*P2 + Main.LAMBDA_3*P3;


					//was: if (!(pcfgCounts.get(conditioningContext)==null) && !(pcfgCounts.get(conditioningContext).get(nextProduction)==null)) 
					//	P1 = ((double) pcfgCounts.get(conditioningContext).get(nextProduction))/((double) pcfgSameRootCounts.get(conditioningContext));
					if (!(historyFreeProbabilities.get(conditioningContext)==null) && !(historyFreeProbabilities.get(conditioningContext).get(nextProduction)==null))
						P1 = historyFreeProbabilities.get(conditioningContext).get(nextProduction);
					P_Backoff1 = (1 - Main.LAMBDA_2)*P1 + Main.LAMBDA_2*P_Backoff2;
				}

				//maxHistory is parameter for evaluating scores of the reranker; it cuts off pathlength at threshold
				for (int maxHistory = Main.MIN_HISTORY; maxHistory<=Main.MAX_HISTORY; maxHistory++) {

					//zero order prob (no back-off)
					double P_episodic = 0.;
					//maxHistory=0 corresponds to the PCFG respectively M&C model: P_episodic=0
					if (!(thisConstituent==null) && maxHistory>0) {	//if episodicConstituent exists in train set
						//contribution = Math.pow(BACK_OFF_FACTOR, ((double) history));	//contribution=1 is reserved for back-off
						//sum all totals of pathCounts <=maxHistory, weighted by history/pathLen
						//int totalTraces=0;
						for (int pathLen=1; pathLen<=Main.MAX_HISTORY; pathLen++) {	//counts of all pathlengths ordered according to their length
							cutOffPathLen = Math.min(maxHistory, pathLen);
							contribution = Main.weightLookupTable.get(cutOffPathLen);

							totalWeightedCounts[maxHistory] += pathLengthFrequencies[pathLen]*contribution;
							totalCorrectCounts[maxHistory] += pathLengthFrequenciesWithCorrectBinding[pathLen]*contribution;
							//check:
							//totalTraces +=pathLengthFrequencies[pathLen];
						}

						P_episodic = ((double) totalCorrectCounts[maxHistory] )/((double) totalWeightedCounts[maxHistory] );
						//if (maxHistory==14) System.out.println("P_episodic=" + P_episodic + "; totalCorrectCounts=" + totalCorrectCounts[maxHistory] + "; totalWeightedCounts=" + totalWeightedCounts[maxHistory]);
					}

					//back-off
					double P_Backoff0 = (1 - Main.LAMBDA_1)*P_episodic + Main.LAMBDA_1*P_Backoff1;

					//if (maxHistory==14) System.out.println("P_Backoff0=" + P_Backoff0 + "; P1=" + P1 + "; P2=" + P2 + "; P3=" + P3 + "; P_episodic=" + P_episodic + "; P_Backoff1=" + P_Backoff1 + "; P_Backoff2=" + P_Backoff2);
					/////////// PRINT ///////// experiments.FEEDBACK && 
					if (Main.FEEDBACK && maxHistory==Main.MAX_HISTORY && nrCandidate==4) System.out.println("nrBinding=" + bindingCounter + "; rule=" + nextProduction + "; backOffLabel=" + backOffContext + "; P3=" + numberFormatter.format(P3) + "; P2=" + numberFormatter.format(P2) + "; P1=" + numberFormatter.format(P1) + "; P_epic=" + P_episodic + "; P_final=" + numberFormatter.format(P_Backoff0));
					//sentenceProbabilities[maxHistory] *= ((double) totalCorrectCounts[maxHistory] )/((double) totalWeightedCounts[maxHistory] );
					sentenceProbabilities[maxHistory] *= P_Backoff0;
					//if (comparativeLikelihoods.FEEDBACK && totalCorrectCounts[maxHistory]==0) System.out.println("**** zero binding probability for history " + maxHistory);
				}
			}
			////////////////////////////////////////////////////////////////////////////////

			time5 += System.currentTimeMillis() - currentTime;

			//only for check
			//double testBindingProbability1 = ((double) totalCorrectCounts[MAX_HISTORY])/((double) totalWeightedCounts[MAX_HISTORY]);
			//double testBindingProbability2 = ((double) totalCorrectCounts[MAX_HISTORY] + LAMBDA_SMOOTHING)/((double) totalWeightedCounts[MAX_HISTORY] + LAMBDA_SMOOTHING);
			//if (FEEDBACK) System.out.println("bindingProbability w/o smoothing (*3^(pathl-1))=" + testBindingProbability1 + "; with smoothing =" + testBindingProbability2);


			bindingCounter++;
			
			//check:
			/*
			if (bindingCounter==bindingsOfTestSentence.size()) {
				for (Integer sen : currentNodeTraceActivations.keySet()) {
					if (currentNodeTraceActivations.get(sen).size()>1) 
						System.out.println("currentNodeTraceActivations lastb .size=" + currentNodeTraceActivations.get(sen).size());
				}
			}
			*/
		}	//for (String binding : bindingsOfTestSentenceLCP) {

		//test
		//System.out.println("testRankNrsOfSentence54.s=" + testRankNrsOfSentence54.size());
		
		nrUnknownProductionsFeedback = nrUnknownProductions + " out of " + bindingsOfTestSentence.size() + " productions in the test sentence are unknown";

		//print sentence probabilities (parametrized on history)
		for (int maxHistory = Main.MIN_HISTORY; maxHistory<=Main.MAX_HISTORY; maxHistory++) {
			if (Main.FEEDBACK) 
				System.out.println("sentenceProbability for order " + maxHistory + ": " + sentenceProbabilities[maxHistory] );
			sentenceProbabilities[maxHistory] = Math.log(sentenceProbabilities[maxHistory]);
		}

		//copy all previousNodeTraceActivations to discontiguousFragmentsOfSentence, for the next sentence
		if (Main.INCLUDE_DISCONTIGUITIES && Main.SYNTACTIC_PRIMING) {
			
			HashMap<Integer, TreeMap<Integer, traceFields>> finalNodeTraceActivations = treeletStates[treeletCounter].getTraceActivations();
			
			for (Integer sentenceNrOfETN : finalNodeTraceActivations.keySet()) {
				//if previousNTActivations not emptied
				if ( finalNodeTraceActivations.get(sentenceNrOfETN).size() >0) {
					//System.out.println("previousNodeTraceActivations.get(sentenceNrOfETN).size()=" + previousNodeTraceActivations.get(sentenceNrOfETN).size());
					ArrayList<Double[]> discontiguousFragmentsOfSentence = discontiguousFragments.get(sentenceNrOfETN);
					
					if (discontiguousFragmentsOfSentence==null) {	//--> previousNodeTraceActivations.get(sentenceNrOfETN).size() >0
							discontiguousFragmentsOfSentence = new ArrayList<Double[]>();
							discontiguousFragments.put(sentenceNrOfETN, discontiguousFragmentsOfSentence);
					}

					//put rankNr and pathLength from previousNodeTraceActivations that have not been used in discontiguousFragments
					//but only if pathLength>1
					for (Integer previousRankNr : finalNodeTraceActivations.get(sentenceNrOfETN).keySet()) {	//TreeMap
						Integer previousPathLength = finalNodeTraceActivations.get(sentenceNrOfETN).get(previousRankNr).getCH();
						if (previousPathLength>2)
							discontiguousFragmentsOfSentence.add(new Double[] {(double) previousRankNr, (double) previousPathLength});	//the leftovers having this sentenceNr
					}
				}	//if ( previousNodeTraceActivations.get(sentenceNrOfETN).size() >0)
			}
			
			//decay all discontiguous fragments between sentences
			HashSet<Double> uniqueRankNrUsed = new HashSet<Double>();
			int counterOfFragments=0;
			for (Integer sentenceNrOfETN : discontiguousFragments.keySet()) {
				//ArrayList<Double[]> discontiguousFragmentsOfSentence = discontiguousFragments.get(sentenceNrOfETN);
				uniqueRankNrUsed.clear();
				// remove <2 and remove all but one of the same rankNr
				int rankNrsOfSentence=0, approvedRankNrs=0;
				for (ListIterator<Double[]> it = discontiguousFragments.get(sentenceNrOfETN).listIterator(discontiguousFragments.get(sentenceNrOfETN).size()); it.hasPrevious();) {
					rankNrsOfSentence++;
					Double[] rankNrPlusPathLength = (Double[]) it.previous();
				
					//for (Double[] rankNrPlusPathLength : discontiguousFragments.get(sentenceNrOfETN)) {
					rankNrPlusPathLength[1] = Main.DISCTG_DECAY_SENTENCE_LEVEL*rankNrPlusPathLength[1];
					if (rankNrPlusPathLength[1]<2.) it.remove();

					else {	//only keep the last entry of the same rankNr
						if (!uniqueRankNrUsed.add((rankNrPlusPathLength[0]))) it.remove();
						else {
							//System.out.println("rankNrPlusPathLength[1]=" + rankNrPlusPathLength[1]);
							counterOfFragments++; approvedRankNrs++;
						}
					}					
				}	//for (ListIterator<Double[]> it = discontiguousFragments.get(sentenceNrOfETN).listIterator(discontiguousFragments.get(sentenceNrOfETN).size()); it.hasPrevious();) {
				//if (rankNrsOfSentence>3) System.out.println("rankNrsOfSentence=" + rankNrsOfSentence + "; approvedRankNrs=" + approvedRankNrs);
			}	//for (Integer sentenceNrOfETN : discontiguousFragments.keySet())
			//System.out.println("counterOfFragments=" + counterOfFragments);
			
		}	//if (experiments.INCLUDE_DISCONTIGUITIES && experiments.SYNTACTIC_PRIMING)
		
		if (Main.COMPUTE_SHORTEST_DERIVATION) {
			//find the shortest derivation in the final treelet, and reconstruct the used fragments by following the pointers back
			//myTrace.setViterbiPointer((int) shortestDerivationViterbiPointer[0], (int) shortestDerivationViterbiPointer[1]);

			//get reference to final treelet
			double shortestDerivationLength = 1000d;
			int[] shortestDerivationPointer = new int[2];
			
			HashMap<Integer, TreeMap<Integer, traceFields>> tracesOfFinalTreelet = treeletStates[treeletCounter-1].getTraceActivations();
			for (Integer sentenceNr : tracesOfFinalTreelet.keySet()) {
				for (Integer rankNr : tracesOfFinalTreelet.get(sentenceNr).keySet()) {
					if (tracesOfFinalTreelet.get(sentenceNr).get(rankNr).getDerivationLength()< shortestDerivationLength) {
						shortestDerivationLength = tracesOfFinalTreelet.get(sentenceNr).get(rankNr).getDerivationLength();
						shortestDerivationPointer[0]  = sentenceNr;
						shortestDerivationPointer[1]  = rankNr;
					}
				}
			}	//for (Integer sentenceNr : tracesOfFinalTreelet.keySet())
			
			//System.out.println("nrTreelets = " + treeletCounter + "; shortest derivation= " + shortestDerivationLength);
			
			//now follow the pointer back to reconstruct the sentenceNrs
			ArrayList<Integer> exemplarNrs = new ArrayList<Integer>();
			ArrayList<String> productions = new ArrayList<String>();
			
			for (int nrTreelet = treeletCounter-2; nrTreelet>=0; nrTreelet--) {
				productions.add(treeletStates[nrTreelet].getTreelet().getUniqueSymbol());
				exemplarNrs.add(shortestDerivationPointer[0]);
				//use shortestDerivationPointer to find the trace in the current treelet
				traceFields viterbiTrace = treeletStates[nrTreelet].getTraceActivations().get(shortestDerivationPointer[0]).get(shortestDerivationPointer[1]);
				System.out.println("nrTreelet=" + nrTreelet + "; production=" + treeletStates[nrTreelet].getTreelet().getUniqueSymbol() + "; sentenceNr=" + shortestDerivationPointer[0] + "; rankNr=" + shortestDerivationPointer[1]);
				shortestDerivationPointer[0] = viterbiTrace.getViterbiPointer()[0];
				shortestDerivationPointer[1] = viterbiTrace.getViterbiPointer()[1];
			}
			
			//print
			System.out.println("Shortest derivation: " + shortestDerivationLength + "; exemplarNrs: " + exemplarNrs);
			
			
			//return shortestDerivationLength in the form of the size of the Array
			//check: longestCommonPathFragment = "" + sentenceNrOfETN + "_" + rankNrOfETN + "_" + pathLength + "_" + nrBindingOfTestSentence;
			for (int i=0; i< shortestDerivationLength; i++) longestFragmentsOfShortestDerivation.add("x");	//heb je nodig om size te weten
			
		}	//if (experiments.COMPUTE_SHORTEST_DERIVATION) {
		
		if (Main.FEEDBACK) {
			System.out.println("Time elapsed in computeBindingProbabilitiesGivenPartialPath = " +(System.currentTimeMillis()-startTime) + " ms; time1=" + time1 + "; time2=" + time2 + "; time3=" + time3 + "; time4=" + time4 + "; time5=" + time5 + "; timeDISCTG1=" + timeDISCTG + "; timeDISCTG2=" + timeDISCTG2);
			System.out.println("nrIterations for  " + bindingsOfTestSentence.size() + " bindings=" + nrIterationsCheck);
		}
		return sentenceProbabilities;
	}

	/**
	 * 
	 * @param previousNodeTraceActivations
	 * @return double[] containing {shortestDerivationSentenceNr, shortestDerivationRankNr, shortestDerivationLength}
	 */
	private double[] prefilterTracesWithShortestDerivations(HashMap<Integer, TreeMap<Integer, traceFields>> previousNodeTraceActivations) {

		//HashMap<Integer, TreeMap<Integer, traceState>> previousNodeTraceActivationsSelected = new HashMap<Integer, TreeMap<Integer, traceState>>();
		
		//find trace with the shortest derivation length
		double shortestDerivationSentenceNr=0d, shortestDerivationRankNr=0d, shortestDerivationLength = 1000d;
		//
		for (int sentenceNr : previousNodeTraceActivations.keySet()) {
			
			for (int rankNr : previousNodeTraceActivations.get(sentenceNr).keySet()) {
				double derivLength = previousNodeTraceActivations.get(sentenceNr).get(rankNr).getDerivationLength();
				if (derivLength < shortestDerivationLength) {
					shortestDerivationLength = derivLength;
					shortestDerivationSentenceNr = sentenceNr;
					shortestDerivationRankNr = rankNr;
				}
			}
		}
		/*
		//now keep only those traces for which derivLength is at most 0.9 longer than shortestDerivLength				
		for (int sentenceNr : previousNodeTraceActivations.keySet()) {
		
			for (int rankNr : previousNodeTraceActivations.get(sentenceNr).keySet()) {
				traceState myTrace = previousNodeTraceActivations.get(sentenceNr).get(rankNr);
				double derivLength = myTrace.getDerivationLength();
				if (derivLength < shortestDerivationLength+1.) {
					//add it to selected traces
					TreeMap<Integer, traceState> selectedNodeTraceActivationsSentenceNr = previousNodeTraceActivationsSelected.get(sentenceNr);
					if (selectedNodeTraceActivationsSentenceNr==null) {
						selectedNodeTraceActivationsSentenceNr = new TreeMap<Integer, traceState>();
					}
					selectedNodeTraceActivationsSentenceNr.put(rankNr, myTrace);
				}
			}
		}
		*/
		double[] anArray = {shortestDerivationSentenceNr, shortestDerivationRankNr, shortestDerivationLength};
		return anArray;//previousNodeTraceActivationsSelected;
	}

	/**
	 * Reconstructs the bindings used in fragment; converts bindings into parseTree of fragment 
	 * (using convertBindingsToTree) and saves fragment tree in fragments; this method only used 
	 * before printing the fragments in printShortestDerivations
	 * 
	 * @param bindingsOfTestSentence
	 * @param sentenceCounter
	 * @param currentBindingOfTestSentence
	 * @param longestCommonPathFragment
	 * @throws NumberFormatException
	 */
	private void recordShortestFragments(ArrayList<String> bindingsOfTestSentence, int sentenceCounter, int currentBindingOfTestSentence,
			String longestCommonPathFragment) throws NumberFormatException {

		if (longestCommonPathFragment.equals("")) {
			if (Main.FEEDBACK) System.out.println("sentence #" + sentenceCounter + "; currentBinding=" + currentBindingOfTestSentence + ": no derivation found");
		}
		else {
			//save the fragment in an external treebank of constructions
			String sentenceNr = longestCommonPathFragment.split("_")[0], rankOfETN=longestCommonPathFragment.split("_")[1];
			int currentBinding= java.lang.Integer.parseInt(longestCommonPathFragment.split("_")[3])+1;
			int lengthOfFragment = java.lang.Integer.parseInt(longestCommonPathFragment.split("_")[2]);
			
			//HashMap<String, ArrayList<Integer>> fragments2
			if (lengthOfFragment>Main.COUNT_FRAGMENTS_OF_LENGTH_GREATER_THAN) {
				//reconstruct the fragment from info by pasting bindings together: going back from currentBinding to currentBindingOfTestSentence-lengthOfFragment 
				
				HashSet<ArrayList<String>> fragmentsOfOneSentence = null;
				//int newSentenceCounter = sentenceCounter;
				if (shortestDerivationFragments.get(sentenceCounter)==null) {
					fragmentsOfOneSentence = new HashSet<ArrayList<String>>();
					shortestDerivationFragments.put(sentenceCounter, fragmentsOfOneSentence);
				}
				else fragmentsOfOneSentence = shortestDerivationFragments.get(sentenceCounter);
				
				//////////////
	
				ArrayList<String> bindingsOfFragment = new ArrayList<String>();
				for (int i=(currentBinding-lengthOfFragment); i<currentBindingOfTestSentence; i++) {
					bindingsOfFragment.add(bindingsOfTestSentence.get(i));
					//newFragment is ArrayList of episodicConstituents
					//look only at thisProduction (binding.split("~")[0]) , because nextProduction can be "attach"
					//waas: episodicConstituent thisConstituent = this.episodicNonTerminalProductions.get(bindingsOfTestSentence.get(i).split("~")[0]);
					//was: newFragment.add(thisConstituent);
				
				}
				
				//new: (ipv fragmentsOfOneSentence.add(newFragment);)
				fragmentsOfOneSentence.add(bindingsOfFragment);
				
				String fragment = Utils.convertBindingsToTree(bindingsOfFragment) + "~" + lengthOfFragment;

				if (Main.FEEDBACK) System.out.println("sentence #" + sentenceCounter + "; currentBinding=" + currentBinding + "; lengthOfFragment=" + lengthOfFragment + "; from exemplar #" + sentenceNr + "; Parse:" + fragment);

				ArrayList<Integer> sentenceNrsInWhichFragmentOccurs = null;
				if (Main.fragments.get(fragment)==null) {
					sentenceNrsInWhichFragmentOccurs = new ArrayList<Integer>();
					Main.fragments.put(fragment, sentenceNrsInWhichFragmentOccurs);
				}
				else sentenceNrsInWhichFragmentOccurs = Main.fragments.get(fragment);
				sentenceNrsInWhichFragmentOccurs.add(sentenceCounter);
			}
		}
		//set all pathLengths in newTraceNumberSequences to 1
		//HashMap<Integer, Integer>[] newTraceNumberSequences
	}

	/*
	 * 
	 * @param sentenceCounter
	 * @param leftSisterLabel
	 * @return
	 * @throws NumberFormatException
	
	private HashMap<Integer, HashMap<Integer,Integer>> getETNsFromLeftSister(int sentenceCounter, episodicConstituent leftSister) throws NumberFormatException {

		//HashMap<sentenceNr, HashMap<rankNr, Activation>>
		HashMap<Integer, HashMap<Integer, Integer>> leftSisterETNActivations = new HashMap<Integer, HashMap<Integer, Integer>>();
			
		//format {SentenceNr, SequenceNr, HashCodeOfBoundEpsidodicUnit}
		HashMap<Double, Integer[]> leftSisterRankedETNs = leftSister.getRankedETNs();

		//get reference to activations of rankedETNs
		for (Integer[] sisterETNs : leftSisterRankedETNs) {
			int sentenceNr = sisterETNs[0];
			int rankNr = sisterETNs[1];
			
			if (!experiments.COMPUTE_SHORTEST_DERIVATION || (experiments.COMPUTE_SHORTEST_DERIVATION && !(sentenceNr==sentenceCounter))) {	//because for this sentenceNr no activation is stored
				//every sentenceNr can have multiple rankedETNs
				HashMap<Integer, Integer> rankNrPlusActivationPerSentence=null;
				if (leftSisterETNActivations.get(sentenceNr)==null) {
					rankNrPlusActivationPerSentence = new HashMap<Integer, Integer>();
					leftSisterETNActivations.put(sentenceNr, rankNrPlusActivationPerSentence);
				}
				else rankNrPlusActivationPerSentence = leftSisterETNActivations.get(sentenceNr);
				
				//get activationValue of rankedETNs in left sister, 
				//that is the pathLengths corresponding to every rankedETNs, which were stored when left sister was visited
				String myETN = "" + sentenceNr + "_" + rankNr;
				rankNrPlusActivationPerSentence.put(rankNr, leftSister.getActivationOfRankedETN(myETN));
			}
		}
		
		return leftSisterETNActivations;
	}
*/
	

	
	/**
	 * Computes sequence of node-slot bindings along direction of 
	 * Left Corner Shifting derivation. 
	 * At the same time creates Integer[] rankedETN = {sentenceNr, rankNr, firstWord.hashCode()};
	 * and stores these inside episodicConstituent.
	 *
	 * Returns bindingsOfLCDerivation ordered sequence of bindings between two successive
	 * episodicConstituents in the left corner derivation. Format of binding:
	 * in case of project: thisProductionName + "~" + nextProductionName + "~" + conditioningContext + "~" + leftSister,
	 * conditioningContext = goalCategory + "@" + leftCorner
	 * in case of attach: replace nextProductionName with "attach", and add  + "~" + leftSister as 4th element of binding
	 * leftSister is used in case of INCLUDE_DISCONTIGUITIES to reconstruct stored traces of left sister after attach)
	 * in case of shift: binding = starredProductionName + "~" + shiftRule + etc., where starredProductionName = productionName
	 * with added ^i to indicate that it is in state before shift, and shiftRule is of the form starredNonTerminal*word
	 *  
	 * @param myParseTree
	 * @param sentenceNr -1 indicates that the rankedETN will not be stored in episodicConstituent
	 * so it will not participate in episodic prob model (for test sentences)
	 * @return bindingsOfLCDerivation ordered sequence of bindings between two successive
	 * episodicConstituents in the left corner derivation
	 */
	public ArrayList<String> rankBindingsForLeftCornerDerivation(parseTree myParseTree, int sentenceNr) {

		//System.out.println("myParseTree=" + myParseTree.printWSJFormat());
		ArrayList<parser.Node> originalParseTree = myParseTree.getNodes();
		
		/**
		 * binding = thisUnit.getProductionName()~nextUnit.getProductionName()~conditioningInfo~leftSisterOfThis
		 * for leftcorner: conditioningInfo=gc+ "@" + lc; back-off info can be extracted from labels of conditioningInfo.
		 */ 
		ArrayList<String> bindingsOfLCDerivation = new ArrayList<String>();

		//find leftmost "unprocessed" node in thee parseTree which is either terminal, or which has all its children processed
		//you may assume that terminal nodes in parseTree are arranged from left to right
		ArrayList<Node> tempParseTree = new ArrayList<Node>();
		tempParseTree.addAll(originalParseTree);
		
		//keep track of the current goal corresponding to current unit in bindings 
		//push goal category on stack after shift, remove from stack after attach, keep the same goal after project
		ArrayList<String> goalStack = new ArrayList<String>();
		
		int rankNr = 0;
		//int fragmentNr = 1;	//there can be more than one fragment if a node is visited more than once
		
		//restore processedChildren to 0
		for (Node myNode : tempParseTree) {
			myNode.processedChildren =0;
		}

		
		
		goalStack.add("TOP");
		/**
		 * Bindings are created from previousProduction to currentProduction, and at the same time
		 * a pointer to currentProduction.hc is stored (as ETN) in the episodicConstituent corresponding to the previousProduction
		 */
		String previousProduction = "START^1*-*TOP";
		
		//remove nodes one by one from tempParseTree after you find leftmost childless node
		
		while (tempParseTree.size()>0) {

			int leftMostWordNr = 150;
			int lowestRecursiveLevel = 100;
			Node leftmostNode = null;

			//########################################################################################
			//1) find the leftmost and deepest node (leftmostNode) all of whose children are processed
			//########################################################################################
			for (Node myNode : tempParseTree) {
				//only if all children processed or terminal
				//System.out.println("myNode=" + myNode.getName()+ "; HPNName=" + myNode.getHPNNodeName() + "; myNode.getChildNodes().size()=" + myNode.getChildNodes().size() + "; myNode.processedChildren=" + myNode.processedChildren);
				if (myNode.getChildNodes().size()==myNode.processedChildren) {	//either terminal or removed children
					// in contrast to episodicConstituents, doppparse.Node only has a single ETN because nodes are duplicated if they occur twice in derivation

					int thisNodeWordNr = myNode.getLeftSpan();	
					int thisNodeRecursiveLevel = myNode.getDepth();	

					if (thisNodeWordNr <= leftMostWordNr)  {
						if (thisNodeWordNr < leftMostWordNr) lowestRecursiveLevel=100;
						if (thisNodeRecursiveLevel < lowestRecursiveLevel) {
							leftmostNode = myNode;

							leftMostWordNr = thisNodeWordNr;
							lowestRecursiveLevel = thisNodeRecursiveLevel;
						}
					}

				}	//if (myNode.getChildNodes().size()==0) {
			}	//end //find the leftmost and deepest node
			
			//set the leftMostNode processed=true and continue with while
			//System.out.println("leftmostNode.getProductionName()=" + leftmostNode.getProductionName());
			String leftmostNodeProduction = leftmostNode.getProductionName();

			//##########################################################################
			//2) insert an EXTRA SHIFT BINDING  if leftmostNode is a terminal  
			//from the terminal to the incomplete node (previousProduction), and increment rankNr
			//##########################################################################
			
			if (leftmostNode.getType() == parser.parameters.TERMINAL) {
				//System.out.println("leftmostNode1=" + leftmostNodeLabel + "; previousLeftmostNodeParent1=" + previousLeftmostNodeParent.getProductionName());
				//construct SHIFT production from the parent of previous + word
									
				//get parent (starredNonT) of previous production 
				String starredNonterminal = previousProduction.split("\\*")[0] ;
				//if this is the first word, then previousProduction = "START^1*-*TOP", and goalCat=TOP
				
				//new goalCat is the right sister of starredNonterminal: if starredNonT has index ^1
				//then goalCat is second child 
				//goalCategPlusLeftCorner is only for the (Manning-Carpenter) back-off!
				int goalCatIndex = java.lang.Integer.parseInt(starredNonterminal.split("\\^")[1])+1;
				String goalCategory = previousProduction.split("\\*")[goalCatIndex];
				String goalCategPlusLeftCorner = goalCategory + "@" + starredNonterminal;
				goalStack.add(goalCategory);
					
				//create the shift rule
				String shiftRuleProduction = leftmostNodeProduction + "*" + starredNonterminal; //parent first
				
				//Bindings are created from previousProduction to currentProduction
				bindingsOfLCDerivation.add(previousProduction + "~" + shiftRuleProduction  + "~" + goalCategPlusLeftCorner + "~-~shift");
									
				//put rankedETN inside the episodicConstituent only for train sentences
				if (sentenceNr >-1)	{	//don't fill rankNr in HPN nodes for test sentences
					
					//store a pointer to the next episodicConstituent in derivation in the third entry of myETN
					Integer[] myETN = {rankNr, shiftRuleProduction.hashCode()};

					this.episodicNonTerminalProductions.get(previousProduction).addRankedETN(myETN, sentenceNr);
				}
					
				previousProduction = shiftRuleProduction;
				rankNr++;
			}
			
			
			//####################################################
			//3) create bindings for project and attach operations 
			//####################################################
			/**
			 * leftmostNodeParent is the node to which leftmostNode binds
			 */
			Node leftmostNodeParent = leftmostNode.getParentNode();
			
			String projectOrAttachProduction="";
			//find child
			if (!(leftmostNodeParent ==null)) {
				boolean blnAttach = false, blnComplete=false;
				
				int whichChild = leftmostNodeParent.getChildNodes().indexOf(leftmostNode) + 1;
				if (whichChild>1) blnAttach = true;
				
				//check completion: if not complete, then the next production is a shift rule, 	
				if (whichChild == leftmostNodeParent.getChildNodes().size()) blnComplete = true;
				
				//reconstruct name of the next episodicConstituent/production in the derivation (projected node, or attached node)
				projectOrAttachProduction  = leftmostNodeParent.getProductionName();
				
				//was: projectedOrAttachedConstituentProductionName  = leftmostNodeParent.getProductionName() + "_" + whichChild;
				//was: if (whichChild>1) projectedOrAttachedConstituentProductionName = "attach";


				//if not complete (e.g. in case of project) you must convert the parent production into a starred production (before shift)
				if (!blnComplete) {
					//add a binding from previousLeftmostNodeParent to terminal (shift production)
					String[] splitProduction = projectOrAttachProduction.split("\\*");
					String rhs ="";
					for (int i =1; i< (splitProduction.length); i++) rhs += "*" + splitProduction[i];
					projectOrAttachProduction = splitProduction[0] + "^" + whichChild + rhs;	
				}
				
				//mark child as processed in parent node
				leftmostNodeParent.processedChildren += 1;

				String goalCategPlusleftCorner = goalStack.get(goalStack.size()-1) + "@" + leftmostNode.getName();
				
				
				//if experiments.INCLUDE_DISCONTIGUITIES: put starredLeftSisterLabel in binding (to reconstruct activation)
				String starredLeftSisterLabel="-";
				if (Main.INCLUDE_DISCONTIGUITIES_OLD && blnAttach) {

					//for discontiguities: find left sister starred nonterminal (including full productionName, e.g., S1.*NP*VP)
					String[] splitProduction = leftmostNodeParent.getProductionName().split("\\*");

					//reconstruct left sister starred nonTerminal
					starredLeftSisterLabel = splitProduction[0] + "^" + (whichChild-1);
					for (int i=1; i< splitProduction.length; i++ ) starredLeftSisterLabel += "*" + splitProduction[i];
				}
				
				//save binding in bindingsOfLCDerivation: bindings are created from previousProduction to currentProduction
				if (!blnAttach)
					bindingsOfLCDerivation.add(previousProduction + "~" + projectOrAttachProduction + "~" + goalCategPlusleftCorner + "~" + starredLeftSisterLabel + "~project");
				else {	//attach
					if (Main.ATTACH_CHOICE)
						bindingsOfLCDerivation.add(previousProduction + "~" + projectOrAttachProduction + "~" + goalCategPlusleftCorner + "~" + starredLeftSisterLabel+ "~" + projectOrAttachProduction + "_" + whichChild);
					else bindingsOfLCDerivation.add(previousProduction + "~" + "attach" + "~" + goalCategPlusleftCorner + "~" + starredLeftSisterLabel+ "~" + projectOrAttachProduction + "_" + whichChild);
					goalStack.remove(goalStack.size()-1); //pop from stack (for back-off)
				}
				
				//set rankNr in dopparser.Node - you only need this for printing!!!
				if (Main.PRINT_LATEX_TREES_WITH_RANKNRS) leftmostNode.setRankNrInDerivation(rankNr);

				//##########################################################
				//store rankedETN inside episodicConstituents in episodicGrammar
				//the third entry in myETN is used to look up what is next node in derivation
				if (sentenceNr >-1)	{	//only for train sentences, don't fill rankNr in HPN nodes for test sentences

					//pointer to hc of the next episodicConstituent in the derivation
					String nextProduction = projectOrAttachProduction;
					if (blnAttach && !Main.ATTACH_CHOICE) nextProduction = "attach";
					Integer[] myETN = {rankNr, nextProduction.hashCode()};
					//System.out.println("previousProduction=" + previousProduction);
					this.episodicNonTerminalProductions.get(previousProduction).addRankedETN(myETN, sentenceNr);
				}
				previousProduction = projectOrAttachProduction;
				rankNr++;
			}
			//remove child from ArrayList
			tempParseTree.remove(leftmostNode);			
		}	//while (tempParseTree.size()>0)


		return bindingsOfLCDerivation;
	}

	
	/**
	 * No longer in use!
	 * Computes sequence of node-slot bindings along direction of 
	 * Left Corner Shifting derivation. 
	 * At the same time creates Integer[] rankedETN = {sentenceNr, rankNr, firstWord.hashCode()};
	 * and stores these inside episodicConstituent.
	 *
	 * Returns bindingsOfLCDerivation ordered sequence of bindings between two successive
	 * episodicConstituents in the left corner derivation. Format of binding:
	 * in case of project: leftCornerConstituentProductionName + "~" + projectedConstituentProductionName + "_" + whichChild,
	 * in case of attach: replace projectedConstituentProductionName = "attach", and add  + "~" + attachInfo to binding
	 * attachInfo adds attach information in binding (to reconstruct position of attach later)
	 * in case of shift: binding = starredNonTerminalProductionName + "~" + word
	 * 
	 *  
	 * @param myParseTree
	 * @param sentenceNr -1 indicates that the rankedETN will not be stored in episodicConstituent
	 * so it will not participate in episodic prob model (for test sentences)
	 * @return bindingsOfLCDerivation ordered sequence of bindings between two successive
	 * episodicConstituents in the left corner derivation
	 */
	public ArrayList<String> rankBindingsForLeftCornerDerivation_old(parseTree myParseTree, int sentenceNr) {

		ArrayList<parser.Node> originalParseTree = myParseTree.getNodes();
		
		/**
		 * binding = thisUnit.getProductionName()~nextUnit.getProductionName()~conditioningInfo~leftSisterOfThis
		 * for leftcorner: conditioningInfo=gc+ "@" + lc; back-off info can be extracted from labels of conditioningInfo.
		 */ 
		ArrayList<String> bindingsOfLCDerivation = new ArrayList<String>();

		//algo: vind de meest linker node in de parseTree waarvan
		//alle children processed of terminal (gebruik gebruik Node.productionName en Node.processedChildren
		//you may assume that terminal nodes in parseTree are arranged from left to right
		ArrayList<Node> tempParseTree = new ArrayList<Node>();
		tempParseTree.addAll(originalParseTree);
		
		//keep track of the current goal corresponding to current unit in bindings 
		//push goal category on stack after shift, remove from stack after attach, keep the same goal after project
		ArrayList<String> goalStack = new ArrayList<String>();
		
		int rankNr = 0;
		//int fragmentNr = 1;
		String firstWord = "";

		//restore processedChildren to 0
		for (Node myNode : tempParseTree) {
			myNode.processedChildren =0;
			if ((myNode.getType()==parser.parameters.TERMINAL) && (myNode.getLeftSpan()==0)) firstWord=myNode.getProductionName();
		}


		//insert a binding from START to the first word
			
		String goalCategPlusleftCorner = "START^1" + "@" + firstWord;
		bindingsOfLCDerivation.add("START^1~" + firstWord + "~" + goalCategPlusleftCorner);
		goalStack.add("START^1");
		
		if (sentenceNr >-1)	{	//don't fill rankNr in HPN nodes for test sentences
			//the third entry in myETN is used to look up what is next node in derivation
			Integer[] myETN = {rankNr, firstWord.hashCode()};
			//System.out.println("nodeStateLabel=" + nodeStateLabel);START^1*x*TOP
			this.episodicNonTerminalProductions.get("START*-*TOP").addRankedETN(myETN,sentenceNr);
		}
		rankNr++;
	

		//remove nodes one by one from tempParseTree after you find leftmost childless node
		Node previousLeftmostNodeParent = null;

		while (tempParseTree.size()>0) {

			int leftMostWordNr = 150;
			int lowestRecursiveLevel = 100;
			Node leftmostNode = null;

			//###################################################
			//find the leftmost and deepest node (leftmostNode) all of whose children are processed
			for (Node myNode : tempParseTree) {
				//only if all children processed or terminal
				//System.out.println("myNode=" + myNode.getName()+ "; HPNName=" + myNode.getHPNNodeName() + "; myNode.getChildNodes().size()=" + myNode.getChildNodes().size() + "; myNode.processedChildren=" + myNode.processedChildren);
				if (myNode.getChildNodes().size()==myNode.processedChildren) {	//either terminal or removed children
					// in contrast to HPN nodes, doppparse.Node only has a single ETN because nodes are duplicated if they occur twice in derivation

					int thisNodeWordNr = myNode.getLeftSpan();	
					int thisNodeRecursiveLevel = myNode.getDepth();	

					if (thisNodeWordNr <= leftMostWordNr)  {
						if (thisNodeWordNr < leftMostWordNr) lowestRecursiveLevel=100;
						if (thisNodeRecursiveLevel < lowestRecursiveLevel) {
							leftmostNode = myNode;

							leftMostWordNr = thisNodeWordNr;
							lowestRecursiveLevel = thisNodeRecursiveLevel;
						}
					}

				}	//if (myNode.getChildNodes().size()==0) {
			}	//end //find the leftmost and deepest node
			//###################################################

			//set the leftMostNode processed=true and continue with while
			//System.out.println("leftmostNode.getProductionName()=" + leftmostNode.getProductionName());
			String leftmostNodeProduction = leftmostNode.getProductionName();

			//###################################################
			//SHIFT BINDING: if leftmostNode is a terminal, then insert an EXTRA binding 
			//from the uncompleted node to the terminal and increment rankNr
			
				if (leftmostNode.getType() == parser.parameters.TERMINAL && !(previousLeftmostNodeParent==null)) {
					//System.out.println("leftmostNode1=" + leftmostNodeLabel + "; previousLeftmostNodeParent1=" + previousLeftmostNodeParent.getProductionName());

					//add a binding from previousLeftmostNodeParent to terminal (shift production)
					String[] splitProduction = previousLeftmostNodeParent.getProductionName().split("\\*");
					String rhs ="";
					for (int i =1; i< (splitProduction.length); i++) rhs += "*" + splitProduction[i];
					String starredNonterminal = splitProduction[0] + "^" + previousLeftmostNodeParent.processedChildren;
					String starredNonterminalProduction = starredNonterminal + rhs;
					
					bindingsOfLCDerivation.add(starredNonterminalProduction + "~" + leftmostNodeProduction  + "~" + starredNonterminal + "@" + leftmostNode.getName());
					goalStack.add(starredNonterminal);
					
					//only for train sentences
					if (sentenceNr >-1)	{	//don't fill rankNr in HPN nodes for test sentences
						//the third entry in myETN is used to look up what is next node in derivation
						Integer[] myETN = {rankNr, leftmostNodeProduction.hashCode()};
						//System.out.println("nodeStateLabel=" + nodeStateLabel);
						//put rankNr inside the HPN node
						this.episodicNonTerminalProductions.get(starredNonterminalProduction).addRankedETN(myETN,sentenceNr);
					}
					rankNr++;
				}
			
			//###################################################

			/**
			 * leftmostNodeParent is the node to which leftmostNode binds
			 */
			Node leftmostNodeParent = leftmostNode.getParentNode();
			previousLeftmostNodeParent = leftmostNodeParent;
			//if (!(leftmostNodeParent==null)) System.out.println("leftmostNode2=" + leftmostNodeLabel + "; currentLeftmostNodeParent2=" + previousLeftmostNodeParent.getProductionName());

			
			String projectedOrAttachedConstituentProductionName="";
			//find slot
			if (!(leftmostNodeParent ==null)) {
				int whichChild = leftmostNodeParent.getChildNodes().indexOf(leftmostNode) + 1;

				//System.out.println("leftmostNodeLabel=" + leftmostNodeLabel + "; leftmostNodeParent=" + leftmostNodeParent.getHPNNodeName() + "; whichChild=" + whichChild);

				projectedOrAttachedConstituentProductionName  = leftmostNodeParent.getProductionName() + "_" + whichChild;
				if (whichChild>1) {
					projectedOrAttachedConstituentProductionName = "attach";
					goalStack.remove(goalStack.size()-1); //pop from stack
				}

				//mark child as processed in parent node
				leftmostNodeParent.processedChildren += 1;

				goalCategPlusleftCorner = goalStack.get(goalStack.size()-1) + "@" + leftmostNode.getName();
				
				//save binding in bindingsOfLCDerivation
				String starredLeftSisterLabel="";
				//if experiments.INCLUDE_DISCONTIGUITIES: put starredLeftSisterLabel in binding (to reconstruct activation)
				if (projectedOrAttachedConstituentProductionName.equals("attach")) {

					//for discontiguities: find left sister starred nonterminal (including full productionName, e.g., S1.*NP*VP)
					String[] splitProduction = leftmostNodeParent.getProductionName().split("\\*");

					//reconstruct left sister starred nonTerminal
					starredLeftSisterLabel = "~" + splitProduction[0] + "^" + (whichChild-1);
					for (int i=1; i< splitProduction.length; i++ ) starredLeftSisterLabel += "*" + splitProduction[i];
				}
				bindingsOfLCDerivation.add(leftmostNodeProduction + "~" + projectedOrAttachedConstituentProductionName + "~" + goalCategPlusleftCorner + starredLeftSisterLabel);

				//set rankNr in dopparser.Node - you only need this for printing!!!
				if (Main.PRINT_LATEX_TREES_WITH_RANKNRS) leftmostNode.setRankNrInDerivation(rankNr);

				//##########################################################
				//store rankedETN inside episodicConstituents in episodicGrammar
				//the third entry in myETN is used to look up what is next node in derivation
				if (sentenceNr >-1)	{	//only for train sentences, don't fill rankNr in HPN nodes for test sentences

					Integer[] myETN = {rankNr, projectedOrAttachedConstituentProductionName.hashCode()};

					//was: if (leftmostNode.getType()==parser.parameters.TERMINAL)
					//	this.episodicInputNodes.get(leftmostNode.getProductionName()).addRankedETN(myETN);
					//else 
					this.episodicNonTerminalProductions.get(leftmostNode.getProductionName()).addRankedETN(myETN,sentenceNr);
				}
				rankNr++;
			}
			//remove child from ArrayList
			tempParseTree.remove(leftmostNode);			
		}	//while (tempParseTree.size()>0)


		return bindingsOfLCDerivation;
	}
	

	
	/**
	 * Computes bindings ranked/ordered along direction of top-down leftmost derivation 
	 * at the same time it stores rank nrs inside episodicConstituent 's in episodicGrammar
	 * @param myParseTree
	 * @param sentenceNr
	 * @return bindingsOfTopDownDerivation
	 */
	public ArrayList<String> rankBindingsForTopDownDerivation(parseTree myParseTree, int sentenceNr) {

		ArrayList<Node> originalParseTree = myParseTree.getNodes();
		/**
		 * binding = thisUnit.getProductionName()~nextUnit.getProductionName()~conditioningInfo~leftSisterOfThis
		 */ 
		ArrayList<String> bindingsOfTopDownDerivation = new ArrayList<String>();

		ArrayList<Node> nodesInOrderOfDerivation = new ArrayList<Node>();
		
		int rankNr = 0;
		//int fragmentNr = 1;
		//strategy: start with TOP node, and recursive enumerate the children if there are any
		//if there are no children then stop

		//find TOP
		parser.Node topNode = null;
		for (Node myNode : originalParseTree)  if (myNode.getName().equals("TOP")) topNode =myNode;


		//save binding in bindingsOfLCDerivation binding: parent~this~this.leftsister
		//3rd element of binding = conditioningContext, 4th part is left sister
		bindingsOfTopDownDerivation.add("START*TOP" + "~" + topNode.getProductionName() + "~TOP~-");
		
		if (Main.PRINT_LATEX_TREES_WITH_RANKNRS) topNode.setRankNrInDerivation(rankNr);
		//copy rankNrs from dopparser.Nodes to HPN nodes, AND index for slot
		if (sentenceNr>-1)	{	//don't fill rankNr in HPN nodes for test sentences
			Integer[] myETN = {rankNr, topNode.getProductionName().hashCode()};
			//note that HPNNodeName is name of production, e.g., TOP*NP*VP
			//get(parent)
			this.episodicNonTerminalProductions.get("START*TOP").addRankedETN(myETN, sentenceNr);
		}
		rankNr++;

		nodesInOrderOfDerivation.add(topNode);
		
		int nrChild=0;
		for (parser.Node childNode : topNode.getChildNodes()) {
			//only add rankNr to nonTerminals (or preTerminals)
			if (childNode.getType()==parser.parameters.NONTERMINAL) {
				//was: String childLeftSister = "-";
				//was: if (nrChild>0) childLeftSister = topNode.getChildNodes().get(nrChild-1).getProductionName();				
				//was: rankNr = recursivelyEnumerateChildren(topNode, "-", childNode, childLeftSister, rankNr, bindingsOfTopDownDerivation, sentenceNr);
				recursivelyEnumerateChildren2(childNode, rankNr, bindingsOfTopDownDerivation, sentenceNr, nodesInOrderOfDerivation, nrChild);
			}
			nrChild++;
		}
		return bindingsOfTopDownDerivation;
	}

	public int recursivelyEnumerateChildren(Node parentNode, String parentLeftSister, Node thisNode, String thisLeftSister, int rankNr, ArrayList<String> bindingsOfTDDerivation, int sentenceNr) {

		//save binding in bindingsOfLCDerivation: binding: parent~this~parent.leftsister
		//NOT parentNode, but previous right node in binding
		bindingsOfTDDerivation.add(parentNode.getProductionName() + "~" + thisNode.getProductionName() + "~" + parentLeftSister);

		//add rank
		if (Main.PRINT_LATEX_TREES_WITH_RANKNRS) parentNode.setRankNrInDerivation(rankNr);
		//copy rankNrs from dopparser.Nodes to HPN nodes, AND index for slot
		if (sentenceNr>-1)	{	//don't fill rankNr in HPN nodes for test sentences
			Integer[] myETN = {rankNr, thisNode.getProductionName().hashCode()};
			//put pointer to thisNode in parentNode
			this.episodicNonTerminalProductions.get(parentNode.getProductionName()).addRankedETN(myETN, sentenceNr);
		}

		rankNr++;

		//recursive call: if myNode hasChildren
		if (thisNode.getChildNodes().size()>0) {	//includes unary rules
			int nrChild=0;
			for (parser.Node childNode : thisNode.getChildNodes()) {
				//only add rankNr to nonTerminals (or preTerminals)
				if (childNode.getType()==parser.parameters.NONTERMINAL) {
					String childLeftSister = "-";
					if (nrChild>0) childLeftSister = thisNode.getChildNodes().get(nrChild-1).getProductionName();
					rankNr = recursivelyEnumerateChildren(thisNode, thisLeftSister, childNode, childLeftSister, rankNr, bindingsOfTDDerivation, sentenceNr);
				}
			nrChild++;
			}
		}
		return rankNr;
	}

	
	public int recursivelyEnumerateChildren2(Node thisNode, int rankNr, ArrayList<String> bindingsOfTDDerivation, int sentenceNr, ArrayList<Node> nodesInOrderOfDerivation, int registerPos) {

		//save binding in bindingsOfLCDerivation: binding: parent~this~parent.leftsister
		//NOT parentNode, but previous right node in binding
		
		//als de vorige een preterminal was...behalve als het laatste woord vd zin is,
		//dan moet je een binding vd preterminal naar "attach" invoegen, en vervolgens previousNodeInDerivation
		//veranderen naar de (parent) treelet, in register position whichChild. 
		//kijk naar previousNodeInDerivation
		//hier komt'ie alleen als thisNode nonT is.
		//je hoeft alleen non-zero register position te maken voor previousNodeInDerivation, want thisNode is altijd in register position 0
		//find previousNodeInDerivation: last one in array
		Node previousNodeInDerivation = nodesInOrderOfDerivation.get(nodesInOrderOfDerivation.size()-1);
		
		String registerPosition = "";
		if (Main.EXTRA_ATTACHMENT_IN_TD_DERIVATION) {
			
			registerPosition = "_" + registerPos;	//for later, when thisNode becomes previousNode.
			
			//if previousNodeInDerivation is preterminal you have to add an extra binding 
			//from the preterminal to the parent of thisNode in next register position 
			if (previousNodeInDerivation.getChildNodes().get(0).getType()==parser.parameters.TERMINAL) {
				previousNodeInDerivation = thisNode.getParentNode();
				/*
				//add binding from parentNode to "attach"
				Node parentNode = thisNode.getParentNode();
				
				String conditioningContext = previousNodeInDerivation.getChildNodes().get(0).getProductionName(); //terminal name
				
				bindingsOfTDDerivation.add(previousNodeInDerivation.getProductionName() + "~attach" + "~" + conditioningContext + "~-");

				//add rank
				if (experiments.PRINT_LATEX_TREES_WITH_RANKNRS) previousNodeInDerivation.setRankNrInDerivation(rankNr);
				//copy rankNrs from dopparser.Nodes to HPN nodes, AND index for slot
				if (sentenceNr>-1)	{	//don't fill rankNr in HPN nodes for test sentences
					String strAttach = "attach";
					Integer[] myETN = {rankNr, strAttach.hashCode()};
					//put pointer to thisNode in parentNode
					this.episodicNonTerminalProductions.get(previousNodeInDerivation.getProductionName()).addRankedETN(myETN, sentenceNr);
				}

				rankNr++;
				
				previousNodeInDerivation = parentNode;	//you want to make another binding from parent of thisNode (in register position childNr) to thisNode.
				*/
			}
		}
		
		nodesInOrderOfDerivation.add(thisNode);
		
		//find sister of parent, and slotNr (which child is this)
		String previousNodeLeftSister = "-";
		Node parentNode = previousNodeInDerivation.getParentNode();
		if (!(parentNode==null)) {
			int whichChild = parentNode.getChildNodes().indexOf(previousNodeInDerivation);
			if (whichChild>0) previousNodeLeftSister = parentNode.getChildNodes().get(whichChild-1).getProductionName();
		}
		
		// 3rd element of binding is conditioningContext (= root of nextProduction)
		String conditioningContext = thisNode.getProductionName();	//for terminals (komt als het goed is nooit voor)
		if (thisNode.getType()==parser.parameters.NONTERMINAL) conditioningContext = conditioningContext.split("\\*")[0];	// productionName is in format parent*chi1*chi2*etc.)
		
		//binding = thisUnit.getProductionName()~nextUnit.getProductionName()~conditioningInfo~leftSisterOfThis
		bindingsOfTDDerivation.add(previousNodeInDerivation.getProductionName() + registerPosition + "~" + thisNode.getProductionName() + "~" + conditioningContext + "~" + previousNodeLeftSister);

		//add rank
		if (Main.PRINT_LATEX_TREES_WITH_RANKNRS) previousNodeInDerivation.setRankNrInDerivation(rankNr);
		//copy rankNrs from dopparser.Nodes to HPN nodes, AND index for slot
		if (sentenceNr>-1)	{	//don't fill rankNr in HPN nodes for test sentences
			Integer[] myETN = {rankNr, thisNode.getProductionName().hashCode()};
			//put pointer to thisNode in parentNode
			//if (previousNodeInDerivation.getProductionName().contains("Mark") ) System.out.println("previousNodeInDerivation.getProductionName()=" + previousNodeInDerivation.getProductionName());
			this.episodicNonTerminalProductions.get((previousNodeInDerivation.getProductionName()+ registerPosition)).addRankedETN(myETN, sentenceNr);
		}

		rankNr++;

		//recursive call: if myNode hasChildren
		if (thisNode.getChildNodes().size()>0) {	//includes unary rules
			int nrChild=0;
			for (parser.Node childNode : thisNode.getChildNodes()) {
				//only add rankNr to nonTerminals (or preTerminals)
				if (childNode.getType()==parser.parameters.NONTERMINAL) {
					String childLeftSister = "-";
					if (nrChild>0) childLeftSister = thisNode.getChildNodes().get(nrChild-1).getProductionName();
					rankNr = recursivelyEnumerateChildren2(childNode, rankNr, bindingsOfTDDerivation, sentenceNr, nodesInOrderOfDerivation, nrChild);
					//rankNr=rankAndFragment[0];
					//fragmentNr=rankAndFragment[1];
				}
				nrChild++;
			}
		}
		//int[] rankAndFragment = {rankNr, fragmentNr};
		return rankNr; 
	}
	
	public void printConditionalProbabilitiesFromETNs(ArrayList<String> probabilityTable, BufferedWriter leftCornerProbabilitiesFile,
			NumberFormat numberFormatter, HashSet<String> nodeLabels) throws IOException, Exception {

		//PRINTING

		boolean PRINT_TREES=false;
		if (PRINT_TREES) {

			for (parseTree aParseTree : this.parseTreesWithETNs) {
				//latexSentences.add(aParseTree.printToLatex(false, true, ""));
				//printToLatex(boolean blnPrintNrSubtrees, boolean blnPrintNodeSpans, String strText){
				boolean printETNs = false, printStructureETNs = true;
				String latexTree = aParseTree.printToLatexParseTreeSty("", printETNs, printStructureETNs);
				//was: printToLatex
				latexTree = Utils.replaceSpecialSymbols(latexTree);

				//was: treeFile.write("\\scalebox{.4}{\\Tree" + latexTree + "}");
				leftCornerProbabilitiesFile.write("\\begin{parsetree} " + latexTree + " \\end{parsetree}");
				leftCornerProbabilitiesFile.newLine();
				leftCornerProbabilitiesFile.newLine();
			}
		}
		int nrCNodes = episodicNonTerminalProductions.size();

		StringBuffer ls = new StringBuffer();
		for (int i =0; i<nrCNodes+3; i++) ls.append("l");

		leftCornerProbabilitiesFile.write("\\begin{longtable}[htbp]{" + ls.toString() + "}"); leftCornerProbabilitiesFile.newLine();

		leftCornerProbabilitiesFile.write("\\multicolumn{" + (nrCNodes+3) + "}{c}{Project and attach probabilities (conditioned on left corner and goal slot)} \\\\ \\hline"); leftCornerProbabilitiesFile.newLine();

		//treeFile.write("left corner & goal slot & P(attach) & P(project to X1) & P(project to X2) \\\\ \\hline"); treeFile.newLine();

		StringBuffer columnHeaderLine = new StringBuffer();
		columnHeaderLine.append("l. corner & goal slot & P(attach) ");			
		for (String projectedNodeLabel : episodicNonTerminalProductions.keySet()) {	
			if (!(projectedNodeLabel.equals("START")))
				columnHeaderLine.append("& Pr(" + projectedNodeLabel + ")");
		}
		columnHeaderLine.append(" \\\\");
		leftCornerProbabilitiesFile.write(columnHeaderLine.toString());
		// content: look up the probabilities in P_ProjectGivenSlotandLC and P_buAttachGivenSlotandLC
		for (String tableLine : probabilityTable) {
			leftCornerProbabilitiesFile.write(tableLine);
		}
		Utils.printCloseLatexTable(leftCornerProbabilitiesFile, "Project and attach probabilities computed from ETNs that are shared by left corner and corresponding goal slot", "tab:probabilities");

	}


	/**
	 * No longer used.
	 * 
	 * @param projections
	 * @param attachments
	 * @param shifts
	 */
	public void setLeftCornerProbabilities(HashMap<String, HashMap<String, Double>> projections,  HashMap<String, Double> attachments, HashMap<String, Double> shifts) {
		//read P_ProjectGivenSlotandLC, P_buAttachGivenSlotandLC, P_shiftGivenSlot
		/*
		String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
		P_ProjectGivenSlotandLC
		nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
		P_buAttachGivenSlotandLC

		shift:
		nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
		P_shiftGivenSlot
		 */
		/*
		//copy from projections to P_ProjectGivenSlotandLC
		for (String goalSlotPlusLC : projections.keySet()) {
			HashMap<String, Double> conditionalProbs = new HashMap<String, Double>();
			conditionalProbs.putAll(projections.get(goalSlotPlusLC));
			//first key is left corner plus goalslot, value is HM of projected nodelabels plus their frequency, given l.c. and goalslot
			//HashMap<String, HashMap<String, Double>> P_ProjectGivenSlotandLC
			P_ProjectGivenSlotandLC.put(goalSlotPlusLC, conditionalProbs);
		}

		for (String goalSlotPlusLC : attachments.keySet()) {
			P_buAttachGivenSlotandLC.put(goalSlotPlusLC, attachments.get(goalSlotPlusLC));
		}

		if (!(shifts==null)) {
			for (String nodeSlotCombi : shifts.keySet()) {
				P_shiftGivenSlot.put(nodeSlotCombi, shifts.get(nodeSlotCombi));
			}
		}
		 */

		///////////////////////////////////////////////////////////////////
		// check whether there are any missing (null) combinations, set their probabilities to zero
		// and normalize
		///////////////////////////////////////////////////////////////////
		HashSet<String> leftCorners = new HashSet<String>();

		//leftCorners.addAll(episodicInputNodes.keySet());
		leftCorners.addAll(episodicNonTerminalProductions.keySet());
		leftCorners.remove("START");

		for (String goalNodeLabel : episodicNonTerminalProductions.keySet()) {
			//get representations of all non-left slots
			for (int slotnr =1; slotnr <= episodicNonTerminalProductions.get(goalNodeLabel).getSlots().size(); slotnr++) {
				if (slotnr >1)	{ //skip left slot

					for (String leftCornerLabel : leftCorners) {

						//goalSlotPlusLC = goalNodeLabel + "@" + (childIndex +1) + "@" + leftCornerLabel;
						String goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						HashMap<String, Double> conditionalProjectProbs = projections.get(goalSlotPlusLC);
						double totalProbability = 0.;

						//compute totals
						for (String projectedNodeLabel : episodicNonTerminalProductions.keySet()) {			
							//String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
							if (conditionalProjectProbs.get(projectedNodeLabel)==null) conditionalProjectProbs.put(projectedNodeLabel, 0.);
							else totalProbability += conditionalProjectProbs.get(projectedNodeLabel);

						}
						//String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						if (attachments.get(goalSlotPlusLC)==null) attachments.put(goalSlotPlusLC, 0.);
						else totalProbability += attachments.get(goalSlotPlusLC);

						//normalize
						if (totalProbability==0.) totalProbability=1.;
						for (String projectedNodeLabel : episodicNonTerminalProductions.keySet()) {
							//nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
							conditionalProjectProbs.put(projectedNodeLabel, conditionalProjectProbs.get(projectedNodeLabel)/totalProbability);
							//if (Main.PRINT_PROBABILITY_MODEL && conditionalProjectProbs.get(projectedNodeLabel)>0.) System.out.println("P_Project to " + projectedNodeLabel + " from LC= " + leftCornerLabel + " given goalSlot= " + goalNodeLabel + "@" + slotnr + ": " + P_ProjectGivenSlotandLC.get(projectedNodeLabel));
						}
						//nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						attachments.put(goalSlotPlusLC, attachments.get(goalSlotPlusLC)/totalProbability);
						//if (Main.PRINT_PROBABILITY_MODEL && P_buAttachGivenSlotandLC.get(goalSlotPlusLC)>0.) System.out.println("P_BU_Attach from LC= " + leftCornerLabel + " given goalSlot= " + goalNodeLabel + "@" + slotnr + ": " + P_buAttachGivenSlotandLC.get(goalSlotPlusLC));
					}	//for (String leftCornerLabel : nodeLabels)
				}	//if (slotnr >1)
			}	//loop over slotNrs
		}	//for (String goalNodeLabel : compressorLayerNodes.keySet())

		// set forgotten shift probabilities to zero
		if (!(shifts==null)) {
			for (String goalNodeLabel : episodicNonTerminalProductions.keySet()) {
				//loop over slots
				for (int slotnr =1; slotnr <= episodicNonTerminalProductions.get(goalNodeLabel).getSlots().size(); slotnr++) {
					double totalProbability = 0.;
					for (String leftCornerLabel : leftCorners) {
						String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						if (shifts.get(nodeSlotCombi)==null) shifts.put(nodeSlotCombi, 0.);
						else totalProbability += shifts.get(nodeSlotCombi);
					}	

					//normalize
					if (totalProbability==0.) totalProbability=1.;
					for (String leftCornerLabel : leftCorners) {
						String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						shifts.put(nodeSlotCombi, shifts.get(nodeSlotCombi)/totalProbability);
						//if (Main.PRINT_PROBABILITY_MODEL && P_shiftGivenSlot.get(nodeSlotCombi)>0.) System.out.println("P_shiftGivenSlot of " + nodeSlotCombi + "=" + P_shiftGivenSlot.get(nodeSlotCombi)/totalProbability);
					}
				}	//loop over slots
			}	//loop over goalNodes
		}
	}






	/**
	 * Not in use.
	 * (this is goalSlot where highest recursive projection from current node is attached)
	 * 
	 * @param myETN
	 * @param mySlot
	 */
	public String getLeftCornerGoalSlot(String myETN) {
		return this.leftCornerGoalSlots.get(myETN);
	}

	/**
	 * Not in use.
	 * 
	 * @param myETN
	 * @param mySlot
	 */
	public void addLeftCornerGoalSlot(String myETN, String mySlot) {
		this.leftCornerGoalSlots.put(myETN, mySlot);
	}


	/**
	 * Not in use. Only for compliance with the grammar class (for chartParser).
	 */
	public HashMap<String, HashMap<String, Double>> getProjectAndAttachProbabilities() {
		return null;
	}

	/**
	 * Not in use. Only for compliance with the grammar class (for chartParser).
	 */
	public HashMap<String, HashMap<String, Double>> getShiftProbabilities() {
		return null;
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
	
}
