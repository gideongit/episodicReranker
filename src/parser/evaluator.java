/*
 * Testing.java
 *
 * Created on 7 maart 2006, 13:21
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package parser;

/**
 *
 * @author gideon
 */
import java.util.*;
import java.io.*;
import java.text.*;

public class evaluator {
    
    public int nrUnknownWordSentences = 0;
    public int nrUnparseableSentences = 0;
    public static HashMap<String, Integer> labelFrequencies1 = new HashMap<String, Integer>();
    public static HashMap<String, Integer> labelFrequencies2 = new HashMap<String, Integer>();
    public static HashMap<String, Integer> matchingLabelsPerCategory = new HashMap<String, Integer>();
    public static ArrayList<Double> parseProbabilities;
    
    public ArrayList<Double> UPMatrix = new ArrayList<Double>();
    public ArrayList<Double> URMatrix = new ArrayList<Double>();
    public HashMap<String, HashMap<String, Integer>> precisionAndRecallGroupedPerGivenSpan = new HashMap<String, HashMap<String, Integer>>();
    public ArrayList<Double> UPMatrixGrouped = new ArrayList<Double>();
    public ArrayList<Double> URMatrixGrouped = new ArrayList<Double>();
   
    public int nrTotalMatchingConstituents=0, nrTotalConstituentsOfComputedParse=0, nrTotalConstituentsOfGivenParse=0;
    public int nrTotalMatchingConstituentsRB=0, nrTotalConstituentsOfRBParse=0, nrTotalMatchingConstituentsLB=0, nrTotalConstituentsOfLBParse=0;
    public double totalLP = 0d;
    public double totalLR = 0d;
    public int nrExactMatch = 0;
    
    /* this method returns the number of matching constituents between the two trees
     *LP and LR are not calculated here
     */
    public static int doPARSEVAL(parseTree parseTree1, parseTree parseTree2) {
        //int nrMatchingConstituents = 0;
        HashSet<String> listOfUniqueBrackets = new HashSet<String>();
        //iterate over nodes of both trees
        for(Node nodeOfTree1 : parseTree1.getNodes()) {
            //skip terminals, skip TOP XXX also skip preterminals???
            //if (nodeOfTree1.getType()==Main.NONTERMINAL && nodeOfTree1.getChildNodes().size()>1 ) { FOUT: dat excludeert TOP node!!!
            if (nodeOfTree1.getType()==parameters.NONTERMINAL && (nodeOfTree1.getRightSpan()- nodeOfTree1.getLeftSpan())>1 ) { 
                for(Node nodeOfTree2 : parseTree2.getNodes()) {
                    //may be unnecessary
                    if (nodeOfTree2.getType()==parameters.NONTERMINAL && (nodeOfTree2.getRightSpan()- nodeOfTree2.getLeftSpan())>1 ) { 
                        //compare labels (case-insensitive)
                        if (nodeOfTree1.getName().toLowerCase().equals(nodeOfTree2.getName().toLowerCase())){
                            //check spans
                            if ((nodeOfTree1.getLeftSpan()==nodeOfTree2.getLeftSpan()) && (nodeOfTree1.getRightSpan()==nodeOfTree2.getRightSpan())) {
                                listOfUniqueBrackets.add(nodeOfTree1.getLeftSpan() + "_" + nodeOfTree1.getRightSpan());
                                if (parameters.PRINT_PARSEVAL_PER_CATEGORY) {
                                    if (matchingLabelsPerCategory.get(nodeOfTree1.getName())==null) {
                                        matchingLabelsPerCategory.put(nodeOfTree1.getName(), 1);
                                    }
                                    else matchingLabelsPerCategory.put(nodeOfTree1.getName(), matchingLabelsPerCategory.get(nodeOfTree1.getName()).intValue() +1);
                                }
                                break; //breaks out of fpr-loop, so the match will be counted only once!!! not true, if 
                                //The computed parse tree is:  [.{TOP (0-3)} [.{A (0-3)} 
                                //The  given   parse tree is:  [.{TOP (0-3)} [.{a (0-3)}
                                
                            }
                        }
                    }
                }
            }
        }
        return listOfUniqueBrackets.size(); 
    }
    
