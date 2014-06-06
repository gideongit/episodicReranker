package episodicReranker;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import parser.Node;
import parser.parameters;
import parser.parseTree;




public class sentencePreprocessor {

	protected ArrayList<parseTree> parseTreesFromTreebank;

	
	/**
	 * Classes of infrequent words in train treebank, containing at least 
	 * MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS unique word types.
	 * Used to classify unknown words in test sentences.
	 */
	protected static HashSet<String> unknownWordClassesInTrainSet = new HashSet<String>(); 

	protected static HashMap<String, HashSet<Integer>> duplicateSentenceNrsInTrainSet = new HashMap<String, HashSet<Integer>>(); 
	
	public static HashSet<String> lowerCaseConversions = new HashSet<String>();
	
	final static String containsDigitMatch = ".*\\d.*";
	final static String containsAlphaMatch = ".*[a-zA-Z].*";

	boolean useFirstWord, useFirstCap, useAllCap;
	boolean useDash, useForwardSlash, useDigit, useAlpha, useDollar;
	String[] affixes, suffixes;
	boolean allowToCombineSuffixAffix;
	boolean useASFixWithDash, useASFixWithSlash, useASFixWithCapital;

	/**
	 * Constructs sentencePreprocessor
	 * 
	 * @param sentenceFile fileName of the treebank
	 * @param blnCaseConversion if true, invokes convertOpenClassWordsInInitialSentencePosition2LowerCase method
	 * @param blnTrainset if true, ignores lower and upper limits of treebank sentences, 
	 * imposed by experiments.FIRSTTESTSENTENCE experiments.LASTTESTSENTENCE
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public sentencePreprocessor(String sentenceFile, boolean blnTrainset) throws FileNotFoundException, IOException {

		loadDefaultParameters();
		lowerCaseConversions = new HashSet<String>();
		
		parseTreesFromTreebank = extractParseTreesFromTreebank(sentenceFile, blnTrainset);

	}

	public sentencePreprocessor()throws FileNotFoundException, IOException {

		loadDefaultParameters();
		lowerCaseConversions = new HashSet<String>();
	}
	
	/**
	 * Extracts parseTrees from strings in WSJ format, given in treebankFile
	 * if treebankFile=Tuebingen does preprocessing to convert it to WSJ format
	 * 
	 * @param sentenceFile fileName of the treebank
	 * @param blnTrainset if true, ignores lower and upper limits of treebank sentences, 
	 * imposed by experiments.FIRSTTESTSENTENCE experiments.LASTTESTSENTENCE
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<parseTree> extractParseTreesFromTreebank(String treebankFile, boolean blnTrainset) throws FileNotFoundException, IOException {
		//ArrayList<parseTree> parseTrees = new ArrayList<parseTree>();
		BufferedReader buff = null;

		//boolean onlyUniqueSentences = experiments.COMPUTE_SHORTEST_DERIVATION;
		
		parser.parameters.TREEBANK_FILE = treebankFile;

		//ArrayList<String> labeledSentences = new ArrayList<String> ();

		int myCounter = 0;
		String mySentence;
		parseTree myParseTree = null;

		ArrayList<parseTree> parseTrees = new ArrayList<parseTree>();
		HashSet<String> uniqueSentences = new HashSet<String>();
		buff = new BufferedReader(new FileReader(treebankFile));

		while ((mySentence = buff.readLine()) !=null){

			
			if (blnTrainset || (myCounter>=Main.FIRSTTESTSENTENCE && myCounter<Main.LASTTESTSENTENCE)) {

				
				
				mySentence = mySentence.trim();
				//if (!(onlyUniqueSentences && uniqueSentences.contains(mySentence))) {
					uniqueSentences.add(mySentence);

					//preprocess Tuebing corpus!
					if (treebankFile.toLowerCase().contains("tuebingen")) {
						mySentence = preprocessTuebingenSentence(mySentence);			
					}

					//upperCase2lowerCase conversion
					//if (experiments.CONVERT_INITIAL_WORD2LOWERCASE) mySentence = convertOpenClassWordsInInitialSentencePosition2LowerCase_old(mySentence);

					myParseTree = ExtractParseFromWSJText(mySentence, false);

					//skip all this to save time if you are working from preprocessed file
					myParseTree.removeEmptyNonTerminalNodes();
					myParseTree.removeEmptyNonTerminalNodes();
					myParseTree.removeEmptyNonTerminalNodes();

					myParseTree.calculateNodeDepth();
					myParseTree.calculateNodeSpans();

					if (Main.CONVERT_INITIAL_WORD2LOWERCASE) 
						convertOpenClassWordsInInitialSentencePosition2LowerCase(myParseTree);
					
					parseTrees.add(myParseTree);
					
				//}
				myCounter++; 
				if (myCounter % 500 ==0) System.out.println("############ reading " + myCounter + " parses from " + treebankFile + " ################");

			}	//if (onlyUniqueSentences && uniqueSentences.contains(mySentence))
		}	//for (String simpleSentence : completeSentences)
		return parseTrees;
	}

	/** Extracts the rules from the annotated OVIS file
     *  @param sentence one sentence of OVIS file
     */
    public static parseTree ExtractParseFromWSJText(String sentence, boolean blnPrintChars){
        //System.out.println("in ExtractParseFromWSJText; blnPrintChars=" + blnPrintChars + "; sentence=>>" + sentence + "<<");
        //(a,[(tv,[(nee_,[])]),(vp,[(v,[(dank_,[])]),(per,[(u_,[])])])]).
    
        //(S (NP (POSTAG hallo)))
        //( (S (NP-SBJ (NNP Ms.) (NNP Waleson) ) (VP (VBZ is) (NP (NP (DT a) (JJ free-lance) (NN writer) ) (VP (VBN based) (NP (-NONE- *) ) (PP-LOC-CLR (IN in) (NP (NNP New) (NNP York) ))))) (. .) ))
        //create temporary sentence with node structure
        //TOP node is automatically created
        parseTree myParseTree = new parseTree();
        
        int characterPosition = 0;
        int wordPosition = 0;
        
        //replace the first occurrence of S by TOP
        if ((parameters.DO_LABEL_ASSOCIATION || parameters.DIRECT_EVALUATION_NO_PARSING) && sentence.startsWith("(top (s ")) sentence = "(top " + sentence.substring(8,  sentence.length()-2);
        //System.out.println(sentence);
        //currentNode is the index of the node in the parseTree (ArrayList of nodes)
        //set currentNode to TOP, which is created in constructor of parseTree
        Node currentNode = myParseTree.getNode(myParseTree.getNodes().size()-1);
   
        //turn sentence into array of characters
        char[] mySentence = sentence.toCharArray();
                
        StringBuffer mySymbol;
        
        while ( characterPosition < sentence.length()) {    
            
            //(np,[(capelle,[(capelle_,[])]),(aan,[(aan_,[])]),(den,[(den_,[])]),(ijssel,[(ijssel_,[])])]).
        	//(a lorillard spokewoman (said)) ( (this) (is (an old story)))
        	//(TOP (S (NP-SBJ (dt a) (nnp lorillard) (nn spokewoman)) (VP (vbd said) (S (NP-SBJ (dt this)) (VP (vbz is) (NP-PRD (dt an) (jj old) (nn story)))))))
            // one of three possibilities:
            // if current character is (, then a new Node starts --> move till after ( 
            // if current character is ), then the rule ends --> move till after )
            // otherwise, if next character is space, then stay within the same rule --> move behind space
            
        	//
            // 1) start of new rule:
            if (mySentence[characterPosition] == '(' ) {
                
                if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                characterPosition++;    //pass the (
                
                //get name of symbol, or LHS of rule
                mySymbol = new StringBuffer();
               // (s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])])])]).

                while (mySentence[characterPosition] != ' ') {
                    mySymbol.append(mySentence[characterPosition]);
                    if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                    characterPosition++;
                }
                
                //only if not again TOP
                String mySymbolString = mySymbol.toString();
                
                //System.out.println("mySymbolString=" + mySymbolString);
                
                if (!mySymbolString.toUpperCase().equals("TOP") ) { 
                    //add the symbol to rule of dominant node
                    //NB at instantiation of myParseTree the first current rule with LHS TOP is automatically created 
                    //take off the -SBJ etc
                    //if (Main.TAKE_OFF_SBJ && mySymbolString.contains("-")) mySymbolString = mySymbolString.split("-")[0];
                    //make new node out of mySymbol, which is daughter of currentNode
                    //currentNode is pointer to parent node (xxx)
                    Node myNode = new Node(mySymbolString, currentNode);
                    myParseTree.addNode(myNode);


                    //add this node as a child to parent node
                    currentNode.addChildNode(myNode);

                    //position is now on space, move ahead till you get behind space
                    if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                    characterPosition++; //pass the space

                    //last word is a Terminal unless current character = (
                    if (mySentence[characterPosition] != '(' ) {
                        //it was a terminal node, move ahead till after )
                        myNode.setType(parameters.TERMINAL); 
                        //add the word position info in leftSpan and rightSpan fields
                        wordPosition++;
                        myNode.setLeftSpan(wordPosition-1);
                        myNode.setRightSpan(wordPosition);

                        //find the name of the terminal and replace the label of the node
                        //continue till )

                        mySymbol = new StringBuffer();
                        while (mySentence[characterPosition] != ')') {
                            mySymbol.append(mySentence[characterPosition]);
                            if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                            characterPosition++;
                        }
                        //replace label, unless it is one of (, . : `` '' -LRB- or -RRB-), in which case you delete it!!!
                        if (myNode.getName().equals("-NONE-") || (parameters.REMOVE_PUNCTUATION && (myNode.getName().equals(",") || myNode.getName().equals(".") || myNode.getName().equals(":") || myNode.getName().equals("``") || myNode.getName().equals("''") || myNode.getName().equals("-LRB-") || myNode.getName().equals("-RRB-") || myNode.getName().equals("$") || myNode.getName().equals("#") ))) {

                            //remove reference in parent Node
                            currentNode.getChildNodes().remove(myNode);
                            //delete node (cross fingers that it works)
                            myParseTree.getNodes().remove(myNode);
                            wordPosition--;
                        }
                        else {
                            if (!parameters.EXTRACT_POSTAGS && !parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) {    //if you don't use POSTAGS then replace POSTAGS by lexical items
                                //normally nodeLabel is only postag, assigned in: myNode = new Node(mySymbolString, currentNode);
                            	if (!parameters.CASE_SENSITIVITY) myNode.replaceLHS((myNode.getName() + " " + mySymbol.toString()).toLowerCase());
                            	else myNode.replaceLHS((myNode.getName() + " " + mySymbol.toString()));		
                            }
                            if (parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) {
                            	//the current node should be only postag, and non-terminal
                            	//change type:
                            	myNode.setType(parameters.NONTERMINAL); 
                            	
                            	//create daughter node for terminal (lexical item mySymbol.toString(), parent myNode
                            	Node extraNode = new Node(mySymbol.toString().trim(), myNode);
                                myParseTree.addNode(extraNode);
                                extraNode.setType(parameters.TERMINAL);
                                //add this node as a child to parent node
                                myNode.addChildNode(extraNode);
                                extraNode.setLeftSpan(wordPosition-1);
                                extraNode.setRightSpan(wordPosition);    
                            }
                        }
                        
                        if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                        characterPosition++; //pass the )
                    }    
                    else {
                        //set type to nonTerminal
                        myNode.setType(parameters.NONTERMINAL);     
                         //make this node the currentNode node
                        currentNode = myNode;    
                    }   
                }   //it was not TOP again
                else {
                    if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                    characterPosition++; //it was TOP, pass the space
                }
            }
            
            //end of RHS of rule
            if (mySentence[characterPosition] == ')') {
            //this is the end of all the sister nodes

                //position is now on ), move on till behind the ) 
                if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                characterPosition++; //pass the )
                //update currentNode to parent of current
                currentNode = currentNode.getParentNode();
            }

            //stay with same node
            if (characterPosition< sentence.length()) {
                if(mySentence[characterPosition] == ' ') {
                    if (blnPrintChars) System.out.println(mySentence[characterPosition]);
                    characterPosition++; //pass the space
                } 
            }       
        }
        
        //test
        //System.out.println(mySentence);
        
        if (parameters.TAKE_OFF_SBJ ) {
	         for (Node myNode : myParseTree.getNodes()) {
	        	 
	        	//take off the _ at end of word, but only for non-terminals!
	        	 //System.out.println("myNode.getName()=" + myNode.getName());
	        	 if (myNode.getType()==parameters.NONTERMINAL) {
	        		 if (Main.BLN_TUEBINGEN) {
	        			 if (myNode.getName().contains(":") ) 
	        				 myNode.setName(myNode.getName().split(":")[0]);
	        			 if (myNode.getName().endsWith("$") ) 
	        				 myNode.setName(myNode.getName().substring(0, myNode.getName().length()-1));
	        		 }
		             if (myNode.getName().contains("-") ) {
		            	// System.out.println("myNode.getName()=" + myNode.getName());
		            	if ((myNode.getName().split("-").length>0) && !(myNode.getName().split("-")[0].equals(""))) myNode.setName(myNode.getName().split("-")[0]);
		             }
		             if (myNode.getName().contains("=") ) 
			            myNode.setName(myNode.getName().split("=")[0]);
		             
	        	 }
	           //  ArrayList<String> childnodes = new ArrayList<String>();
	            //for (Node childNode : myNode.getChildNodes()) {
	           //     childnodes.add(childNode.getName());
	            //}
	           
	           // System.out.println(myNode.getName() + " --> " + childnodes);
	        }
        }
        return myParseTree;
    }
    
