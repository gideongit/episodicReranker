package episodicReranker;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import parser.parseTree;

public class Utils {

	 public static ArrayList<Double> normalizeVector(ArrayList<Double> vector, boolean blnEuclidean) {
			ArrayList<Double> normalizedVector = new ArrayList<Double>();
			double vectorLength;
			if (blnEuclidean) vectorLength = computeVectorLength(vector, true);	//sum of squares
			else vectorLength = computeVectorLength(vector, false);	//linear sum
			if (vectorLength==0.) {
				//System.out.println("SHIT BUG!!! vectorLength=0");
				return vector;
			}
			for (Double component : vector) {
				normalizedVector.add(component/vectorLength);
			}
			return normalizedVector;
		}
	    
	    
	    
	    public static double computeVectorLength(ArrayList<Double> vector, boolean blnEuclidean) {
			//compute Euclidean length
			double vectorLength = 0.;
			for (Double component : vector) {
				if (blnEuclidean) vectorLength += component*component;
				else vectorLength += component;
			}
			if (blnEuclidean) vectorLength = Math.sqrt(vectorLength);
			return vectorLength;
		}
	    
	public static ArrayList<Double> sampler_old(TreeSet<Double> distribution, int branchingFactor) {
		
		//set branchingFactor=1 if you want a single sample
		//distribution needs NOT be accumulative function, and needs NOT add up to 1
		
		ArrayList<Double> NBestMatches = new ArrayList<Double>();
		//accumulative sum of the goodnessOfMatches; thus it defines an area in the distribution where this root is selected
		TreeMap<Double, Double> accumulatedDistribution = new TreeMap<Double, Double>();
	
		//double highestValue = 0.;
		double totalValue = 0.;
		for (Double prob : distribution) {
			totalValue += prob;
			//if (accumulatedDistribution.size()>0) highestValue = accumulatedDistribution.lastKey();
			accumulatedDistribution.put(prob, totalValue);
		}
		
		//for (Double prob : accumulatedDistribution.keySet()) {
		//	System.out.println("p=" + prob + "; ac=" + accumulatedDistribution.get(prob));
		//}
		//sampling: branchingFactor times, or until branchingFactor distinct  are sampled
		while (NBestMatches.size() < branchingFactor) {
			double randomNr = totalValue*Math.random();
			//find the ProductionRoot whose accumulatedMatch lies in the interval 
			for (Double prob : accumulatedDistribution.keySet()) {	//ordered from small to large values
				Double accumulatedMatch = accumulatedDistribution.get(prob);
				if (accumulatedMatch > randomNr) {
					NBestMatches.add(prob);
					break;
				}
			
			}
			
			
		}
		return NBestMatches;
		
	}
	
public static double sampler(ArrayList<Double> distribution) {
		
		//distribution needs NOT be accumulative function, and needs NOT add up to 1
		
		double bestMatch=0.;
		//accumulative sum of the goodnessOfMatches; thus it defines an area in the distribution where this root is selected
		ArrayList<Double> accumulatedDistribution = new ArrayList<Double>();
	
		//double highestValue = 0.;
		double totalValue = 0.;
		for (Double prob : distribution) {
			totalValue += prob;
			//if (accumulatedDistribution.size()>0) highestValue = accumulatedDistribution.lastKey();
			accumulatedDistribution.add(totalValue);
		}
		
		//sampling: branchingFactor times, or until branchingFactor distinct  are sampled
		double randomNr = totalValue*Math.random();
			//find the ProductionRoot whose accumulatedMatch lies in the interval 
			for (int i =0; i< distribution.size(); i++) {	//ordered from small to large values
				double accumulatedMatch = accumulatedDistribution.get(i);
				if (accumulatedMatch > randomNr) {
					bestMatch = distribution.get(i);
					break;
				}
			}

		return bestMatch;
		
	}
	/*
	public static HashMap<nodeState, Double> sampler(TreeMap<Double, nodeState> distribution, int branchingFactor) {
		//HashMap<String, Double> distribution
		HashMap<nodeState, Double> NBestMatches = new HashMap<nodeState, Double>();
		HashMap<nodeState, Double> distributionIndexedByState = new HashMap<nodeState, Double>();
		
		//distributionOfproductionRootMatches assigns a number to every productionRoot that is the 
		//accumulative sum of the goodnessOfMatches; thus it defines an area in the distribution where this root is selected
		TreeMap<Double, nodeState> accumulatedDistribution = new TreeMap<Double, nodeState>();
	
		double highestValue = 0.;
		double totalValue = 0.;
		for (Double prob : distribution.keySet()) {
			nodeState myState = distribution.get(prob);
			distributionIndexedByState.put(myState, prob);
			if (accumulatedDistribution.size()>0) highestValue = accumulatedDistribution.lastKey();
			accumulatedDistribution.put(highestValue + prob, myState);
			totalValue += prob;
		}
		
		//sampling: branchingFactor times, or until branchingFactor distinct  are sampled
		nodeState sampledState;
		while (NBestMatches.size() < branchingFactor) {
			double randomNr = totalValue*Math.random();
			//find the ProductionRoot whose accumulatedMatch lies in the interval 
			for (Double accumulatedMatch : accumulatedDistribution.keySet()) {	//ordered from small to large values
				
				if (accumulatedMatch > randomNr) {
					sampledState = accumulatedDistribution.get(accumulatedMatch);
					NBestMatches.put(sampledState, distributionIndexedByState.get(sampledState));
					//System.out.println("randomNr=" + randomNr + "; root=" + sampledState);
					break;
				}
			}
			
		}
		//for(nodeState sampledRoot : NBestMatches.keySet()) {
		//	System.out.println("root=" + sampledRoot + "; match=" + NBestMatches.get(sampledRoot));
		//}
		return NBestMatches;
		
	}
	*/
	
