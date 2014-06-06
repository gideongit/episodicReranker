package episodicReranker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import parser.Node;
import parser.parseTree;


public class reranker {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {

		//evaluateSingleBestCharniakParses();
		
		//evaluateNBestCharniakParses(1);
		String lcOrtd = "lc";
		int nBest=5;
		String rankNrFile = "C:/Users/Owner/Desktop/PARSEVALrecalculaties/" + lcOrtd + "NBest" + nBest + "/" + lcOrtd + "NBest" + nBest + ".csv";
		String sentenceNrFile = "C:/Users/Owner/Desktop/PARSEVALrecalculaties/" + lcOrtd + "NBest" + nBest + "/sentenceNrs1576.txt";
		//String rankNrFile = "./output/rankNrs_gt40.txt";
		//String sentenceNrFile = "./output/sentenceNrs_gt40.txt";
		String nBestFile = "./input/WSJ22Charniak_best10_noAux.txt"; // WSJ22Charniak_best10_noAux.txt // WSJ22Charniak_best20f
		String goldStandardFile = "./input/wsj-22clean.mrg"; //wsj-22clean.mrg //wsj-22clean_forNBest20.txt
		int nrCandidatesInNBestFile=10;
		
		findSelectedParsesFromRankNrFile(rankNrFile, sentenceNrFile, nBestFile, goldStandardFile, nrCandidatesInNBestFile, nBest, lcOrtd);
		System.out.println("Done.");
		
		
	}

	/**
	 * Determines the F-scores against Gold standard from WSJ22
	 * if the single best Charniak parse would be selected
	 * @throws IOException
	 */
	public static void evaluateSingleBestCharniakParses()   throws IOException {
		
		ArrayList<ArrayList<parseTree>> bestCharniakParses = readNBestParsesOfCharniak("./input/wsj-22_1000best_noAux_cleaned.mrg", 1, true);

		//put them in single array:
		ArrayList<parseTree> oneBestCharniakParses = new ArrayList<parseTree>();
		for (ArrayList<parseTree> singleParseArray : bestCharniakParses) {
			oneBestCharniakParses.add(singleParseArray.get(0));
		}
		
		//read Gold standard test sentences
		String goldStandardTestFile = "./input/WSJ22test_full_GOLD_STANDARD.txt";
		
		ArrayList<parseTree> goldStandardParses = readGoldStandard(goldStandardTestFile);
	    
		doLabeledPrecisionAndRecall(oneBestCharniakParses, goldStandardParses, true);
	}

	public static void findSelectedParsesFromRankNrFile(String rankNrFile, String sentenceNrFile, String nBestFile, String goldStandardFile, int nrCandidatesInNBestFile, int nBest, String lcOrtd)  throws IOException {
		//pick all sentences listed in sentenceNrFile from goldStandard, and write to file new goldStandard
		
		ArrayList<String> goldStandard = readFileIntoArray(goldStandardFile);
		ArrayList<String> nBestCandidateParses = readFileIntoArray(nBestFile);
		ArrayList<String> sentenceNrsOfTestParses = readFileIntoArray(sentenceNrFile);
		ArrayList<String> rankNrs = null;
		ArrayList<String>[] rankNrsForAllHistories = null;
		
		ArrayList<String> headers = new ArrayList<String>();
		
		boolean blnSINGLE_FILE = false;
		
		if (blnSINGLE_FILE) rankNrs = readFileIntoArray(rankNrFile);
		else rankNrsForAllHistories = readCSVFileIntoArray(rankNrFile, headers);
		
		//create new goldStandard of selected sentences
		ArrayList<String> newGoldStandard = new ArrayList<String>(); 
		for (String nrString : sentenceNrsOfTestParses) {
			int sentenceNr = java.lang.Integer.parseInt(nrString);
			newGoldStandard.add(goldStandard.get(sentenceNr));
		}
		
		//print goldStandard
		//printArrayToFile(newGoldStandard, "./output/newGoldStandard.txt");
		printArrayToFile(newGoldStandard, "C:/Users/Owner/Desktop/PARSEVALrecalculaties/" + lcOrtd + "NBest" + nBest + "/newGoldStandard.txt");
		
		if (blnSINGLE_FILE) {
			//create ArrayList of selected parses
			ArrayList<String> selectedParses = new ArrayList<String>(); 
			int counter=0;
			for (String nrString : sentenceNrsOfTestParses) {
				
				int sentenceNr = java.lang.Integer.parseInt(nrString);
				int rankNr = java.lang.Integer.parseInt(rankNrs.get(counter));
				
				//the test parse = (nBest+1)*sentenceNr + candidateNr + 1
				int selectedNr = (nrCandidatesInNBestFile +1)*sentenceNr + rankNr;
				selectedParses.add(nBestCandidateParses.get(selectedNr));
				counter++;
			}
			//print testParses
			printArrayToFile(selectedParses, "./output/testParses.txt");
			
			
		}	//blnSINGLE_FILE
		else {	//multiple histories combined in csv file
			
			for (int his = 0; his< rankNrsForAllHistories.length; his++) {
				
				String header = headers.get(his);
				//create ArrayList of selected parses
				ArrayList<String> selectedParses = new ArrayList<String>(); 
				int counter=0;
				for (String nrString : sentenceNrsOfTestParses) {
					
					int sentenceNr = java.lang.Integer.parseInt(nrString);
					int rankNr = java.lang.Integer.parseInt(rankNrsForAllHistories[his].get(counter));
					
					//the test parse = (nBest+1)*sentenceNr + candidateNr + 1
					int selectedNr = (nrCandidatesInNBestFile +1)*sentenceNr + rankNr;
					selectedParses.add(nBestCandidateParses.get(selectedNr));
					counter++;
				}
				//print testParses
				printArrayToFile(selectedParses, "C:/Users/Owner/Desktop/PARSEVALrecalculaties/" + lcOrtd + "NBest" + nBest + "/testParses_NBest" + nBest + "_" + header  + ".txt");
			}
		}

	}
	