	/**
	 * Invokes removeSingleWordAndTooLongSentences, removeUnaryNodesFromParseTrees
	 * and horizontal_markovization
	 * Note that long sentences must removed in test corpus, but not in train corpus
	 * therefore in train corpus MAX_SENTENCE_SIZE = 120
	 * @param REMOVE_UNARY_NODES_FROM_PARSETREES
	 * @param DO_HORIZONTAL_MARKOVIZATION
	 */
	public void preProcess(boolean REMOVE_UNARY_NODES_FROM_PARSETREES, boolean DO_HORIZONTAL_MARKOVIZATION, boolean trainSet) {
		
		//ArrayList<Integer> skipIndices = new ArrayList<Integer>();
		
		//remove single word sentences and sentences > MAX_SENTENCE_SIZE
		//but not during training (if !trainSet) MAX_SENTENCE_SIZE = 120 
		//if (!trainSet) skipIndices = removeSingleWordAndTooLongSentences();

		//you want either the full parse trees (for symbolic grammar) or the reduced parse trees for HPN grammar
		if (REMOVE_UNARY_NODES_FROM_PARSETREES) removeUnaryNodesFromParseTrees();

		//binarize
		if (DO_HORIZONTAL_MARKOVIZATION) horizontal_markovization();
		
		//return skipIndices;
	}

