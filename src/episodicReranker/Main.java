package episodicReranker;


import java.io.BufferedWriter;
import java.io.FileNotFoundException;
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

import parser.Node;
import parser.parseTree;


public class Main {

	public final static int EPISODIC_TOPDOWN_MODEL = 1;
    public final static int EPISODIC_LEFTCORNER_MODEL = 2;
    public final static int PCFG = 4;
    public final static int Manning_CarpenterLCP = 5;
   
	/**
	 * Choose the probability model of the parser
	 * @param  leftcorner_with_path episodic left corner shifting parser, with rankedETNs
	 * @param topdown_with_path episodic top-down parser, with rankedETNs
	 * @param PCFG standard PCFG parser, not episodic. calls PCFG class.
	 * @param Manning_CarpenterLCP Manning and Carpenter probability model
	 * (non-episodic) with shift probabilities. calls lcsGrammar class.
	 */
	//public static String PROBABILITY_MODEL = "leftcorner_with_path";	// PCFG  // Manning_CarpenterLCP // leftcorner_with_path // notclear_with_path // topdown_with_path 
    public static int PROBABILITY_MODEL = EPISODIC_LEFTCORNER_MODEL;
    
	/**
	 * Select either one of the following experiments
	 */
	protected static boolean COMPUTE_CORPUS_LIKELIHOOD = false;
	
	protected static boolean DO_RERANKER = true;
	protected static int nBest=5;
	/**
	 * Experiment to find fragments using the shortest derivation.
	 * In this case no likelihood is computed, or reranking done.
	 * Assumes EPISODIC_LEFTCORNER_MODEL, not implemented for EPISODIC_TOPDOWN_MODEL
	 */
	public static boolean COMPUTE_SHORTEST_DERIVATION;
	public static boolean COMPUTE_GREEDY_SHORTEST_DERIVATION = false; //obsolete...
	public static boolean COMPUTE_SHORTEST_DERIVATION_RERANKER = false;
	public static boolean COMPUTE_SHORTEST_DERIVATION_LIKELIHOOD = false;
	protected static boolean INCLUDE_RECENT_SHORTEST_DERIVATIONs = false;
	
	public static boolean EXTRA_ATTACHMENT_IN_TD_DERIVATION = false;
	protected static boolean ATTACH_CHOICE = false;
	
	protected static int COUNT_FRAGMENTS_OF_LENGTH_GREATER_THAN =2;
	protected static int RECENCY =5;
	protected static int fragmentNr =100000;
	protected static HashMap<Integer, HashSet<Integer>> fragmentNrsCoupledToSentenceNr = new HashMap<Integer, HashSet<Integer>>();
	
	protected static boolean BUILD_CHART = false;
	
	public static boolean DO_LABELED_PRECISION_AND_RECALL = true;
		
	/**
	 * By default set to true if left corner shifting grammar 
	 * (leftcorner_with_path) is used as PROBABILITY_MODEL.
	 * This reads shift productions (X*-->w) from the treebank,
	 * and estimates their probabilities.
	 */
	//public static boolean INCLUDE_SHIFT_PRODUCTIONS = true; //are always included for left corner models
	
		
	/**
	 * Takes into account discontiguities in the computation of the 
	 * likelihood of a sentence, by allowing to reset the pathLength
	 * (activation) of a trace to the activation of the left sister
	 * starred nonterminal after every attach operation.
	 */
	protected static boolean INCLUDE_DISCONTIGUITIES = false;
	
	protected static double DISCTG_DECAY = 0.9;
	
	protected static boolean LINEAR_DECAY = false;
	
	protected static boolean SYNTACTIC_PRIMING = false;
	
	protected static double DISCTG_DECAY_SENTENCE_LEVEL = 0.8; //used in combination with SYNTACTIC_PRIMING
	
	protected static boolean INCLUDE_DISCONTIGUITIES_OLD = false;
	
	/**
	 * Takes into account discontiguities by querying for traces with
	 * any rankNr lower than the current, instead of requiring the rankNr
	 * to be 1 lower than the current.
	 */
	protected static boolean INCLUDE_DISCONTIGUITIES_ALTERNATIVE = false;
	
	/**
	 * Takes the average weight between the non-discontiguous and the discontiguous
	 * weight if the latter is higher? you should do it also if the disctg is lower?
	 */
	protected static boolean DISCONTIGUITIES_AVERAGED = true;
	protected static double DISCONTIGUITIES_REDUCTION = 0.5;
	
	/**
	 * LAMBDA_SMOOTHING: smoothes unknown productions, by add-lambda
	 * (1 - lambda1)*BoffLevel1 + lambda1*((1 - lambda2)*BoffLevel2 + lambda2*BoffLevel3)			
	 */
	protected static double LAMBDA_1 = 0.2;
	protected static double LAMBDA_2 = 0.2;
	protected static double LAMBDA_3 = 0.2;
	
	/**
	 * Used for exponential weighing of the common path in episodic parser. 
	 * weight = Math.pow(BACK_OFF_FACTOR, (history));
	 */
	protected static double BACK_OFF_FACTOR = 4.;
	protected static boolean POLYNOMIAL_WEIGHTS = false;
	
	protected static int MAX_SENTENCE_SIZE_FOR_TESTSENTENCES = 40;
	
	//SMOOTHING parameters
	protected static boolean SMOOTH_FOR_UNKNOWN_WORDS = true;
	
	/**
	 * Replaces unknown word ending on -s with its stem if the stem
	 * occurs in train treebank, and vice versa
	 */
	protected static boolean DO_LEMMATIZATION = false;
	
	/**
	 * Creates all binary rules from all possible combinations of nonterminals
	 * and preterminal rules from all possible combinations of preterminals and words
	 * and adds these in the relative frequency counts  
	 * Only works if DO_HORIZONTAL_MARKOVIZATION = true;
	 */
	public static boolean SMOOTH_FOR_UNKNOWN_PRODUCTIONS = true;
	/**
	 * Word types in treebank with frequency < threshold are 
	 * replaced by unknown word classes
	 */
	protected static int WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING = 4;
	
	/**
	 * Only if there are at least this number of types (unique words)
	 * in the unknown word class they get their own unknown word class
	 * otherwise the unknown words of this class are merged with the "unk" class
	 */
	protected static int MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS =3;	
	
	/**
	 * For unknown word smoothing: converts all open class words in initial 
	 * sentence position in the treebank to lower case (useless for Tuebingen corpus?)
	 */
	protected static boolean CONVERT_INITIAL_WORD2LOWERCASE = false;
	
	/**
	 * For smoothing: binarizes productions in the treebank and test set. 
	 * Invokes the horizontal_markovization method.
	 */
	public static boolean DO_HORIZONTAL_MARKOVIZATION = true;
	
	/**
	 * Default false. For compliance with HPN-style grammar.
	 */
	public static boolean REMOVE_UNARY_NODES_FROM_PARSETREES = false;
	public static boolean BLN_TUEBINGEN = false;
	
