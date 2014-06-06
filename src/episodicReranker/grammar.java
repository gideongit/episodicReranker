package episodicReranker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import parser.Node;
import parser.parseTree;


public abstract class grammar {

	/**
	 * Identical to episodicInputNodes, and redundant.
	 * Only for compliance with grammar (chart parser)
	 */ 
	protected HashMap<String, synUnit> terminalUnits = new HashMap<String, synUnit>();

	/**
	 * Identical to episodicNonTerminalProductions, and redundant.
	 * Only for compliance with grammar (chart parser)
	 */ 
	protected HashMap<String, synUnit> nonTerminalProductions = new HashMap<String, synUnit>();

	/**
	 * nonTerminals includes preterminals and nonPreTerminals
	 */
	protected HashSet<String> nonTerminals = new HashSet<String>();

	protected HashSet<String> preTerminals = new HashSet<String>();

	/**
	 * nonTerminals that are not preTerminals
	 */
	protected HashSet<String> nonPreTerminals = new HashSet<String>();

	protected HashSet<String> starredNonTerminals = new HashSet<String>();

	
	/**
	 * Total number of "context free" productions conditioned on a single label (either left corner, or root)
	 * This is the last back-off level (Lambda2), where rule probabilities are uniformly distributed given label,
	 * So their probability is P(expansion|root)= 1/count(#expansions|root)
	 */
	protected HashMap<String, Integer> uniformSmoothLevelCounts = new HashMap<String, Integer>();
	
	protected HashMap<String, Integer> uniformSmoothLevelAttachCounts = new HashMap<String, Integer>();

	public abstract HashMap<String, HashMap<String, Double>> getProjectAndAttachProbabilities();

	public abstract HashMap<String, HashMap<String, Double>> getShiftProbabilities();

	public abstract HashMap<String, synUnit> getNonTerminalUnits();

	public abstract HashMap<String, synUnit> getTerminalUnits();
	
	public abstract HashSet<String> getTerminals();

	/**
	 * extracts the terminals and nonterminals from the preprocessed parseTree
	 * and stores terminals in lexicon, and node.getProductionName in nonterminalProductions
	 * nonterminalProductions have the format parent*chi1*chi2 etc.
	 * @param myParseTree
	 * @param lexicon HashMap of synUnits, augmented with terminals from the parseTree 
	 * @param nonterminalProductions HashMap of synUnits, augmented with nonterminals from the parseTree 
	 */
	public void addToLexicons(parseTree myParseTree) {

		//, HashMap<String, synUnit> lexicon, HashMap<String, synUnit> nonterminalProductions

		for (Node myNode : myParseTree.getNodes()) {

			if (myNode.getType()==parser.parameters.TERMINAL) {

				terminalUnits.put(myNode.getProductionName(), null);

				
				//add shift production from starred nonT to word, format: X.i * word
				if (Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) {
					
					if (myNode.getLeftSpan()>0) {
						String shiftProduction = lcsGrammar.findShiftProductionAssociatedWithTerminal(myParseTree, myNode);
					//System.out.println("shiftProduction=" + shiftProduction);

						//returns starredNonTerminal + "~" + goalNonTerminal + "~" + terminalNode.getProductionName();
						//whereas shiftRule is of the form word*starredNonTerminal (parent first to be consistent with preterminal rules etc.)
						nonTerminalProductions.put(shiftProduction.split("~")[2] + "*" + shiftProduction.split("~")[0], null);
						//shiftProductionsEncounteredInTrainSet.add(shiftProduction);
					}
					if (myNode.getLeftSpan()==0) {
						nonTerminalProductions.put(myNode.getProductionName() + "*START^1", null);
						//shiftProductionsEncounteredInTrainSet.add("START^1~TOP~" + myNode.getProductionName());
					}
	            }
				
			}

			if (myNode.getType()==parser.parameters.NONTERMINAL) {


				nonTerminalProductions.put(myNode.getProductionName(), null);	//does not include starredNonT children in productionName

				nonTerminals.add(myNode.getName());

				if ((Main.PROBABILITY_MODEL == Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL == Main.Manning_CarpenterLCP) && myNode.getChildNodes().size()>1) {
					for (int i=1; i< myNode.getChildNodes().size(); i++) {

						//if it is not the last child put starred nonT in between the children
						//add number of dots equal to i
						starredNonTerminals.add(myNode.getName() + "^" + i);
					}
				}

				if (myNode.getChildNodes().size()>0 && (myNode.getChildNodes().get(0).getType()==parser.parameters.TERMINAL))
					preTerminals.add(myNode.getName());
				else nonPreTerminals.add(myNode.getName());

			}	//if (!(myNode.getName().contains(">")))

		}
	}


