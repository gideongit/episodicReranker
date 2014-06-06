package episodicReranker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import parser.parseTree;


public class printer {

public static void printLatexTreeFile(ArrayList<parseTree> parseTrees, episodicGrammar myEpisodicGrammar, String TREE_FILE, boolean printRankNrs) throws IOException {
        
		//HashMap<String, episodicConstituent> episodicInputNodes = myEpisodicGrammar.episodicInputNodes;
		HashMap<String, episodicConstituent> episodicNonTerminalProductions = myEpisodicGrammar.episodicNonTerminalProductions;
		
	       //dopparser.Printer.printSentences(latexSentences, "WSJ_latex.txt");
			boolean printStructureETNs = false;
		
			BufferedWriter treeFile = new BufferedWriter(new FileWriter(TREE_FILE));
		            
		    openLatexDoc(treeFile, true);
		       
			for (parseTree aParseTree : parseTrees) {
	        	//latexSentences.add(aParseTree.printToLatex(false, true, ""));
				//printToLatex(boolean blnPrintNrSubtrees, boolean blnPrintNodeSpans, String strText){
				
				String latexTree = aParseTree.printToLatexParseTreeSty("", printRankNrs, printStructureETNs);
				//was: printToLatex
		        latexTree = Utils.replaceSpecialSymbols(latexTree);

		        //was: treeFile.write("\\scalebox{.4}{\\Tree" + latexTree + "}");
		        treeFile.write("\\begin{parsetree} " + latexTree + " \\end{parsetree}");
		        treeFile.newLine();
		        treeFile.newLine();
	        }

			//for double check also print the HPN nodes plus ETN contents
			treeFile.newLine();
			treeFile.write("HPN inputNodes and episodicProductions plus ETNs \\\\");
			treeFile.newLine();
			/*
			for (String inputNodeLabel : episodicInputNodes.keySet()) {
				StringBuffer nodeLabelPlusETNs = new StringBuffer();
				nodeLabelPlusETNs.append("inputNode=" + inputNodeLabel + "; ETNs: ");
				
				if (printRankNrs) {
					for (Integer[] rankedETN : episodicInputNodes.get(inputNodeLabel).getRankedETNs()) 
						//nodeLabelPlusETNs.append(rankedETN.split("_")[0] + "*" + rankedETN.split("_")[1] + " ");
						nodeLabelPlusETNs.append(rankedETN[0] + "*" + rankedETN[1] + " ");
				}
				if (printStructureETNs) {
					for (String slotKey : episodicInputNodes.get(inputNodeLabel).getStructureETNs().keySet()) {
						for (String strETN : episodicInputNodes.get(inputNodeLabel).getStructureETNs().get(slotKey)) {
							
							String[] strETNArray = strETN.split("_");
							//Integer[] strETNArray = comparativeLikelihoods.strETN2ArrayETN.get(strETN); //inputLayerNodes.get(inputNodeLabel).getStructureETNs().get(slotKey).get(strETN);
							nodeLabelPlusETNs.append(strETNArray[0] + "*" + strETNArray[1] + "*" + strETNArray[2] + " ");
							//nodeLabelPlusETNs.append("cf: " + strETN.split("_")[0] + "*" + strETN.split("_")[1] + "*" + strETN.split("_")[2] + " ");
						}
					}
				}
				treeFile.write(nodeLabelPlusETNs.toString() + "\\\\");
				treeFile.newLine();
			}
			*/
			for (String cNodeLabel : episodicNonTerminalProductions.keySet()) {
				StringBuffer nodeLabelPlusETNs = new StringBuffer();
				nodeLabelPlusETNs.append("episodicProduction=" + cNodeLabel + "; ETNs: ");
				
				if (printRankNrs) {
					for (Integer sentenceNr : episodicNonTerminalProductions.get(cNodeLabel).getRankedETNs().keySet()) 
						//nodeLabelPlusETNs.append(rankedETN.split("_")[0] + "*" + rankedETN.split("_")[1] + " ");
						//HashMap<Integer, ArrayList<Integer[]>> rankedETNs
						for (Integer[] rankedETN : episodicNonTerminalProductions.get(cNodeLabel).getRankedETNs().get(sentenceNr))
						//nodeLabelPlusETNs.append(rankedETN[0] + "*" + rankedETN[1] + " ");
							nodeLabelPlusETNs.append(sentenceNr + "*" + rankedETN[0] + " ");
				}
				
				
				treeFile.write(nodeLabelPlusETNs.toString() + "\\\\");
				treeFile.newLine();
			}
		    treeFile.write("\\end{document}"); treeFile.newLine();
		    treeFile.flush();
		    treeFile.close();
		    
		    //System.out.println("Done");
		}


public static void openLatexDoc(BufferedWriter treeFile, boolean includeQtree)  throws IOException {
    treeFile.write("\\documentclass[10pt]{article}"); treeFile.newLine();
    
    treeFile.write("\\oddsidemargin 0in"); treeFile.newLine();
    treeFile.write("\\evensidemargin 0in"); treeFile.newLine();
    
    treeFile.write("\\usepackage[latin2]{inputenc}"); treeFile.newLine();
    treeFile.write("\\usepackage{../../../Latex/styles/graphicx}"); treeFile.newLine();
    treeFile.write("\\usepackage[T1]{fontenc}"); treeFile.newLine();
    if (includeQtree)
    	treeFile.write("\\usepackage{../../../Latex/styles/parsetree}"); treeFile.newLine();
    treeFile.write("\\topmargin -1.5cm"); treeFile.newLine();

    treeFile.write("\\begin{document}"); treeFile.newLine();
}

}