    public evaluator(){
    	
         ArrayList<Double> UPMatrix = new ArrayList<Double>();
        ArrayList<Double> URMatrix = new ArrayList<Double>();
       HashMap<String, HashMap<String, Integer>> precisionAndRecallGroupedPerGivenSpan = new HashMap<String, HashMap<String, Integer>>();
        ArrayList<Double> UPMatrixGrouped = new ArrayList<Double>();
        ArrayList<Double> URMatrixGrouped = new ArrayList<Double>();
        nrTotalMatchingConstituents=0; nrTotalConstituentsOfComputedParse=0; nrTotalConstituentsOfGivenParse=0;
        nrTotalMatchingConstituentsRB=0; nrTotalConstituentsOfRBParse=0; nrTotalMatchingConstituentsLB=0; nrTotalConstituentsOfLBParse=0;
        totalLP = 0d;
        totalLR = 0d;
        nrExactMatch = 0;
    }
    /* this method returns the number of matching constituents between the two trees
     *LP and LR are not calculated here
     */
    /* this method returns the number of matching constituents between the two trees
     *LP and LR are not calculated here
     */
    public static int doUPARSEVAL(parseTree parseTree1, parseTree parseTree2) {
        int nrMatchingConstituents = 0;
        HashSet<String> listOfUniqueBrackets = new HashSet<String>();
        //iterate over nodes of both trees
        for(Node nodeOfTree1 : parseTree1.getNodes()) {
            //skip terminals, skip TOP XXX also skip preterminals???
            //if (nodeOfTree1.getType()==Main.NONTERMINAL && nodeOfTree1.getChildNodes().size()>1 ) { WRONG: that excludes TOP!!!
            if (nodeOfTree1.getType()==parameters.NONTERMINAL && (nodeOfTree1.getRightSpan()- nodeOfTree1.getLeftSpan())>1 ) {
                for(Node nodeOfTree2 : parseTree2.getNodes()) {
                    //may be unnecessary
                    if (nodeOfTree2.getType()==parameters.NONTERMINAL && (nodeOfTree2.getRightSpan()- nodeOfTree2.getLeftSpan())>1 ) { 
                        //check spans
                        if ((nodeOfTree1.getLeftSpan()==nodeOfTree2.getLeftSpan()) && (nodeOfTree1.getRightSpan()==nodeOfTree2.getRightSpan())) {
                            listOfUniqueBrackets.add(nodeOfTree1.getLeftSpan() + "_" + nodeOfTree1.getRightSpan());
                            //nrMatchingConstituents++;
                            break;
                        }
                    }
                }
            }
        }
        return listOfUniqueBrackets.size(); //nrMatchingConstituents;
    }
    
    