	public static boolean FEEDBACK = false;
	
	/**
	 * MIN_HISTORY and MAX_HISTORY are used to parametrize the probability
	 * model, according to how much history (context) is taken into account.
	 * MAX_HISTORY cuts off the common history (pathlength) of sequences of traces
	 * at a certain value, such that all pathlengths that are longer than this
	 * are considered the same length.
	 * history=0 corresponds to the PCFG respectively M&C model: P_episodic=0
	 */
	protected static int MIN_HISTORY = 0;	//set this to 1 if you want parametrized histories, set it equal to MAX_HISTORY if you only want MAX_HISTORY
	protected static int MAX_HISTORY = 14;

	//protected static HashSet<String> shiftProductionsEncounteredInTrainSet = null;
	
	protected static int nrTrainSentences = 0;
	
	/**
	 * Used to restrict the test to fewer sentences in reranker, 
	 * and at the same time not loose synchrony between gold standard and NBest
	 */
	protected static int FIRSTTESTSENTENCE = 0;
	protected static int LASTTESTSENTENCE = 3000;

	protected static boolean PRINT_LATEX_TRAINTREES = false;
	protected static boolean PRINT_LATEX_TESTTREES = false;
	protected static boolean PRINT_LATEX_TREES_WITH_RANKNRS = true;
	protected static boolean PRINT_LEFT_CORNER_PROBABILITIES = false;
	
	/**
	 * Precomputed weights per pathlength/history, for weighing the contribution of a trace (ETN)
	 * in the parse move decision by its history. A weight equals back-off-value ^(history)
	 */
	protected static HashMap<Integer, Double> weightLookupTable = new HashMap<Integer, Double>();
	
	//public static boolean CREATE_HPN_STYLE_LABELS = false;	//duplicates labels for different numbers of slots
	
	/**
	 * Shortest fragments: String=fragment (series of bindings);
	 * ArrayList<Integer>=sentenceNrs in which fragment occurs 
	 * (only for printing statistics; see also episodicGrammar.shortestDerivations) 
	 */
	protected static HashMap<String, ArrayList<Integer>> fragments = new HashMap<String, ArrayList<Integer>>();
	
	protected static String RERANKER_STATISTICS_FILE = "reranker_statistics_lc_nodisctg_nbest20.csv";
	protected static String FSCORES_FILE = "Fscores_lc_nodisctg_nbest20.csv";
	
	static long startTime =0;
	
	///////////////////////////////////////////////////
	/////////////////      DATA        ////////////////
	///////////////////////////////////////////////////

	protected static String TRAIN_CORPUS = "./input/wsj-02-21clean_shorter.txt";	//wsj-02-21clean_short.txt // wsj-02-21clean.mrg // toebingen10_train_new// Tueb_filtered// abc //  toebingen10_train // Elman_testsentences_shorter // Elman_trainsentences_disctg // attachment_ambiguity // WSJtrain_full_sample
	protected static String TEST_CORPUS = "./input/wsj-22clean.mrg";	// wsj-22clean.mrg // wsj-22clean_forNBest20 // disctgtest_goldstnd // wsj22auxified // WSJ22test_full_GOLD_STANDARD // toebingen10_test // Elman_testsentences_test //	Elman_testsentences_disctg // XabYc XaYbc	
	protected static String nBestCharniakParsesFile = "./input/WSJ22Charniak_best10_noAux.txt";	//WSJ22Charniak_best10_noAux // WSJ22Charniak_best20f // disctgtest_10best