	public static void sampler_bu(HashMap<String, Double> distribution, int branchingFactor) {
		
		HashMap<String, Double> NBestMatches = new HashMap<String, Double>();
		
		//distributionOfproductionRootMatches assigns a number to every productionRoot that is the 
		//accumulative sum of the goodnessOfMatches; thus it defines an area in the distribution where this root is selected
		TreeMap<Double, String> accumulatedDistribution = new TreeMap<Double, String>();
	
		double highestValue = 0.;
		double totalValue = 0.;
		for (String myWord : distribution.keySet()) {
			if (accumulatedDistribution.size()>0) highestValue = accumulatedDistribution.lastKey();
			accumulatedDistribution.put(highestValue + distribution.get(myWord), myWord);
			totalValue += distribution.get(myWord);
		}
			
	
		//sampling: branchingFactor times, or until branchingFactor distinct  are sampled
		String sampledProductionRoot;
		while (NBestMatches.size() < branchingFactor) {
			double randomNr = totalValue*Math.random();
			//find the ProductionRoot whose accumulatedMatch lies in the interval 
			for (Double accumulatedMatch : accumulatedDistribution.keySet()) {	//ordered from small to large values
				
				if (accumulatedMatch > randomNr) {
					sampledProductionRoot = accumulatedDistribution.get(accumulatedMatch);
					NBestMatches.put(sampledProductionRoot, distribution.get(sampledProductionRoot));
					System.out.println("randomNr=" + randomNr + "; root=" + sampledProductionRoot);
					break;
				}
			}
			
		}
		for(String sampledRoot : NBestMatches.keySet()) {
			System.out.println("root=" + sampledRoot + "; match=" + NBestMatches.get(sampledRoot));
		}
	}
	
	public static String vector2String(ArrayList<Double> vector) {
		
		NumberFormat numberFormatter = new DecimalFormat("#.######");
        //numberFormatter = NumberFormat.getNumberInstance();
       
		StringBuffer vectorString = new StringBuffer();
		//vectorString.append("(");
		for (Double comp : vector) {
			vectorString.append(numberFormatter.format(comp).replace(",", ".")).append(" ");
			//vectorString.append(comp).append(" ");
		}
		
		return vectorString.toString().replaceAll(",", ".").trim();
	}
	
	public static void generateRandomPermutations(ArrayList<String> lexicon, String fileName, int maxLength) throws Exception {
		//generates all possible (grammatical and non-grammatical) sequences of words 
		
		ArrayList<String>[] randomSentences = new ArrayList[maxLength+1];

		randomSentences[0] = new ArrayList<String>();
		randomSentences[0].add("");
		
		for (int sentenceLength = 1; sentenceLength<=maxLength; sentenceLength++) {
			
			randomSentences[sentenceLength] = new ArrayList<String>();
			
			//for (int i=1; i<=sentenceLength; i++) {
				for (int j=0; j< lexicon.size(); j++) {
					//add the word to every entry in the HashSet
					for (String shorterSentence : randomSentences[sentenceLength-1]) {
						randomSentences[sentenceLength].add(shorterSentence + lexicon.get(j) + " ");
					}
				}
			//}
		}
		
		//print it and save it to file
		BufferedWriter out_random_sentences = new BufferedWriter(new FileWriter("./input/" + fileName));
		   	
		int counter = 0;
		for (int sentenceLength = 2; sentenceLength<=maxLength; sentenceLength++) {
			for (String randomSentence : randomSentences[sentenceLength]) {
				counter++;
				System.out.println(randomSentence.trim());
				out_random_sentences.write(randomSentence.trim());
				out_random_sentences.newLine();
			}
		}
		
		out_random_sentences.flush();
		out_random_sentences.close();
		
		System.out.println(counter + " sentences were generated.");
		System.out.println("Done.");
	}
	