	public static void printPrecisionRecallTotals(
			NumberFormat numberFormatter, int unlabeledSentencesSize,
			parser.evaluator myEvaluator, int nrParsedSentences,
			int nrUnknownWordSentences, int nrUnparseableSentences,
			int nrTooLongSentences, int sentenceCounterForPrecisionRecall)
			throws IOException, NumberFormatException {
		printUPandUR(numberFormatter, myEvaluator.UPMatrix, myEvaluator.URMatrix, myEvaluator.UPMatrixGrouped, myEvaluator.URMatrixGrouped);
		
		System.out.println(" ");
		System.out.println(nrParsedSentences + " out of " + unlabeledSentencesSize + " sentences of length>2 were processed.");
		System.out.println(nrUnknownWordSentences + " sentences contained unknown words.");
		System.out.println(nrTooLongSentences + " sentences contained too many words.");
		System.out.println(nrUnparseableSentences + " sentences could not be parsed.");
		 
		System.out.println("total # MatchingConstituents=" + myEvaluator.nrTotalMatchingConstituents);
		System.out.println("total # ConstituentsOfComputedParse=" + myEvaluator.nrTotalConstituentsOfComputedParse);
		System.out.println("total # ConstituentsOfGivenParse=" + myEvaluator.nrTotalConstituentsOfGivenParse);

		String strLR = "LR", strLP = "LP";
		if (parameters.DO_UNLABELED_PRECISION_AND_RECALL) { strLR = "UR"; strLP = "UP";}
		System.out.println("Average " + strLP + "=" + ( (double) myEvaluator.nrTotalMatchingConstituents/ myEvaluator.nrTotalConstituentsOfGivenParse) );
		System.out.println("Average " + strLR + "=" + ( (double) myEvaluator.nrTotalMatchingConstituents/ myEvaluator.nrTotalConstituentsOfComputedParse) );
		           
		System.out.println("Exact Match=" + myEvaluator.nrExactMatch);
		
        //baselines
		if (parameters.COMPUTE_BASELINES_PRECISION_AND_RECALL) {
			//averaged w/o considering sentence length
			
			System.out.println("Averages without considering sentence length (excluding length 2 sentences)");
			System.out.println("Average " + strLP + "=" + ( (double) myEvaluator.totalLP/ sentenceCounterForPrecisionRecall) );
			System.out.println("Average " + strLR + "=" + ( (double) myEvaluator.totalLR/ sentenceCounterForPrecisionRecall ) );
         
			System.out.println("");
			System.out.println("Averages for right branching");
			System.out.println("Average " + strLP + "=" + ( (double) myEvaluator.nrTotalMatchingConstituentsRB/ myEvaluator.nrTotalConstituentsOfGivenParse) );
			System.out.println("Average " + strLR + "=" + ( (double) myEvaluator.nrTotalMatchingConstituentsRB/ myEvaluator.nrTotalConstituentsOfRBParse) );
			System.out.println("Averages for left branching");
			System.out.println("Average " + strLP + "=" + ( (double) myEvaluator.nrTotalMatchingConstituentsLB/ myEvaluator.nrTotalConstituentsOfGivenParse) );
			System.out.println("Average " + strLR + "=" + ( (double) myEvaluator.nrTotalMatchingConstituentsLB/ myEvaluator.nrTotalConstituentsOfLBParse) );
		
		    //grouped according to givenSpans
		    //HashMap<String, HashMap<String, Integer>> precisionAndRecallGroupedPerGivenSpan
			System.out.println("");
			System.out.println("Scores specified according to the original bracketings:");
		    for (String givenSpan : myEvaluator.precisionAndRecallGroupedPerGivenSpan.keySet()) {
		    	double totalUPforGivenSpan =0., totalURforGivenSpan =0.;
		    	int totalParsesForGivenSpan =0;
		    	for (String computedSpanPlusScores : myEvaluator.precisionAndRecallGroupedPerGivenSpan.get(givenSpan).keySet()) {
		    		int freq = myEvaluator.precisionAndRecallGroupedPerGivenSpan.get(givenSpan).get(computedSpanPlusScores);
		    		String computedSpan = computedSpanPlusScores.split("@")[0];
		    		double UPForGivenSpan = java.lang.Double.parseDouble(computedSpanPlusScores.split("@")[1])*((double) freq);
		    		double URForGivenSpan = java.lang.Double.parseDouble(computedSpanPlusScores.split("@")[2])*((double) freq);
		    		totalParsesForGivenSpan += freq;
		    		totalUPforGivenSpan += UPForGivenSpan; totalURforGivenSpan += URForGivenSpan; 
		    	}
		    	System.out.println("Given span: " + givenSpan + "; average UP=" + numberFormatter.format(totalUPforGivenSpan/((double) totalParsesForGivenSpan)) + "; average UR=" + numberFormatter.format(totalURforGivenSpan/((double) totalParsesForGivenSpan)) + "; # sentences=" + totalParsesForGivenSpan);
		    }
		}
	}


