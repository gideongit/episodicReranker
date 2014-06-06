package episodicReranker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import parser.Node;
import parser.parseTree;

public class lcsGrammar extends grammar {

	//attachments have "attachment" written in the second String; first String is goalCategory plus leftCornerCategory (for conditioning)
	protected HashMap<String, HashMap<String, Double>> projectionsAndAttachments = new HashMap<String, HashMap<String, Double>>();
	//String = conditioning context: dotted nonterminal; second String = terminal
	protected HashMap<String, HashMap<String, Double>> shiftProbabilities = new HashMap<String, HashMap<String, Double>>();
	
	public static boolean conditioningOnProduction = false;
	
	// HPNNet class constructor.
	public lcsGrammar(ArrayList<parseTree> preprocessedParseTrees) {

		//add special START production; the following will also take care of adding a START^1 nonterminal to lexicon
		//if (experiments.INCLUDE_SHIFT_PRODUCTIONS) 
			nonTerminalProductions.put("START*-*TOP", null);
		
		for (parseTree myParseTree : preprocessedParseTrees) {
	
			myParseTree.assignProductionNamesToNodes();
			//fills terminalUnits and nonTerminalUnits
			addToLexicons(myParseTree);
		}
		
		if (Main.SMOOTH_FOR_UNKNOWN_PRODUCTIONS && Main.DO_HORIZONTAL_MARKOVIZATION)
			 createVirtualProductions();
		
		//this.lexicon = (HashSet) terminalUnits.keySet();
	    //create nonTerminal nodes  
		for (String nodeName : nonTerminalProductions.keySet()) {          
	        
			//if (nodeName.contains("*")) {: they all do
			String[] production = nodeName.split("\\*");
			String rootSymbol = production[0];
			ArrayList<String> children = new ArrayList<String>();
			
			for (int i=1; i<production.length; i++) {
				children.add(production[i]);
				//if it is not the last child put starred nonT in between the children
				if (i< (production.length-1)) {
					//add number of dots equal to i
					String dottedNonT =production[0] + "^" + i;
					//for (int j=0; j<i; j++) dottedNonT +="^";
					children.add(dottedNonT);
				}
			}
			
	        episodicReranker.synUnit cNode = new lcsgRule(rootSymbol, children, nodeName);
            this.nonTerminalProductions.put(nodeName, cNode);
            
           /* shift rules NOT HERE
            * TOCH WEL!!! voor elke combinatie van starredNonT en woord.
            * zie episodicChartParser:
            * //create a node out of shift treelet from starredNonTerminal to newWord
					ArrayList<String> children = new ArrayList<String>();
					children.add(starredNonTerminal );
			        synUnit shiftNode = new lcsgRule(newWord, children, starredNonTerminal + "*" + newWord);
			     
			     
            if (comparativeLikelihoods.INCLUDE_SHIFT_PROBABILITIES) {
            	//create extra nodes for the positions between the slots: these must receive their own ETNs
            	String[] splitProduction = nodeName.split("\\*");

            	//number of children = splitProduction.length-1; number of intermediate slotpositions = splitProduction.length-1
            	if (splitProduction.length >=3) {	//skip unary productions
            		String rhs ="";
            		for (int i =1; i< (splitProduction.length); i++) rhs += "*" + splitProduction[i];
            		for (int i =1; i<= (splitProduction.length-2); i++) {
            			String nodeStateName = splitProduction[0] + "^" + i + rhs;
            			cNode = new lcsgRule(nodeStateName);
                        this.nonTerminalUnits.put(nodeStateName, cNode); 
            		}
            	}
            }
            */
	    }
	}
	