	/**
	 * Replaces infrequent words in the train treebank with unknown word classes,
	 * which are determined by morphology in method findUnknownWordClassesForInfrequentWords.
	 * If there are sufficiently many types in unknown word class, it stores this class in
	 * unknownWordClassesInTrainSet, to be used for classifying unknown words in test set
	 * 
	 */
	public void smoothUnknownWordsInTrainSet(ArrayList<parseTree> parseTreesFromTreebank) {

		/** 
		 * unique word types plus token frequency
		 */
		HashMap<String, Integer> wordCounts = doWordFrequencyCounts(parseTreesFromTreebank);

		HashMap<String, Integer> unknownWordClassFrequencies = new HashMap<String, Integer>();
		HashMap<String, String> infrequentWordsPlusReplacements = new HashMap<String, String>();


		infrequentWordsPlusReplacements = findUnknownWordClassesForInfrequentWords(wordCounts);

		//some statistics (you need at least MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS unique words in every unknownWordClass)
		for (String infrequentWord : infrequentWordsPlusReplacements.keySet()) {
			String unknownWordClass = infrequentWordsPlusReplacements.get(infrequentWord);
			if (unknownWordClassFrequencies.get(unknownWordClass)==null) unknownWordClassFrequencies.put(unknownWordClass, 1);
			else unknownWordClassFrequencies.put(unknownWordClass, unknownWordClassFrequencies.get(unknownWordClass) + 1);
		}


		for (String unknownWordClass : unknownWordClassFrequencies.keySet()) {
			if (unknownWordClassFrequencies.get(unknownWordClass)>=Main.MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS) unknownWordClassesInTrainSet.add(unknownWordClass);
			else {
				//replace unknownWordClass back with the "unk" class, because there are not enough samples!
				for (String infrequentWord : infrequentWordsPlusReplacements.keySet()) {
					if (infrequentWordsPlusReplacements.get(infrequentWord).equals(unknownWordClass))
						infrequentWordsPlusReplacements.put(infrequentWord, "unk");
				}
			}
			//print smoothing statistics
			//System.out.println("smoothing class: " + unknownWordClass + "; frequency=" + unknownWordClassFrequencies.get(unknownWordClass));
		}

		for (parseTree myParseTree : parseTreesFromTreebank) {
			//replace infrequent words in the parseTrees: checks if it is open class
			replaceInfrequentOpenClassWordsInTrainset(myParseTree, infrequentWordsPlusReplacements);
		}

	}

	
	public HashMap<String, HashSet<Integer>> findDuplicates() {
	
		int myCounter = 0;
		for (parseTree myParseTree : this.parseTreesFromTreebank) {
			String WSJString = myParseTree.printWSJFormat();
			HashSet<Integer> sentenceNrsOfDuplicates = null;
			if (duplicateSentenceNrsInTrainSet.get(WSJString)==null) {
				sentenceNrsOfDuplicates = new HashSet<Integer>();
				duplicateSentenceNrsInTrainSet.put(WSJString, sentenceNrsOfDuplicates);
			}
			else sentenceNrsOfDuplicates = duplicateSentenceNrsInTrainSet.get(WSJString);
			
			sentenceNrsOfDuplicates.add(myCounter);
			myCounter++;
		}
		return duplicateSentenceNrsInTrainSet;
	}
	
	
	/**
	 * Replaces unknown words (not in terminalUnits of grammar) in test sentences 
	 * by unknown word classes if these are also found in unknownWordClassesInTrainSet. <br>
	 * First checks whether the same word with or without final "s", and with or without
	 * initial capital letter is perhaps found in the train set
	 * 
	 * @param lexicon yields the known terminals in the train treebank 
	 * 
	 * @return HashMap of unknownWordReplacements <unknown word, unknown word class>
	 * that can be reused in reranker for smoothing the 10 best parses, since the 
	 * replacements need to be found only once in the gold standard test sentences.
	 */	
	public HashMap<String, String> replaceUnknownWordsInTestSet(HashSet<String> lexicon, boolean DO_HORIZONTAL_MARKOVIZATION, boolean DO_LEMMATIZATION){

		HashMap<String, String> unknownWordReplacements = new HashMap<String, String>();
		//HashSet<String> wordExistsWithOrWithoutFinal_s = new HashSet<String>();

		//HashSet<String> lexicon = (HashSet) myGrammar.getTerminalUnits().keySet();

		for (parseTree testParseTree : this.parseTreesFromTreebank) {
			for (Node myNode : testParseTree.getNodes()) {

				if (myNode.getType()==parser.parameters.TERMINAL) {
					String wordLabel = myNode.getName();
					
					int wlen = wordLabel.length();

					//check if it is unknown word
					if (!(lexicon.contains(wordLabel))) {

						
						//informed guesses: check if unknown word does not occur with/out -s or with/out capital letter)
						//does the unknown word occur in the train set without final "s"? or with?
						boolean blnStemKnownOrCapitalLetter = false;

						if (DO_LEMMATIZATION) {
							if (wordLabel.endsWith("s") && wlen >= 3) {
								//check if word stem without s is known
								String wordStem = wordLabel.substring(0, wordLabel.length()-1);
								//you should in fact also check for stem = -es, ed and many other variations
								if (lexicon.contains(wordStem)) {
									myNode.setName(wordStem);
									if (myNode.getParentNode().getName().equals("NNS")) myNode.getParentNode().setName("NN");
									if (myNode.getParentNode().getName().equals("VBZ")) myNode.getParentNode().setName("VBP");
									if (DO_HORIZONTAL_MARKOVIZATION) {
										//you must also replace the postag after the > in the binarized grandparent nonterminal
										String grandParent = myNode.getParentNode().getParentNode().getName();
										if (grandParent.contains(">NNS")) grandParent = grandParent.replace(">NNS", ">NN");
										if (grandParent.contains(">VBZ")) grandParent = grandParent.replace(">VBZ", ">VBP");
										myNode.getParentNode().getParentNode().setName(grandParent);
									}
									//myNode.setProductionName(wordStem);
									unknownWordReplacements.put(wordLabel, "s");	//use this in best Charniak sentences

									blnStemKnownOrCapitalLetter = true;
								}
							}
							//other way round (word exists in train set only with final "s":
							if (wlen >= 2 && lexicon.contains(wordLabel + "s")) {
								myNode.setName(wordLabel + "s");
								if (myNode.getParentNode().getName().equals("NN")) myNode.getParentNode().setName("NNS");
								if (myNode.getParentNode().getName().equals("VBP")) myNode.getParentNode().setName("VBZ");
								if (DO_HORIZONTAL_MARKOVIZATION) {
									//you must also replace the postag after the > in the binarized grandparent nonterminal
									String grandParent = myNode.getParentNode().getParentNode().getName();
									if (grandParent.contains(">NN")) grandParent = grandParent.replace(">NN", ">NNS");
									if (grandParent.contains(">VBP")) grandParent = grandParent.replace(">VBP", ">VBZ");
									myNode.getParentNode().getParentNode().setName(grandParent);
								}
								//myNode.setProductionName(wordStem);
								unknownWordReplacements.put(wordLabel, "s");

								blnStemKnownOrCapitalLetter = true;
							}
						}	//if (experiments.DO_LEMMATIZATION)

						//capital letter at initial position of test sentence: kijk of het zonder hoofdletter voorkomt in train.
						if (!(wordLabel.equals(wordLabel.toLowerCase()))) {
							if (myNode.getLeftSpan()==0) {
								if (lexicon.contains(wordLabel.toLowerCase())) {
									myNode.setName(wordLabel.toLowerCase());
									//myNode.setProductionName(wordLabel.toLowerCase());
									unknownWordReplacements.put(wordLabel, wordLabel.toLowerCase());
									blnStemKnownOrCapitalLetter = true;
								}
							}
						}

						//replace word with unknownWordClass (if unknown word does not occur with/out -s or with/out capital letter)
						if (!blnStemKnownOrCapitalLetter) {
							String smoothClass = getFeatureOfWord(wordLabel.trim());
							
							//replace production in terminal, but only if the unknown wordclass also occurs in train set at least 10 times
							if (unknownWordClassesInTrainSet.contains(smoothClass)) {
								//if (smoothClass.equals("1capY_dashY_slshN_alfY_digN_sfx:NONE_afx:NONE")) System.out.println("rare ding in train");
								myNode.setName(smoothClass);
								//myNode.setProductionName(smoothClass);
								unknownWordReplacements.put(wordLabel, smoothClass);	//this is for nBest Charniak parses
							}
							else {
								myNode.setName("unk");
								//myNode.setProductionName(smoothClass);
								unknownWordReplacements.put(wordLabel, "unk");
							}
						}
					}	//if (!(lexicon.contains(myNode.getProductionName()))) {
				}	//if (myNode.getType()==dopparser.Main.TERMINAL) {
			}	//for (Node myNode : testParseTree.getNodes())
		}	//for (parseTree testParseTree : goldStandardParses)

		return unknownWordReplacements;
	}