	/**
	 * Determines the highest possible F-scores from nBest parses, using an oracle
	 * (independently of likelihood, or reranker).
	 * 
	 * @param nBest
	 * @throws IOException
	 */
	public static void evaluateNBestCharniakParses(int nBest)   throws IOException {
		
		//ArrayList<ArrayList<parseTree>> bestCharniakParses = readNBestParsesOfCharniak("./input/wsj-22_chiarniak_parsed1000_clean.mrg", nBest, true);
		ArrayList<ArrayList<parseTree>> bestCharniakParses = readNBestParsesOfCharniak("./input/wsj-22_1000best_noAux_cleaned.mrg", nBest, true);
		
		//read Gold standard test sentences
		String goldStandardTestFile = "./input/wsj-22clean.mrg";
		
		ArrayList<parseTree> goldStandardParses = readGoldStandard(goldStandardTestFile);
	  
		//for  every nBest (10) candidate parses of the same sentence find the one with best F-score
		//and you also want to determine precision and recall if you choose one parse at random
		
		
		
		//put them in single array:
		ArrayList<parseTree> bestFScoreParses = new ArrayList<parseTree>();
		ArrayList<parseTree> randomParses = new ArrayList<parseTree>();
		ArrayList<parseTree> firstCharniakParses = new ArrayList<parseTree>();
		ArrayList<parseTree> lastCharniakParses = new ArrayList<parseTree>();
		
		double Fscore=0d, maxFscore = 0d;
		String strLPLRFScores = "";
		
		int sentenceCounter = 0;
		System.out.println("bestCharniakParses.s=" + bestCharniakParses.size());
		for (ArrayList<parseTree> bestNParsesArray : bestCharniakParses) {
			System.out.println("sent. " + sentenceCounter + "; bestNParsesArray.s=" + bestNParsesArray.size());
			System.out.println(bestNParsesArray.get(0).printWSJFormat());
			//candidate with best Fscore: 
			maxFscore = 0.;
			int counter=0, indexOfBestFScoreParse=-1;
			//loop over nBest candidate parses of the same sentence 
			for (parseTree myParseTree : bestNParsesArray) {
				strLPLRFScores = doLabeledPrecisionAndRecallForSingleSentence(myParseTree, goldStandardParses.get(sentenceCounter), "", false);
				Fscore = java.lang.Double.parseDouble(strLPLRFScores.split("#")[2]);
				//System.out.println("counter=" + counter + "; Fscore=" + Fscore);
				if (Fscore>maxFscore) {
					indexOfBestFScoreParse = counter;
					maxFscore = Fscore;
				}
				counter++;
			}
			if (!(java.lang.Double.isNaN(Fscore))) 	{	//avoid single word sentences 
				bestFScoreParses.add(bestNParsesArray.get(indexOfBestFScoreParse));
			}
			else bestFScoreParses.add(bestNParsesArray.get(0));
				////remove from Gold standard
				//goldStandardParses.remove(sentenceCounter);

			//random parse from nBest
			int randomInteger = (int)(Math.random() * nBest);
			randomParses.add(bestNParsesArray.get(randomInteger));
			
			firstCharniakParses.add(bestNParsesArray.get(0));
			
			lastCharniakParses.add(bestNParsesArray.get(nBest-1));
			sentenceCounter++;
		}
		
		System.out.println("Optimal LP/LR for best Fscore candidate from nBest:"); 
		doLabeledPrecisionAndRecall(bestFScoreParses, goldStandardParses, false);
		
		System.out.println("LP/LR for random candidate from nBest");
		doLabeledPrecisionAndRecall(randomParses, goldStandardParses, false);
		
		System.out.println("LP/LR for first candidate from nBest");
		doLabeledPrecisionAndRecall(firstCharniakParses, goldStandardParses, false);
			
		System.out.println("LP/LR for last candidate from nBest");
		doLabeledPrecisionAndRecall(lastCharniakParses, goldStandardParses, false);
	}

	public static ArrayList<parseTree> readGoldStandard(String goldStandardTestFile) throws FileNotFoundException,
			IOException {
		ArrayList<parseTree> goldStandardParses = new ArrayList<parseTree>();
		
		BufferedReader buff = new BufferedReader(new FileReader(goldStandardTestFile));
		String mySentence=null;
		int myCounter=0;
		parseTree goldStandardParseTree = null;
		
	    while (((mySentence = buff.readLine()) !=null) ) {
	    	if (myCounter>=Main.FIRSTTESTSENTENCE && myCounter<Main.LASTTESTSENTENCE) {
	    	    
	            mySentence = mySentence.trim();
	             
	            goldStandardParseTree = sentencePreprocessor.ExtractParseFromWSJText(mySentence, false);
	            
	            goldStandardParseTree.removeEmptyNonTerminalNodes();
	            goldStandardParseTree.removeEmptyNonTerminalNodes();
	            goldStandardParseTree.removeEmptyNonTerminalNodes();
			    
			    goldStandardParseTree.calculateNodeDepth();
	            goldStandardParseTree.calculateNodeSpans();
	            goldStandardParses.add(goldStandardParseTree);
			    
	            
		    }
	    	myCounter++;
        }
		return goldStandardParses;
	}
	
	/**
	 * 
	 * @param treebankFile
	 * @param nBest
	 * @param blnWriteNBestToFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ArrayList<parseTree>> readNBestParsesOfCharniak(String treebankFile, int nBest, boolean blnWriteNBestToFile) throws IOException {
		
		ArrayList<ArrayList<parseTree>> bestCharniakParses = new ArrayList<ArrayList<parseTree>> ();
		
		parser.parameters.TAKE_OFF_SBJ = true;
		parser.parameters.READOFF_GRAMMAR_FROM_TREEBANK = true;
		parser.parameters.TREEBANK_FILE = treebankFile;  
		
		parser.parameters.BRACKET_FORMAT_WSJ_STYLE = true;
		parser.parameters.EXTRACT_POSTAGS = false;
		
		 int myCounter = 0;
	     String mySentence;
	     parseTree myParseTree = null;
	     int uniqueSentenceCounter = 0, counterOfSameSentences=0;
	     ArrayList<String> copyOfFirstCharniakParses = new  ArrayList<String>();
	       
		BufferedReader buffCharniakBest = new BufferedReader(new FileReader(treebankFile));
		
			
	    while (((mySentence = buffCharniakBest.readLine()) !=null) && uniqueSentenceCounter<Main.LASTTESTSENTENCE) {
	    	if (!(mySentence.equals(""))) {
	            myCounter++; 
	            counterOfSameSentences++;
	            if (counterOfSameSentences <= nBest) {
	            	
	            	if (uniqueSentenceCounter>=Main.FIRSTTESTSENTENCE) {
	            	copyOfFirstCharniakParses.add(mySentence);
	            	
		            mySentence = mySentence.trim();
		             
		            myParseTree = sentencePreprocessor.ExtractParseFromWSJText(mySentence, false);
		            //you still need to remove nonTerminal nodes that have no children, recursively
		            myParseTree.removeEmptyNonTerminalNodes();
		            myParseTree.removeEmptyNonTerminalNodes();
		            myParseTree.calculateNodeDepth();
		            
		            myParseTree.calculateNodeSpans();
		    		
		            ArrayList<parseTree> nBestParseTreesOfSameSentence = null;
		            if (counterOfSameSentences ==1) {
		            	nBestParseTreesOfSameSentence = new ArrayList<parseTree>();
		            	bestCharniakParses.add(nBestParseTreesOfSameSentence);
		            }
		            else nBestParseTreesOfSameSentence = bestCharniakParses.get(bestCharniakParses.size()-1);
		            
		            nBestParseTreesOfSameSentence.add(myParseTree);
	            }
	            }	//if (nrOfSameSentences <= nBest)
	    	}
	    	else {
	    		uniqueSentenceCounter++;
	    		counterOfSameSentences=0;
	    		if (uniqueSentenceCounter%50==0) System.out.println("##############################   " + uniqueSentenceCounter + " different sentences processed   ##############################");
	            
	    	}
	    }
	    
		//print the nBest to file
	    if (blnWriteNBestToFile) {
		    BufferedWriter nBestCharniakParsesFile = new BufferedWriter(new FileWriter("./input/WSJ22Charniak_best1.txt"));
			
		    /*
	    	for (ArrayList<parseTree> nBestParseTreesOfSameSentence : bestCharniakParses) {
	    		for (parseTree bestParseTree : nBestParseTreesOfSameSentence) {
	    			nBestCharniakParsesFile.write(bestParseTree.printWSJFormat()); nBestCharniakParsesFile.newLine();
	    		}
	    		nBestCharniakParsesFile.write(""); nBestCharniakParsesFile.newLine();
	    	}
		    */
		    
		    myCounter=1;
		    for (String nBestParse : copyOfFirstCharniakParses) {
		    	nBestCharniakParsesFile.write(nBestParse); nBestCharniakParsesFile.newLine();
		    	if (myCounter%nBest==0) {
		    		nBestCharniakParsesFile.write(""); nBestCharniakParsesFile.newLine(); 
		    		}
		    	myCounter++;
		    }