	public static void precisionRecall_all(parseTree givenParseTree,
			parser.evaluator myEvaluator, int sentenceCounterForPrecisionRecall, int sentenceCounter,
			int sentenceLength, parseTree computedParseTree) {
		
		NumberFormat numberFormatter = new DecimalFormat("#.#####");
		double LP;
		double LR;
		//public int nrMatchingConstituents=0, nrConstituentsOfComputedParse=0, nrConstituentsOfGivenParse=0;
		int nrMatchingConstituents=0, nrConstituentsOfComputedParse=0, nrConstituentsOfGivenParse=0;
		
		if (!parameters.DO_UNLABELED_PRECISION_AND_RECALL)
			nrMatchingConstituents = parser.evaluator.doPARSEVAL(computedParseTree, givenParseTree);
		else //if DO_UNLABELED_PRECISION_AND_RECALL
			nrMatchingConstituents = parser.evaluator.doUPARSEVAL(computedParseTree, givenParseTree);
		myEvaluator.nrTotalMatchingConstituents += nrMatchingConstituents;

		nrConstituentsOfComputedParse = parser.Utils.computeUniqueBrackets(computedParseTree.getNodes());
		myEvaluator.nrTotalConstituentsOfComputedParse += nrConstituentsOfComputedParse;
 
		nrConstituentsOfGivenParse = parser.Utils.computeUniqueBrackets(givenParseTree.getNodes());
		myEvaluator.nrTotalConstituentsOfGivenParse += nrConstituentsOfGivenParse;

		System.out.println("given parse: " +  givenParseTree.printWSJFormat());
		System.out.println("given parse spans: " + givenParseTree.printSpans() + "; computed parse spans: " + computedParseTree.printSpans());
		System.out.println("nrMatchingConstituents=" + nrMatchingConstituents + "; nrConstituentsOfGivenParse=" + nrConstituentsOfGivenParse + "; nrConstituentsOfComputedParse=" + nrConstituentsOfComputedParse);
		
		//if nrConstituentsOfGivenParse = 0, or nrConstituentsOfComputedParse=0, then don't include
		
		LP = ((double) nrMatchingConstituents)/((double) nrConstituentsOfGivenParse);
		LR = ((double) nrMatchingConstituents)/((double) nrConstituentsOfComputedParse);
		myEvaluator.totalLP = myEvaluator.totalLP + LP;
		myEvaluator.totalLR = myEvaluator.totalLR + LR;

		if (LP==1. && LR==1.) {
			myEvaluator.nrExactMatch++;
			boolean exactMatch = true;
		}
		
		String strLR = "LR", strLP = "LP";
		if (parameters.DO_UNLABELED_PRECISION_AND_RECALL) { strLR = "UR"; strLP = "UP";}
		System.out.println("Test Sentence " + sentenceCounter + ": " + strLP + "=" + LP + "; " + strLR + "=" + LR);
		
		//baselines : right branching and left branching statistics
		if (parameters.COMPUTE_BASELINES_PRECISION_AND_RECALL) {
		    int nrMatchingConstituentsRB = parser.evaluator.doRightBranchingUPARSEVAL(givenParseTree, sentenceLength);
		    myEvaluator.nrTotalMatchingConstituentsRB += nrMatchingConstituentsRB;
		    myEvaluator.nrTotalConstituentsOfRBParse += (sentenceLength -1);
		    int nrMatchingConstituentsLB = parser.evaluator.doLeftBranchingUPARSEVAL(givenParseTree, sentenceLength);
		    myEvaluator.nrTotalMatchingConstituentsLB += nrMatchingConstituentsLB;
		    myEvaluator.nrTotalConstituentsOfLBParse += (sentenceLength -1);
		}
		
		//save it for file output
		//if (!SEMI_SUPERVISED_MODE) {
		myEvaluator.UPMatrix.add(LP); myEvaluator.URMatrix.add(LR);
			if (sentenceCounterForPrecisionRecall%100==0) {

				myEvaluator.UPMatrixGrouped.add(LP); myEvaluator.URMatrixGrouped.add(LR);
			}
			else {
				//System.out.println("k%10<>0; k=" + k + "; UPMatrixGrouped.size()-1=" + (UPMatrixGrouped.size()-1));
				myEvaluator.UPMatrixGrouped.set(myEvaluator.UPMatrixGrouped.size()-1, myEvaluator.UPMatrixGrouped.get(myEvaluator.UPMatrixGrouped.size()-1) + LP);
				myEvaluator.URMatrixGrouped.set(myEvaluator.URMatrixGrouped.size()-1, myEvaluator.URMatrixGrouped.get(myEvaluator.URMatrixGrouped.size()-1) + LR);
				//System.out.println("UPMatrixGrouped=" + (URMatrixGrouped.get(URMatrixGrouped.size()-1) + UR));
			}
		
		//group UP and UR according to givenSpan
		//HashMap<String, HashMap<String, Integer>> precisionAndRecallGroupedPerGivenSpan
		
		HashMap<String, Integer> precisionAndRecallFrequenciesForGivenSpan =null;
		if (myEvaluator.precisionAndRecallGroupedPerGivenSpan.get(givenParseTree.printSpans())==null) {
			precisionAndRecallFrequenciesForGivenSpan = new HashMap<String, Integer>();
			myEvaluator.precisionAndRecallGroupedPerGivenSpan.put(givenParseTree.printSpans(), precisionAndRecallFrequenciesForGivenSpan);
		}
		else precisionAndRecallFrequenciesForGivenSpan = myEvaluator.precisionAndRecallGroupedPerGivenSpan.get(givenParseTree.printSpans());
		
		//get reference to the computed spans for the given span
		//construct string from computedParseTree.printSpans() + UP + UR
		String computedSpanPlusScores = computedParseTree.printSpans() + "@" + numberFormatter.format(LP)  + "@" + numberFormatter.format(LR);
		if (precisionAndRecallFrequenciesForGivenSpan.get(computedSpanPlusScores)==null) {
			precisionAndRecallFrequenciesForGivenSpan.put(computedSpanPlusScores, 1);
		}
		else precisionAndRecallFrequenciesForGivenSpan.put(computedSpanPlusScores, precisionAndRecallFrequenciesForGivenSpan.get(computedSpanPlusScores) + 1);
		//}
	
	}
	
	
	private static void printUPandUR(NumberFormat numberFormatter,
			ArrayList<Double> UPMatrix, ArrayList<Double> URMatrix,
			ArrayList<Double> UPMatrixGrouped, ArrayList<Double> URMatrixGrouped)
			throws IOException {
		BufferedWriter outScores = new BufferedWriter(new FileWriter("./UPandUR.csv"));

		double totalUP = 0., totalUR=0.;
		for (int i = 0; i< UPMatrix.size(); i++) {
			outScores.write(numberFormatter.format(UPMatrix.get(i)) + "," + numberFormatter.format(URMatrix.get(i)));
			outScores.newLine();
			totalUP += UPMatrix.get(i);
			totalUR += URMatrix.get(i);
		}
		outScores.newLine();
		outScores.write(numberFormatter.format(totalUP/UPMatrix.size()) + "," + numberFormatter.format(totalUR/UPMatrix.size()));
		outScores.newLine();
		outScores.newLine();
		outScores.write("Averages");
		outScores.newLine();
		for (int i = 0; i< UPMatrixGrouped.size(); i++) {
			outScores.write(numberFormatter.format(UPMatrixGrouped.get(i)) + "," + numberFormatter.format(URMatrixGrouped.get(i)));
			outScores.newLine();
		}
		
		outScores.flush();
		outScores.close();
	}
	
	
   /* this method returns the number of matching constituents between the two trees
     *LP and LR are not calculated here
     */
    public static int doLeftBranchingUPARSEVAL(parseTree parseTree1, int nrWordsInSentence) {
        int nrMatchingConstituents = 0;
        HashSet<String> listOfUniqueBrackets = new HashSet<String>();
        //iterate over nodes of both trees
        for(Node nodeOfTree1 : parseTree1.getNodes()) {
            //skip terminals
            if (nodeOfTree1.getType()==parameters.NONTERMINAL && nodeOfTree1.getChildNodes().size()>1 ) { 
                //check spans: Left spans are (0,2), (0,3) ... (0,nrWords)
                for (int myRightSpan = 2; myRightSpan <= nrWordsInSentence; myRightSpan++) {
                    if ((nodeOfTree1.getLeftSpan()==0) && (nodeOfTree1.getRightSpan()==myRightSpan)) {
                        listOfUniqueBrackets.add(nodeOfTree1.getLeftSpan() + "_" + nodeOfTree1.getRightSpan());
                        //nrMatchingConstituents++;
                        break;
                    }
                }     
            }
        }
        return listOfUniqueBrackets.size(); //nrMatchingConstituents;
    }
    