	/**
	 * For add-one smoothing: you want to add a single (virtual) instance of every 
	 * possible rewrite rule that is permitted by the (non)terminals of the grammar.
	 * Create all possible binary nonT productions and unary preterminal productions
	 * (and shift productions) by generating all binary combinations of nonterminals, 
	 * preterminals and unary combis of preterminals/starredNonT and words
	 * Which productions are created and how they are indexed depends on whether
	 * it is top-down or left corner grammar.  
	 * 
	 */
	public HashMap<String, Integer> createVirtualProductions() {
	
		/**
		 * Set of all productions permitted by the (non)terminals of the grammar, 
		 * generated for the purpose of add-one smoothing.
		 * HM<conditioning label: either lhs or left corner, HS of right hand sides> 
		 */
		//HashMap<String, HashSet<String>> virtualProductions = new HashMap<String, HashSet<String>>();

		//horizontal Markovization:
		//VP --> VBZ NP PP wordt: (splits steeds eentje ad rechterkant eraf)
		//VP -> <VP...PP> : dit betekent "een VP-regel (lhs) met rechts PP"
		//<VP...PP> -> <VP...NP> PP
		//<VP...NP> -> <VP...VBZ> NP
		//<VP...VBZ> -> VBZ

		//1) vind alle nonterminals, en onderscheid preterminals en en internal nonT
		//maak alle combinaties <X...Y> --> <X...Z> Y!!! , en X --> <X...Y> (bovenste regel)
		//en bovendien <X...Y> --> Y (onderste regel, is in feite andere Y maar dat maakt niks uit)
		//waarbij X geen preterminal mag zijn, maar Y en Z wel.

		 //test
		//for (String Y : nonTerminals) {
		//	if (!(Y.contains(">"))) {
		//		System.out.println("nonterminal: " + Y);
		//}}
		
		
		//consider only nonTerminals, preTerminals, etc. without >, and ne TOP
		ArrayList<String> nonTerminalsFiltered = new ArrayList<String>();
		ArrayList<String> nonPreTerminalsFiltered = new ArrayList<String>();
		ArrayList<String> preTerminalsFiltered = new ArrayList<String>();
		
		for (String X : nonTerminals) {
			if (!(X.contains(">")) ) nonTerminalsFiltered.add(X);	//&& !(X.equals("TOP"))	
		}
		for (String X : nonPreTerminals) {
			if (!(X.contains(">"))) nonPreTerminalsFiltered.add(X);		// && !(X.equals("TOP"))
		}
		for (String X : preTerminals) {
			if (!(X.contains(">")) ) preTerminalsFiltered.add(X);		//&& !(X.equals("TOP"))
			//System.out.println("preterminal:" + X);
		}
		
		//Internal nonterminals from Markovization are indicated as X>Y; nonterminals in production are separated by * 
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL || Main.PROBABILITY_MODEL==Main.PCFG) {

			for (String X : nonPreTerminalsFiltered) {
					HashSet<String> productionsConditionedOnX = new HashSet<String>();

					for (String Y : nonTerminalsFiltered) {
						/*
							HashSet<String> productionsConditionedOnXY = new HashSet<String>();

							//X --> <X...Y> (for top rule of Markovization)
							String production = X + "*" + X + ">" + Y;

							productionsConditionedOnX.add(production);


							//<X...Y> --> Y (for bottom rule of Markovization)
							production = X + ">" + Y + "*" + Y;
							productionsConditionedOnXY.add(production);

							
							for (String Z : nonTerminalsFiltered) {

									//<X...Y> --> <X...Z> Y!!! (middle rule expansions)
									production = X + ">" + Y + "*" + X + ">" + Z + "*" + Y;
									productionsConditionedOnXY.add(production);
								}
								*/
							//virtualProductions.put(X + ">" + Y, productionsConditionedOnXY);
							uniformSmoothLevelCounts.put(X + ">" + Y, nonTerminalsFiltered.size());
					}
					//virtualProductions.put(X, productionsConditionedOnX);
					uniformSmoothLevelCounts.put(X, nonTerminalsFiltered.size()*nonTerminalsFiltered.size());
			}	//for (String X : nonPreTerminals)

			//create all possible preterminal rules by combining preterminals and terminals
			//voor LCP moet je conditioneren op word ipv op preterminal
			for (String X : preTerminalsFiltered) {	//top-down model
				/*
					HashSet<String> preterminalRulesConditionedOnX = new HashSet<String>();

					for (String Y : terminalUnits.keySet()) {
						preterminalRulesConditionedOnX.add(X + "*" + Y);
					}
					if (!(virtualProductions.get(X)==null)) virtualProductions.get(X).addAll(preterminalRulesConditionedOnX);
					else virtualProductions.put(X, preterminalRulesConditionedOnX);
				*/	
					if (!(uniformSmoothLevelCounts.get(X)==null)) uniformSmoothLevelCounts.put(X, uniformSmoothLevelCounts.get(X) + terminalUnits.size());
					else uniformSmoothLevelCounts.put(X, terminalUnits.size());
			}

			//create all possible unary rules by combining nonPreterminals and nonTerminals
			//voor LCP opnieuw andersom!
			for (String X : nonPreTerminalsFiltered) {
				/*
					HashSet<String> unaryRulesConditionedOnX = new HashSet<String>();

					for (String Y : nonTerminalsFiltered) {
						unaryRulesConditionedOnX.add(X + "*" + Y);
					}
					if (!(virtualProductions.get(X)==null)) virtualProductions.get(X).addAll(unaryRulesConditionedOnX);
					else virtualProductions.put(X, unaryRulesConditionedOnX);
				*/	
					if (!(uniformSmoothLevelCounts.get(X)==null)) uniformSmoothLevelCounts.put(X, uniformSmoothLevelCounts.get(X) + nonTerminalsFiltered.size());
					else uniformSmoothLevelCounts.put(X, nonTerminalsFiltered.size());
			}
			
			uniformSmoothLevelCounts.put("START", 1);
			
			//if (episodicReranker.experiments.EXTRA_ATTACHMENT_IN_TD_DERIVATION) {
			//	//add virtual productions for terminal attachments; in fact, for all (pre-)terminals attach prob. is 1
			//	for (String X : terminalUnits.keySet()) {
			//		uniformSmoothLevelCounts.put(X, terminalUnits.size());
			//	}
			//}
		}	//		if (experiments.PROBABILITY_MODEL.equals("topdown_with_path") || experiments.PROBABILITY_MODEL.equals("PCFG"))