	/**
	 *  Removes sentences of 1 word, and longer than MAX_SENTENCE_SIZE.
	 *  Note that long sentences must removed in test corpus, but not in train corpus
	 * therefore in train corpus MAX_SENTENCE_SIZE = 120	
	 */
	public ArrayList<Integer>  removeSingleWordAndTooLongSentences() {

		//filter out sentences  >MAX_SENTENCE_SIZE and single word
		ArrayList<Integer> sentenceIndicesToSkip = new ArrayList<Integer>();
		//ArrayList<parseTree> selectedParseTrees = new ArrayList<parseTree>();

		int counter = 0;
		for (parseTree myParseTree : this.parseTreesFromTreebank) {
			int nrTerminals = 0;
			for (parser.Node myNode : myParseTree.getNodes()) {
				if (myNode.getType()==parser.parameters.TERMINAL) nrTerminals++;
			}
			if (nrTerminals==1 || nrTerminals>parser.parameters.MAX_SENTENCE_SIZE) {
				//selectedParseTrees.add(myParseTree);
				sentenceIndicesToSkip.add(counter);
			}
			counter++;
		}
		//this.parseTreesFromTreebank = selectedParseTrees;
		
		return sentenceIndicesToSkip;
	}

	/**
	 * Counts token frequencies of words in treebank
	 * @param preprocessedParseTrees
	 * @return wordCounts
	 */
	public static HashMap<String, Integer> doWordFrequencyCounts(ArrayList<parseTree> preprocessedParseTrees) {
		HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
		String wordLabel ="";

		for (parseTree myParseTree : preprocessedParseTrees) {
			for (Node myNode : myParseTree.getNodes()) {
				if (myNode.getType()==parser.parameters.TERMINAL){
					if (parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES)
						wordLabel = myNode.getName();
					else wordLabel = myNode.getName().split(" ")[0].trim() + "@" + myNode.getName().split(" ")[1].trim();

					if (wordCounts.get(wordLabel)==null)
						wordCounts.put(wordLabel, 1);
					else wordCounts.put(wordLabel, wordCounts.get(wordLabel) + 1);
				}
			}
		}
		return wordCounts;
	}