    public static void selectOnlyFrequentWordSentences(String fileName, boolean labeled, boolean skipSingleWordSentences) throws Exception {
    	
    	int MINIMUM_WORD_FREQUENCY = 5;
    	
        HashMap<String, Integer> wordFrequencies = new HashMap<String, Integer>();
        HashMap<String, Integer> wordFrequenciesOfFilteredSentences = new HashMap<String, Integer>();
        
        HashSet<String> uniqueSentences = new HashSet<String>();
        
        ArrayList<String> unlabeledSentences = new ArrayList<String>();
        ArrayList<String> unprocessedSentences = new ArrayList<String>();
        ArrayList<String> filteredSentences = new ArrayList<String>();
        ArrayList<String> filteredUnprocessedSentences = new ArrayList<String>();
        
        parseTree myParseTree = null;
        parser.parameters.EXTRACT_POSTAGS=false;
        boolean hasPostags = false;
        
        int totalNrSentences = 0;
        //compute wordFrequencies
        
		String myLine;
		String unlabeledSentence = "";
		//read the sentences (assuming they are unlabeled)
        BufferedReader buffPostags = new BufferedReader(new FileReader("./input/" + fileName));
        while ((myLine = buffPostags.readLine()) !=null){
        	if (!(myLine.startsWith("%"))) {
        		unprocessedSentences.add(myLine.trim());
        		
	        	totalNrSentences ++;
	        
	        	if (labeled) {
	        		myParseTree = episodicReranker.sentencePreprocessor.ExtractParseFromWSJText(myLine.trim(), false);
		            //you still need to remove nonTerminal nodes that have no children, recursively
		            myParseTree.removeEmptyNonTerminalNodes();
		            myParseTree.removeEmptyNonTerminalNodes();
		            myParseTree.removeEmptyNonTerminalNodes();
		            myParseTree.calculateNodeDepth();
		          
		            unlabeledSentence = myParseTree.getYieldString(hasPostags).trim();
	        		unlabeledSentences.add(unlabeledSentence);
	        	}
	        	else {
	        		unlabeledSentence = myLine.trim();
	        		unlabeledSentences.add(myLine.trim());
	        	}
	        	
	        	//skip duplicate sentences in word frequency count!
	            if (uniqueSentences.add(unlabeledSentence)) {
	                for (String myWord : unlabeledSentence.split(" ")) {
	                    if (wordFrequencies.get(myWord)==null) {
	                        wordFrequencies.put(myWord, new Integer(1));
	                    }
	                    else {
	                        wordFrequencies.put(myWord, new Integer(wordFrequencies.get(myWord).intValue() + 1));
	                    }
	                }
	            }
        	}
        }
        
	    System.out.println("Vocabulary size before filtering: " + wordFrequencies.size());
        //now check every sentence, and if 80% of words has freq of 3 or more put it into filteredSentencesArray
        //rhsOfSentence = new ArrayList<String>();
              
        int nrOfFrequentWords = 0;
        int nrOfSentencesWithFrequentWords = 0;
        
        int counter=0;
        for (String mySentence : unlabeledSentences) {
            //mySentence = mySentence.split("#")[0];
        	String[] mySentenceArray = mySentence.split("#")[0].split(" ");
            nrOfFrequentWords = 0;
            for (String myWord : mySentenceArray) {
                if (wordFrequencies.get(myWord).intValue()>=MINIMUM_WORD_FREQUENCY) nrOfFrequentWords++;
            }
            if ((double) nrOfFrequentWords /mySentenceArray.length > .80) {
            //if (nrOfFrequentWords == myUnLabeledSentence.length) {
            	filteredSentences.add(mySentence);
            	filteredUnprocessedSentences.add(unprocessedSentences.get(counter));
            	
                nrOfSentencesWithFrequentWords++;
                //compute word frequencies of filtered sentences
                for (String myWord : mySentenceArray) {
                    if (wordFrequenciesOfFilteredSentences.get(myWord)==null) {
                        wordFrequenciesOfFilteredSentences.put(myWord, new Integer(1));
                    }
                    else {
                        wordFrequenciesOfFilteredSentences.put(myWord, new Integer(wordFrequenciesOfFilteredSentences.get(myWord).intValue() + 1));
                    }
                }   //end compute word frequencies of filtered sentences
                
            }
            counter++;
        }
        
        //loop over filtered sentences, write to file, and check if 80% still holds
        System.out.println("There are " + nrOfSentencesWithFrequentWords + " out of " + totalNrSentences + " sentences with more than 80% words of frequency >=" + MINIMUM_WORD_FREQUENCY);
       
       //double check, count again after recomputing 
        nrOfSentencesWithFrequentWords = 0;
        
	        for (String mySentence : filteredSentences) { 		       
	           String[] mySentenceArray = mySentence.split("#")[0].split(" ");
	           nrOfFrequentWords = 0;
	            for (String myWord : mySentenceArray) {
	                if (wordFrequenciesOfFilteredSentences.get(myWord).intValue()>MINIMUM_WORD_FREQUENCY) nrOfFrequentWords++;
	            }
	            if ((double) nrOfFrequentWords /mySentenceArray.length > .80) nrOfSentencesWithFrequentWords++;
	            //if (nrOfFrequentWords == myUnLabeledSentence.length) nrOfSentencesWithFrequentWords++;
	        }
        
        
        System.out.println("Of those, " + nrOfSentencesWithFrequentWords + " sentences still have 80% or more frequent words after filtering.");
        System.out.println("Vocabulary size after filtering: " + wordFrequenciesOfFilteredSentences.size());
       //print filtered sentences to file
		BufferedWriter out_filtered_sentences = new BufferedWriter(new FileWriter("./input/" + fileName.substring(0, fileName.length()-4) + "_filtered.txt"));
		   
		for (String filtered_sentence : filteredUnprocessedSentences) {
			out_filtered_sentences.write(filtered_sentence);
			out_filtered_sentences.newLine();
		}
		
		out_filtered_sentences.flush();
		out_filtered_sentences.close();
		
		System.out.println("Done.");
    }
    
    
    public static String convertBindingsToTree(ArrayList<String> bindings) {
    
    	boolean feedBack = false;
    	/*
  
    	// begin met attach
    	bindings.add("VP*VT*NP~attach~RC*WHO*VP_2");
    	bindings.add("RC*WHO*VP~attach~NP*N*RC_2");
    	bindings.add("NP*N*RC~S*NP*VP_1");
    	bindings.add("S.1*NP*VP~walks");
    	bindings.add("walks~VI*walks_1");
    	bindings.add("VI*walks~VP*VI_1");
    	bindings.add("VP*VI~attach~S*NP*VP_2");
    	
    	
    	// +  +  +  +  +  +
    	bindings.add("feeds~VT*feeds_1");
    	bindings.add("VT*feeds~VP*VT*NP_1");
    	bindings.add("VP.1*VT*NP~boy");
    	bindings.add("boy~N*boy_1");
    	bindings.add("N*boy~NP*N_1");
    	bindings.add("NP*N~attach~VP*VT*NP_2");
    	bindings.add("VP*VT*NP~attach~S*NP*VP_2");
    	
    	
    	//begin met shift
    	bindings.add("S.1*NP*VP~walks");
    	bindings.add("walks~VI*walks_1");
    	bindings.add("VI*walks~VP*VI_1");
    	bindings.add("VP*VI~attach~S*NP*VP_2");
    	
    	
    	bindings.add("START.1~boy");
    	bindings.add("boy~N*boy_1");
    	bindings.add("N*boy~NP*N*RC_1");
    	bindings.add("NP.1*N*RC~who");
    	bindings.add("who~WHO*who_1");
    	bindings.add("WHO*who~RC*WHO*VP_1");
    	bindings.add("RC.1*WHO*VP~walks");
    	bindings.add("walks~VI*walks_1");
    	bindings.add("VI*walks~VP*VI_1");
    	bindings.add("VP*VI~attach~RC*WHO*VP_2");
    	bindings.add("RC*WHO*VP~attach~NP*N*RC_2");
    	bindings.add("NP*N*RC~S*NP*VP_1");
    	bindings.add("S.1*NP*VP~likes");
    	bindings.add("likes~VT*likes_1");
    	bindings.add("VT*likes~VP*VT*NP_1");
    	bindings.add("VP.1*VT*NP~girl");
    	bindings.add("girl~N*girl_1");
    	bindings.add("N*girl~NP*N_1");
    	bindings.add("NP*N~attach~VP*VT*NP_2");
    	bindings.add("VP*VT*NP~attach~S*NP*VP_2");
    	bindings.add("S*NP*VP~TOP*S_1");
    	*/
    	
    	//consider second entry of the binding (boundSlot)
    	ArrayList<String[]> constituents = new ArrayList<String[]>();
    	String parseTree =""; 
    	//boolean blnAttach = false, blnProject = false, blnShift = false;
    	int nrBinding=0;
    	
    	//handig
    	if (feedBack) {
    		for (String binding : bindings) System.out.println(binding);
    		System.out.println("**************");
    	}
    	for (String binding : bindings) {
    		
    		//blnAttach = false; blnProject = false; blnShift = false;
    		String thisProduction = binding.split("~")[0];
    		String nextProduction = binding.split("~")[1];
    		//String[] projectRule = "";
    		//determine whether it is project, attach or shift
    		if (nextProduction.equals("attach")) {
    			//blnAttach = true;
    			//attach means that the last constituentArray in AL constituents is filled (completed)
    			
    			if (nrBinding==0) {
    				//only for the first binding, the completed constit information is in boundNodeLabel
    				//create a completed constituent filled with attachedItem
    				String attachedItem = thisProduction;
    				if (thisProduction.contains("*")) {
    					String[] tempArray = thisProduction.split("\\*");
    					attachedItem = tempArray[0] + "@[A0] ";
    					for (int i=1; i< tempArray.length; i++) attachedItem += "(" + tempArray[i] + " )";
    					//attachedItem += ")";
    				}
    				String[] constituentArray = {attachedItem};
    				
					constituents.add(constituentArray);
					if (feedBack) System.out.println("ATTACH: added constituent " + attachedItem + " for first binding");	
    			}
    			//get the last entry
    			if (feedBack) System.out.println("nrBinding=" + nrBinding + "; constituents.size()=" + constituents.size());
				int lastIndexConstituents = constituents.size()-1;
				
				/*
				if (lastIndexConstituents < 0) {	////in case there is nothing to attach to
					String[] emptyConstit = {"---", "---", ""};	
					constituents.add(emptyConstit);
					lastIndexConstituents=0;
					System.out.println("ATTACH with 0 constit: added empty constituent");
				}
				*/
				//String[] leftCornerArray = constituents.get(lastIndexConstituents);
				
				//remove last constituentArray in constituents
				String[] completeConstit = constituents.remove(lastIndexConstituents);
				String attachingItem = "("; 
				int nrWord=0;
				for (String word : completeConstit) {
					if (nrWord==0) {
						if (nrBinding==0) attachingItem += word + " ";
						else attachingItem += word.trim() + "@[A" + nrBinding + "] ";
					}
					else {
						if  (word.startsWith("(")) attachingItem += word + " ";
						else attachingItem += "(" + word + " )";
					}
					nrWord++;
				}
				attachingItem += ")";

				if (feedBack) System.out.println("ATTACH: binding=" + binding + "; attachingItem=" + attachingItem + "; cstSize=" + constituents.size());
				
				//put attachingItem in first free []: the index is given as last
				if (constituents.size()>0) {
				String[] incompleteArray = constituents.get(lastIndexConstituents-1);
					for (int i=1; i<incompleteArray.length; i++ ) {
						if (incompleteArray[i].equals("")) {
							incompleteArray[i] = attachingItem;
							break;
						}
					}
				}
				
				else {
					//this means that the constit that is being attached to has not been introduced before
					//but it is available in the attach-info component of the binding (binding.split("~")[4])
					
					//bindingsOfLCDerivation.add(previousProduction + "~" + "attach" + "~" + goalCategPlusleftCorner + starredLeftSisterLabel);
				/////////shit: je wilt de attach production (dus dezelfde nextproduction die ook in project zit maar die is vervangen door "attach"
					String attachRecipient = binding.split("~")[4].split("_")[0];
					String[] attachConstitArray = attachRecipient.split("\\*");
					//parseTree = attachingItem;
					//String[] emptyConstit = {"-", "-", attachingItem + "(" + nrBinding + ")"};
				////////shit: klopt ook niet meer!
					//int attachedChildNr = java.lang.Integer.parseInt(binding.substring(binding.length()-1));
					int attachedChildNr = java.lang.Integer.parseInt(binding.split("~")[4].split("_")[1]);
					if (feedBack) System.out.println("attachedChildNr=" + attachedChildNr);
					attachConstitArray[attachedChildNr] = attachingItem;	//.substring(0, attachingItem.length()-1) + "@" + nrBinding + ")";
					constituents.add(attachConstitArray);	//emptyConstit
					//lastIndexConstituents=0;
					if (feedBack) System.out.println("ATTACH with 0 constit: added empty constituent");
				}
    		}	//if (boundSlot.equals("attach"))
    		else {	//distinguish between project and shift
    			if (binding.split("~")[4].equals("project")) {
    			//if (!nextProduction.contains("^")) {	//starred nonterminal contains ^. was: if (nextProduction.contains("_")) {
    				//blnProject = true;
    				//the current constituent is completed: rewrite the Array as a String, surround by brackets,
    				//and put the String in the second index of the Array (parent is first index
    				//if it is the first binding, then add fake shift-rule
    				if (nrBinding==0) {
    					//only for the first binding, the completed constit information is in boundNodeLabel
        				String[] constituentArray = {thisProduction};
        				if (thisProduction.contains("*")) constituentArray = thisProduction.split("\\*");
    					//String[] constituentArray= new String[3];
    					//constituentArray[0] = "---"; constituentArray[1] = "---";
    					//constituentArray[2] = "";
    					constituents.add(constituentArray);
    					if (feedBack) System.out.println("PROJECT: added constituent " + thisProduction + " for first binding");
        			}
    				String[] projectRule = nextProduction.split("_")[0].split("\\*");
    				//project means that the last constituentArray in AL constituents is filled (completed)!
    				//replace last entry of constituents by new one, where first [] is parent, second [] is first child
    				//which is filled with previous entry surrounded by ( ) 
    				
    				//get the last entry
    				int lastIndexConstituents = constituents.size()-1;
    				/*
    				if (lastIndexConstituents < 0) {	////in case there is nothing to attach to
    					String[] emptyConstit = {"---", "---", ""};	
    					constituents.add(emptyConstit);
    					lastIndexConstituents=0;
    					if (feedBack) System.out.println("PROJECT with 0 constit: added empty constituent");
    				}
    				*/
    				String[] leftCornerArray = constituents.get(lastIndexConstituents);
    				String projectingItem = "("; 
    				int nrWord =0;
    				for (String word : leftCornerArray) {
    					if (nrWord==0) {
    						//add nrBinding, but not if this projection from word, because then it has already been added
    						if (word.endsWith(")")) projectingItem += word + " ";
    						else projectingItem += word.trim() + "@[P" + nrBinding + "] ";
    					}
    					else {
    						if  (word.startsWith("(")) projectingItem += word + " ";
    						else projectingItem += "(" + word + " )";
    					}
    					
						nrWord++;
    				}
    				projectingItem += ")";	//projectingItem.trim()
    				if (feedBack) System.out.println("PROJECT: binding=" + binding + "; projectingItem=" + projectingItem + "; cstSize=" + constituents.size());
    				//replace second [] in projectRule by projectedThing
    				projectRule[1] = projectingItem;
    				//empty the other entries
    				for (int i=2; i<projectRule.length; i++ ) projectRule[i] ="";
    				//replace last constituent
    				constituents.set(lastIndexConstituents, projectRule);
    			}	//if (boundSlot.contains("_"))
    			else {
    				//blnShift = true;
    				//add a new String[] of one element to the ArrayList of constituents, and fill it with boundSlot
    				String[] constituentArray= new String[1];
    				//boundSlot is the terminal
    				//add an extra node for the shifting starred nonT
    				//shiftRule is of the form: String shiftRuleProduction = leftmostNodeProduction + "*" + starredNonterminal; //parent first
    				
    				String starredNonT = thisProduction.split("\\*")[0];
    				//replace number by stars, for example S.2 becomes S**
    				//System.out.println("SHIFT: binding=" + binding + "; thisProduction=" + thisProduction + "; starredNonT=" + starredNonT);
    				int nrStars = java.lang.Integer.parseInt(starredNonT.split("\\^")[1]);
    				starredNonT = starredNonT.split("\\^")[0];
    				for (int i=0; i<nrStars; i++) starredNonT +="*";
    				//add nrBinding for projection from word, and for shift to word
    				constituentArray[0] = nextProduction.trim() + "@[P" + (nrBinding+1) + "] (" + starredNonT + "@[S" + nrBinding + "] )";
    				constituents.add(constituentArray);
    				if (feedBack) System.out.println("SHIFT: binding=" + binding + "; shiftItem=" + nextProduction + "; starredNonT=" + starredNonT + "; cstSize=" + constituents.size());
    			}
    		}	//not if (boundSlot.equals("attach"))
    		nrBinding++;
    	}	//for (String binding : bindings) 
    	
    	//now connect remaining elements of constituent through attach, and link by ...
    	for (int i = constituents.size()-1; i>=0; i--) {
    		//int lastIndexConstituents = constituents.size()-1;
			String[] leftCornerArray = constituents.get(i);
			String attachingItem = "(";
			if (i>0) attachingItem = "(=== ("; 
			int nrWord=0;
			for (String word : leftCornerArray) {
				if (nrWord==0) attachingItem += word.trim() + " "; // + "@[A" + nrBinding + "] "
				else {
					if  (word.startsWith("(")) attachingItem += word + " ";
					else attachingItem += "(" + word + " )";
				}
				nrWord++;
			}
			attachingItem += ")";
			if (i>0) attachingItem += " )";
			//remove last constituentArray in constituents
			//constituents.remove(lastIndexConstituents);
			//System.out.println("ATTACH: binding=" + binding + "; attachingItem=" + attachingItem + "; cstSize=" + constituents.size());
			
			//put attachingItem in first free []: the index is given as last
			if (i>0) {
			String[] incompleteArray = constituents.get(i-1);
				for (int j=1; j<incompleteArray.length; j++ ) {
					if (incompleteArray[j].equals("")) {
						incompleteArray[j] = attachingItem;
						break;
					}
				}
			}
			else parseTree = attachingItem;
    	}
    	return parseTree;
    }
    