	//results in updated attachment and projection rules, also prints leftCornerProbabilities to ./treebanks/left_corner_probabilities
	public void estimateSymbolicLCProbabilities(ArrayList<parseTree> reducedParseTreesFromTreebank) throws Exception {

		//HashMap<String, HashMap<String, Double>> projectionsAndAttachments = myGrammar.getProjectAndAttachProbabilities();
		//HashMap<String, HashMap<String, Double>> shiftProbabilities = myGrammar.getShiftProbabilities();
		
		parser.Node topNode = null;
		if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) {
			HashMap<String, Double> shiftsFromSTART = new HashMap<String, Double>();
			String goalCat = "TOP";
			if (conditioningOnProduction) goalCat = "TOP_START*-*TOP";
			
			shiftProbabilities.put(goalCat + "@START^1", shiftsFromSTART);
		}
		//enumerate the parsetrees, and count the rules
		for (parseTree myParseTree : reducedParseTreesFromTreebank) {			

			//find TOP
			for (Node myNode : myParseTree.getNodes())  if (myNode.getName().equals("TOP")) topNode =myNode;
			
			// enumerate nodes in dopparser.parseTree:	
			for (parser.Node treeNode : myParseTree.getNodes()) {

					String leftCornerLabel = treeNode.getName();	//treeNode.getProductionName();

					parser.Node parentNode = treeNode.getParentNode();
					int childIndex = 0;
					
					
					/* moet niet: want dan krijg je dat projected node overgrootvader is!
					//find parent node: skip unary parent nodes, and continue up until you find parent with more than one child
					while (!(parentNode==null) && singleChild ) {
						//find out which one of the children this is
						//System.out.println("parentNode=" + parentNode.getHPNNodeName());
						//System.out.println("grandParentNode=" + parentNode.getParentNode().getHPNNodeName() + "; #children=");
						if (!(parentNode.getParentNode()==null)) childIndex = parentNode.getParentNode().getChildNodes().indexOf(parentNode);
						parentNode = parentNode.getParentNode();	//in fact grandparent
						if (!(parentNode==null) && parentNode.getChildNodes().size()>1) singleChild = false; //breaks from while loop
					}
					*/
					//check
					//if (!(parentNode==null))
					//	System.out.println("leftCornerLabel=" + leftCornerLabel + "; parentNode=" + parentNode.getHPNNodeName() + "; childIndex=" + childIndex);
					//else System.out.println("leftCornerLabel=" + leftCornerLabel + "; parentNode=null");

					if (!(parentNode==null)) {	
						
						childIndex = parentNode.getChildNodes().indexOf(treeNode);
						//if this is the left child, then projection
						if (childIndex==0) {	//projection
							//unique name for category labels only if they have same number of children
							//projectedNode is the first non-unary parent in the chain (of which treeNode is left descendant
							String projectedRule = parentNode.getProductionName();
							
							//continue looking up for goalslot, until you find a parent that it is not left child
							boolean leftChild = true;
							//find parent node of projectedNode: skip parent nodes for which child is left child, and continue up until you find paren with more than one child
							while (!(parentNode==null) && leftChild ) {
								//find out which one of the children this is
								if (!(parentNode.getParentNode()==null)) childIndex = parentNode.getParentNode().getChildNodes().indexOf(parentNode);
								parentNode = parentNode.getParentNode();
								if (childIndex>0) leftChild = false; //breaks from while loop
							}
							//project probabilities : nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
							String goalSlotPlusLC = "";
							if (!(parentNode==null)) {	//you found a parent for which the current branch is not attached to left child (goalslot is not left slot) 
								String goalNodeLabel = parentNode.getChildNodes().get(childIndex).getName();	//.getProductionName();
								
								//getProductionName = parentLabel*childLabels
								if (conditioningOnProduction) goalNodeLabel += "_" + parentNode.getProductionName();
								//goalSlotPlusLC = goalNodeLabel + "@" + (childIndex +1) + "@" + leftCornerLabel;
								goalSlotPlusLC = goalNodeLabel + "@" + leftCornerLabel;
							}
							
							else {	//no parent for which not left child: left chaining, goalslot is START_slot2
								//in this case the goal category is simply the TOP node production	
								//goalSlotPlusLC = topNode.getProductionName()+ "@1@" + leftCornerLabel;	
								goalSlotPlusLC = topNode.getName()+ "@" + leftCornerLabel;
								if (conditioningOnProduction) //goalSlotPlusLC = "TOP_" + topNode.getProductionName()+ "@" + leftCornerLabel;
									goalSlotPlusLC = "TOP_START*-*TOP" + "@" + leftCornerLabel;
							}
							
							//System.out.println("projection; projectedNodeLabel=" + projectedNodeLabel + "; nodeSlotCombi=" + nodeSlotCombi);
							//HashMap<String, HashMap<String, Double>> projections
							//first key is left corner plus goalslot, value is HM of projected nodelabels plus their frequency, given l.c. and goalslot
							HashMap<String, Double> projectedNodeCounts = null;
							if (projectionsAndAttachments.get(goalSlotPlusLC)==null) {	
								//new HM
								projectedNodeCounts = new HashMap<String, Double>();
								projectionsAndAttachments.put(goalSlotPlusLC, projectedNodeCounts);
								projectedNodeCounts.put(projectedRule, 1.);
							}
							else {
								projectedNodeCounts = projectionsAndAttachments.get(goalSlotPlusLC);
								if (projectedNodeCounts.get(projectedRule)==null) 
									projectedNodeCounts.put(projectedRule, 1.);
								else projectedNodeCounts.put(projectedRule, projectedNodeCounts.get(projectedRule) + 1.);
							}

						}
						else {	//non-left child: b-u attachment
							//keep the frequency counts, probabilities are stored after normalization
							// nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
							String goalNodeLabel = parentNode.getChildNodes().get(childIndex).getName();	//.getProductionName();
							if (conditioningOnProduction) goalNodeLabel += "_" + parentNode.getProductionName();
							
							String nodeSlotCombi = goalNodeLabel + "@" + leftCornerLabel;	// + (childIndex +1) + "@"
							//System.out.println("attachment;  nodeSlotCombi=" + nodeSlotCombi);
							//was: attachmentFrequencies ipv attachments
							HashMap<String, Double> projAndattachFreqOfLCGoal;
							if (projectionsAndAttachments.get(nodeSlotCombi)==null) {
								projAndattachFreqOfLCGoal = new HashMap<String, Double>();
								projectionsAndAttachments.put(nodeSlotCombi, projAndattachFreqOfLCGoal);
							}
							else projAndattachFreqOfLCGoal = projectionsAndAttachments.get(nodeSlotCombi);
							
							if (projAndattachFreqOfLCGoal.get("attachment")==null) {
								projAndattachFreqOfLCGoal.put("attachment", 1.);
							}
							else projAndattachFreqOfLCGoal.put("attachment", projAndattachFreqOfLCGoal.get("attachment") + 1.);
						}
						
						
						//shift probabilities
						//if (experiments.INCLUDE_SHIFT_PRODUCTIONS) {
							if (treeNode.getType()==parser.parameters.TERMINAL) {
								String shiftTerminalLabel = treeNode.getProductionName();
								//find starred nonterminal from which shift happened
								if (treeNode.getLeftSpan()>0) {
				
									String shiftProduction = findShiftProductionAssociatedWithTerminal(myParseTree, treeNode);
									String dottedNonTerminal = shiftProduction.split("~")[0];
									String goalCat = shiftProduction.split("~")[1];
									if (conditioningOnProduction) goalCat += "_" + parentNode.getProductionName();
									
									String goalCatPlusLC = goalCat + "@" + dottedNonTerminal;
									
									//System.out.println("shiftProduction=" + shiftProduction);
											
									HashMap<String, Double> shiftTerminalCounts = null;
									if (shiftProbabilities.get(goalCatPlusLC)==null) {	
										//new HM
										shiftTerminalCounts = new HashMap<String, Double>();
										shiftProbabilities.put(goalCatPlusLC, shiftTerminalCounts);
										shiftTerminalCounts.put(shiftTerminalLabel, 1.);
									}
									else {
										shiftTerminalCounts = shiftProbabilities.get(goalCatPlusLC);
										if (shiftTerminalCounts.get(shiftTerminalLabel)==null) 
											shiftTerminalCounts.put(shiftTerminalLabel, 1.);
										else shiftTerminalCounts.put(shiftTerminalLabel, shiftTerminalCounts.get(shiftTerminalLabel) + 1.);
									}
									//shiftProductions.add(shiftProduction);
								}
								else {	//leftSpan=0: shift from START^1 "START^1~"
									HashMap<String, Double> shiftTerminalCounts = shiftProbabilities.get("TOP@START^1");
									if (conditioningOnProduction) shiftTerminalCounts = shiftProbabilities.get("TOP_START*-*TOP@START^1");
									
									if (shiftTerminalCounts.get(shiftTerminalLabel)==null) 
										shiftTerminalCounts.put(shiftTerminalLabel, 1.);
									else shiftTerminalCounts.put(shiftTerminalLabel, shiftTerminalCounts.get(shiftTerminalLabel) + 1.);		
								}
							}
						//}	//if (INCLUDE_SHIFT_PROBABILITIES)
						
					}	//	if (!(parentNode==null)) {
			}	//for (dopparser.Node treeNode : myParseTree.getNodes())
		}	//for (String simpleSentence : completeSentences)
		