	/**
	 * Determines classes of infrequent words in train treebank, 
	 * based on their frequency in wordCounts.
	 * Note no actual replacement done here. For local use only.
	 * 
	 * @param wordCounts words plus token frequencies
	 * @return wordReplacements: <word, unknownWordClass>
	 */
	public HashMap<String, String> findUnknownWordClassesForInfrequentWords(HashMap<String, Integer> wordCounts){
		
		HashMap<String, String> wordReplacements = new HashMap<String, String>();
		//rare words are those that occur in wordCounts with frequency < WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING
		//determine their unknownWordClass
		for (String rareWord : wordCounts.keySet()) {	//loop over unique words (lexicon)
			if (wordCounts.get(rareWord) <= Main.WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING) {
				String unknownWordClass = getFeatureOfWord(rareWord.trim());
				wordReplacements.put(rareWord, unknownWordClass);
			}
		}

		return wordReplacements;
	}

	/**
	 * Has been replaced by getFeatureOfWord
	 * Determines unknown word classes based on morphology of the word.
	 * If word is not in one of the fixed classes, then classify it as "unk".
	 * @param rareWord
	 * @return wordClass
	 */
	public static String findWordClass(String rareWord) {
		//String wordClass = "";

		if (isValidInteger(rareWord) || isValidDouble(rareWord)) return "classNumber";
		if (rareWord.contains("-")) return "classHyphen";
		if (rareWord.contains(":")) return "classTime";
		//assuming initial sentence position words have been turned to lower case!!!
		if (!(rareWord.equals(rareWord.toLowerCase())))	 return "classCapital";
		//see: http://www.prefixsuffix.com/affixes.php
		//prefixes: see http://en.wikipedia.org/wiki/English_prefixes
		if (rareWord.endsWith("ble")) return "classBLE";
		if (rareWord.endsWith("ly")) return "classLY";
		if (rareWord.endsWith("ve")) return "classVE";
		if (rareWord.endsWith("ment")) return "classMENT";
		if (rareWord.endsWith("ent")) return "classENT";
		if (rareWord.endsWith("ence")) return "classENCE";
		if (rareWord.endsWith("en")) return "classEN";
		if (rareWord.endsWith("ism")) return "classISM";
		if (rareWord.endsWith("ist")) return "classIST";
		if (rareWord.endsWith("hood")) return "classHOOD";
		if (rareWord.endsWith("ity")) return "classITY";
		if (rareWord.endsWith("ness")) return "classNESS";
		if (rareWord.endsWith("ess")) return "classESS";
		if (rareWord.endsWith("er")) return "classER";
		if (rareWord.endsWith("est")) return "classEST";
		if (rareWord.endsWith("ing")) return "classING";
		if (rareWord.endsWith("ed")) return "classED";
		if (rareWord.endsWith("n't")) return "classNOT";
		if (rareWord.endsWith("ful")) return "classFUL";
		if (rareWord.endsWith("tion")) return "classTION";
		if (rareWord.endsWith("ion")) return "classION";
		if (rareWord.endsWith("ify")) return "classIFY";
		if (rareWord.endsWith("ise") || rareWord.endsWith("ize")) return "classISE";
		if (rareWord.endsWith("es")) return "classES";
		if (rareWord.endsWith("s")) return "classS";

		if (rareWord.startsWith("ab")) return "classAB";
		if (rareWord.startsWith("anti")) return "classANTI";
		if (rareWord.startsWith("auto")) return "classAUTO";
		if (rareWord.startsWith("con") || rareWord.startsWith("com")) return "classCON";
		if (rareWord.startsWith("dis")) return "classDIS";
		if (rareWord.startsWith("extra") || rareWord.startsWith("inter") || rareWord.startsWith("intra") || rareWord.startsWith("under")) return "classEXTRA";
		if (rareWord.startsWith("ex")) return "classEX";
		if (rareWord.startsWith("in")) return "classIN";
		if (rareWord.startsWith("non")) return "classNON";
		if (rareWord.startsWith("pre") || rareWord.startsWith("pro") || rareWord.startsWith("post")) return "classPRE";
		if (rareWord.startsWith("syn") || rareWord.startsWith("sym") || rareWord.startsWith("sys")) return "classSYN";
		if (rareWord.startsWith("un")) return "classUN";
		if (rareWord.startsWith("hetero") || rareWord.startsWith("homo")) return "classHETERO";
		if (rareWord.startsWith("sur") || rareWord.startsWith("super") || rareWord.startsWith("sub") || rareWord.startsWith("sup") || rareWord.startsWith("sus") || rareWord.startsWith("trans") || rareWord.startsWith("hyper")) return "classSUR";
		if (rareWord.startsWith("pseudo") || rareWord.startsWith("quasi") || rareWord.startsWith("semi") || rareWord.startsWith("neo")  || rareWord.startsWith("micro")  || rareWord.startsWith("macro")) return "classQUASI";
		if (rareWord.startsWith("re")) return "classRE";
		if (rareWord.startsWith("per")) return "classPER";


		return "unk";
	}