    public static void openLatexDoc(BufferedWriter treeFile, boolean includeQtree)  throws Exception {
        treeFile.write("\\documentclass[10pt]{article}"); treeFile.newLine();
        
        treeFile.write("\\oddsidemargin 0in"); treeFile.newLine();
        treeFile.write("\\evensidemargin 0in"); treeFile.newLine();
        
        treeFile.write("\\usepackage[latin2]{inputenc}"); treeFile.newLine();
        treeFile.write("\\usepackage{../../../Latex/styles/graphicx}"); treeFile.newLine();
        treeFile.write("\\usepackage[T1]{fontenc}"); treeFile.newLine();
        if (includeQtree)
        	treeFile.write("\\usepackage{../../../Latex/styles/qtree}"); treeFile.newLine();
        treeFile.write("\\usepackage{../../../Latex/styles/longtable}"); treeFile.newLine();
        treeFile.write("\\topmargin -1.5cm"); treeFile.newLine();

        treeFile.write("\\begin{document}"); treeFile.newLine();
	}
	
    public static void openLatexDoc2(BufferedWriter treeFile, boolean includeQtree)  throws IOException {
        treeFile.write("\\documentclass[10pt]{article}"); treeFile.newLine();
        
        treeFile.write("\\oddsidemargin 0in"); treeFile.newLine();
        treeFile.write("\\evensidemargin 0in"); treeFile.newLine();
        
        treeFile.write("\\usepackage[latin2]{inputenc}"); treeFile.newLine();
        treeFile.write("\\usepackage{./graphicx}"); treeFile.newLine();
        //treeFile.write("\\usepackage{./amssymb}"); treeFile.newLine();
        //treeFile.write("\\usepackage{./amsthm}"); treeFile.newLine();
       
        if (includeQtree)
        	treeFile.write("\\usepackage{./parsetree}"); treeFile.newLine();
      
        treeFile.write("\\usepackage[T1]{fontenc}"); treeFile.newLine();
       
        //treeFile.write("\\topmargin -1.5cm"); treeFile.newLine();

        treeFile.write("\\begin{document}"); treeFile.newLine();
	}
    