		normalizeSymbolicLeftCornerProbabilities(projectionsAndAttachments, shiftProbabilities);
		
	}

public double computeManningCarpenterSentenceProbability(int sentenceCounter, parseTree myTestParseTree) {
		
		
		HashMap<String, HashMap<String, Double>> projectionsAndAttachments = this.getProjectAndAttachProbabilities();
		HashMap<String, HashMap<String, Double>> shiftProbabilities = this.getShiftProbabilities();
		
		double sentenceProbability = 1.;
		parser.Node parentNode;

		//find TOP
		parser.Node topNode = null;
		for (Node myNode : myTestParseTree.getNodes())  if (myNode.getName().equals("TOP")) topNode =myNode;
		
		for (Node myNode : myTestParseTree.getNodes()) {

			parentNode = myNode.getParentNode();
			String leftCornerLabel = myNode.getName();	//.getProductionName();

			int childIndex = 0;
			
			if (!(parentNode== null)) {
								
				//find goalSlot in tree; left corner = currentNode --> construct goalSlotPlusLC
				
				childIndex = parentNode.getChildNodes().indexOf(myNode);
				
				if (childIndex==0) {	//projection

					// in case of projection projected node = parent
					String projectedRule = parentNode.getProductionName();
					
					//continue looking up for goalslot, until you find a parent that it is not left child
					boolean leftChild = true;
					//find parent node of projectedNode: skip parent nodes for which child is left child, and continue up until you find paren with more than one child
					while (!(parentNode==null) && leftChild ) {
						//find out which one of the children this is
						if (!(parentNode.getParentNode()==null)) childIndex = parentNode.getParentNode().getChildNodes().indexOf(parentNode);
						parentNode = parentNode.getParentNode();
						if (childIndex>0) leftChild = false; //breaks from while loop
					}
					
					//project probabilities : nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
					String goalSlotPlusLC = "";
					if (!(parentNode==null)) {	//you found a parent for which the current branch is not attached to left child (goalslot is not left slot) 
						String goalNodeLabel = parentNode.getChildNodes().get(childIndex).getName();
						//String goalNodeLabel = parentNode.getName();	//.getProductionName();
						//goalSlotPlusLC = goalNodeLabel + "@" + (childIndex +1) + "@" + leftCornerLabel;
						goalSlotPlusLC = goalNodeLabel + "@" + leftCornerLabel;
					}
					
					else {	//no parent for which not left child: left chaining, goalslot is START_slot2
						//in this case the goal category is simply the TOP node production	
						//goalSlotPlusLC = topNode.getProductionName() + "@1@" + leftCornerLabel;
						goalSlotPlusLC = topNode.getName() + "@" + leftCornerLabel;
					}
					
					//System.out.println("projection; projectedNodeLabel=" + projectedNodeLabel + "; nodeSlotCombi=" + nodeSlotCombi);

					//first key is left corner plus goalslot, value is HM of projected nodelabels plus their frequency, given l.c. and goalslot

					if (!(projectionsAndAttachments.get(goalSlotPlusLC)==null) && !(projectionsAndAttachments.get(goalSlotPlusLC).get(projectedRule)==null)) {	
						double ruleProbability = projectionsAndAttachments.get(goalSlotPlusLC).get(projectedRule);
						sentenceProbability *= ruleProbability;
						if (Main.FEEDBACK) System.out.println("project to " + projectedRule + " from l.c. " + goalSlotPlusLC.split("@")[1] + " with goalNode=" + goalSlotPlusLC.split("@")[0] + "; ruleProbability=" + ruleProbability);	// + ", slot_" + goalSlotPlusLC.split("@")[1]
					}
					else {
						//if (!(projectionsAndAttachments.get(goalSlotPlusLC)==null)) System.out.println("goalSlotPlusLC bestaat wel");
						//for (String a : projectionsAndAttachments.get(goalSlotPlusLC).keySet()) System.out.println("projectedNodeLabel=" + a);
						if (Main.FEEDBACK) System.out.println("Sentence #" + sentenceCounter + ": projection probability==null for goalSlotPlusLC=" + goalSlotPlusLC + " and projectedNodeLabel=" + projectedRule);
						sentenceProbability=0.;
					}

				}
				else {	//non-left child: attachment
					//find conditional attach (replace projectedNodeLabel by "attachment") of project probability in projectionsAndAttachments
					// nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
					String goalNodeLabel = parentNode.getChildNodes().get(childIndex).getName();
					//String goalNodeLabel = parentNode.getName();	//.getProductionName();

					String goalSlotPlusLC = goalNodeLabel + "@" + leftCornerLabel;	//+ (childIndex +1) + "@" 
					//System.out.println("attachment;  nodeSlotCombi=" + nodeSlotCombi);

					if (!(projectionsAndAttachments.get(goalSlotPlusLC)==null) && !(projectionsAndAttachments.get(goalSlotPlusLC).get("attachment")==null)) {	
						double ruleProbability = projectionsAndAttachments.get(goalSlotPlusLC).get("attachment");
						sentenceProbability *= ruleProbability;
						if (Main.FEEDBACK) System.out.println("attach to " + goalSlotPlusLC.split("@")[0] + " from l.c. " + goalSlotPlusLC.split("@")[1] + "; ruleProbability=" + ruleProbability); // + ", slot_" + goalSlotPlusLC.split("@")[1]
					}
					else {
						if (Main.FEEDBACK) System.out.println("Sentence #" + sentenceCounter + ": attach probability==null for goalSlotPlusLC=" + goalSlotPlusLC);
						sentenceProbability=0.;
					}	
				}
				
				if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) {
					if (myNode.getType()==parser.parameters.TERMINAL) {
						
						if (myNode.getLeftSpan()>0) {
							String shiftTerminalLabel = myNode.getProductionName();
							String shiftProduction = findShiftProductionAssociatedWithTerminal(myTestParseTree, myNode);
							String dottedNonTerminal = shiftProduction.split("~")[0];
							String goalCatPlusLC = shiftProduction.split("~")[1] + "@" + dottedNonTerminal;
							
							if (!(shiftProbabilities.get(goalCatPlusLC)==null) && !(shiftProbabilities.get(goalCatPlusLC).get(shiftTerminalLabel)==null)) {	
								double ruleProbability = shiftProbabilities.get(goalCatPlusLC).get(shiftTerminalLabel);
								sentenceProbability *= ruleProbability;
							}
							else {
								System.out.println("Sentence #" + sentenceCounter + ": shift probability==null for goalc plus dottedNonTerminal=" + goalCatPlusLC + " and shiftTerminalLabel=" + shiftTerminalLabel);
								sentenceProbability=0.;
							}
							
						}	//if (myNode.getLeftSpan()>0)
					}	//if (myNode.getType()==dopparser.Main.TERMINAL)	
				}	//if (INCLUDE_SHIFT_PROBABILITIES)
			
			}	//if (!(parentNode== null))
			
			
		}	//for (Node myNode : myTestParseTree.getNodes())
		
		return sentenceProbability;
	}


