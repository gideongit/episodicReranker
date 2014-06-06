/*
 * Main.java
 *
 * Created on 21 februari 2006, 21:10
 *
 */

package parser;

/**
 *
 * @author gideon
 */
import java.io.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;

public class parameters {
       
	
    //special symbols
    public final static boolean PRINT_OUTPUT = false;   //to screen
    //WRITE_CNF_GRAMMAR_TO_FILE exports the grammar (read off from Treebank) in format that can be imported (GrammarToTextWriter, in printStuff)
    //so that next time Goodman doesn't have to be trained (choose GRAMMAR_PRINTING_FORMAT=1)
    public static boolean EXPORT_CNF_GRAMMAR_TO_FILE = false;
    public final static boolean EXPORT_CFG_GRAMMAR_TO_FILE = false;
    //formats: 1=print terminals, nonT, and rules+probabilities in one file
    //2=only rules 3=rules plus probabilities, 4=//rules plus counts
    //for exporting target grammar to BMM-algorithm you need format 4
    public static int GRAMMAR_PRINTING_FORMAT = 1;        
    public final static boolean GRAMMAR_PRINTING_PRETERMINALS = false;        
    public final static boolean CALCULATE_LIKELIHOOD = false;
    public final static boolean CALCULATE_PERPLEXITY = false;
    
    public final static boolean DO_UNLABELED_PRECISION_AND_RECALL=false;
    
    public final static boolean COMPUTE_BASELINES_PRECISION_AND_RECALL=false;
    
    /**
     * READ_GRAMMAR_FROM_FILE if TRUE reads explicit grammar rules from StolckeGrammar.txt file 
     * if false, then createGrammarFromTrainingSamples
     */ 
    public static boolean READOFF_GRAMMAR_FROM_TREEBANK = true;
    //depending on READOFF_GRAMMAR_FROM_TREEBANK user must give either TREEBANK_FILE or GRAMMAR_FILE
    
    //WSJ_TREEBANK is read only iff READ_WSJ_TREEBANK_FROM_DIRECTORY=false
    public static String TREEBANK_FILE = "Input/WSJ_labeled_lexical_and_postags.txt"; 
    //TREEBANK_FILE_UNLABELLED only for purposes of lookup in case of FIND_PARSES_CORRESPONDING_WITH_UNLABELED_FROM_TREEBANK
    public static String TREEBANK_FILE_UNLABELLED = "Input/WSJ_unlabeled_postags.txt";
    
    //  Read original WSJ files from directory of WSJ
    public static boolean READ_WSJ_TREEBANK_FROM_DIRECTORY = true;
    //public static String TREEBANK_FILE = "Input/OVIS_compleet_labeled.txt";
    
    public static String GRAMMAR_FILE = "Grammars/TreebankTrainSetCFG_Grammar.txt";
    
    //  default OVIS parse, if DO_WSJ_PARSE then WSJ parse
    public static boolean BRACKET_FORMAT_WSJ_STYLE = true;
    public static int BRANCHING_FACTOR = 2;
    public static int BEAM_WIDTH = 1000;
    
    //DOGOODMANDOP: if false then regular CYK parser
    public static boolean DOGOODMANDOP = false;
    public static boolean DOTESTING = false;
    
    public static boolean PRINT_STANDARD_OUTPUT_TO_FILE = false;
    public static boolean PRINT_COMPUTED_PARSES = true;
    //public final static boolean PRINT_SUMMARY = false;
    //if you parse for multiple parameter settings, then set iterations>1
    //public static int ITERATIONS = 1;
    //for printing output parses of CHILDES
    public static boolean DO_PARSING_NO_EVALUATION = false;
    //for reading in Tree Bank and printing/outputting the grammar
    //public final static boolean DO_NO_PARSING = true;
    //CASE_SENSITIVITY for lexical words in pre-terminal rules
    public static boolean CASE_SENSITIVITY = true;
    
    /**
     * EXTRACT_POSTAGS: reads only postags from the treebank, and puts these in the parsetree; 
     * if you don't use POSTAGS then it replaces POSTAGS by lexical items
     */
    public static boolean EXTRACT_POSTAGS = false;
    public static boolean CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES = true;
    public static boolean REMOVE_PUNCTUATION = false;
    
    public static String TESTSET_UNLABELLED = "Input/WSJ_unlabeled_postags_uppercase.txt";
    public static String TESTSET_GOLD_STANDARD = "Input/WSJ_labeled_lexical_and_postags.txt";
    public static String OUTPUT_PARSEFILE = "";
    
    //output unlabeled text to WSJ_unlabeled.txt after reading labeled text
    public static String OUTPUT_DIR = "./Output";
    public static boolean PRINT_UNLABELED_FILE = true;
    public static boolean PRINT_LATEX_FILE = false;
    public static boolean PRINT_CLEANEDUP_LABELED_FILE = true;
    public static boolean PRINT_CNF_CONVERTED_LABELED_FILE = false;
    
    //compute file of spans (0-3 1-3 etc) from WSJ-style labeled file
    public final static boolean PRINT_SPANS = false;
    public static boolean PRINT_BINARYSPANS = false;
    //public final static boolean 
    public final static boolean DO_RIGHT_BRANCHING_TEST = false;
    public final static boolean SWITCH_LEFT_BRANCHING = false;
    
    
    //DIRECT_EVALUATION_NO_PARSING for comparing two labeled treebanks, without computing the parses
    //TAKE_OFF_SBJ: if true, disregards suffixes of the labels in gold standard, such as NP_SBJ, etc, and identifies all NPs, etc.
    public static boolean TAKE_OFF_SBJ = true;
    //OTHERWAYROUND if false, matches induced labels to gold standard labels, if true, other way round
    public final static boolean OTHERWAYROUND = false;
    public final static boolean DIRECT_EVALUATION_NO_PARSING = false;
    public final static boolean FIND_PARSES_CORRESPONDING_WITH_UNLABELED_FROM_TREEBANK = false;
    //DO_LABEL_ASSOCIATION finds optimal associations between labels of induced parses and gold standard parses
    public final static boolean DO_LABEL_ASSOCIATION = false;
    //DO_LABELED_PRECISION_AND_RECALL: if true, evaluates LP and LR, otherwise UP and UR
    public static boolean DO_LABELED_PRECISION_AND_RECALL = false;
    public final static boolean PRINT_LABEL_FREQUENCIES = false;
    public final static boolean PRINT_PARSEVAL_PER_CATEGORY = false;
    public final static boolean REWRITE_OVIS_PARSES_IN_WSJ_FORMAT = false;
    public final static boolean REPLACE_LABELS_IN_GRAMMAR = false;
    public final static boolean DO_BRANCH_COUNTING = false;
    public final static boolean CONSTRUCTIONS_EXPERIMENT = false;
    //public final static boolean REPLACE_EXTERNAL_LABELS_BY_X = false;
    public static int MAX_SENTENCE_SIZE = 80;
    public static double MU = 1.;
    public final static double START_MU = 0.5;
    public final static int TERMINAL = 1;
    public final static int NONTERMINAL = 2;
    
    public static boolean EXIT_APPLICATION = false;
    
    
    
    
}