	    	nBestCharniakParsesFile.flush();
	    	nBestCharniakParsesFile.close();
	    }
		return bestCharniakParses;
	}
	
	public static ArrayList<String> readFileIntoArray(String myFile) throws FileNotFoundException,
			IOException {
		ArrayList<String> mySentences = new ArrayList<String>();
		
		BufferedReader buff = new BufferedReader(new FileReader(myFile));
		String mySentence=null;
		int myCounter=0;
		
	    while (((mySentence = buff.readLine()) !=null) ) {
	    	    
	        mySentences.add(mySentence.trim());

	    	myCounter++;
        }
		return mySentences;
		
	}
	
	
	
	public static ArrayList<String>[] readCSVFileIntoArray(String myFile, ArrayList<String> headers) throws FileNotFoundException, IOException {
		
		ArrayList<String>[] rankNrsForAllHistories = new ArrayList[17];
		
		for (int j = 0; j<17; j++) {
			//chart[j] = new HashMap<state, chartCellInfo>();
			rankNrsForAllHistories[j] = new ArrayList<String>();
		}
		
		BufferedReader buff = new BufferedReader(new FileReader(myFile));
		String mySentence=null;
		int myCounter=0;
		
		while (((mySentence = buff.readLine()) !=null) ) {
			
			String[] columns = mySentence.trim().split(";");
			if (myCounter==0) 
				for (int i=0; i<columns.length; i++) headers.add(columns[i]);
			else {
				for (int i=0; i<columns.length; i++) 
					rankNrsForAllHistories[i].add(columns[i]);
			}
		   
			myCounter++;
		}
		return rankNrsForAllHistories;

	}
	
	
	public static void printArrayToFile(ArrayList<String> myArray, String myFile) throws FileNotFoundException,
	IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(myFile));
		
	    for (String myLine : myArray) {
	    	outFile.write(myLine); outFile.newLine();
	     	
	    }

    	outFile.flush();
    	outFile.close();
	}
	
	public static String doLabeledPrecisionAndRecall(ArrayList<parseTree> computedParseTrees, ArrayList<parseTree> givenParseTrees, boolean printScorePerSentence) {
	
		int nrTotalMatchingConstituents=0, nrTotalConstituentsOfComputedParse=0, nrTotalConstituentsOfGivenParse=0, nrExactMatch = 0;;
        double totalLP = 0d, totalLR = 0d;
       
        int sentenceCounter = 0;
        
        //parseTree computedParseTree = dopparser.Grammar.ExtractParseFromWSJText("(" + parse.trim() + ")", false);
		//computedParseTree.calculateNodeDepthAndLabelNodes();
		//computedParseTree.calculateNodeSpans();
		
        for (parseTree computedParseTree : computedParseTrees) {
        	
        	//hack
        	if (givenParseTrees.size()>sentenceCounter) {
        		
        		parseTree givenParseTree = givenParseTrees.get(sentenceCounter);
        		
			boolean exactMatch = false;
			int nrMatchingConstituents=0, nrConstituentsOfComputedParse=0, nrConstituentsOfGivenParse=0;
	        double LP = 0d, LR = 0d;
	        
	        //only if without feedback of brackets, and if sentenceLength>2

			nrMatchingConstituents = parser.evaluator.doPARSEVAL(computedParseTree, givenParseTree);
	
			nrTotalMatchingConstituents += nrMatchingConstituents;
	
			nrConstituentsOfComputedParse = parser.Utils.computeUniqueBrackets(computedParseTree.getNodes());
	        nrTotalConstituentsOfComputedParse += nrConstituentsOfComputedParse;
	
	        nrConstituentsOfGivenParse = parser.Utils.computeUniqueBrackets(givenParseTree.getNodes());
	        nrTotalConstituentsOfGivenParse += nrConstituentsOfGivenParse;
	
	        if (printScorePerSentence) {
		        System.out.println("given parse: " +  givenParseTree.printWSJFormat());
		        System.out.println("compu parse: " +  computedParseTree.printWSJFormat());
		        System.out.println("given parse spans: " + givenParseTree.printSpans() + "; computed parse spans: " + computedParseTree.printSpans());
		        System.out.println("nrMatchingConstituents=" + nrMatchingConstituents + "; nrConstituentsOfGivenParse=" + nrConstituentsOfGivenParse + "; nrConstituentsOfComputedParse=" + nrConstituentsOfComputedParse);
		        }
	        //if nrConstituentsOfGivenParse = 0, or nrConstituentsOfComputedParse=0, then don't include
	        
	        LP = ((double) nrMatchingConstituents)/((double) nrConstituentsOfGivenParse);
	        LR = ((double) nrMatchingConstituents)/((double) nrConstituentsOfComputedParse);
	        totalLP = totalLP + LP;
	        totalLR = totalLR + LR;
	
	        if (LP==1. && LR==1.) {
	        	nrExactMatch++;
	        	exactMatch = true;
	        }
	        
	        if (printScorePerSentence) System.out.println("Test Sentence " + sentenceCounter + ": LP=" + LP + "; LR=" + LR);
	        
        	}
        	else System.out.println("FOUTJE");
	        sentenceCounter++;
        }
        
        System.out.println("total # MatchingConstituents=" + nrTotalMatchingConstituents);
        System.out.println("total # ConstituentsOfComputedParse=" + nrTotalConstituentsOfComputedParse);
        System.out.println("total # ConstituentsOfGivenParse=" + nrTotalConstituentsOfGivenParse);

        double avLP = ( (double) nrTotalMatchingConstituents/ nrTotalConstituentsOfGivenParse);
        double avLR = ( (double) nrTotalMatchingConstituents/ nrTotalConstituentsOfComputedParse);
        double avF = 2.*(avLP*avLR)/(avLP + avLR);
        System.out.println("Average LP=" + avLP);
        System.out.println("Average LR=" + avLR);
        System.out.println("Average F-score=" + avF);
        System.out.println("Exact Match=" + nrExactMatch);
        
        //double[] results = {avLP, avLR, ((double) nrTotalMatchingConstituents), ((double) nrTotalConstituentsOfComputedParse), ((double) nrTotalConstituentsOfGivenParse)};
        String results = "" + avLP + ";" + avLR + ";" + avF + ";" + nrTotalMatchingConstituents + ";" + nrTotalConstituentsOfComputedParse + ";" + nrTotalConstituentsOfGivenParse;
        return results;
	}
       
	
	public static String doLabeledPrecisionAndRecallForSingleSentence(parseTree computedParseTree, parseTree givenParseTree, String strPrefix, boolean blnPrint) {
	
		NumberFormat numberFormatter = new DecimalFormat("#.######");
		
		int nrMatchingConstituents = parser.evaluator.doPARSEVAL(computedParseTree, givenParseTree);
	
		int nrConstituentsOfComputedParse = parser.Utils.computeUniqueBrackets(computedParseTree.getNodes());
   
		int nrConstituentsOfGivenParse = parser.Utils.computeUniqueBrackets(givenParseTree.getNodes());
   
       //System.out.println("nrMatchingConstituents=" + nrMatchingConstituents + "; nrConstituentsOfGivenParse=" + nrConstituentsOfGivenParse + "; nrConstituentsOfComputedParse=" + nrConstituentsOfComputedParse);
        
       double LP = ((double) nrMatchingConstituents)/((double) nrConstituentsOfGivenParse);
       double LR = ((double) nrMatchingConstituents)/((double) nrConstituentsOfComputedParse);
   
       //System.out.println(strPrefix + ": LP=" + LP + "; LR=" + LR + ";nrConstituentsOfGivenParse=" + nrConstituentsOfGivenParse + "; nrConstituentsOfComputedParse=" + nrConstituentsOfComputedParse);
	   if (blnPrint) System.out.println(strPrefix + ": LP=" + LP + "; LR=" + LR + "; nrMatchingConstituents=" + nrMatchingConstituents + "; nrConstituentsOfGivenParse=" + nrConstituentsOfGivenParse + "; nrConstituentsOfComputedParse=" + nrConstituentsOfComputedParse);
	  
	    double F_score = 2*LP*LR/(LP+LR);
	    return "" + LP + "#" + LR + "#" + F_score + "#" + nrConstituentsOfGivenParse + "#" + nrConstituentsOfComputedParse + "#" + nrMatchingConstituents;
	}
	
	/**
	 * Reranker computes the likelihood of nBest parses from nBestCharniakParsesFile
	 * according to myGrammar, and selects the most likely. This one is evaluated with Parseval
	 * against the goldStandardTestFile.
	 * 
	 * @param myGrammar trained grammar, either episodicGrammar, or PCFG, or lcsGrammar
	 * @param goldStandardTestFile
	 * @param nBestCharniakParsesFile
	 * @param nBest number of best Charniak parses considered for each sentence
	 * @throws IOException
	 */
	public static void rerankAndEvaluateBestCharniakParses(grammar myGrammar, String goldStandardTestFile, String nBestCharniakParsesFile, int nBest) throws IOException {

		BufferedWriter rerankerStatisticsFile = new BufferedWriter(new FileWriter("./output/" + Main.RERANKER_STATISTICS_FILE));	//reranker_statistics.csv
		writeParamsToFile(rerankerStatisticsFile, 0, 0);
		
		rerankerStatisticsFile.write("Test sent.;length;depth;#cstit;ch;#mcst;#cst;h0;#cand;#mcst;#cst;h1;#cand;#mcst;#cst;h2;#cand;#mcst;#cst;h3;#cand;#mcst;#cst;h4;#cand;#mcst;#cst;h5;#cand;#mcst;#cst;h6;#cand;#mcst;#cst;h7;#cand;#mcst;#cst;h8;#cand;#mcst;#cst;h9;#cand;#mcst;#cst;h10;#cand;#mcst;#cst;h11;#cand;#mcst;#cst;h12;#cand;#mcst;#cst;h13;#cand;#mcst;#cst;h14;#cand;#mcst;#cst;ran;#cand;#mcst;#cst");
		rerankerStatisticsFile.newLine();
		long currentTime =System.currentTimeMillis();
		
		NumberFormat numberFormatter = new DecimalFormat("#.######");
		
		//////////////////////////////////////////////////////////////
		////   read and preprocess Gold standard test sentences   ////
		//////////////////////////////////////////////////////////////
		sentencePreprocessor myPreprocessor = new sentencePreprocessor(goldStandardTestFile, false);
		
		HashMap<String, String> unknownWordReplacements=null;	
		if (Main.SMOOTH_FOR_UNKNOWN_WORDS)
			// the same unknownWordReplacements are later reused for smoothing the 10 best parses
			unknownWordReplacements = myPreprocessor.replaceUnknownWordsInTestSet((HashSet<String>) myGrammar.getTerminals(), Main.DO_HORIZONTAL_MARKOVIZATION, Main.DO_LEMMATIZATION);
		
		//for (String w1 : unknownWordReplacements.keySet()) System.out.println(">>" + w1 + "<< replaced by >>" + unknownWordReplacements.get(w1) + "<<");
		
		ArrayList<parseTree> goldStandardParses = myPreprocessor.getPreprocessedParseTrees();	//was: reranker.readGoldStandard(goldStandardTestFile);

		HashSet<String> lowerCaseConversionsFromGoldStandard = myPreprocessor.lowerCaseConversions;
		
		//this sets myNode.setProductionName of nodes in the the parseTree to lhs*rhs1*rhs2*etc
		for (parseTree goldStandardParse : goldStandardParses) goldStandardParse.assignProductionNamesToNodes();

		//looks in lexicon, but replacements are in lexicon
		ArrayList<Integer> indicesOfSentencesToSkip = Main.skipTooLongAndSingleWordSentences(goldStandardParses);

		//indicesOfSentencesToSkip = experiments.checkSentencesWithUnknownLabels_obsolete(goldStandardParses, indicesOfSentencesToSkip, myGrammar);

		//System.out.println("######    There are " + indicesOfSentencesToSkip.size() + " sentences in the test set that are either too long or have unknown words/productions    ######");

		double[] totalLikelihood = new double[Main.MAX_HISTORY+1]; //parametrized for max history length

		ArrayList<parseTree>[] winningParseTrees = new ArrayList[Main.MAX_HISTORY+1];
		ArrayList<Integer>[] winningCandidateIndices = new ArrayList[Main.MAX_HISTORY+1];
		for (int h=Main.MIN_HISTORY; h<=Main.MAX_HISTORY; h++) {
			winningParseTrees[h] = new ArrayList<parseTree>();
			winningCandidateIndices[h] = new ArrayList<Integer>();
		}
		
		/**
		 * These are the first trees of the list of nBest trees, indexed by sentenceCounter in test file
		 */
		//TreeMap<Integer, parseTree> bestCharniakParseTrees = new TreeMap<Integer, parseTree>();
		ArrayList<parseTree> bestCharniakParseTrees = new ArrayList<parseTree>();
		/**
		 * These are random trees from the nBest, used to compute baseline F-score of random picks
		 */
		ArrayList<parseTree> randomCharniakParseTrees = new ArrayList<parseTree>();

		ArrayList<parseTree> selectedGoldStandardParses = new ArrayList<parseTree>();
		
		BufferedReader buffCharniakBest = new BufferedReader(new FileReader(nBestCharniakParsesFile));
		
		parser.parameters.TREEBANK_FILE = nBestCharniakParsesFile;  
		
		int uniqueSentenceCounter = 0, notSelectedSentenceCounter=0, counterOfSameSentences=0;	//used to find out whether sentence is included in removedIndices (unknown words)
		
		ArrayList<parseTree> nBestParseTreesOfSameSentence = null;
		/**
		 * Used to restore the original Charniak parse (before preprocessing 
		 * and Markovization) for the evaluation
		 */
		ArrayList<parseTree> nBestUnprocessedParseTreesOfSameSentence = null; 
		
		System.out.println("Time elapsed from reading gold standard=" + (System.currentTimeMillis()-currentTime) + " milliseconds");
		currentTime =System.currentTimeMillis();
		
		////////////////////////////////////////////////////////////////////////////
		///////////////////    LOOP OVER nBestCharniakParsesFile    ////////////////
		////////////////////////////////////////////////////////////////////////////
		String mySentence;
		while (((mySentence = buffCharniakBest.readLine()) !=null) && uniqueSentenceCounter<Main.LASTTESTSENTENCE) {

			
			if (counterOfSameSentences==0) {
				//start with next nBest parseTrees of the same sentences 
				nBestParseTreesOfSameSentence = new ArrayList<parseTree>();
				nBestUnprocessedParseTreesOfSameSentence = new ArrayList<parseTree>();
			}


			if (!(mySentence.equals(""))) {
				counterOfSameSentences++;
				
			    //upperCase2lowerCase conversion
			    //if (experiments.CONVERT_INITIAL_WORD2LOWERCASE) mySentence = sentencePreprocessor.convertOpenClassWordsInInitialSentencePosition2LowerCase_old(mySentence);
		
				//adds one parseTree to ArrayList nBestParseTreesOfSameSentence
				nBestParseTreesOfSameSentence = readOneSentence(nBest, mySentence, counterOfSameSentences, nBestParseTreesOfSameSentence, lowerCaseConversionsFromGoldStandard);
				nBestUnprocessedParseTreesOfSameSentence = readOneSentence(nBest, mySentence, counterOfSameSentences, nBestUnprocessedParseTreesOfSameSentence, lowerCaseConversionsFromGoldStandard);
			}
			
			//mySentence.equals("") after nBest parses of same sentence have been added to ArrayList nBestParseTreesOfSameSentence
			else {	

				counterOfSameSentences=0;	//   to continue with the next nBest

				if (!(indicesOfSentencesToSkip.contains(uniqueSentenceCounter))) {	//too long sentences are not evaluated

					boolean testTreesWithUnknownNonT = false;
					int nrCandidate=0;
					int[] bestCandidateIndex = new int[Main.MAX_HISTORY+1];
					double[] mostLikeliParseProbability = new double[Main.MAX_HISTORY+1]; 
					for (int hist = Main.MIN_HISTORY; hist <=Main.MAX_HISTORY; hist++) mostLikeliParseProbability[hist] = java.lang.Double.NEGATIVE_INFINITY;
					
					int minLengthOfShortestDerivation=1000;
					
					double logLikelihoodOfCharniak = 0d;
					//for comparing F-score of most likely parse with F-score of randomly selected parse from nBest 
					int randomIndex = (int)(Math.random() * nBest); 
					//System.out.println("randomIndex=" + randomIndex);

					for (parseTree candidateParseTree : nBestParseTreesOfSameSentence) {
						
						currentTime = System.currentTimeMillis();
						
						ArrayList<Integer> nrOfFragmentsInShortestDerivation =new ArrayList<Integer>();
						
						if (nrCandidate==0) {
							//bestCharniakParseTrees.put(uniqueSentenceCounter, candidateParseTree);
							bestCharniakParseTrees.add(nBestUnprocessedParseTreesOfSameSentence.get(0));	//candidateParseTree
							selectedGoldStandardParses.add(goldStandardParses.get(uniqueSentenceCounter));
						}
						if (nrCandidate==randomIndex) randomCharniakParseTrees.add(nBestUnprocessedParseTreesOfSameSentence.get(randomIndex));	//candidateParseTree

						//replace unknown words in the nBest Charniak parses: you already know the correct unknownWordReplacements from the gold standard test set!
						if (Main.SMOOTH_FOR_UNKNOWN_WORDS)
							replaceUnknownWordsInNBestCharniak(candidateParseTree, unknownWordReplacements);

						if (Main.DO_HORIZONTAL_MARKOVIZATION) { 
							candidateParseTree.binarize();	
							candidateParseTree.calculateNodeDepth();
						}
						
						candidateParseTree.assignProductionNamesToNodes();

						//check again for unknown nonterminals in the Charniak parse
						//but not if you have done SMOOTH_FOR_UNKNOWN_PRODUCTIONS; note that virtual nonterminals are not in the grammar!
						if (Main.SMOOTH_FOR_UNKNOWN_PRODUCTIONS || !checkForUnknownNonTerminal(candidateParseTree, myGrammar)) {

							///////////////////////////////////////////////////
							//////     compute sentence probability      //////
							///////////////////////////////////////////////////
							double[] logSentenceProbability = new double[Main.MAX_HISTORY+1];
							
							if (Main.PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL || Main.PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL ) {
								episodicGrammar myEpisodicGrammar = (episodicGrammar) myGrammar;
								
								if (Main.COMPUTE_SHORTEST_DERIVATION_RERANKER) {
									Main.COMPUTE_SHORTEST_DERIVATION=true;
									myEpisodicGrammar.computeLikelihoodOfSentenceWHistory(uniqueSentenceCounter, goldStandardParses.size(), candidateParseTree, nrCandidate, nrOfFragmentsInShortestDerivation);
									//System.out.println("There are " + nrOfFragmentsInShortestDerivation.get(0) + " fragments in shortest derivation.");
									
									Main.COMPUTE_SHORTEST_DERIVATION=false;
								}
								logSentenceProbability = myEpisodicGrammar.computeLikelihoodOfSentenceWHistory(uniqueSentenceCounter, goldStandardParses.size(), candidateParseTree, nrCandidate, null);
								
							}
							if (Main.PROBABILITY_MODEL==Main.PCFG) {
								PCFG myPCFG = (PCFG) myGrammar;
								//only logSentenceProbability[MAX_HISTORY] is filled in, other entries are zero.
								logSentenceProbability[Main.MAX_HISTORY] = Math.log(myPCFG.computePCFGSentenceProbability(uniqueSentenceCounter, candidateParseTree));
							}
							if (Main.PROBABILITY_MODEL==Main.Manning_CarpenterLCP) {
								lcsGrammar myLCSGrammar = (lcsGrammar) myGrammar;
								double sentenceProbability = myLCSGrammar.computeManningCarpenterSentenceProbability(uniqueSentenceCounter, candidateParseTree);
								if (!(sentenceProbability==0.)) {
									//only logSentenceProbability[MAX_HISTORY] is filled in, other entries are zero.
									logSentenceProbability[Main.MAX_HISTORY] = Math.log(sentenceProbability);
								}
								else {
									testTreesWithUnknownNonT=true; 	
									//System.out.println("cancelled: testTreesWithUnknownNonT=true;");
									break;
								}
							}
							if (Main.FEEDBACK) 
								System.out.println("logSentenceProbability of candidate " + nrCandidate + " is: " + logSentenceProbability[Main.MAX_HISTORY]);

							
							/////////////////////////////////////////////////////////////////////
							//determine best of candidates; remember if this is not the first one
							
							//find out if this is shortest derivation (the same for all histories)
							boolean shortestDerivation = true;
							boolean shorterDerivation = false;
							if (Main.COMPUTE_SHORTEST_DERIVATION_RERANKER) {
								shortestDerivation = false;
								 if(nrOfFragmentsInShortestDerivation.get(0)<=minLengthOfShortestDerivation) {
									 if (nrOfFragmentsInShortestDerivation.get(0)< minLengthOfShortestDerivation) shorterDerivation = true;
									 minLengthOfShortestDerivation = nrOfFragmentsInShortestDerivation.get(0);
									 shortestDerivation=true;
								 }
							}
							
							if (shortestDerivation) {
								for (int hist = Main.MIN_HISTORY; hist <=Main.MAX_HISTORY; hist++) {
									if (nrCandidate==0) {
										mostLikeliParseProbability[hist] = logSentenceProbability[hist];		
										logLikelihoodOfCharniak = logSentenceProbability[hist];	//only for comparison
									}
									else {
										if ((logSentenceProbability[hist]>mostLikeliParseProbability[hist]) || shorterDerivation) {
											mostLikeliParseProbability[hist] = logSentenceProbability[hist];
											bestCandidateIndex[hist] = nrCandidate;
										}
									}
								}
							}	//if (shortestDerivation) {
						}	//if (!checkForUnknownNonTerminal(candidateParseTree))
						else {
							testTreesWithUnknownNonT=true; 	
							//System.out.println("cancelled: testTreesWithUnknownNonT=true;");
							break;
						}
						
						String strShortestFragments ="";
						if (Main.COMPUTE_SHORTEST_DERIVATION_RERANKER) strShortestFragments = "; nrFragments=" + nrOfFragmentsInShortestDerivation.get(0);
						System.out.println("nrCandidate=" + nrCandidate + strShortestFragments + "; likelihood[14]=" + numberFormatter.format(mostLikeliParseProbability[Main.MAX_HISTORY]) + "; total time="  + (System.currentTimeMillis()-currentTime) + " milliseconds. " + episodicGrammar.nrUnknownProductionsFeedback);
					
						
						nrCandidate++;	
					}	//for (parseTree candidateParseTree : nBestParseTreesOfSameSentence)

					////////////////////////////////////////////////////////////////
					///////////    EVALUATION OF THE MOST LIKELY PARSE    //////////
					////////////////////////////////////////////////////////////////
					if (!testTreesWithUnknownNonT) {	

						for (int hist = Main.MIN_HISTORY; hist <=Main.MAX_HISTORY; hist++) {
							//for evaluation (LP/LR) you need to look at unprocessed parseTree
							winningParseTrees[hist].add(nBestUnprocessedParseTreesOfSameSentence.get(bestCandidateIndex[hist]));
							winningCandidateIndices[hist].add(bestCandidateIndex[hist]);

							totalLikelihood[hist] += mostLikeliParseProbability[hist];	//not really of any interest here...
						}

						if (Main.FEEDBACK) 
							System.out.println("Test sentence " + uniqueSentenceCounter + "; best candidate is #" + bestCandidateIndex[Main.MAX_HISTORY] + "; minShortestDerivation=" + minLengthOfShortestDerivation + "; likelihood diff with Charniak: " + (mostLikeliParseProbability[Main.MAX_HISTORY]-logLikelihoodOfCharniak));

						//small check for LP/LR of candidate 0 vs bestCandidate vs random
						double[] all_scores = new double[9]; //LP, LR, F-score of all 3 evals

						String strLPLRFScores = "";
						//je wilt voor alle histories (matchingConstit computedConst, 1x givenConstit, depth, length)
						//rerankerStatisticsFile.write("Test sent.;length;depth;#cstit;ch;#mcst;#cst;h0;#cand;#mcst;#cst;h1;#cand;#mcst;#cst;h2;#cand;#mcst;#cst;h3;#cand;#mcst;#cst;h4;#cand;#mcst;#cst;h5;#cand;#mcst;#cst;h6;#cand;#mcst;#cst;h7;#cand;#mcst;#cst;h8;#cand;#mcst;#cst;h9;#cand;#mcst;#cst;h10;#cand;#mcst;#cst;h11;#cand;#mcst;#cst;h12;#cand;#mcst;#cst;ran;#cand;#mcst;#cst");
						StringBuffer lineOfScores = new StringBuffer();
						int depth = goldStandardParses.get(uniqueSentenceCounter).deepestLevel;
						int length = goldStandardParses.get(uniqueSentenceCounter).getYieldString(false).split(" ").length;
						lineOfScores.append("" + uniqueSentenceCounter + ";" + length + ";" + depth  + ";");

						//charniak
						//System.out.println("1-Ch: " + nBestUnprocessedParseTreesOfSameSentence.get(0).getYieldString(false));
						//System.out.println("2-GS: " + goldStandardParses.get(uniqueSentenceCounter).getYieldString(false));
						strLPLRFScores = reranker.doLabeledPrecisionAndRecallForSingleSentence(nBestUnprocessedParseTreesOfSameSentence.get(0), goldStandardParses.get(uniqueSentenceCounter), "Charniak", false);
						all_scores[0] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[0]);
						all_scores[1] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[1]);
						all_scores[2] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[2]);
						int nrConstituentsOfGivenParse = java.lang.Integer.parseInt(strLPLRFScores.split("#")[3]);

						//return "" + LP + "#" + LR + "#" + F_score + "#" + nrConstituentsOfGivenParse + "#" + nrConstituentsOfComputedParse + "#" + nrMatchingConstituents;
						lineOfScores.append(nrConstituentsOfGivenParse + ";ch;" + java.lang.Integer.parseInt(strLPLRFScores.split("#")[5]) + ";" + java.lang.Integer.parseInt(strLPLRFScores.split("#")[4]) + ";");

						//reranker
						strLPLRFScores = reranker.doLabeledPrecisionAndRecallForSingleSentence(nBestUnprocessedParseTreesOfSameSentence.get(bestCandidateIndex[Main.MAX_HISTORY]), goldStandardParses.get(uniqueSentenceCounter), "reranker", false);
						all_scores[3] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[0]);
						all_scores[4] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[1]);
						all_scores[5] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[2]);
						//System.out.println("Test sent.;" + sentenceCounter + ";best cand=;" + bestCandidateIndex[h] + ";RER;" + all_scores[3] + ";" + all_scores[4] + ";" + all_scores[5]);

						for (int h=Main.MIN_HISTORY; h<=Main.MAX_HISTORY; h++) {
							strLPLRFScores = reranker.doLabeledPrecisionAndRecallForSingleSentence(nBestUnprocessedParseTreesOfSameSentence.get(bestCandidateIndex[h]), goldStandardParses.get(uniqueSentenceCounter), "reranker", false);
							lineOfScores.append("" + h + ";" + bestCandidateIndex[h] + ";" + java.lang.Integer.parseInt(strLPLRFScores.split("#")[5]) + ";" + java.lang.Integer.parseInt(strLPLRFScores.split("#")[4]) + ";");
						}
						//random
						int randomInteger = (int)(Math.random() * nBest);
						strLPLRFScores = reranker.doLabeledPrecisionAndRecallForSingleSentence(nBestUnprocessedParseTreesOfSameSentence.get(randomInteger), goldStandardParses.get(uniqueSentenceCounter), "random", false);
						all_scores[6] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[0]);
						all_scores[7] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[1]);
						all_scores[8] = java.lang.Double.parseDouble(strLPLRFScores.split("#")[2]);
						//if ((mostLikeliParseProbability-logLikelihoodOfCharniak)>MIN_LIKELIHOOD_DIFFERENCE) System.out.println("*****;" + all_scores[0] + ";" + all_scores[1] + ";" + all_scores[2] + all_scores[3] + ";" + all_scores[4] + ";" + all_scores[5] + all_scores[6] + ";" + all_scores[7] + ";" + all_scores[8] + ";" + nrConstituentsOfGivenParse);
						System.out.println(">>>Test sent.;" + uniqueSentenceCounter + ";best cand=;" + bestCandidateIndex[Main.MAX_HISTORY] + ";llh_diff=;" + numberFormatter.format(mostLikeliParseProbability[Main.MAX_HISTORY]-logLikelihoodOfCharniak) + ";CH;" + numberFormatter.format(all_scores[0]) + ";" + numberFormatter.format(all_scores[1]) + ";" + numberFormatter.format(all_scores[2])  + ";RER;" + numberFormatter.format(all_scores[3]) + ";" + numberFormatter.format(all_scores[4]) + ";" + numberFormatter.format(all_scores[5])  + ";RAN;" + numberFormatter.format(all_scores[6]) + ";" + numberFormatter.format(all_scores[7]) + ";" + numberFormatter.format(all_scores[8]) + ";" + nrConstituentsOfGivenParse);

						lineOfScores.append("ran;" + randomInteger + ";" + java.lang.Integer.parseInt(strLPLRFScores.split("#")[5]) + ";" + java.lang.Integer.parseInt(strLPLRFScores.split("#")[4]));
						rerankerStatisticsFile.write(lineOfScores.toString());	rerankerStatisticsFile.newLine();

					}	//if (!testTreesWithUnknownNonT)
					else {		//unknown nonT in test sentence
						notSelectedSentenceCounter++;
						indicesOfSentencesToSkip.add(uniqueSentenceCounter);
						//goldStandardParses.remove(selectedSentenceCounter);	//otherwise ArrayLists of computed vs given parseTrees in total LP/LR gets shifted
						//bestCharniakParseTrees.remove(bestCharniakParseTrees.size()-1); //remove the last one
						//if (randomIndex==0) 
						//	randomCharniakParseTrees.remove(randomCharniakParseTrees.size()-1);
						System.out.println("Test sentence " + uniqueSentenceCounter + " is skipped because of unknown nonT in Charniak parse. " + notSelectedSentenceCounter + " sentences have been skipped.");
					}
				}	//if (!(indicesOfSentencesToSkip.contains(sentenceCounter)))
				else {
					notSelectedSentenceCounter++;
					System.out.println("Test sentence " + uniqueSentenceCounter + " is skipped because it is longer than " + parser.parameters.MAX_SENTENCE_SIZE + " words. "  + notSelectedSentenceCounter + " sentences have been skipped.");
				}
				uniqueSentenceCounter++;

			}	//else if (!(mySentence.equals(""))) {
		}	//reading 

		buffCharniakBest.close();
		///////////////////////////////////////////////////////////////////
		///////////    END OF READING nBestCharniakParsesFile    //////////
		///////////    averaging overall scores and printing     //////////
		///////////////////////////////////////////////////////////////////
		
		//print total likelihoods, but not really of any interest here...
		System.out.println("totalLikelihood : " + totalLikelihood[Main.MAX_HISTORY]);

		BufferedWriter rerankerScoreFile = new BufferedWriter(new FileWriter("./output/" + Main.FSCORES_FILE));
		writeParamsToFile(rerankerScoreFile, goldStandardParses.size(), selectedGoldStandardParses.size());
		
		rerankerScoreFile.write("his;LP;LR;F;#matchCst;#cstComp;#cstGiven"); rerankerScoreFile.newLine();

		//remove indicesOfSentencesToSkip from goldStandardParses, bestCharniakParseTrees, randomCharniakParseTrees
		
		//ArrayList<parseTree> selectedGoldStandardParses = new ArrayList<parseTree>();
		//ArrayList<parseTree> selectedBestCharniakParses = new ArrayList<parseTree>();
		//ArrayList<parseTree> selectedRandomCharniakParses = new ArrayList<parseTree>();
		System.out.println("##### check sizes: goldStandardParses.size=" + selectedGoldStandardParses.size() + "; bestCharniakParseTrees.size=" + bestCharniakParseTrees.size() + "; randomCharniakParseTrees.size=" + randomCharniakParseTrees.size());
		/*
		for (int counter=0; uniqueSentenceCounter< goldStandardParses.size(); counter++) {
			if (!(indicesOfSentencesToSkip.contains(counter))) {
				selectedGoldStandardParses.add(goldStandardParses.get(counter));
				selectedBestCharniakParses.add(bestCharniakParseTrees.get(counter));
				selectedRandomCharniakParses.add(randomCharniakParseTrees.get(counter));
			}
		}
		*/
		
		// do LP/LR for the ArrayList of winningParseTrees containing the test sentences with highest likelihood
		System.out.println("Overall LP/LR of reranker:");
		for (int h=Main.MIN_HISTORY; h<=Main.MAX_HISTORY; h++) {
			//double[] results = {avLP, avLR, ((double) nrTotalMatchingConstituents), ((double) nrTotalConstituentsOfComputedParse), ((double) nrTotalConstituentsOfGivenParse)};
			System.out.println("history=" + h + ":");
			String results = reranker.doLabeledPrecisionAndRecall(winningParseTrees[h], selectedGoldStandardParses, false);

			//write to file:
			rerankerScoreFile.write("" + h + ";" + results); rerankerScoreFile.newLine();
		}
		
		//recompute LP/LR for bestCharniak, but now without indicesOfSentencesWithUnknownWords
		System.out.println("Recomputed LP/LR of best Charniak parses where sentencesWithUnknownWords are left out");
		String results = reranker.doLabeledPrecisionAndRecall(bestCharniakParseTrees, selectedGoldStandardParses, false); 
		rerankerScoreFile.write("Ch;" + results); rerankerScoreFile.newLine();
		
		//compute LP/LR for random of bestCharniak
		System.out.println("LP/LR of random Charniak parse where sentencesWithUnknownWords are left out");
		results = reranker.doLabeledPrecisionAndRecall(randomCharniakParseTrees, selectedGoldStandardParses, false); 
		rerankerScoreFile.write("Ran;" + results); rerankerScoreFile.newLine();
		
		rerankerScoreFile.flush();
		rerankerScoreFile.close();
		rerankerStatisticsFile.flush();
		rerankerStatisticsFile.close();
	}
	
	
	public static void writeParamsToFile(BufferedWriter outFile, int goldStandardParsesSize, int selGoldStandardParsesSize) throws IOException {
		outFile.write("TRAIN_CORPUS=" + Main.TRAIN_CORPUS);
		outFile.newLine();
		outFile.write("TEST_CORPUS=" + Main.TEST_CORPUS);
		outFile.newLine();
		outFile.write("nBestCharniakParsesFile=" + Main.nBestCharniakParsesFile);
		outFile.newLine();
		outFile.write("nBest=" + Main.nBest);
		outFile.newLine();
		outFile.write(selGoldStandardParsesSize + " sentences were reranked out of " + goldStandardParsesSize);
		outFile.newLine();
		outFile.write("");
		outFile.newLine();
		String probability_model="";
		switch (Main.PROBABILITY_MODEL) {
        case 1: probability_model = "EPISODIC_TOPDOWN_MODEL"; break;
        case 2: probability_model = "EPISODIC_LEFTCORNER_MODEL"; break;
        case 3: probability_model = "EPISODIC_UNCLEAR_MODEL"; break;
        case 4: probability_model = "PCFG"; break;
        case 5: probability_model = "Manning_CarpenterLCP"; break;
        }
		outFile.write("PROBABILITY_MODEL=" + probability_model);
		outFile.newLine();
		if (Main.EXTRA_ATTACHMENT_IN_TD_DERIVATION) {
			outFile.write("EXTRA_ATTACHMENT_IN_TD_DERIVATION=true"); outFile.newLine(); }
		
		outFile.write("INCLUDE_DISCONTIGUITIES=" + Main.INCLUDE_DISCONTIGUITIES);
		outFile.newLine();
		//outFile.write("INCLUDE_DISCONTIGUITIES_ALTERNATIVE=" + experiments.INCLUDE_DISCONTIGUITIES_ALTERNATIVE);
		//outFile.newLine();
		
		if (Main.INCLUDE_DISCONTIGUITIES) {
			outFile.write("DISCTG_DECAY=" + Main.DISCTG_DECAY); outFile.newLine();
			outFile.write("DISCTG_AVERAGED=" + Main.DISCONTIGUITIES_AVERAGED); outFile.newLine();
			outFile.write("DISCTG_REDUCTION=" + Main.DISCONTIGUITIES_REDUCTION); outFile.newLine();
			outFile.write("SYNTACTIC_PRIMING=" + Main.SYNTACTIC_PRIMING); outFile.newLine();
    	}
    	
		outFile.write("COMPUTE_SHORTEST_DERIVATION_RERANKER=" + Main.COMPUTE_SHORTEST_DERIVATION_RERANKER);
		outFile.newLine();
		outFile.write("LAMBDA_1=" + Main.LAMBDA_1 + "; LAMBDA_2=" + Main.LAMBDA_2 + "; LAMBDA_3=" + Main.LAMBDA_3);
		outFile.newLine();
		outFile.write("BACK_OFF_FACTOR=" + Main.BACK_OFF_FACTOR);
		outFile.newLine();
		outFile.write("BACK_OFF_FACTOR=" + Main.BACK_OFF_FACTOR);
		outFile.newLine();
		if (Main.POLYNOMIAL_WEIGHTS) {
			outFile.write("POLYNOMIAL_WEIGHTS=true");
			outFile.newLine();	}
		outFile.write("SMOOTH_FOR_UNKNOWN_WORDS=" + Main.SMOOTH_FOR_UNKNOWN_WORDS);
		outFile.newLine();
		if (Main.SMOOTH_FOR_UNKNOWN_WORDS) {
			outFile.write("DO_LEMMATIZATION=" + Main.DO_LEMMATIZATION);
			outFile.newLine();
			//outFile.write("CONVERT_INITIAL_WORD2LOWERCASE=" + experiments.CONVERT_INITIAL_WORD2LOWERCASE);
			//outFile.newLine();
			outFile.write("WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING=" + Main.WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING);
			outFile.newLine();
			//outFile.write("MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS=" + experiments.MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS);
			//outFile.newLine();
		}
		//outFile.write("SMOOTH_FOR_UNKNOWN_PRODUCTIONS=" + experiments.SMOOTH_FOR_UNKNOWN_PRODUCTIONS);
		//outFile.newLine();
		//outFile.write("DO_HORIZONTAL_MARKOVIZATION=" + experiments.DO_HORIZONTAL_MARKOVIZATION);
		//outFile.newLine();
		outFile.write("");
		outFile.newLine();

	}
	
	public static boolean checkForUnknownNonTerminal(parseTree myTestParseTree, grammar myGrammar) {
		boolean blnUnknownNonTerminals = false;
		for (parser.Node myNode : myTestParseTree.getNodes()) {
			
			String nodeProduction = myNode.getProductionName();
			//System.out.println("myNode.getName()=" + myNode.getName() + "; myNode.getHPNNodeName()=" + myNode.getHPNNodeName());
			if	((myNode.getType()== parser.parameters.NONTERMINAL) && !(myGrammar.getNonTerminalUnits().keySet().contains(nodeProduction))) {
				System.out.println("unknown nonterminal production in Charniak parse: ##" + nodeProduction + "## Likelihood not computed.");
				blnUnknownNonTerminals = true; 	break;
			}
			
			/*
			// xxx in case of Manning_CarpenterLCP you can actually check with shiftProbabilities instead of shiftProductionsEncounteredInTrainSet
			if (myNode.getType()== dopparser.Main.TERMINAL && comparativeLikelihoods.INCLUDE_SHIFT_PROBABILITIES) {
				if (myNode.getLeftSpan()>0) {
					String shiftProduction = lcsGrammar.findShiftProductionAssociatedWithTerminal(myTestParseTree, myNode);
					//System.out.println("shiftProduction=" + shiftProduction);
					if (!(shiftProductionsEncounteredInTrainSet.contains(shiftProduction))) {
						System.out.println("XXX************ The test sentence contains unknown shiftProduction:" + shiftProduction + "*");
						blnUnknownNonTerminals = true; 	break;
					}
				}
				else {
					if (!(shiftProductionsEncounteredInTrainSet.contains("START^1~TOP~" + myNode.getProductionName()))) {
						System.out.println("XXX************ The test sentence contains unknown shiftProduction: START^1~TOP~" + myNode.getProductionName() + "*");
						blnUnknownNonTerminals = true; 	break;
					}
				}
            }
            */
	
		}
		return blnUnknownNonTerminals;
	}
	
	//unknownWordReplacements are obtained from gold Standard
	public static void replaceUnknownWordsInNBestCharniak(parseTree myParseTree, HashMap<String, String> unknownWordReplacements) {
		
		for (Node myNode : myParseTree.getNodes()) {
       		if (myNode.getType()==parser.parameters.TERMINAL){
       			String wordLabel = myNode.getName();
       			//if (!dopparser.Main.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES)
       				//wordLabel = myNode.getName().split(" ")[0].trim() + "@" + myNode.getName().split(" ")[1].trim();
            
       			if (unknownWordReplacements.containsKey(wordLabel)) {
       				//replace the word with same unknownWordClass as in gold standard, unless the unknownWordClass is "s".
       				String replacingWord = unknownWordReplacements.get(wordLabel);
       				if (!(replacingWord.equals("s")))
       					myNode.setName(replacingWord);
       				
       				else {
       					lemmatize(myNode, wordLabel);
       				}
       				
       			}
       			
       		}
		}
	}

	private static void lemmatize(Node myNode, String wordLabel) {
		String replacingWord;
		//the word occurs in the lexicon of the train set with or without the final "s"
		//you also need to replace the postag, and the grandparent label of the binarized tree
		if (wordLabel.endsWith("s")) {
			//remove the final "s"
			replacingWord = wordLabel.substring(0, wordLabel.length()-1);
			myNode.setName(replacingWord);
			//replace postag: NNS = plural noun, NN=singular; VBZ=Verb, 3rd ps. sing. present, VBP is non-3rd
			if (myNode.getParentNode().getName().equals("NNS")) myNode.getParentNode().setName("NN");
			if (myNode.getParentNode().getName().equals("VBZ")) myNode.getParentNode().setName("VBP");
			if (Main.DO_HORIZONTAL_MARKOVIZATION) {
				//you must also replace the postag after the > in the binarized grandparent nonterminal
				String grandParent = myNode.getParentNode().getParentNode().getName();
				if (grandParent.contains(">NNS")) grandParent = grandParent.replace(">NNS", ">NN");
				if (grandParent.contains(">VBZ")) grandParent = grandParent.replace(">VBZ", ">VBP");
				myNode.getParentNode().getParentNode().setName(grandParent);
			}
		}
		else {	//add a final "s"
			myNode.setName(wordLabel + "s");
			//replace postag: NNS = plural noun, NN=singular; VBZ=Verb, 3rd ps. sing. present, VBP is non-3rd
			if (myNode.getParentNode().getName().equals("NN")) myNode.getParentNode().setName("NNS");
			if (myNode.getParentNode().getName().equals("VBP")) myNode.getParentNode().setName("VBZ");
			if (Main.DO_HORIZONTAL_MARKOVIZATION) {
				//you must also replace the postag after the > in the binarized grandparent nonterminal
				String grandParent = myNode.getParentNode().getParentNode().getName();
				if (grandParent.contains(">NN")) grandParent = grandParent.replace(">NN", ">NNS");
				if (grandParent.contains(">VBP")) grandParent = grandParent.replace(">VBP", ">VBZ");
				myNode.getParentNode().getParentNode().setName(grandParent);
			}
		}
	}
	
	
	private static ArrayList<parseTree> readOneSentence(int nBest, String mySentence, 
			int counterOfSameSentences, ArrayList<parseTree> nBestParseTreesOfSameSentence, HashSet<String> lowerCaseConversionsFromGoldStandard) {
		
		parseTree myParseTree;
		
		if (counterOfSameSentences <= nBest) {
			
			    mySentence = mySentence.trim();
			     
			    myParseTree = sentencePreprocessor.ExtractParseFromWSJText(mySentence, false);
			    //you still need to remove nonTerminal nodes that have no children, recursively
			    myParseTree.removeEmptyNonTerminalNodes();
			    myParseTree.removeEmptyNonTerminalNodes();
			    myParseTree.removeEmptyNonTerminalNodes();
			    
			    myParseTree.calculateNodeDepth();
			    myParseTree.calculateNodeSpans();

			    //upperCase2lowerCase conversion
			    if (Main.CONVERT_INITIAL_WORD2LOWERCASE) sentencePreprocessor.convertOpenClassWordsInInitialSentencePosition2LowerCase(myParseTree, lowerCaseConversionsFromGoldStandard);
			    
			    nBestParseTreesOfSameSentence.add(myParseTree);
			
		}	//if (nrOfSameSentences <= nBest)
		return nBestParseTreesOfSameSentence;
	}
} 