/**
 * returns starredNonTerminal + "~" + goalNonTerminal + "~" + terminalNode.getProductionName();
 * goalNonTerminal = right sister of starredNonT
 * 
 * @param myParseTree
 * @param terminalNode
 * @return
 */	
public static String findShiftProductionAssociatedWithTerminal(parseTree myParseTree, Node terminalNode) {	//, Node topNode

	//als het een terminal is (en niet de eerste) zoek dan de goalCategory 
	//en doe het volgende voor de goalCategory:
	Node goalNode = null;

	boolean leftChild = true;
	int childIndex =0;
	Node parentNode = terminalNode.getParentNode();

	//System.out.println("terminalNode=" + terminalNode.getName() + "; parentNode=" + parentNode.getName() +  "; myParseTree=" + myParseTree.printWSJFormat());
	
	//find parent node of projectedNode: skip parent nodes for which child is left child, and continue up until you find paren with more than one child
	if (!(parentNode==null)) {

		childIndex = parentNode.getChildNodes().indexOf(terminalNode);
		//System.out.println("parentNode=" + parentNode.getName() + "; childIndex=" + childIndex);
		
		//if this is the left child, then projection
		if (childIndex==0) {	//projection

			//continue looking up for goalslot, until you find a parent that it is not left child

			//find parent node of projectedNode: skip parent nodes for which child is left child, and continue up until you find paren with more than one child
			while (!(parentNode==null) && leftChild ) {
				//find out which one of the children this is
				if (!(parentNode.getParentNode()==null)) childIndex = parentNode.getParentNode().getChildNodes().indexOf(parentNode);
				parentNode = parentNode.getParentNode();
				if (childIndex>0) leftChild = false; //breaks from while loop
			}

			//you found a parent for which the current branch is not attached to left child (goalslot is not left slot)
			if (!(parentNode==null)) 	 goalNode = parentNode;

			//no parent for which not left child: left chaining, goalslot is START_slot2
			//else goalNode = topNode;

		}
		//non-left child: b-u attachment
		else goalNode = parentNode;

	}
	
	//add a binding from previousLeftmostNodeParent to terminal (shift)
	//System.out.println("goalNode=" + goalNode.getName() + "; goalNode.getProductionName()=" + goalNode.getProductionName() + "; childIndex=" + childIndex);
	String[] splitProduction = goalNode.getProductionName().split("\\*");
	//String rhs =""; no specific production, only root
	//for (int i =1; i< (splitProduction.length); i++) rhs += "*" + splitProduction[i];
	String starredNonTerminal = splitProduction[0] + "^" + childIndex;	// + rhs;
	String goalNonTerminal = splitProduction[childIndex+1];
	return starredNonTerminal + "~" + goalNonTerminal + "~" + terminalNode.getProductionName() + "~" + goalNode.getProductionName();
}

	public static void normalizeSymbolicLeftCornerProbabilities(HashMap<String, HashMap<String, Double>> P_ProjectOrAttachGivenSlotandLC, HashMap<String, HashMap<String, Double>> shiftProbabilities) {
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
		HashSet<String> goalSlotsPlusLCs = new HashSet<String>();	//combines conditionals of project and attach
		
		for (String goalSlotPlusLC : P_ProjectGivenSlotandLC.keySet()) {
			//HashMap<String, Double> conditionalProbs = new HashMap<String, Double>();
			//conditionalProbs.putAll(projections.get(goalSlotPlusLC));
			//projections.get(goalSlotPlusLC) is HashMap<String, Double>() where String = projectedNodeLabel
			//P_ProjectGivenSlotandLC.put(goalSlotPlusLC, projections.get(goalSlotPlusLC));	
			goalSlotsPlusLCs.add(goalSlotPlusLC);
		}

		for (String goalSlotPlusLC : P_buAttachGivenSlotandLC.keySet()) {
			//P_buAttachGivenSlotandLC.put(goalSlotPlusLC, attachments.get(goalSlotPlusLC));
			goalSlotsPlusLCs.add(goalSlotPlusLC);
		}
*/
		///////////////////////////////////////////////////////////////////
		// normalize probabilities: the prob model assumes that a single step choice between 
		// one attach and project to all cNodes
		///////////////////////////////////////////////////////////////////
		//for (String goalSlotPlusLC : goalSlotsPlusLCs) {
		for (String goalSlotPlusLC : P_ProjectOrAttachGivenSlotandLC.keySet()) {
			HashMap<String, Double> conditionalProjectProbs = P_ProjectOrAttachGivenSlotandLC.get(goalSlotPlusLC);
			double totalProbability = 0.;
			
			//compute totals, sum over all projectedNodeLabel for a certain goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
			//if (!(conditionalProjectProbs==null)) {
				for (String projectedNodeLabel : conditionalProjectProbs.keySet()) {			
					//String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
					//if (conditionalProjectProbs.get(projectedNodeLabel)==null) conditionalProjectProbs.put(projectedNodeLabel, 0.); else 
					totalProbability += conditionalProjectProbs.get(projectedNodeLabel);	
				}
			//}
			//String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
			//if (!(P_buAttachGivenSlotandLC.get(goalSlotPlusLC)==null)) //P_buAttachGivenSlotandLC.put(goalSlotPlusLC, 0.);
			//	totalProbability += P_buAttachGivenSlotandLC.get(goalSlotPlusLC);

			//normalize
			if (totalProbability==0.) totalProbability=1.;
			//if (!(conditionalProjectProbs==null)) {
				for (String projectedNodeLabel : conditionalProjectProbs.keySet()) {
					//nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
					conditionalProjectProbs.put(projectedNodeLabel, conditionalProjectProbs.get(projectedNodeLabel)/totalProbability);
					//if (Main.PRINT_PROBABILITY_MODEL && conditionalProjectProbs.get(projectedNodeLabel)>0.) System.out.println("P_Project to " + projectedNodeLabel + " from LC= " + leftCornerLabel + " given goalSlot= " + goalNodeLabel + "@" + slotnr + ": " + P_ProjectGivenSlotandLC.get(projectedNodeLabel));
				}
			//}
			//nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
			//if (!(P_buAttachGivenSlotandLC.get(goalSlotPlusLC)==null))
			//	P_buAttachGivenSlotandLC.put(goalSlotPlusLC, P_buAttachGivenSlotandLC.get(goalSlotPlusLC)/totalProbability);
			//if (Main.PRINT_PROBABILITY_MODEL && P_buAttachGivenSlotandLC.get(goalSlotPlusLC)>0.) System.out.println("P_BU_Attach from LC= " + leftCornerLabel + " given goalSlot= " + goalNodeLabel + "@" + slotnr + ": " + P_buAttachGivenSlotandLC.get(goalSlotPlusLC));
		}
		
		//shift probabilities
		//if (experiments.INCLUDE_SHIFT_PRODUCTIONS) {
			for (String dottedNonT : shiftProbabilities.keySet()) {
				HashMap<String, Double> conditionalShiftProbs = shiftProbabilities.get(dottedNonT);
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
		//}
		/*
		///////////////////////////////////////////////////////////////////
		// check whether there are any missing (null) combinations, set their probabilities to zero
		// and normalize
		///////////////////////////////////////////////////////////////////
		HashSet<String> leftCorners = new HashSet<String>();
		
		leftCorners.addAll(inputLayerNodes.keySet());
		leftCorners.addAll(compressorLayerNodes.keySet());
		leftCorners.remove("START");
		
		//enumerate all possible combinations of goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" (+ projectedNodeLabel;)
		//you need to create entries for all of them, and fill those that don't have an entry in P_ProjectGivenSlotandLC with zero's. 
		for (String goalNodeLabel : compressorLayerNodes.keySet()) {
			//get representations of all non-left slots
			for (int slotnr =1; slotnr <= compressorLayerNodes.get(goalNodeLabel).getSlots().size(); slotnr++) {
				if (slotnr >1)	{ //skip left slot
					
					for (String leftCornerLabel : leftCorners) {
						
						//goalSlotPlusLC = goalNodeLabel + "@" + (childIndex +1) + "@" + leftCornerLabel;
						String goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						HashMap<String, Double> conditionalProjectProbs = P_ProjectGivenSlotandLC.get(goalSlotPlusLC);
						double totalProbability = 0.;
						
						//compute totals, sum over all projectedNodeLabel for a certain goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						for (String projectedNodeLabel : compressorLayerNodes.keySet()) {			
							//String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
							if (conditionalProjectProbs.get(projectedNodeLabel)==null) conditionalProjectProbs.put(projectedNodeLabel, 0.);
							else totalProbability += conditionalProjectProbs.get(projectedNodeLabel);	
						}
						//String nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						if (P_buAttachGivenSlotandLC.get(goalSlotPlusLC)==null) P_buAttachGivenSlotandLC.put(goalSlotPlusLC, 0.);
						else totalProbability += P_buAttachGivenSlotandLC.get(goalSlotPlusLC);

						//normalize
						if (totalProbability==0.) totalProbability=1.;
						for (String projectedNodeLabel : compressorLayerNodes.keySet()) {
							//nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel + "@" + projectedNodeLabel;
							conditionalProjectProbs.put(projectedNodeLabel, conditionalProjectProbs.get(projectedNodeLabel)/totalProbability);
							//if (Main.PRINT_PROBABILITY_MODEL && conditionalProjectProbs.get(projectedNodeLabel)>0.) System.out.println("P_Project to " + projectedNodeLabel + " from LC= " + leftCornerLabel + " given goalSlot= " + goalNodeLabel + "@" + slotnr + ": " + P_ProjectGivenSlotandLC.get(projectedNodeLabel));
						}
						//nodeSlotCombi = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
						P_buAttachGivenSlotandLC.put(goalSlotPlusLC, P_buAttachGivenSlotandLC.get(goalSlotPlusLC)/totalProbability);
						//if (Main.PRINT_PROBABILITY_MODEL && P_buAttachGivenSlotandLC.get(goalSlotPlusLC)>0.) System.out.println("P_BU_Attach from LC= " + leftCornerLabel + " given goalSlot= " + goalNodeLabel + "@" + slotnr + ": " + P_buAttachGivenSlotandLC.get(goalSlotPlusLC));
					}	//for (String leftCornerLabel : nodeLabels)
				}	//if (slotnr >1)
			}	//loop over slotNrs
		}	//for (String goalNodeLabel : compressorLayerNodes.keySet())
		*/
	}
	
	public static void printLeftCornerProbabilities(grammar myGrammar, boolean printCounts) throws IOException,	Exception {
		
		String latexFile= "./output/left_corner_probabilities.tex";
		if (printCounts) latexFile= "./output/left_corner_counts.tex";
		BufferedWriter leftCornerProbabilitiesFile = new BufferedWriter(new FileWriter(latexFile));
		Utils.openLatexDoc2(leftCornerProbabilitiesFile, true);
		
		ArrayList<String> probabilityTable  = writeLeftCornerProbabilityTableForLatex(myGrammar, printCounts);
		
		int nrCNodes = myGrammar.getNonTerminalUnits().size();
		
		StringBuffer ls = new StringBuffer();
        for (int i =0; i<nrCNodes+4; i++) ls.append("l");
       	
		leftCornerProbabilitiesFile.write("\\begin{table}[htbp] \\centering \\resizebox{1.3\\linewidth}{!}{ \\begin{tabular}[t]{" + ls.toString() + "}"); leftCornerProbabilitiesFile.newLine();
		
		leftCornerProbabilitiesFile.write("\\multicolumn{" + (nrCNodes+4) + "}{c}{Project and attach probabilities (conditioned on left corner and goal slot)} \\\\ \\hline"); 
		leftCornerProbabilitiesFile.newLine();
        
			// content: look up the probabilities in P_ProjectGivenSlotandLC and P_buAttachGivenSlotandLC
		for (String tableLine : probabilityTable) {
			leftCornerProbabilitiesFile.write(tableLine); leftCornerProbabilitiesFile.newLine();
		}
		Utils.printCloseLatexTable2(leftCornerProbabilitiesFile, "Left corner probabilities computed directly from labels in treebank", "tab:attach_project_probabilities");
		
		
		if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) {
			ArrayList<String> shiftProbabilityTable  = writeShiftProbabilityTableForLatex(myGrammar, printCounts);
		
			int nrTerminals = myGrammar.getTerminalUnits().size();
			ls = new StringBuffer();
			ls.append("ll|");
	        for (int i =0; i<nrTerminals; i++) ls.append("l");
	       	
			leftCornerProbabilitiesFile.write("\\begin{table}[htbp] \\centering \\resizebox{1.3\\linewidth}{!}{ \\begin{tabular}[t]{" + ls.toString() + "}"); leftCornerProbabilitiesFile.newLine();
			leftCornerProbabilitiesFile.write("\\multicolumn{" + (nrTerminals+2) + "}{c}{Shift probabilities (conditioned on starred nonT and its right sister)} \\\\ \\hline"); 
			leftCornerProbabilitiesFile.newLine();
	        
			for (String tableLine : shiftProbabilityTable) {
				leftCornerProbabilitiesFile.write(tableLine); leftCornerProbabilitiesFile.newLine();
			}
			Utils.printCloseLatexTable2(leftCornerProbabilitiesFile, "Left corner probabilities estimated from treebank", "tab:shift_probabilities");
			
		}
		
		leftCornerProbabilitiesFile.write("\\end{document}"); leftCornerProbabilitiesFile.newLine();
		
		leftCornerProbabilitiesFile.flush();
		leftCornerProbabilitiesFile.close();
	}