    /** this method returns the number of matching constituents between the two trees
     *LP and LR are not calculated here
     */
    public static int doRightBranchingUPARSEVAL(parseTree parseTree1, int nrWordsInSentence) {
        int nrMatchingConstituents = 0;
        HashSet<String> listOfUniqueBrackets = new HashSet<String>();
        //iterate over nodes of both trees
        for(Node nodeOfTree1 : parseTree1.getNodes()) {
            //skip terminals
            if (nodeOfTree1.getType()==parameters.NONTERMINAL && nodeOfTree1.getChildNodes().size()>1 ) { 
                //check spans Right spans are (0,3), (1,3) ... (nrWords-1,nrWords)
                for (int myLeftSpan = 0; myLeftSpan <= nrWordsInSentence-2; myLeftSpan++) {
                    if ((nodeOfTree1.getLeftSpan()==myLeftSpan) && (nodeOfTree1.getRightSpan()==(nrWordsInSentence))) {
                        listOfUniqueBrackets.add(nodeOfTree1.getLeftSpan() + "_" + nodeOfTree1.getRightSpan());
                        //nrMatchingConstituents++;
                        break;
                    }
                }     
            }
        }
        return listOfUniqueBrackets.size(); //nrMatchingConstituents;
    }
    
    /*
    public int doUPARSEVAL(parseTree parseTree1, parseTree parseTree2) {
        int nrMatchingConstituents = 0;
        //iterate over nodes of both trees
        for(Node nodeOfTree1 : parseTree1.getNodes()) {
            //skip terminals, skip TOP XXX also skip preterminals???
            if (nodeOfTree1.getType()==Main.NONTERMINAL && nodeOfTree1.getChildNodes().size()>1 && !nodeOfTree1.getName().equals("TOP")) { 
                for(Node nodeOfTree2 : parseTree2.getNodes()) {
                    //may be unnecessary
                    if (nodeOfTree2.getType()==Main.NONTERMINAL && nodeOfTree2.getChildNodes().size()>1 && !nodeOfTree2.getName().equals("TOP")) { 
                        //check spans
                        if ((nodeOfTree1.getLeftSpan()==nodeOfTree2.getLeftSpan()) && (nodeOfTree1.getRightSpan()==nodeOfTree2.getRightSpan())) {
                            nrMatchingConstituents++;
                            break;
                        }
                    }
                }
            }
        }
        return nrMatchingConstituents;
    }
    */
    /* this method returns true if the parses are identical (according to PARSEVAL measure)
     
    public boolean identicalParses(parseTree parseTree1, parseTree parseTree2) {
        
        //iterate over nodes of both trees
        boolean existsIdenticalNode;
        for(Node nodeOfTree1 : parseTree1.getNodes()) {
            existsIdenticalNode = false;
            for(Node nodeOfTree2 : parseTree2.getNodes()) {
                if (nodeOfTree1.getName().equals(nodeOfTree2.getName())){
                    //check spans
                    if ((nodeOfTree1.getLeftSpan()==nodeOfTree2.getLeftSpan()) && (nodeOfTree1.getRightSpan()==nodeOfTree2.getRightSpan())) {
                        existsIdenticalNode = true;
                        break;
                    }   
                }
            }
            if(!existsIdenticalNode) return false;
        }
        return true;
    }
     **/
}