	/**
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {

		startTime = System.currentTimeMillis();
		
		// accept arguments for Lambda's, for nBest, for topdown/lc 
		if (COMPUTE_SHORTEST_DERIVATION_RERANKER || COMPUTE_SHORTEST_DERIVATION_LIKELIHOOD) {
			COMPUTE_SHORTEST_DERIVATION = true;
			INCLUDE_DISCONTIGUITIES = false;
		}
		else COMPUTE_SHORTEST_DERIVATION = false;
		
    	readCommandLineArgs(args);
    	
		setDOPParserParameters(TRAIN_CORPUS);
		
		//precompute the weights for traces as function of history
		double weight=0.;
		for (int history=1; history<=20; history++) {
			if (POLYNOMIAL_WEIGHTS) weight = Math.pow(((double) history), BACK_OFF_FACTOR);
			else weight = Math.pow(BACK_OFF_FACTOR, ((double) history-1));	//weight=1 is reserved for back-off
			weightLookupTable.put(history, weight);
		}
		
		///////////////////////////////////////////////////////
		////////////         READING TRAIN          ///////////
		///////////////////////////////////////////////////////

		//reading train sentences
		System.out.println("Reading treebank...");
		
		
		sentencePreprocessor myTrainSetPreprocessor = new sentencePreprocessor(TRAIN_CORPUS, true);

		print_parameters_to_console();
	
		//includes horizontal Markovization
    	myTrainSetPreprocessor.preProcess(REMOVE_UNARY_NODES_FROM_PARSETREES, DO_HORIZONTAL_MARKOVIZATION, true);
		
    	sentencePreprocessor.duplicateSentenceNrsInTrainSet = myTrainSetPreprocessor.findDuplicates();
		
		ArrayList<parseTree> preprocessedParseTreesFromTreebank = myTrainSetPreprocessor.getPreprocessedParseTrees();
		
		//smoothing
		if (SMOOTH_FOR_UNKNOWN_WORDS) 
			myTrainSetPreprocessor.smoothUnknownWordsInTrainSet(preprocessedParseTreesFromTreebank);
		 
			
		
		//############################################
		//#############      TRAIN      ##############
		//############################################
		//train method either fills nodes of episodicGrammar with ranked ETNs, or if PROBABILITY_MODEL = "PCFG" learns rulesOfPCFG
		/**
		 * Trains treebank grammar, depending on chosen PROBABILITY_MODEL
		 * episodicGrammar, lcsGrammar, or PCFG.
		 */
		grammar myGrammar = train(preprocessedParseTreesFromTreebank);
		
			
		//evaluate only on sentence length <=40
		parser.parameters.MAX_SENTENCE_SIZE = MAX_SENTENCE_SIZE_FOR_TESTSENTENCES;
		
		
		if (COMPUTE_SHORTEST_DERIVATION_LIKELIHOOD) {
			
			computeShortestDerivation(myGrammar);
		}
		if (COMPUTE_CORPUS_LIKELIHOOD)
			computeLikelihoodOfTestCorpus(myGrammar, TEST_CORPUS);
		if (DO_RERANKER) {			
			reranker.rerankAndEvaluateBestCharniakParses(myGrammar, TEST_CORPUS, nBestCharniakParsesFile, nBest);
		}

		
		long endTime = System.currentTimeMillis();
		System.out.println("Done. Time elapsed=" + (endTime-startTime) + " milliseconds");
	}

	private static void computeShortestDerivation(grammar myGrammar)
			throws FileNotFoundException, IOException {
		boolean INCLUDE_DISCONTIGUITIES_ALTERNATIVEwas = INCLUDE_DISCONTIGUITIES_ALTERNATIVE;
		boolean INCLUDE_DISCONTIGUITIESwas = INCLUDE_DISCONTIGUITIES;
		
		INCLUDE_DISCONTIGUITIES_ALTERNATIVE = false;
		INCLUDE_DISCONTIGUITIES = false;
		PROBABILITY_MODEL = EPISODIC_LEFTCORNER_MODEL;

		//this in fact computes shortest derivations
		computeLikelihoodOfTestCorpus(myGrammar, TEST_CORPUS);
		
		/*
		boolean evenoverslaan = true;
		if (!evenoverslaan) {
		//put fragments as ETNs (traces) in episodic constituents
		//check fragmentIDs in epCst
		episodicGrammar myEGrammar = (episodicGrammar) myGrammar;
		for (String prod : myEGrammar.episodicNonTerminalProductions.keySet()) {
			episodicConstituent thisConstituent = myEGrammar.episodicNonTerminalProductions.get(prod);
			int totalETNs = 0;
			for (Integer sentenceNr : thisConstituent.getRankedETNs().keySet()) {
				int nrETNsWithSameSentenceNr= thisConstituent.getRankedETNs().get(sentenceNr).size();
				//System.out.println(sentenceNr + " has ");
				totalETNs += nrETNsWithSameSentenceNr;
			}
			System.out.println("epCst: " + prod + " has " + totalETNs + " fragments.");
			
		}
		
		for (int sentenceCounter=0; sentenceCounter< episodicGrammar.shortestDerivationFragments.size(); sentenceCounter++) { 
			//was: HashSet<ArrayList<episodicConstituent>> fragmentsOfOneSentence = episodicGrammar.shortestDerivationFragments.get(sentenceCounter);
			HashSet<ArrayList<String>> fragmentsOfOneSentence = episodicGrammar.shortestDerivationFragments.get(sentenceCounter);
			int fragmentNr =0;
			if (!(fragmentsOfOneSentence==null)) {
			//for (ArrayList<episodicConstituent> fragment : fragmentsOfOneSentence) {
				for (ArrayList<String> fragment : fragmentsOfOneSentence) {
				//System.out.println("fragment " + fragmentCounter + " of sentence " + sentenceCounter + " with productions:");
				//fragmentID = sentenceNr + "." + fragmentNr is assigned in method
				
				for ( int rankNr =0; rankNr<fragment.size()-1; rankNr++) {
					
					String binding = fragment.get(rankNr);
					episodicConstituent thisConstituent = myEGrammar.episodicNonTerminalProductions.get(binding.split("~")[0]);
					
					//was: episodicConstituent thisConstituent = fragment.get(rankNr);
					//was: episodicConstituent nextConstituent = fragment.get(rankNr+1);
					//System.out.println(myProduction.getUniqueSymbol());
					//Integer[] ETN is format {SequenceNr, HashCodeOfBoundEpsidodicUnit}
					//public int addRankedETN(Integer[] ETN, int sentenceNr, int fragmentNr) {
					//was: Integer[] myETN = {rankNr, nextConstituent.hashCode()};
					Integer[] myETN = {rankNr, binding.split("~")[1].hashCode()};
					thisConstituent.addRankedETN_forShortestDerivation(myETN, sentenceCounter, fragmentNr);	
					//	double fragmentId = java.lang.Double.parseDouble("" + sentenceNr + "." + fragmentNr);
					
					rankNr++;
				}
				fragmentNr++;
			}}
		}	//sentenceCounter
		
		//recheck
		for (String prod : myEGrammar.episodicNonTerminalProductions.keySet()) {
			episodicConstituent thisConstituent = myEGrammar.episodicNonTerminalProductions.get(prod);
			int totalETNs = 0;
			for (Integer sentenceNr : thisConstituent.getRankedETNs().keySet()) {
				int nrETNsWithSameSentenceNr= thisConstituent.getRankedETNs().get(sentenceNr).size();
				//System.out.println(sentenceNr + " has ");
				totalETNs += nrETNsWithSameSentenceNr;
			}
			System.out.println("epCst: " + prod + " has " + totalETNs + " fragments.");
		}
		}	*///if (!evenoverslaan) {
		
		//todo: zet defaults terug
		INCLUDE_DISCONTIGUITIES_ALTERNATIVE = INCLUDE_DISCONTIGUITIES_ALTERNATIVEwas;
		INCLUDE_DISCONTIGUITIES = INCLUDE_DISCONTIGUITIESwas;
		
		COMPUTE_SHORTEST_DERIVATION = false;	//in case you want to continue with likelihood computatino
		INCLUDE_RECENT_SHORTEST_DERIVATIONs = true;	//starts to add and remove ETNs for shortest derivation sentences
	}

	private static void print_parameters_to_console() {
		System.out.println("PARAMETER SETTINGS:");
    	String probability_model="";
		switch (Main.PROBABILITY_MODEL) {
        case 1: probability_model = "EPISODIC_TOPDOWN_MODEL"; break;
        case 2: probability_model = "EPISODIC_LEFTCORNER_MODEL"; break;
        case 3: probability_model = "EPISODIC_UNCLEAR_MODEL"; break;
        case 4: probability_model = "PCFG"; break;
        case 5: probability_model = "Manning_CarpenterLCP"; break;
        }
    	System.out.println("PROBABILITY_MODEL=" + probability_model);
    	if (EXTRA_ATTACHMENT_IN_TD_DERIVATION) System.out.println("EXTRA_ATTACHMENT_IN_TD_DERIVATION=true");
    	System.out.println("TRAIN_CORPUS=" + TRAIN_CORPUS);
    	System.out.println("TEST_CORPUS=" + TEST_CORPUS);
    	System.out.println("RERANKER_STATISTICS_FILE=" + RERANKER_STATISTICS_FILE);
    	System.out.println("FSCORES_FILE=" + FSCORES_FILE);
    	System.out.println("nBest=" + nBest);
    	System.out.println("LAMBDA_1=" + LAMBDA_1);
    	System.out.println("LAMBDA_2=" + LAMBDA_2);
    	System.out.println("LAMBDA_3=" + LAMBDA_3);
    	System.out.println("BACK_OFF_FACTOR=" + BACK_OFF_FACTOR);
    	System.out.println("POLYNOMIAL_WEIGHTS=" + POLYNOMIAL_WEIGHTS);
    	System.out.println("INCLUDE_DISCONTIGUITIES=" + INCLUDE_DISCONTIGUITIES);
    	System.out.println("COMPUTE_SHORTEST_DERIVATION_RERANKER=" + COMPUTE_SHORTEST_DERIVATION_RERANKER);
    	//System.out.println("INCLUDE_DISCONTIGUITIES_ALT=" + INCLUDE_DISCONTIGUITIES_ALTERNATIVE);
    	if (INCLUDE_DISCONTIGUITIES) {
    		System.out.println("DISCTG_DECAY=" + DISCTG_DECAY);
    		System.out.println("DISCTG_AVERAGED=" + DISCONTIGUITIES_AVERAGED);
    		System.out.println("DISCTG_REDUCTION=" + DISCONTIGUITIES_REDUCTION);
    		System.out.println("SYNTACTIC_PRIMING=" + SYNTACTIC_PRIMING);   		
    	}
    	System.out.println("CONVERT_INITIAL_WORD2LOWERCASE=" + CONVERT_INITIAL_WORD2LOWERCASE); 
    	System.out.println("DO_LEMMATIZATION=" + DO_LEMMATIZATION);
    	System.out.println("LASTTESTSENTENCE=" + LASTTESTSENTENCE);
	}

	public static void putETNsFromOneShortestDerivationInEpisodicConstituents(int sentenceCounter, episodicGrammar myEGrammar, boolean bln_print) {
		
		//ArrayList<String> contains the bindings of one fragment from shortest derivation
		HashSet<ArrayList<String>> fragmentsOfOneSentence = episodicGrammar.shortestDerivationFragments.get(sentenceCounter);

		if (!(fragmentsOfOneSentence==null)) {
			HashSet<Integer> fragmentNrsOfThisShortestDerivation = new HashSet<Integer>();
			fragmentNrsCoupledToSentenceNr.put(sentenceCounter, fragmentNrsOfThisShortestDerivation);

			//a fragment is an ArrayList of bindings
			for (ArrayList<String> fragment : fragmentsOfOneSentence) {
				//System.out.println("fragment " + fragmentCounter + " of sentence " + sentenceCounter + " with productions:");
				//fragmentID = sentenceNr + "." + fragmentNr is assigned in method

				for ( int rankNr =0; rankNr < (fragment.size()-1); rankNr++) {

					String binding = fragment.get(rankNr);
					episodicConstituent thisConstituent = myEGrammar.episodicNonTerminalProductions.get(binding.split("~")[0]);

					//was: episodicConstituent thisConstituent = fragment.get(rankNr);
					//was: episodicConstituent nextConstituent = fragment.get(rankNr+1);
					//System.out.println(myProduction.getUniqueSymbol());
					//Integer[] ETN is format {SequenceNr, HashCodeOfBoundEpsidodicUnit}
					//public int addRankedETN(Integer[] ETN, int sentenceNr, int fragmentNr) {
					//was: Integer[] myETN = {rankNr, nextConstituent.hashCode()};
					Integer[] myETN = {rankNr, binding.split("~")[1].hashCode()};
					//thisConstituent.addRankedETN_forShortestDerivation(myETN, sentenceCounter, fragmentNr);	
					thisConstituent.addRankedETN(myETN, fragmentNr);
					if (bln_print) System.out.println("Added fragment to epCst: " + binding.split("~")[0]); 
					//	double fragmentId = java.lang.Double.parseDouble("" + sentenceNr + "." + fragmentNr);

				}
				fragmentNrsOfThisShortestDerivation.add(fragmentNr);
				if (bln_print) System.out.println("Added fragment " + fragmentNr + " with " + (fragment.size()-1) + " rankNrs for sentence " + sentenceCounter);
				fragmentNr++;
			}
		}	//if (!(fragmentsOfOneSentence==null)) {
	}

	public static void removeETNsFromOneShortestDerivationInEpisodicConstituents(int sentenceCounter, episodicGrammar myEGrammar) {
		HashSet<ArrayList<String>> fragmentsOfOneSentence = episodicGrammar.shortestDerivationFragments.get(sentenceCounter);
		//int fragmentNr =0;
		if (!(fragmentsOfOneSentence==null)) {
		//for (ArrayList<episodicConstituent> fragment : fragmentsOfOneSentence) {
			for (ArrayList<String> fragment : fragmentsOfOneSentence) {	//String is binding
			//System.out.println("fragment " + fragmentCounter + " of sentence " + sentenceCounter + " with productions:");
			//fragmentID = sentenceNr + "." + fragmentNr is assigned in method
			
				//loop over bindings of the fragment, to find episodicConstituent
			for (int rankNr =0; rankNr<fragment.size()-1; rankNr++) {
				
				String binding = fragment.get(rankNr);
				episodicConstituent thisConstituent = myEGrammar.episodicNonTerminalProductions.get(binding.split("~")[0]);
				
				thisConstituent.removeETNs(sentenceCounter);	
				
				rankNr++;
			}
			fragmentNr++;
		}}
	}
		
	private static void readCommandLineArgs(String[] args)
			throws NumberFormatException {
		for (String s: args) {
    		
    		if (s.toLowerCase().startsWith("experiment=")) {
    			if (s.split("=")[1].equals("reranker")) {
    				COMPUTE_CORPUS_LIKELIHOOD = false;
    				DO_RERANKER = true;
    				BUILD_CHART = false;
    			}
    			if (s.split("=")[1].equals("likelihood")) {
    				COMPUTE_CORPUS_LIKELIHOOD = true;
    				DO_RERANKER = false;
    				BUILD_CHART = false;
    			}
    		}
    		
    		if (s.toLowerCase().startsWith("shortest_derivation=")) {
    			if (s.split("=")[1].toLowerCase().equals("true")) {
    				COMPUTE_SHORTEST_DERIVATION_RERANKER = true;
    				COMPUTE_SHORTEST_DERIVATION = true;
    			}
    			else {
    				COMPUTE_SHORTEST_DERIVATION_RERANKER = false;
    				COMPUTE_SHORTEST_DERIVATION = false;
    			}
    		}
    				
    		
    		if (s.toLowerCase().startsWith("model=")) {
    			String prob_model = s.split("=")[1].toLowerCase();
    		
    			if (prob_model.equals("episodic_topdown")) 
    				PROBABILITY_MODEL = EPISODIC_TOPDOWN_MODEL;
    			if (prob_model.equals("episodic_leftcorner")) 
    				PROBABILITY_MODEL = EPISODIC_LEFTCORNER_MODEL; 
    			if (prob_model.equals("pcfg"))
    				PROBABILITY_MODEL = PCFG; 
    			if (prob_model.equals("Manning_Carpenter"))
    				PROBABILITY_MODEL = Manning_CarpenterLCP; 
    	        //default: System.out.println("Invalid probability model.");break;       						
    		}
    		
    				
    		if (s.toLowerCase().startsWith("traincorpus=")) 
    			TRAIN_CORPUS = "./input/" + s.split("=")[1];
    		
    		if (s.toLowerCase().startsWith("testcorpus=")) 
    			TEST_CORPUS = "./input/" + s.split("=")[1];
    		
    		if (s.toLowerCase().startsWith("nbest_parsefile=")) 
    			nBestCharniakParsesFile = "./input/" + s.split("=")[1];
    		
    		
    		if (s.toLowerCase().startsWith("statisticsfile=")) 
    			RERANKER_STATISTICS_FILE =  s.split("=")[1];
    		
    		if (s.toLowerCase().startsWith("fscoresfile=")) 
    			FSCORES_FILE = s.split("=")[1];
    		
    		if (s.toLowerCase().startsWith("nbest=")) 
    			nBest=java.lang.Integer.parseInt(s.split("=")[1]);
    		
    		if (s.toLowerCase().startsWith("max_sentencelength=")) 
    			MAX_SENTENCE_SIZE_FOR_TESTSENTENCES=java.lang.Integer.parseInt(s.split("=")[1]);
    		
    		
    		if (s.toLowerCase().startsWith("max_sentences=")) 
    			LASTTESTSENTENCE=java.lang.Integer.parseInt(s.split("=")[1]);
    		
    		
    		if (s.toLowerCase().startsWith("lambda1=")) 
    			LAMBDA_1 = java.lang.Double.parseDouble(s.split("=")[1]);
    		 		
    		if (s.toLowerCase().startsWith("lambda2=")) 
    			LAMBDA_2 = java.lang.Double.parseDouble(s.split("=")[1]);
    		
    		if (s.toLowerCase().startsWith("lambda3=")) 
    			LAMBDA_3 = java.lang.Double.parseDouble(s.split("=")[1]);
    		
    		if (s.toLowerCase().startsWith("back-off=")) 
    			BACK_OFF_FACTOR = java.lang.Double.parseDouble(s.split("=")[1]);
    		
    		if (s.toLowerCase().startsWith("polynomial=")) {
    			if (s.split("=")[1].toLowerCase().equals("true"))
    				POLYNOMIAL_WEIGHTS = true;
    			else POLYNOMIAL_WEIGHTS = false;
    		}
    		   		
    		if (s.toLowerCase().startsWith("ukword_threshold=")) 
    			 WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING=java.lang.Integer.parseInt(s.split("=")[1]);
     		
    		
    		if (s.toLowerCase().startsWith("lemmatization=")) {
    			if (s.split("=")[1].toLowerCase().equals("true"))
    				DO_LEMMATIZATION = true;
    			else DO_LEMMATIZATION = false;
    		}
    		
    		if (s.toLowerCase().startsWith("case_conversion=")) {
    			if (s.split("=")[1].toLowerCase().equals("true"))
    				CONVERT_INITIAL_WORD2LOWERCASE = true;
    			else CONVERT_INITIAL_WORD2LOWERCASE = false;
    		}

    		if (s.toLowerCase().startsWith("smooth_productions=")) {
    			if (s.split("=")[1].toLowerCase().equals("false"))
    				SMOOTH_FOR_UNKNOWN_PRODUCTIONS = false;
    			else SMOOTH_FOR_UNKNOWN_PRODUCTIONS = true;
    		}
    		
    		if (s.toLowerCase().startsWith("markovization=")) {
    			if (s.split("=")[1].toLowerCase().equals("false"))
    				DO_HORIZONTAL_MARKOVIZATION = false;
    			else DO_HORIZONTAL_MARKOVIZATION = true;
    		}
    			
    		if (s.toLowerCase().startsWith("discontiguities=")) {
    			if (s.split("=")[1].toLowerCase().equals("false"))
    				INCLUDE_DISCONTIGUITIES = false;
    			else INCLUDE_DISCONTIGUITIES = true;
    		}
    		
    		if (s.toLowerCase().startsWith("extra_attach=")) {
    			if (s.split("=")[1].toLowerCase().equals("false"))
    				EXTRA_ATTACHMENT_IN_TD_DERIVATION = false;
    			else EXTRA_ATTACHMENT_IN_TD_DERIVATION = true;
    		}
    		
    		if (s.toLowerCase().startsWith("syntactic_priming=")) {
    			if (s.split("=")[1].toLowerCase().equals("true"))
    				SYNTACTIC_PRIMING = true;
    			else SYNTACTIC_PRIMING = false;
    		}
    		
    		if (s.toLowerCase().startsWith("average_discontiguities=")) {	//default false
    			if (s.split("=")[1].toLowerCase().equals("true"))
    				DISCONTIGUITIES_AVERAGED = true;
    			else DISCONTIGUITIES_AVERAGED = false;
    		}
    		
    		if (s.toLowerCase().startsWith("disct_reduction=")) 
    			DISCONTIGUITIES_REDUCTION = java.lang.Double.parseDouble(s.split("=")[1]);
    		
    		if (s.toLowerCase().startsWith("disct_decay=")) 
    			DISCTG_DECAY = java.lang.Double.parseDouble(s.split("=")[1]);
    		
    		if (s.toLowerCase().startsWith("disct_decay_sentence=")) 
    			DISCTG_DECAY_SENTENCE_LEVEL = java.lang.Double.parseDouble(s.split("=")[1]);
    				
    	}
	}

	public static void setDOPParserParameters(String TRAIN_CORPUS) {
		parser.parameters.MAX_SENTENCE_SIZE = 120;	//for train corpus consider all sentences; only max size for test sentences
		//you do want to keep postags if you don't remove unary nodes
		parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES = false; 
		parser.parameters.READOFF_GRAMMAR_FROM_TREEBANK = true;
		parser.parameters.EXTRACT_POSTAGS = false;
		parser.parameters.BRACKET_FORMAT_WSJ_STYLE = true;
		if (!REMOVE_UNARY_NODES_FROM_PARSETREES) parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES = true; 
		//TAKE_OFF_SBJ: if true, disregards suffixes of the labels in gold standard, such as NP_SBJ, etc, and identifies all NPs, etc.
		parser.parameters.TAKE_OFF_SBJ = true;
		if (TRAIN_CORPUS.toLowerCase().contains("tuebingen") || TRAIN_CORPUS.toLowerCase().contains("toebingen")) {
			//parser.parameters.TAKE_OFF_SBJ = false;
			BLN_TUEBINGEN=true;
			//parser.parameters.REMOVE_PUNCTUATION = true;
		}
		
	}

	private static void computeLikelihoodOfTestCorpus(grammar myGrammar, String testCorpus)
			throws FileNotFoundException, IOException {
		
		sentencePreprocessor myTestSetPreprocessor = new sentencePreprocessor(testCorpus, false);
		

		myTestSetPreprocessor.preProcess(false, DO_HORIZONTAL_MARKOVIZATION, false);
		
		if (SMOOTH_FOR_UNKNOWN_WORDS)
			myTestSetPreprocessor.replaceUnknownWordsInTestSet((HashSet<String>) myGrammar.getTerminalUnits().keySet(), DO_HORIZONTAL_MARKOVIZATION, DO_LEMMATIZATION); //only from unknownWordClassesInTrainSet
		
		ArrayList<parseTree> parseTreesFromTestCorpus = myTestSetPreprocessor.getPreprocessedParseTrees();
		
			
		//this sets myNode.setProductionName of nodes in the the parseTree to lhs*rhs1*rhs2*etc
		for (parseTree myParseTree : parseTreesFromTestCorpus) myParseTree.assignProductionNamesToNodes();
			
		
		//skip sentences with unknown words (check lexicon)
		 ArrayList<Integer> indicesOfSentencesToSkip = skipTooLongAndSingleWordSentences(parseTreesFromTestCorpus);

		 //if (!SMOOTH_FOR_UNKNOWN_PRODUCTIONS)
		//	 indicesOfSentencesToSkip = skipSentencesWithUnknownLabels(parseTreesFromTestCorpus, indicesOfSentencesToSkip, myGrammar);
		
		
		int sentenceCounter = 0;
		double[] totalLikelihood = new double[24]; //parametrized for max history length
		
		if ( PROBABILITY_MODEL==Main.EPISODIC_TOPDOWN_MODEL  || PROBABILITY_MODEL==Main.EPISODIC_LEFTCORNER_MODEL ) {
			
			episodicGrammar myEpisodicGrammar = (episodicGrammar) myGrammar;
			int nrZeroProbabilitySentence=0;
			
			for (parseTree myTestParseTree : parseTreesFromTestCorpus) {
				
				if (!(indicesOfSentencesToSkip.contains(sentenceCounter))) {
				
					///////////////////////////////////////////////////////////////
					//////    compute log likelihood of a single sentence    //////
					///////////////////////////////////////////////////////////////
					double[] logSentenceProbability = myEpisodicGrammar.computeLikelihoodOfSentenceWHistory(sentenceCounter, parseTreesFromTestCorpus.size(), myTestParseTree, 0, new ArrayList<Integer>());
				
					System.out.println("test sentence #" + sentenceCounter + " out of " + parseTreesFromTestCorpus.size() + ": " + myTestParseTree.printWSJFormat() + "; logLikelihood=" + logSentenceProbability[MAX_HISTORY]);
				
					if (!(java.lang.Double.isInfinite(logSentenceProbability[MAX_HISTORY]))) {
					for (int history=MIN_HISTORY; history<=MAX_HISTORY; history++) totalLikelihood[history] += logSentenceProbability[history];
					}
					else nrZeroProbabilitySentence++;
				}
				else System.out.println("test sentence #" + sentenceCounter + " was skipped because too short or too long");
				
				sentenceCounter++;
			}
	
			System.out.println(">>> There are " + nrZeroProbabilitySentence + " sentences in the test set with zero probability.");
			
			//print parseTrees in Latex format (//nodes are relabeled)
			boolean printRankNrs = false;
			if (PRINT_LATEX_TREES_WITH_RANKNRS) printRankNrs = true;
	        if (PRINT_LATEX_TESTTREES) printer.printLatexTreeFile(parseTreesFromTestCorpus, myEpisodicGrammar, "./treebanks/testtrees_with_ETNs.tex", printRankNrs);
		
			//print total likelihoods
	        if (!COMPUTE_SHORTEST_DERIVATION)
	        	for (int history=MIN_HISTORY; history<=MAX_HISTORY; history++) System.out.println("totalLikelihood with history " + history + " : " + totalLikelihood[history]);
			
	        if (COMPUTE_SHORTEST_DERIVATION_LIKELIHOOD && COMPUTE_GREEDY_SHORTEST_DERIVATION) {
	        	printShortestDerivations(); 
	        }
		}	//if (PROBABILITY_MODEL.equals("bottomup_with_path") etc.
		
		
		
		if (PROBABILITY_MODEL==PCFG || PROBABILITY_MODEL==Manning_CarpenterLCP) {
			
			ArrayList<String> parseableSentences = new ArrayList<String>();
			
			//compute likelihood of test sentences
			double totalLogProbability = 0., totalProbability=1.;
			int nrSentencesWithZeroProb =0;
			sentenceCounter = 0;
			
			for (parseTree myTestParseTree : parseTreesFromTestCorpus) {

				if (!(indicesOfSentencesToSkip.contains(sentenceCounter))) {
					double sentenceProbability =0d;
					if (PROBABILITY_MODEL==PCFG) {
						PCFG myPCFG = (PCFG) myGrammar;
						sentenceProbability = myPCFG.computePCFGSentenceProbability(sentenceCounter, myTestParseTree);
					}
					if (PROBABILITY_MODEL==Manning_CarpenterLCP) {
							lcsGrammar myLCSGrammar = (lcsGrammar) myGrammar;
							sentenceProbability = myLCSGrammar.computeManningCarpenterSentenceProbability(sentenceCounter, myTestParseTree);
					}
	
					if (!(sentenceProbability==0.)) {
						totalLogProbability += Math.log(sentenceProbability);
						//totalProbability *= sentenceProbability;
						parseableSentences.add(myTestParseTree.printWSJFormat());
						if (FEEDBACK) System.out.println("###### Test sentence #" + sentenceCounter + ":" + myTestParseTree.printWSJFormat() + "; logLikelihood=" + Math.log(sentenceProbability));
					}
					else nrSentencesWithZeroProb++;
				}
				sentenceCounter++;
			}	//for (parseTree myTestParseTree : parseTreesFromTestCorpus)
			
			String pModel = "PCFG probability model (no history)";
			if (PROBABILITY_MODEL==Manning_CarpenterLCP) pModel = "M & C left corner probability model ";
			System.out.println("totalLikelihood for " + pModel + " : " + totalLogProbability);
			System.out.println("Likelihood includes " + parseableSentences.size() + " test sentences; " + nrSentencesWithZeroProb + " out of " + sentenceCounter + " test sentences have zero prob.");
			
			//store parseable test sentences in file
			//System.out.println("The following sentences could be parsed");
			//for (String mySentence : parseableSentences) System.out.println(mySentence);	
		}
	}

	private static void printShortestDerivations() throws IOException {
		/*
		 * print shortest derivations
		sentenceCounter=0;
		for (ArrayList<String> shortestDerivation : shortestDerivations) {
			System.out.println("Shortest derivation for sentence #" + sentenceCounter + ":");
			for (String fragment : shortestDerivation) System.out.println(fragment);
			sentenceCounter++;
		}
		*/
		//treebank of fragments
		System.out.println("Fragments and their frequencies");
		//first integer = frequency; String=fragment (series of bindings); ArrayList<Integer>=sentenceNrs in which fragment occurs
		TreeMap<Integer, HashMap<String, ArrayList<Integer>>> fragmentsOrderedByFreq = new TreeMap(Collections.reverseOrder());
		//rearrange
		for (String fragment : fragments.keySet()) {	//fragments is HashMap<String, ArrayList<Integer>>
			int freq = fragments.get(fragment).size();	//ArrayList<Integer> of sentences where fragment occurs
			HashMap<String, ArrayList<Integer>> fragmentsOfCertainFreq = null;
			if (fragmentsOrderedByFreq.get(freq)==null) {
				fragmentsOfCertainFreq = new HashMap<String, ArrayList<Integer>>();
				fragmentsOrderedByFreq.put(freq, fragmentsOfCertainFreq);
			}
			else fragmentsOfCertainFreq = fragmentsOrderedByFreq.get(freq);
			
			fragmentsOfCertainFreq.put(fragment, fragments.get(fragment));
		}
		
		BufferedWriter fragmentsFile = new BufferedWriter(new FileWriter("./output/fragments.csv"));
		fragmentsFile.write("freq;,length;occ;cxs");
		fragmentsFile.newLine();
		
		//also write the top 50 most frequent fragments to Latex
		int topMostFrequentFragments = 0;
		BufferedWriter treeFragmentFile = new BufferedWriter(new FileWriter("./treebanks/topFragments.tex"));	        	
		Utils.openLatexDoc2(treeFragmentFile, true);
      
		parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES = true; 
		parser.parameters.EXTRACT_POSTAGS =false;
		parser.parameters.REMOVE_PUNCTUATION = false;
		
		for (Integer frequency : fragmentsOrderedByFreq.keySet()) {
			HashMap<String, ArrayList<Integer>> fragmentsOfSameFrequency = fragmentsOrderedByFreq.get(frequency);
			//System.out.println("frequency=" + frequency);
			for (String fragment : fragmentsOfSameFrequency.keySet()) {
				StringBuffer occurrences = new StringBuffer();
				int counter=0;
				String myComma ="";
				for (Integer sentenceNr : fragmentsOfSameFrequency.get(fragment)) {
					if (counter>0) myComma = ",";
					occurrences.append(myComma + sentenceNr);
					counter++;
				}	
				System.out.println("f=" + frequency + " l=" + fragment.split("~")[1] + " [" + occurrences.toString() + "]: " + fragment.split("~")[0]);
				fragmentsFile.write("" + frequency + ";" + fragment.split("~")[1] + ";" + occurrences.toString() + ";" + fragment.split("~")[0]);
		    	fragmentsFile.newLine();
		    	
		    	if (topMostFrequentFragments <50) {
		        	//write Latex tree
		    		String shit = fragment.split("~")[0].trim();
		    		parser.parseTree myParseTree = sentencePreprocessor.ExtractParseFromWSJText(shit, false); 
		        	String latexTree = myParseTree.printToLatexParseTreeSty("", false, false);
		        	
		        	latexTree = latexTree.replace("&", "-");
					latexTree = latexTree.replace("%", "\\%");
					//remove (.TOP. 
					latexTree = latexTree.substring(8, latexTree.length()-2);
					
			        treeFragmentFile.write("\\begin{parsetree} " + latexTree + " \\end{parsetree}");
			        treeFragmentFile.newLine(); treeFragmentFile.newLine();
			        treeFragmentFile.write("Frequency: " + frequency  + "; Occurs in sentences: [" + occurrences.toString() + "]");
			        treeFragmentFile.newLine(); treeFragmentFile.newLine(); treeFragmentFile.newLine();
		    	}
		        topMostFrequentFragments++;
			}
		}
		fragmentsFile.flush(); fragmentsFile.close(); 
		treeFragmentFile.write("\\end{document}"); treeFragmentFile.newLine();
		treeFragmentFile.flush(); treeFragmentFile.close();
	}

	/**
	 * Trains treebank grammar, depending on chosen PROBABILITY_MODEL
	 * if PROBABILITY_MODEL is either leftcorner_with_path or topdown_with_path
	 * it constructs episodicGrammar and fills the constituents with rankedETNs
	 * if PROBABILITY_MODEL is either PCFG or Manning_Carpenter, then
	 */
	private static grammar train(ArrayList<parseTree> preprocessedParseTrees) throws Exception, IOException {
		
		NumberFormat numberFormatter = new DecimalFormat("#.######");
		grammar	myGrammar = null;
		
		
		//shiftProductionsEncounteredInTrainSet = new HashSet<String>();

		if (PROBABILITY_MODEL==EPISODIC_LEFTCORNER_MODEL || PROBABILITY_MODEL==EPISODIC_TOPDOWN_MODEL) {
			
			System.out.println("initializing episodicGrammar...");
			
			//this sets myNode.setProductionName of nodes in the the parseTree to lhs*rhs1*rhs2*etc
			for (parseTree myParseTree : preprocessedParseTrees) myParseTree.assignProductionNamesToNodes();
			
			episodicGrammar myEpisodicGrammar = new episodicGrammar(preprocessedParseTrees);
			
			
			//HashMap<String, HashSet<episodicConstituent>> nodesWithSameRootLabel
			//myEpisodicGrammar.nodesWithSameRootLabel = myEpisodicGrammar.createSetOfNodesWithSameRootLabel();
			
			//check
			//for (String myWord : lexicon) System.out.println(myWord);
			if (FEEDBACK) {
				System.out.println("non-terminals:");
				for (String mynonT : myEpisodicGrammar.getNonTerminalUnits().keySet()) System.out.println("*" + mynonT + "*");
				System.out.println("terminals:");
				for (String mynonT : myEpisodicGrammar.getTerminalUnits().keySet()) System.out.println("*" + mynonT + "*");
			}
			
			//convert node representations to ETN's that are stored in slots and nodes
			//if SUPERVISED_MODE initializes episodic trace numbers (ETNs) such that they give correct attach probabilities
			//when conditioned on slot (and only history-less)
			//net.initializeCompressorNodeETNs(); 
	
			nrTrainSentences = preprocessedParseTrees.size();
			System.out.println("adding episodic traces to nodes in episodicGrammar...");
	
			// fills HPN compressorNodes and inputNodes with rankedETNs
			// and at the same time puts temporary ETNs in dopparser.Node's of the parseTree
			myEpisodicGrammar.fillEpisodicNodeswithTraces(preprocessedParseTrees);
			
			//print parseTrees in Latex format (//nodes are relabeled)
			boolean printRankNrs = false;
			if (PRINT_LATEX_TREES_WITH_RANKNRS) printRankNrs = true;
	        if (PRINT_LATEX_TRAINTREES) printer.printLatexTreeFile(preprocessedParseTrees, myEpisodicGrammar, "./treebanks/traintrees_with_ETNs.tex", printRankNrs);	
			
	        myGrammar = myEpisodicGrammar;
		}
		
		
		if (PROBABILITY_MODEL==PCFG) {
			
			PCFG myLCSGrammar = new PCFG(preprocessedParseTrees);
			
		}
		
		//symbolic left corner probabilities
		//Note: this assumes node labels from HPN grammar, hence no unary rules and duplication of node labels for categories with 2 or 3 slots!
		if (PROBABILITY_MODEL==Manning_CarpenterLCP) {
			
			
			//this methods computes and normalizes symbolic leftcorner probabilities and stores them in myReader.
			//myReader.projections and myReader.attachments (maar let op, sommige zijn null)
			//en let op: prob. model sommeert over attach en project choices. dus genormaliseerd over alles wat je vanuit leftcorner kunt doen.gebruik die nu om likelihood uit te rekenen.

			System.out.println("initializing M&C grammar...");
			
			//constructor of episodicGrammar initialize inputNodes, compressorNodes, and slot representations
			//Nee, doetie niet! also adds special START node if (!(Main.SUPERVISED_MODE && (Main.READ_GRAMMAR_IN_HPN_FORMAT || Main.SYMBOLIC_MODE))) 
				
			//net = new episodicGrammar(lexicon, nonterminalProductions);
			lcsGrammar myLCSGrammar = new lcsGrammar(preprocessedParseTrees);
	
			
			//results in updated HashMap<String, HashMap<String, Double>> projections and attachments rules, also prints leftCornerProbabilities to ./treebanks/left_corner_probabilities
			myLCSGrammar.estimateSymbolicLCProbabilities(preprocessedParseTrees);

			//PRINTING	
			if (PRINT_LEFT_CORNER_PROBABILITIES) lcsGrammar.printLeftCornerProbabilities(myLCSGrammar, false);
			
			myGrammar = myLCSGrammar;
			
			if (PRINT_LATEX_TRAINTREES) printLatexTreeFile(preprocessedParseTrees, "./treebanks/traintrees_with_ETNs.tex");
			
		}
		
		return myGrammar;
	}

	/**
	 * Adds the indices of test sentences whose length exceeds 
	 * MAX_SENTENCE_SIZE to an ArrayList of indices to be skipped
	 * 
	 * @param parseTreesFromTestCorpus
	 * @return skipIndices
	 */
	public static ArrayList<Integer> skipTooLongAndSingleWordSentences(ArrayList<parseTree> parseTreesFromTestCorpus) {
		
		ArrayList<Integer> skipIndices = new ArrayList<Integer>();
		
		int sentenceCounter = 0;
		int nrTooLongSentences = 0;
			
		for (parseTree myParseTree : parseTreesFromTestCorpus) {
			
			
			//check if all terminals are in the vocabulary (lexicon)
			
			int nrTerminals =0; //filter out WSJ40: count nrTerminals
			for (parser.Node myNode : myParseTree.getNodes()) {
				
				if (myNode.getType()== parser.parameters.TERMINAL) nrTerminals++;
			}
			
			if (nrTerminals==1 || nrTerminals > parser.parameters.MAX_SENTENCE_SIZE)  {
				nrTooLongSentences++;
				skipIndices.add(sentenceCounter);
			}
				
			sentenceCounter++;
		}
		
		System.out.println("######################################################");
		System.out.println("######    " + nrTooLongSentences + " out of " + parseTreesFromTestCorpus.size() + " sentences from the test are single words or longer than " + parser.parameters.MAX_SENTENCE_SIZE + " words.");
		System.out.println("######################################################");
		return skipIndices;
	}

	public static ArrayList<Integer> skipSentencesWithUnknownLabels(ArrayList<parseTree> parseTreesFromTestCorpus, ArrayList<Integer> removedIndices, grammar myGrammar) {	
		
		//ArrayList<parseTree> selectedParseTrees = new ArrayList<parseTree>();
		
		int sentencesWithUnknownWords = 0;
		int sentenceCounter = 0;
		String unknownWordOrNonTerminal = "";
		
		for (parseTree myParseTree : parseTreesFromTestCorpus) {
			
			if (sentenceCounter%50==0)
				System.out.println("checking for unknown words in test sentence " + sentenceCounter);
			
			//check if all terminals are in the vocabulary (lexicon)
			boolean throwout = false;
			unknownWordOrNonTerminal = "";
			
			int nrTerminals =0; //filter out WSJ40: count nrTerminals
			for (parser.Node myNode : myParseTree.getNodes()) {
				
				String nodeLabel = myNode.getProductionName();
				//System.out.println("myNode.getHPNNodeName()=" + nodeLabel +"*; myNode.getType()=" + myNode.getType());
				
				//System.out.println("nodelabel*" + nodeLabel +"*; myNode.getType()=" + myNode.getType());
				if (myNode.getType()== parser.parameters.TERMINAL) {
					
					nrTerminals++;
					
					if (!parser.parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES) nodeLabel =	myNode.getName().split(" ")[1].trim();
					if( !(myGrammar.getTerminalUnits().keySet().contains(nodeLabel))) { 
						
						unknownWordOrNonTerminal = "unknown word: *" + nodeLabel +"*";
						throwout = true; 	break;
					
					}
					
					/*
					if (comparativeLikelihoods.INCLUDE_SHIFT_PROBABILITIES) {
						if (myNode.getLeftSpan()>0) {
							String shiftProduction = lcsGrammar.findShiftProductionAssociatedWithTerminal(myParseTree, myNode);
							//System.out.println("shiftProduction=" + shiftProduction);
							if (!(shiftProductionsEncounteredInTrainSet.contains(shiftProduction))) {
								unknownWordOrNonTerminal = "unknown shiftProduction: *" + shiftProduction +"*";
								//System.out.println("************ Test sentence " + sentenceCounter + " contains " + unknownWordOrNonTerminal);
								throwout = true; 	break;
							}
						}
						else {
							//throw out unknown first words (shift productions from START)
							if (!(shiftProductionsEncounteredInTrainSet.contains("START^1~TOP~" + myNode.getProductionName()))) {
								unknownWordOrNonTerminal = "unknown shiftProduction: START^1~TOP~" + myNode.getProductionName() +"*";
								//System.out.println("************ Test sentence " + sentenceCounter + " contains " + unknownWordOrNonTerminal);
								throwout = true; 	break;
							}
						}
		            }
		            */
				}
				
				
				if	((myNode.getType()== parser.parameters.NONTERMINAL ) && !(myGrammar.getNonTerminalUnits().keySet().contains(nodeLabel))) {
					unknownWordOrNonTerminal = "unknown production: *" + nodeLabel +"*";
					throwout = true; 	break;
				}
				
			}
			
			if (throwout) {
				
				System.out.println("*************** Test sentence " + sentenceCounter + " contains " + unknownWordOrNonTerminal);
				sentencesWithUnknownWords++;
				removedIndices.add(sentenceCounter);
			}
			sentenceCounter++;
		}
		
		System.out.println("######################################################");
		System.out.println("######    " + sentencesWithUnknownWords + " out of " + parseTreesFromTestCorpus.size() + " sentences from the test set contain unknown words");
		System.out.println("######################################################");
		return removedIndices;
	}
	
	
	
	public static void printLatexTreeFile(ArrayList<parseTree> parseTrees, String TREE_FILE) throws Exception {
		BufferedWriter treeFile = new BufferedWriter(new FileWriter(TREE_FILE));
	    
	    Utils.openLatexDoc2(treeFile, true);
	       
		for (parseTree aParseTree : parseTrees) {
	    	
			String latexTree = aParseTree.printToLatexParseTreeSty("", false, false);
			//latexTree = replaceSpecialSymbols(latexTree);
			latexTree = latexTree.replace("&", "-");
			latexTree = latexTree.replace("%", "\\%");
			
	        treeFile.write("\\begin{parsetree} " + latexTree + " \\end{parsetree}");
	        treeFile.newLine();
	        treeFile.newLine();
	    }
		treeFile.write("\\end{document}"); treeFile.newLine();
	    treeFile.flush();
	    treeFile.close();
	    
		}
	
}