public static ArrayList<String> writeLeftCornerProbabilityTableForLatex(grammar myGrammar, boolean printCounts) {
	
	NumberFormat numberFormatter = new DecimalFormat("#.######");
	
	HashMap<String, HashMap<String, Double>> P_ProjectOrAttachGivenSlotandLC = null;
	P_ProjectOrAttachGivenSlotandLC = myGrammar.getProjectAndAttachProbabilities();
	
	
	ArrayList<String> probabilityTable = new ArrayList<String>();
	//treeFile.write("\\left corner & goal slot & P(attach) & P(project to X1) & P(project to X2) \\\\ \\hline"); treeFile.newLine();
	
	//HashSet<String> goalSlotsPlusLCs = new HashSet<String>();	//combines conditionals of project and attach
	//goalSlotsPlusLCs.addAll(P_ProjectGivenSlotandLC.keySet());
	//goalSlotsPlusLCs.addAll(P_buAttachGivenSlotandLC.keySet());
	
	StringBuffer columnHeaderLine = new StringBuffer();
	columnHeaderLine.append("l. corner & goal categ. & P(attach) ");			
	//for (String projectedNodeLabel : comparativeLikelihoods.nonterminalProductions) {	
	for (String projectedNodeLabel : myGrammar.getNonTerminalUnits().keySet()) {
		//if (!(projectedNodeLabel.equals("START")))
		columnHeaderLine.append("& Pr(" + projectedNodeLabel + ")");
	}
	columnHeaderLine.append(" & sum \\\\");

	probabilityTable.add(columnHeaderLine.toString().replace("_", "-"));

	//for sorting the table
	TreeMap<String, TreeSet<String>> sortedTableLines = new TreeMap<String, TreeSet<String>>();
	
	for (String goalSlotPlusLC : P_ProjectOrAttachGivenSlotandLC.keySet()) {

		String goalNodeLabel = goalSlotPlusLC.split("@")[0];
		//String slotnr = goalSlotPlusLC.split("@")[1];
		String leftCornerLabel = goalSlotPlusLC.split("@")[1];
		
		StringBuffer tableLine = new StringBuffer();
		boolean entryHasNonZeroProbabilities = false;
		
		double sumProbs = 0.;
		
		String prefix = "";
		if (leftCornerLabel.equals("%")) prefix = "\\";
		tableLine.append(prefix + leftCornerLabel + " & " +  goalNodeLabel);	// + "-sl" + slotnr
		
		//P_attach
		if (!(P_ProjectOrAttachGivenSlotandLC.get(goalSlotPlusLC).get("attachment")==null)) {
			tableLine.append(" & " + numberFormatter.format(P_ProjectOrAttachGivenSlotandLC.get(goalSlotPlusLC).get("attachment")));
			sumProbs += P_ProjectOrAttachGivenSlotandLC.get(goalSlotPlusLC).get("attachment");
			if (P_ProjectOrAttachGivenSlotandLC.get(goalSlotPlusLC).get("attachment")>0.) entryHasNonZeroProbabilities = true;
		}	
		else tableLine.append(" & " );
		
		//P_project
		HashMap<String, Double> conditionalProjectProbs = P_ProjectOrAttachGivenSlotandLC.get(goalSlotPlusLC);
		
		if (!(conditionalProjectProbs==null)) {
			
			for (String projectedNodeLabel : myGrammar.getNonTerminalUnits().keySet()) {
			//for (String projectedNodeLabel : conditionalProjectProbs.keySet()) {	
				//if (!(projectedNodeLabel.equals("START"))) {
					if (!(conditionalProjectProbs.get(projectedNodeLabel)==null)) {
						tableLine.append(" & " + numberFormatter.format(conditionalProjectProbs.get(projectedNodeLabel)));
						sumProbs += conditionalProjectProbs.get(projectedNodeLabel);
						if (conditionalProjectProbs.get(projectedNodeLabel)>0.) entryHasNonZeroProbabilities = true;
					}
					else tableLine.append(" & " );
			}
		}
		tableLine.append(" & " + numberFormatter.format(sumProbs) );
		tableLine.append(" \\\\ ");
		
		//if (entryHasNonZeroProbabilities) {
			//sort the rows in the table according to leftCornerLabel: add to sorted HashMap<String, TreeSet<String>> sortedTableLines
			TreeSet<String> linesWithSameLeftCorner = null;
			if (sortedTableLines.get(leftCornerLabel)==null) {
				linesWithSameLeftCorner = new TreeSet<String>();
				sortedTableLines.put(leftCornerLabel , linesWithSameLeftCorner);
			}
			else linesWithSameLeftCorner = sortedTableLines.get(leftCornerLabel);
			linesWithSameLeftCorner.add(tableLine.toString());
		//}		
	}	//for (String goalSlotPlusLC : P_ProjectOrAttachGivenSlotandLC.keySet())
	
	for (String lc : sortedTableLines.keySet()) {
		for (String tableLine : sortedTableLines.get(lc)) {
			probabilityTable.add(tableLine.toString().replace("_", "-"));
		}
	}
	
	
	return probabilityTable;
}