		//check
		/*
		for (String lhs : virtualProductions.keySet()) {
			for (String production : virtualProductions.get(lhs)) {
				if (lhs.equals("SBAR")) System.out.println("virtualProductionsNU: " + production);
			}
		}
		*/
		
		
		if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL==Main.Manning_CarpenterLCP) {
			
			//VP --> VBZ NP PP wordt: (splits steeds eentje ad rechterkant eraf)
			//VP -> <VP...PP> : dit betekent "een VP-regel (lhs) met rechts PP"
			//<VP...PP> -> <VP...NP> PP
			//<VP...NP> -> <VP...VBZ> NP
			//<VP...VBZ> -> VBZ
			
			//let op: condition on left corner: dan mag X dus juist weer wel preterminal zijn???
			
			//create all productions of the form <X...Y> --> <X...Z> Y
			//for every binary production there is one projection from <X...Z> and one attachment from Y
			
			//attachments
			for (String Y : nonTerminalsFiltered) {
				/*
					HashSet<String> attachmentsConditionedOnY = new HashSet<String>();
					//when computing bindings for test sentence with unknown production
					//"attach" replaces the name of the production in the rightSideBinding
					attachmentsConditionedOnY.add("attach");
				
					virtualProductions.put(Y, attachmentsConditionedOnY);
					*/	
				//for every Y there are X.size()*Z.size() rules to attach to: //count attaches more than once
				uniformSmoothLevelCounts.put(Y, nonPreTerminalsFiltered.size()*nonTerminalsFiltered.size());
				//instead of 1
				uniformSmoothLevelAttachCounts.put(Y, nonPreTerminalsFiltered.size()*nonTerminalsFiltered.size());
			}
			
			//binary projections <X...Y> --> <X...Z> Y : conditioned on <X...Z>
			System.out.println("#nonTerminals=" + nonTerminalsFiltered.size() + "; #nonPreTerminals=" + nonPreTerminalsFiltered.size() + "; #preTerminals=" + preTerminalsFiltered.size() + "; #words=" + terminalUnits.size() + "; loopsize=" + nonTerminalsFiltered.size() * nonTerminalsFiltered.size() * nonPreTerminalsFiltered.size());
			for (String X : nonPreTerminalsFiltered) {

					for (String Z : nonTerminalsFiltered) {
						/*
							HashSet<String> projectionsConditionedOnXZ = new HashSet<String>();
							
							for (String Y : nonTerminalsFiltered) {
							
									//<X...Y> --> <X...Z> Y (middle rule expansions)
									//becomes PROJECTION from leftcorner <X...Z>, and ATTACHMENT from Y
									String production = X + ">" + Y + "*" + X + ">" + Z + "*" + Y;
									
									projectionsConditionedOnXZ.add(production);
							}	//for (String Y : nonTerminals)
							
							virtualProductions.put(X + ">" + Z, projectionsConditionedOnXZ);
							*/
							uniformSmoothLevelCounts.put(X + ">" + Z, nonTerminalsFiltered.size());
							
					}
			}
			
			//X --> <X...Y> (for top rule of Markovization)
			//becomes PROJECTION from <X...Y> to X
			for (String X : nonPreTerminalsFiltered) {

					for (String Y : nonTerminalsFiltered) {
							/*
							String production = X + "*" + X + ">" + Y;
								
							if (!(virtualProductions.get(X + ">" + Y)==null)) virtualProductions.get(X + ">" + Y).add(production);
							else {
								HashSet<String> projectionsConditionedOnXY = new HashSet<String>();
								projectionsConditionedOnXY.add(production);
								virtualProductions.put(X + ">" + Y, projectionsConditionedOnXY);
							}				
							*/
							if (!(uniformSmoothLevelCounts.get(X + ">" + Y)==null)) 
								uniformSmoothLevelCounts.put(X + ">" + Y, uniformSmoothLevelCounts.get(X + ">" + Y) + 1);
							else uniformSmoothLevelCounts.put(X + ">" + Y, 1);
					}	//for (String Y : nonTerminals)
			}	//for (String X : nonPreTerminals)
			
			
			//<X...Y> --> Y (for bottom rule of Markovization)
			//becomes PROJECTION from Y to <X...Y>
			for (String Y : nonTerminalsFiltered) {
				/*
					HashSet<String> projectionsConditionedOnY = new HashSet<String>();
					
					for (String X : nonPreTerminalsFiltered) {
							
							String production = X + ">" + Y + "*" + Y;							
							projectionsConditionedOnY.add(production);
							
					}	//for (String X : nonPreTerminals)
					
					
					if (!(virtualProductions.get(Y)==null)) virtualProductions.get(Y).addAll(projectionsConditionedOnY);
					else virtualProductions.put(Y, projectionsConditionedOnY);
				*/	
					if (!(uniformSmoothLevelCounts.get(Y)==null)) 
						uniformSmoothLevelCounts.put(Y, uniformSmoothLevelCounts.get(Y) + nonPreTerminalsFiltered.size());
					else uniformSmoothLevelCounts.put(Y, nonPreTerminalsFiltered.size());
					
			}	//for (String Y : nonTerminals)


			//create all possible preterminal PROJECTIONS from terminals to preterminals (preterminal rules)
			for (String myWord : terminalUnits.keySet()) {
				/*
				HashSet<String> preterminalRulesConditionedOnWord = new HashSet<String>();

				for (String myPreterminal : preTerminalsFiltered) {
					preterminalRulesConditionedOnWord.add(myWord + "*" + myPreterminal);
				}
				//if (!(virtualProductions.get(X)==null)) virtualProductions.get(X).addAll(preterminalRulesConditionedOnX);
				virtualProductions.put(myWord, preterminalRulesConditionedOnWord);
				*/
				//if (myWord.equals("1capY_dashY_slshN_alfY_digN_sfx:NONE_afx:NONE")) System.out.println("rare ding in smooth");
				uniformSmoothLevelCounts.put(myWord, preTerminalsFiltered.size());
			}
			
			//create all possible unary PROJECTIONS from nonTerminals to nonPreterminals (unary rules)
			for (String X : nonTerminalsFiltered) {

				/*
					HashSet<String> unaryRulesConditionedOnX = new HashSet<String>();

					for (String Y : nonPreTerminalsFiltered) {
							unaryRulesConditionedOnX.add(Y + "*" + X);
					}
					
					if (!(virtualProductions.get(X)==null)) virtualProductions.get(X).addAll(unaryRulesConditionedOnX);
					else virtualProductions.put(X, unaryRulesConditionedOnX);
				*/	
					if (!(uniformSmoothLevelCounts.get(X)==null)) 
						uniformSmoothLevelCounts.put(X, uniformSmoothLevelCounts.get(X) + nonPreTerminalsFiltered.size());
					else uniformSmoothLevelCounts.put(X, nonPreTerminalsFiltered.size());				
			}
			
			//create all possible shift productions from a starred nonTerminal to a word
			//and from START to a word
			
			//bindingsOfLCDerivation.add("START^1~" + firstWord);
			//String starredNonterminalProduction = X^1 + rhs;
			//bindingsOfLCDerivation.add(starredNonterminalProduction + "~" + word);

			//maar voor back-off wil je alleen op starredNonterminal conditioneren (conditioning label)
			//binary rules zijn altijd van de vorm: <X...Y> --> <X...Z> Y
			//dus betekent dat dat je helemaal geen starred nonT hebt X* maar alleen <X...Y>*???klopt
			//let op: starredNonTerminals uit het lexicon bevat niet alle mogelijke combi's <X...Y>*
			int loopCounter=0;
			//System.out.println("loop6 size=" + nonPreTerminalsFiltered.size()*nonTerminalsFiltered.size()*terminalUnits.size());
			
			for (String X : nonPreTerminalsFiltered) {
				//System.out.println("outer loop6 " + loopCounter);
				loopCounter++;
					for (String Y : nonTerminalsFiltered) {
							/*
							HashSet<String> shiftRulesConditionedOnXY = new HashSet<String>();
						
							for (String myWord : terminalUnits.keySet()) {		
								shiftRulesConditionedOnXY.add(myWord);
							}
							*/
							String myStarredNonT = X + ">" + Y + "^1";
							//if (X.equals("NP")) System.out.println("unonT=" + Y);
							//virtualProductions.put(myStarredNonT, shiftRulesConditionedOnXY);
							
							uniformSmoothLevelCounts.put(X + ">" + Y + "^1", terminalUnits.size());
							
					}	//for (String Y : nonTerminals) {
			}	//for (String X : nonPreTerminals)
			
			//from START*:
			/*
			HashSet<String> shiftRulesCondintionedOnSTART = new HashSet<String>();
			
			for (String myWord : terminalUnits.keySet()) {		
				shiftRulesCondintionedOnSTART.add(myWord);
			}
			virtualProductions.put("START^1", shiftRulesCondintionedOnSTART);
			*/
			uniformSmoothLevelCounts.put("START^1", terminalUnits.size());
			uniformSmoothLevelCounts.put("TOP@START^1", terminalUnits.size());
			
		}	//if (experiments.PROBABILITY_MODEL.equals("leftcorner_with_path") || experiments.PROBABILITY_MODEL.equals("Manning_CarpenterLCP"))
		

		//om PCFG base probabilities uit te rekenen kun je deze counts vervolgens optellen bij
		//de echte frequency counts vd treebank, mbv rulesOfPCFG in PCFG class. 
		//uiteindelijk heb je echter geen probabilities nodig maar unigrams voor totaal x dat label als conditioning 
		//voorkomt + bigrams: #keer dat je productie krijgt geconditioneerd op label.

		
		return this.uniformSmoothLevelCounts;
	}

	public HashMap<String, Integer> getUniformSmoothLevelCounts() {
		return this.uniformSmoothLevelCounts;
	}
	
	public HashMap<String, Integer> getUniformSmoothLevelAttachCounts() {
		return this.uniformSmoothLevelAttachCounts;
	}
	
}