    public static void printCloseLatexTable(BufferedWriter treeFile, String strCaption, String label)  throws Exception {
        treeFile.write("\\hline"); treeFile.newLine();
        //treeFile.write("\\end{tabular}"); treeFile.newLine();
        //treeFile.write("\\end{minipage}}"); treeFile.newLine();
        treeFile.write("\\caption{" + strCaption + "}"); treeFile.newLine();
        treeFile.write("\\label{" + label + "}"); treeFile.newLine();
        treeFile.write("\\end{longtable}"); treeFile.newLine();
	 
	}
	 
    public static void printCloseLatexTable2(BufferedWriter treeFile, String strCaption, String label)  throws Exception {
        treeFile.write("\\hline"); treeFile.newLine();
        treeFile.write("\\end{tabular}}"); treeFile.newLine();
        
        treeFile.write("\\caption{" + strCaption + "}"); treeFile.newLine();
        treeFile.write("\\label{" + label + "}"); treeFile.newLine();
        treeFile.write("\\end{table}"); treeFile.newLine();
	 
	}
    
    public static String doCreateWSJParseFromSpans(String lexicalSentence, String postagSequence, String sentenceSpan) throws IOException {

		//System.out.println("sentenceSpan=" + sentenceSpan + "; lexicalSentence=" + lexicalSentence + "; postagSequence=" + postagSequence);
		/*0) lees unlabeled en spans
        1) voeg TOP toe, en preterminals
        2) maak arraylist van unieke spans
        3) tel linker- (uit leftSpan) en rechterhaken op elke positie: dwz 2XArrayList<Integer> met #haken voor elke word position 
        4) maak zin , voeg haken in op word positions ; spaties tussen l en r-haak
        na elke l-haak (behalve laatste) moet je naam voor nonT verzinnen, maakt niet uit wat, gevolgd door spatie
        bijv (CHNK~1126 (MRG~1930 (_DT (dt )) 
        alleen na l-haak die voor preterminal/woord staat vul je gewone woord in gevolgd door spatie, maw altijd na laatste l-haak

		 */
		//System.out.println(postagSequence + " spans:" + sentenceSpan);
		//note, that this includes the . at end
		String sentence = lexicalSentence.trim();

		String[] wordsOfSentence = sentence.split(" ");   
		System.out.println("sentence=" + sentence + "; size=" + wordsOfSentence.length);
		String[] posTags = new String[wordsOfSentence.length];
        if (!(postagSequence.equals(""))) posTags = postagSequence.split(" ");
        else  for (int i=0; i<posTags.length; i++) posTags[i]="pt";
        
		//spanSet is set of unique spans plus labels, e.g. 1-3:JCT
		HashSet<String> spanSet = null;
		String[] spanArray;
		//leftSpans and righSpans contain number of brackets; index corresponds to position in the sentence
		//leftSpans.set(leftSpanIndex, leftSpans.get(leftSpanIndex)+1);
		ArrayList<Integer> leftSpans = new ArrayList<Integer>(); 
		ArrayList<Integer> rightSpans = new ArrayList<Integer>();
		//leftSpans stores for every position in the sentence labels, separated by @
		//their order is determined by the associated right span
		ArrayList<String> leftSpanLabels = new ArrayList<String>();
		//ArrayList<String> reOrderedLeftSpanLabels = null;
		int leftSpanIndex, rightSpanIndex;

		
		//trim spaces off the end
		sentenceSpan = sentenceSpan.trim();

		//System.out.println("Sentence " + myCounter + ": " + sentence + "; >>" + sentenceSpan + "<<" + "; nrWords=" + wordsOfSentence.length);
		//put spans into HashSet spanSet

		spanSet = new HashSet<String>();
		if (!sentenceSpan.equals("")) {
			//if (sentenceSpan.contains(" ")) {
			spanArray = sentenceSpan.split(" ");
			for (int i =0; i< spanArray.length; i++) {
				//System.out.println(i);
				spanSet.add(spanArray[i]);
			}
		}

		//create arrays and fill with zeros
		for (int i =0; i<= wordsOfSentence.length; i++) {
			leftSpans.add(0); 
			leftSpanLabels.add("");
			rightSpans.add(0); 
		}
		//compute for every position left and right brackets

		for (String mySpan : spanSet) { 
			//get left bracket of span
			leftSpanIndex = java.lang.Integer.parseInt(mySpan.split(":")[0].split("-")[0]);
			//System.out.println("mySpan=" + mySpan + "; split=" + mySpan.split(":")[0] + "<<");
			rightSpanIndex = java.lang.Integer.parseInt(mySpan.split(":")[0].split("-")[1]);
			//System.out.println("leftSpanIndex=" + leftSpanIndex + "; rightSpanIndex=" +rightSpanIndex); 
			//increase number of brackets at that position by 1;     
			leftSpans.set(leftSpanIndex, leftSpans.get(leftSpanIndex)+1);
			//leftSpanLabels.set(leftSpanIndex, leftSpanLabels.get(leftSpanIndex)+ "#" + mySpan.split(":")[1]);

			leftSpanLabels.set(leftSpanIndex, leftSpanLabels.get(leftSpanIndex)+ "#" + mySpan);
			//rightSpans stores for every position in the sentence number of brackets
			rightSpans.set(rightSpanIndex, rightSpans.get(rightSpanIndex)+1); 
		}

		//sort the leftSpanLabels at a certain position according to rightSpan, and strip off the numbers
		//leftSpanLabels.set(leftSpanIndex, leftSpanLabels.get(leftSpanIndex)+ "#" + mySpan.split(":")[1]);
		int myIndex=0;
		for (String leftSpanLabel : leftSpanLabels) {
			if (leftSpanLabel.split("#").length>2) {
				//System.out.println("leftSpanLabel=" + leftSpanLabel);
				//sort it according to rightSpan: use TreeMap
				TreeMap<Double, String> mySortedSpans = new TreeMap<Double, String>();
				for (String mySpan : leftSpanLabel.split("#")) {
					if (!mySpan.equals("")) {
						double rightSpan= java.lang.Double.parseDouble(mySpan.split(":")[0].split("-")[1]);
						//in case of unary productions there might be more than one with the same left and right span
						while (!(mySortedSpans.get(rightSpan)==null)) rightSpan += 0.01;
						mySortedSpans.put(rightSpan, mySpan.split(":")[1]);  
					}
				}
				//now put them back together other way round of TreeMap

				StringBuffer leftSpanBuffer = new StringBuffer();
				for (Double myRightSpan : mySortedSpans.keySet()) {
					leftSpanBuffer.insert(0, "#" + mySortedSpans.get(myRightSpan));
				}
				//leftSpanLabel = leftSpanBuffer.toString();
				leftSpanLabels.set(myIndex, leftSpanBuffer.toString());
				//System.out.println("Reordered leftSpanLabel=" + leftSpanLabel);
			}
			else {
				//
				if (!leftSpanLabel.equals("")) {
					//leftSpanLabel = "#" + leftSpanLabel.split(":")[1];
					leftSpanLabels.set(myIndex, "#" + leftSpanLabel.split(":")[1]);
				}
			}
			myIndex++;
		}
		//test
		//for (int i =0; i< wordsOfSentence.length; i++) {
		//   System.out.println("position " + i + ": leftSpans=" + leftSpans.get(i) + "; rightSpans=" + rightSpans.get(i) + "; label=" + leftSpanLabels.get(i)); 
		//}

		//create the parse, inserting brackets at the correct positions
		//4) maak zin , voeg haken in op word positions ; spaties tussen l en r-haak
		//na elke l-haak (behalve laatste) moet je naam voor nonT verzinnen, maakt niet uit wat, gevolgd door spatie
		//bijv (CHNK~1126 (MRG~1930 (_DT (dt )) 
		//alleen na l-haak die voor preterminal/woord staat vul je gewone woord in gevolgd door spatie, maw altijd na laatste l-haak

		StringBuffer WSJParse = new StringBuffer();

		//keep count of the nr of occurrences of this word: in HashMap, and number it accordingly
		String preTerminal = null, nonTerminal=null, suffix="";

		//wordsOfSentence.length-1 because of . at end
		for (int wordPosition =0; wordPosition< wordsOfSentence.length; wordPosition++) {
			if (leftSpans.get(wordPosition).intValue()>0)  { 
				//write a ( with the label
				//System.out.println("sentence=" + sentence + "; wordPosition=" +wordPosition + "; #leftspans=" + leftSpans.get(wordPosition).intValue() + "; labels=" + leftSpanLabels.get(wordPosition));
				for (int i=0; i<leftSpans.get(wordPosition).intValue(); i++) {

					nonTerminal = leftSpanLabels.get(wordPosition).split("#")[i+1];
					suffix = "";

					if (nonTerminal.equals("TOP")) nonTerminal = "GRTOP";


					WSJParse.append("(" + nonTerminal + suffix + " ");
				}
			}
			//write one bracket before the word in any case, and one bracket after
			//preTerminal = wordsOfSentence[wordPosition].toUpperCase();
			preTerminal = posTags[wordPosition].toUpperCase();

			//if( WSJ_OUTPUT_FOR_PNP) {	//no brackets arount terminals!!!
			//System.out.println("x=(" + preTerminal + " " + wordsOfSentence[wordPosition] + ")");
			WSJParse.append("(" + preTerminal + " " + wordsOfSentence[wordPosition] + ")");
			//}
			//WSJParse.append("(" + preTerminal + suffix + " " + "(" + wordsOfSentence[wordPosition] + " ))");
			
			//right spans, NB look at position behind the word!!!
			if (rightSpans.get(wordPosition+1).intValue()>0)  { 
				//write a ( with a random non-terminal
				for (int i=0; i<rightSpans.get(wordPosition+1).intValue(); i++) {
					WSJParse.append(")");
				}
			}
			WSJParse.append(" ");
		}   //for (int wordPosition =0; wordPosition< wordsOfSentence.length-1; wordPosition++) {

		String WSJParse_final = WSJParse.toString();


		//System.out.println("WSJParse_final = " + WSJParse_final);
		return WSJParse_final;
	}
	public static ArrayList<String> sortWordsAccordingToFrequency(HashMap<String, Integer> wordCounts) {
		   
		   //  create a TreeMapthat is automatically ordered according to the weights of the s.c.
		   //and make the value of the treemap equal to the s.c. index
		   
		   TreeMap<Double, String> sortedWordsPlusIndices = new TreeMap(Collections.reverseOrder());
	    	
		   ArrayList<String> sortedWords = new ArrayList<String>();
		   
		   double noise = 0.0001;
		   for (String word : wordCounts.keySet()) {
			   sortedWordsPlusIndices.put(((double) wordCounts.get(word) + noise), word);
			   noise = noise + 0.0001;
		   }
		   
		   for (Double sortedSC : sortedWordsPlusIndices.keySet()) {
			   sortedWords.add(sortedWordsPlusIndices.get(sortedSC));
		   }
		   return sortedWords;
	   }
	