public static ArrayList<String> writeShiftProbabilityTableForLatex(grammar myGrammar, boolean printCounts) {
	
	NumberFormat numberFormatter = new DecimalFormat("#.######");
	
	HashMap<String, HashMap<String, Double>> shiftProbabilities = null;
	shiftProbabilities = myGrammar.getShiftProbabilities();
	
	ArrayList<String> probabilityTable = new ArrayList<String>();
	//treeFile.write("\\left corner & goal slot & P(attach) & P(project to X1) & P(project to X2) \\\\ \\hline"); treeFile.newLine();
	
	//HashSet<String> goalSlotsPlusLCs = new HashSet<String>();	//combines conditionals of project and attach
	//goalSlotsPlusLCs.addAll(P_ProjectGivenSlotandLC.keySet());
	//goalSlotsPlusLCs.addAll(P_buAttachGivenSlotandLC.keySet());
	
	StringBuffer columnHeaderLine = new StringBuffer();
	columnHeaderLine.append("starCat. & goalCat.");			
	for (String word : myGrammar.getTerminalUnits().keySet()) {	
		columnHeaderLine.append("& Sh(" + word + ")");
	}
	columnHeaderLine.append(" \\\\");

	probabilityTable.add(columnHeaderLine.toString());

	//order shiftProbabilities in treemap for sorting the table
	TreeMap<String, HashMap<String, Double>> shiftProbabilitiesOrdered = new TreeMap<String, HashMap<String, Double>>();
	for (String goalPlusNonT : shiftProbabilities.keySet()) {
		//turn around starred nonterminal and goal category
		String starredNonTerminal = goalPlusNonT.split("@")[1];
		//replace NP^1/2/3 by NP*/*/*
		if (starredNonTerminal.contains("^")) {
			int nrStars = java.lang.Integer.parseInt(starredNonTerminal.split("\\^")[1]);
			starredNonTerminal = starredNonTerminal.split("\\^")[0];
			for (int i=0; i<nrStars; i++) starredNonTerminal +="*";
		}
		String starredNonTPlusGoalCat = starredNonTerminal + " & " + goalPlusNonT.split("@")[0];
		shiftProbabilitiesOrdered.put(starredNonTPlusGoalCat, shiftProbabilities.get(goalPlusNonT));
	}
	
	for (String starredNonTPlusGoalCat : shiftProbabilitiesOrdered.keySet()) {
		//System.out.println("goalCatPlusDottedNonT=" + starredNonTPlusGoalCat);
		HashMap<String, Double> shiftProbFromOneNonT = shiftProbabilitiesOrdered.get(starredNonTPlusGoalCat);

		StringBuffer tableLine = new StringBuffer();
		//boolean entryHasNonZeroProbabilities = false;
		
		tableLine.append(starredNonTPlusGoalCat);	//.split("@")[1] + " & " + goalCatPlusDottedNonT.split("@")[0]
		
		for (String word : myGrammar.getTerminalUnits().keySet()) {
			
			if (!(shiftProbFromOneNonT.get(word)==null))
				tableLine.append(" & " + shiftProbFromOneNonT.get(word));
			else tableLine.append(" & " );
		}
		tableLine.append(" \\\\ ");
		probabilityTable.add(tableLine.toString().replace("_", "-"));
		
	}

	return probabilityTable;
}
/*
public ArrayList<String> writeLeftCornerProbabilityTableForLatexOld(HashMap<String, HashMap<String, Double>> P_ProjectGivenSlotandLC, HashMap<String, Double> P_buAttachGivenSlotandLC,
		NumberFormat numberFormatter, HashSet<String> nodeLabels) {
	
	
	ArrayList<String> probabilityTable = new ArrayList<String>();
	//treeFile.write("\\left corner & goal slot & P(attach) & P(project to X1) & P(project to X2) \\\\ \\hline"); treeFile.newLine();
	
	for (String leftCornerLabel : nodeLabels) {

		//System.out.println("leftCornerLabel=" + leftCornerLabel);
		
		for (String goalNodeLabel : episodicNonTerminalProductions.keySet()) {
			int slotnr=0;
			for (ArrayList<Double> goalNodeSlot : episodicNonTerminalProductions.get(goalNodeLabel).getSlots()) {
				slotnr++;
				if (slotnr >1)	{ //skip left slot
					//String slotLabel = goalNodeLabel + slotnr;
					//construct the key for HM projections and HM attachments
					String goalSlotPlusLC = goalNodeLabel + "@" + slotnr + "@" + leftCornerLabel;
					HashMap<String, Double> conditionalProjectProbs = P_ProjectGivenSlotandLC.get(goalSlotPlusLC);
					
					StringBuffer tableLine = new StringBuffer();
					boolean entryHasNonZeroProbabilities = false;
					
					String prefix = "";
					if (leftCornerLabel.equals("%")) prefix = "\\";
					tableLine.append(prefix + leftCornerLabel + " & " +  goalNodeLabel + "-sl" + slotnr);
					//P_attach
					tableLine.append(" & " + numberFormatter.format(P_buAttachGivenSlotandLC.get(goalSlotPlusLC)));
							if (P_buAttachGivenSlotandLC.get(goalSlotPlusLC)>0.) entryHasNonZeroProbabilities = true;
							
					//P_project
					for (String projectedNodeLabel : episodicNonTerminalProductions.keySet()) {	
						if (!(projectedNodeLabel.equals("START"))) {
						tableLine.append(" & " + numberFormatter.format(conditionalProjectProbs.get(projectedNodeLabel)));
						if (conditionalProjectProbs.get(projectedNodeLabel)>0.) entryHasNonZeroProbabilities = true;
						}
					}
					tableLine.append(" \\\\ ");
					if (entryHasNonZeroProbabilities)
						probabilityTable.add(tableLine.toString());
				}
			}
		}
	}
	
	return probabilityTable;
}
*/

	public HashMap<String, HashMap<String, Double>> getProjectAndAttachProbabilities() {
		return this.projectionsAndAttachments;
	}
	
	//public HashMap<String, HashMap<String, Integer>>  getProjectAndAttachCounts() {
	//	return this.projectAndAttachCountsGivenSlotandLC;
	//}
	
	public HashMap<String, HashMap<String, Double>> getShiftProbabilities() {
		return this.shiftProbabilities;
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