	public static boolean isValidInteger(String s) {
		try {
			int i = java.lang.Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	public static boolean isValidDouble(String s) {
		try {
			double d = java.lang.Double.parseDouble(s);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	
	/**
	 * Replaces the names of terminal nodes in parseTree if they occur 
	 * as key in infrequentWordReplacements, but only if the word (terminal) 
	 * belongs to an open postag class  
	 * 
	 * @param myParseTree
	 * @param infrequentWordReplacements
	
	public static void replaceInfrequentOpenClassWordsInTrainset(parseTree myParseTree, HashMap<String, String> infrequentWordReplacements) {
		//if (parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) {	//otherwise it doesn't work

			for (Node myNode : myParseTree.getNodes()) {
				if (myNode.getType()==parser.parameters.TERMINAL){

					if (infrequentWordReplacements.containsKey(myNode.getName())) {
						//only replace if POSTAG is open class
						//see: Building a Large Annotated Corpus of English: The Penn Treebank (page 5)
						//http://acl.ldc.upenn.edu/J/J93/J93-2004.pdf
						//determine postag
						String postag = myNode.getParentNode().getName();

						boolean includeNNP=true;
						//if (memberOfOpenClass(postag, includeNNP)) {
							myNode.setName(infrequentWordReplacements.get(myNode.getName()));
						//}
					}
				}
			}
		//}
	}
	**/
	
	
	/**
	 * Replaces the names of terminal nodes in parseTree if they occur 
	 * as key in infrequentWordReplacements, but only if the word (terminal) 
	 * belongs to an open postag class  
	 * 
	 * @param myParseTree
	 * @param infrequentWordReplacements
	*/
	public static void replaceInfrequentOpenClassWordsInTrainset(parseTree myParseTree, HashMap<String, String> infrequentWordReplacements) {
		if (parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) {	//otherwise it doesn't work

			for (Node myNode : myParseTree.getNodes()) {
				if (myNode.getType()==parser.parameters.TERMINAL){
					if (infrequentWordReplacements.containsKey(myNode.getName())) {
						//only replace if POSTAG is open class
						//see: Building a Large Annotated Corpus of English: The Penn Treebank (page 5)
						//http://acl.ldc.upenn.edu/J/J93/J93-2004.pdf
						//determine postag
						String postag = myNode.getParentNode().getName();

						boolean includeNNP=true;
						if (memberOfOpenClass(postag, includeNNP)) {
							//System.out.println("old name=" + myNode.getName() + "; new name=" + rareWordReplacements.get(myNode.getName()));
							myNode.setName(infrequentWordReplacements.get(myNode.getName()));
						}
					}
				}
			}
		}
	}
	

	/**
	 * Invokes myParseTree.removeUnaryNodes() for all parseTrees from the treebank
	 */
	public void removeUnaryNodesFromParseTrees() {
		for (parseTree myParseTree : this.parseTreesFromTreebank) {
			//remove unary nodes from parseTree
			//System.out.println("before remove unary: " + myParseTree.printWSJFormat());
			myParseTree.removeUnaryNodes();
			myParseTree.removeUnaryNodes();

		}
	}

	/**
	 * Invokes myParseTree.binarize() for all parseTrees from the treebank
	 */
	public void horizontal_markovization() {
		for (parseTree myParseTree : this.parseTreesFromTreebank) {

			//binarize nodes from parseTree
			myParseTree.binarize();	
			myParseTree.calculateNodeDepth();
		}
	}

	public ArrayList<parseTree> getPreprocessedParseTrees(){
		return parseTreesFromTreebank;
	}

	/**
	 * Converts sentence from Tuebingen corpus into format that is parseable as WSJ format.
	 * @param mySentence
	 * @return mySentence
	 */
	public static String preprocessTuebingenSentence(String mySentence) {
		//the Tuebingen corpus lacks a TOP label at the beginning of the sentence
		mySentence = "(TOP" + mySentence.substring(1);

		//this method separates the brackets from the words, and adds a single space between brackets and words
		//preprocessedSentences = addSingleSpaceBetweenBrackets(preprocessedSentences);
		StringBuffer myCleanedSentence = new StringBuffer();
		String [] sentenceArray = null;

		//System.out.println(">>" +mySentence+ "<<");
		//put precisely one space before and after every bracket
		sentenceArray = mySentence.split("\\(");
		myCleanedSentence = new StringBuffer();
		//myCleanedSentence.append("( ");
		for (String word : sentenceArray) {
			//System.out.println(">>" +word.trim()+ "<<");
			if (word.trim().equals("")) myCleanedSentence.append(word.trim()).append("( ");
			else myCleanedSentence.append(word.trim()).append(" ( ");

		}

		//replace )
		sentenceArray = myCleanedSentence.toString().split("\\)");
		myCleanedSentence = new StringBuffer();
		for (String word : sentenceArray) {
			if (word.trim().equals("")) myCleanedSentence.append(word.trim()).append(") ");
			else myCleanedSentence.append(word.trim()).append(" ) ");
		}

		String sentenceWithSpaces = myCleanedSentence.toString().trim();
		mySentence = sentenceWithSpaces.substring(0, sentenceWithSpaces.length()-4).trim();
		return mySentence;
	}

	/**
	 * Converts the first word of the sentence to lower case,
	 * but only if the first word belongs to an open postag class
	 * 
	 * @param myParseTree
	 */
	public static HashSet<String> convertOpenClassWordsInInitialSentencePosition2LowerCase(parseTree myParseTree) {

		
    	for (Node myNode : myParseTree.getNodes()) {

    		if (myNode.getType()==parser.parameters.TERMINAL && myNode.getLeftSpan()==0) {
    			String wordLabel = myNode.getName();

    			//capital letter at initial position of test sentence: kijk of het zonder hoofdletter voorkomt in train.
    			if (!(wordLabel.equals(wordLabel.toLowerCase()))) {
    				//check open class
    				String firstPostag = myNode.getParentNode().getName();
    				if (memberOfOpenClass(firstPostag, false) && !wordLabel.contains("*")) {
    					myNode.setName(wordLabel.toLowerCase());
    					lowerCaseConversions.add(wordLabel);
    				}
    			}
    			break;
    		}
    	}
    	
    	return lowerCaseConversions;
    }
	
	/**
	 * Converts the first word of the sentence to lower case,
	 * but only if the same conversion has been performed in the Gold Standard parses
	 * 
	 * @param myParseTree
	 */
	public static HashSet<String> convertOpenClassWordsInInitialSentencePosition2LowerCase(parseTree myParseTree, HashSet<String> lowerCaseConversionsFromGoldStandard) {

		
    	for (Node myNode : myParseTree.getNodes()) {

    		if (myNode.getType()==parser.parameters.TERMINAL && myNode.getLeftSpan()==0) {
    			String wordLabel = myNode.getName();

    			//capital letter at initial position of test sentence
    			if (lowerCaseConversionsFromGoldStandard.contains(wordLabel)) {
    				//System.out.println("lowerCaseConversionsFromGoldStandard contains " + wordLabel);
    					myNode.setName(wordLabel.toLowerCase());
    			}
    
    			break;
    		}
    	}
    	
    	return lowerCaseConversions;
    }
	
	/**
	 * No longer used.
	 * Converts the first word of the sentence to lower case,
	 * but only if the first word belongs to an open postag class
	 * 
	 * @param mySentence
	 * @return mySentence 
	 */
	public static String convertOpenClassWordsInInitialSentencePosition2LowerCase_old(String mySentence) {

		//System.out.println("unconverted se: " + mySentence);
		//find the first word by splitting the sentence at the space. the first word is the first one that is not preceded by (
		String[] splitSentence  = mySentence.split(" ");
		String sentencePart = "", firstWord="", firstPostag="";
		for (int i =0; i< splitSentence.length ; i++) {
			sentencePart = splitSentence[i];
			if (!(sentencePart.startsWith("("))) {
				firstWord = sentencePart;
				while (firstWord.endsWith(")")) firstWord = firstWord.substring(0, firstWord.length()-1); 
				firstPostag = splitSentence[i-1].substring(1);
				break;
			}
		}

		//System.out.println("firstWord=" + firstWord + "; firstPostag=" + firstPostag);
		//if postag is open class, then convert to lowercase (but not NNP = personal nouns)
		if (memberOfOpenClass(firstPostag, false) && !firstWord.contains("*")) {
			//replace first occurrence of the firstWord in the sentence
			//System.out.println("firstWord=>>>" + firstWord + "<<<");
			mySentence = mySentence.replaceFirst(firstWord, firstWord.toLowerCase());
		}
		//check: 
		//System.out.println("converted sent: " + mySentence);
		return mySentence;
	}

	/**
	 * Checks whether word belongs to an open postag class, but not NNP.
	 * @param posTag
	 * @return
	 */
	public static boolean  memberOfOpenClass(String posTag, boolean includeNNP) {
		boolean isMember=false;

		if (posTag.equals("NN") || posTag.equals("NNS") || posTag.startsWith("JJ") || posTag.startsWith("VB") || posTag.startsWith("RB") || posTag.equals("VBN") || posTag.equals("VBP")  || posTag.equals("VBZ") || posTag.equals("VBG") || posTag.equals("VBP") || posTag.equals("VBP") || posTag.equals("FW") || posTag.equals("POS") ||  posTag.equals("RP") || posTag.equals("CD")) 
			isMember=true;
		if (includeNNP && posTag.equals("NNP")) isMember=true;	//you do want to replace personal nouns as infrequent words!
		if (Main.BLN_TUEBINGEN && (posTag.equals("JJR") || posTag.equals("JJS") ||posTag.equals("LS") ||posTag.equals("NPS") )) isMember=true;
		
		return isMember;
	}

	protected void loadDefaultParameters() {

		useFirstWord = true;
		useFirstCap = true;
		useAllCap = false;
		useDash = true; 
		useForwardSlash = true; 
		useDigit = true;
		useAlpha = true;
		useDollar = false;
		allowToCombineSuffixAffix = false;
		useASFixWithDash = false;
		useASFixWithSlash = false;
		useASFixWithCapital = false;


		//String affixesAll = "inter trans under over non com con dis pre pro co de in re un";
		String suffixesAll = "ments ance dent ence ists line ment ship time ans ant are " +
		"ate ble cal ess est ful ian ics ing ion ist ive man ons ory ous son tor " +
		"ure al ce ck cy de ds ed er es et ey fy gs gy ic is ks ld le ls ly ne rd " +
		"rs se sh sm th ts ty ze s y";

		affixes = null;
		//affixes = affixesAll.split("\\s");
		suffixes = suffixesAll.split("\\s");

	}

	public String getFeatureOfWord(String word) {	//, boolean firstWord
		StringBuilder result = new StringBuilder();		
		//if (useFirstWord) {
		//	result.append(firstWord ? "_1stY" : "_1stN");
		//}
		boolean firstCapital = false;
		if (useFirstCap) {
			char firstChar = word.charAt(0);
			firstCapital = Character.isUpperCase(firstChar);
			result.append(firstCapital ? "_1capY" : "_1capN");
		}
		boolean allCapital = false;
		if (useAllCap) {			
			allCapital = allCapitals(word);
			result.append(allCapital ? "_AcapY" : "_AcapN");
		}
		boolean hasDash = false;
		if (useDash) {
			hasDash = word.indexOf('-')!=-1;
			result.append(hasDash ? "_dashY" : "_dashN");
		}	
		boolean hasForwardSlash = false;
		if (useForwardSlash) {
			hasForwardSlash = word.indexOf('/')!=-1;
			result.append(hasForwardSlash ? "_slshY" : "_slshN");
		}
		if (useDollar) {
			boolean hasDollar = word.indexOf('$')!=-1;
			result.append(hasDollar ? "_$Y" : "_$N");
		}
		if (useAlpha) {
			boolean hasDigit = word.matches(containsAlphaMatch);			
			result.append(hasDigit ? "_alfY" : "_alfN");
		}
		if (useDigit) {
			boolean hasDigit = word.matches(containsDigitMatch);			
			result.append(hasDigit ? "_digY" : "_digN");
		}

		if ( (useASFixWithDash || !hasDash) &&
				(useASFixWithSlash || !hasForwardSlash) && 	
				(useASFixWithCapital || (!firstCapital && !allCapital)) ){			
			String wordLower = word.toLowerCase();
			boolean foundSuffix = false;
			if (suffixes!=null) {
				String suff = getSuffix(wordLower);
				if (suff==null) result.append("_sfx:NONE");
				else {
					result.append("_sfx:" + suff);
					foundSuffix = true;
				}
			}
			if (affixes!=null) {
				if (!allowToCombineSuffixAffix && foundSuffix) {
					result.append("_afx:NONE");
				}
				else {
					String aff = getAffix(wordLower);
					result.append(aff==null ? "_afx:NONE" : ("_afx:" + aff));
				}
			}
		}
		else {
			result.append("_sfx:NONE");
			result.append("_afx:NONE");
		}
		return result.substring(1);		
	}

	public static boolean allCapitals(String w) {
		char[] charArray = w.toCharArray();
		for(char c : charArray) {
			if (Character.isLowerCase(c)) return false;
		}
		return true;
	}

	public String getAffix(String wordLower) {
		int index = 0;
		for(String a : affixes) {
			if (wordLower.startsWith(a)) {
				return a;
			}
			index++;
		}
		return null;
	}

	public String getSuffix(String wordLower) {
		int index = 0;
		for(String a : suffixes) {
			if (wordLower.endsWith(a)) {
				return a;
			}
			index++;
		}
		return null;
	}
}