	public static void printLatexTreeFile(ArrayList<parseTree> parseTrees, String TREE_FILE) throws Exception {
        
	       //dopparser.Printer.printSentences(latexSentences, "WSJ_latex.txt");
			boolean printStructureETNs = false;
			
			BufferedWriter treeFile = new BufferedWriter(new FileWriter(TREE_FILE));
		            
		    openLatexDoc(treeFile, true);
		       
			for (parseTree aParseTree : parseTrees) {
	        	//latexSentences.add(aParseTree.printToLatex(false, true, ""));
				//printToLatex(boolean blnPrintNrSubtrees, boolean blnPrintNodeSpans, String strText){
				
				String latexTree = aParseTree.printToLatexParseTreeSty("", false, printStructureETNs);
				//was: printToLatex
		        latexTree = replaceSpecialSymbols(latexTree);

		        //was: treeFile.write("\\scalebox{.4}{\\Tree" + latexTree + "}");
		        treeFile.write("\\begin{parsetree} " + latexTree + " \\end{parsetree}");
		        treeFile.newLine();
		        treeFile.newLine();
	        }
			
			treeFile.flush();
			treeFile.close();
	}
	
public static String replaceSpecialSymbols(String myString) {
    
    //constructionWSJ = 
   /*replacements:
    & door /&
    ROTOP door TOP, _ weghalen
    @ door *
    ^door @
    */
   myString = myString.replace("&", "-");
   myString = myString.replace("%", "\\%");
   myString = myString.replace("ROTOP", "TOP");
   //myString = myString.replace("_", "");
   //myString = myString.replace("@", "*");
   //myString = myString.replace("^", "@");
   return myString;
    }

}
