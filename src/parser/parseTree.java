/*
 * parseTree.java
 *
 * Created on 1 december 2005, 9:35
 *
 */

package parser;

/**
 *
 * @author gideon
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class parseTree {
    
    
    private ArrayList<Node> associatedNodes;
    public int deepestLevel = 0;
    protected double parseProbability;
    
    /**
     * Creates a new instance of parseTree 
     */
    public parseTree() {
        this.associatedNodes = new ArrayList<Node>();
        //add TOP rule
        Node topNode = new Node("TOP", null);
        topNode.setType(parameters.NONTERMINAL);
        topNode.setDepth(0);
        this.associatedNodes.add(topNode);
    }
    
    //this constructor is for reading in rules from an external grammar and converting them to CNF
    public parseTree(String myLHS) {
        this.associatedNodes = new ArrayList<Node>();
        //add TOP rule
        Node topNode = new Node(myLHS, null);
        topNode.setType(parameters.NONTERMINAL);
        topNode.setDepth(0);
        this.associatedNodes.add(topNode);
    }
    /**
     * add a rule of the form X->YZ to nonTerminal 
     */
    public void addNode(Node myNode){
        this.associatedNodes.add(myNode);
        //return this.associatedNodes.size() - 1;
        
    }
    
    public void removeNode(int nrNode){
        this.associatedNodes.remove(nrNode);
 
    }
    
    public Node getNode(int nrNode){
        //todo: error if nrNode > totalRules
        //int totalRules = this.associatedNodes.size();
        //System.out.println("nonTerminal= " + this.getName() + "; totalRules= " + totalRules + "; nrNode=" + nrNode);
        //assert(nrNode>0 && nrNode < totalRules); ??? werkt niet
        return this.associatedNodes.get(nrNode);
        
    }
    
    public ArrayList<Node> getNodes(){
        
        return this.associatedNodes;
    }
    
    public double getProbability(){
            return this.parseProbability;
        }
        
    public void setProbability(double myProbability) {
         this.parseProbability = myProbability;
     }
        
    /** This function transforms the grammer to the CNF.
     *
         *enumerate nodes vd parseTree
         *maak nwe parseTree en schrijf ondertussen nieuwe nodes daarnaartoe met nwe indices
         *check of node in CNF staat
         *indien unary doe dan zoals boven
         *zoniet, scheidt eerste child van andere children
         *als eerste child terminal dan maak je nwe nonTerminal, met als child de terminal
         *als rest single nonTerminal 
         *als rest single Terminal, maak dan je nwe nonTerminal, met als child de terminal
         *als rest meer dan een symbool, maar dan nwe nonTerminal, en maak deze child en de rest worden
         *children vd nieuwe
     */
    public void transformToCNF(){
        
        //System.out.println("in transformToCNF");
        //you need to store the new nodes aside, before you can add them to parseTree
        ArrayList<Node> tempNodeStore = new ArrayList<Node>();
        
        for (Iterator<Node> it = this.associatedNodes.iterator(); it.hasNext(); ) {
            Node myNode = it.next();
            
            int nrOfChildNodes = myNode.getChildNodes().size();
            //don't bother with unary productions here, only after you have converted this node to CNF format
            //you check if this node is single child of the parent
            if (nrOfChildNodes >= 2) {
                
                /*
                //controle
                StringBuffer testNode = new StringBuffer(); 
                testNode.append(myNode.getName()).append("-->");
                for (Node myChildNode : myNode.getChildNodes()) {
                    testNode.append(myChildNode.getName()).append(" + ");
                }
                System.out.println("controle: node=" + testNode.toString());
                */
                
                //first, replace all terminal nodes by nonterminal nodes, and append terminals on them
                int nrChildNode = 0;
                for (Node myChildNode : myNode.getChildNodes()) {
                    if (myChildNode.getType()==parameters.TERMINAL){
                        //create new node
                        String newNodeName = "X_" + Node.newNodeIndex++; //XXX ??? to think about
                   
                        Node newNode = new Node(newNodeName, myNode);    
                        newNode.setType(parameters.NONTERMINAL);
                        
                        //add terminal node as a child to this one
                        newNode.addChildNode(myChildNode);
                        
                        //this.addNode(newNode);
                        tempNodeStore.add(newNode);
                                           
                        //replace parent in terminal node
                        myChildNode.replaceParentNode(newNode);
                        
                        //replace terminal node by new node in parent node
                        myNode.replaceChildNode(nrChildNode, newNode );
                        //myNode.addChildNode(newChildNode);
                    }
                    nrChildNode++;
                }
                
                //now, all nodes are nonterminals, now recursively remove one nonterminal and descend one level
                //untill there are exactly 2 children left
                Node tempNode = myNode;
                while (nrOfChildNodes > 2)    {
                    
                    //create new node
                    String newNodeName = "X_" + Node.newNodeIndex++;
                    //String newNodeName = tempNode.getName() + "_X"; //XXX ??? think about this: 
                    //tempNode = parent
                    Node newNode = new Node(newNodeName, tempNode);    
                    newNode.setType(parameters.NONTERMINAL);
                    //iterate over children, put all but first child in new node (and parent), remember first child
                    int i = 0;
                    Node firstChild = tempNode.getChildNodes().get(0);
                    
                    for (Node myChildNode : tempNode.getChildNodes()) {
                      if (i>0) {
                          newNode.addChildNode(myChildNode);
                          
                          //replace parent in terminal node
                          myChildNode.replaceParentNode(newNode);        
                        }       
                      i++; 
                    }
                    //this.addNode(newNode);
                    tempNodeStore.add(newNode);
                    
                    //remove all children from original, then put first child and new node in it
                    tempNode.removeAllChildNodes();
                    tempNode.addChildNode(firstChild);
                    tempNode.addChildNode(newNode);
                    
                    //descend to child and recompute nrOfChildNodes
                    nrOfChildNodes = newNode.getChildNodes().size();
                    tempNode = newNode;
                }    
            }   //if (nrOfChildNodes>=2)
            
            //remove unary rules: look at parent and check if this one is single child
            //if it is, move all its children to the parent and delete this one
                
            //check whether the current rule is not a unary rule, if so, remove it,
            //and update children of parent node    (TOP is allowed to be unary)
           
            if (!myNode.getName().equals("TOP") && (myNode.getParentNode()!=null)) {
                if (myNode.getType()==parameters.NONTERMINAL && myNode.getParentNode().getChildNodes().size()==1 && !(myNode.getParentNode().getName().equals("TOP"))) {

                    //remove this child from parent node and put all children of this node inside
                    myNode.getParentNode().removeAllChildNodes();
                    for (Node myChildNode : myNode.getChildNodes()) {
                        myNode.getParentNode().addChildNode(myChildNode);
                        //replace pointer to parent nodes in children
                        myChildNode.replaceParentNode(myNode.getParentNode());
                    }

                    //remove this node from parseTree
                    //System.out.println("node removed ");
                    it.remove();
                }   
            }
               
        }   //iterator mynode 
        
        //only now you may add new nodes to parsetree
        this.associatedNodes.addAll(tempNodeStore);

    }
    
    public void transformBackToCFG(String aSymbol){
    
    	//if transformed from CNF then aSymbol="X_"
        //identify all CNF nodes by their name, and remove/collapse them
       //first move all the children of the CNF node to the parent node
       //work bottom-up, start with deepest level and climb up the tree
        for (int currentDepth = this.deepestLevel; currentDepth >=0; currentDepth--) {
               
            for (Iterator<Node> it = this.associatedNodes.iterator(); it.hasNext(); ) {
                Node myNode = it.next();
                if (myNode.getDepth() == currentDepth) {
                    if (myNode.getName().contains(aSymbol)) {   
                        //System.out.println("remove node " + myNode.getName());
                        for (Node myChildNode : myNode.getChildNodes()) {
                            //System.out.println("parent= " + myNode.getParentNode().getName());
                            //myNode.getParentNode().addChildNode(myChildNode);
                            //replace pointer to parent nodes in children
                            myChildNode.replaceParentNode(myNode.getParentNode());
                        }
                        //remove reference to this node in parent and get its index 
                        int myIndex =0;
                        for (Iterator<Node> it2 = myNode.getParentNode().getChildNodes().iterator(); it2.hasNext(); ) {
                            //Node myParentsChildrenNode = it2.next();
                            //XXX FOUT als er meerdere children met zelfde naam zijn!!! kijk naar equals method
                            //LHS, getLeftSpan, rightSpan en ChildNodes moeten gelijk zijn!!!
                            //if (myParentsChildrenNode.getName().equals(myNode.getName()) && myParentsChildrenNode.getChildNodes().equals(myNode.getChildNodes()) && myParentsChildrenNode.getLeftSpan() ==myNode.getLeftSpan() && myParentsChildrenNode.getRightSpan()==myNode.getRightSpan()) {
                            if (it2.next().equals(myNode)) {    
                                myIndex = myNode.getParentNode().getChildNodes().indexOf(myNode);
                                //TEMP solution
                                if (myIndex == -1)
                                    System.out.println("BUG!!! index=-1 for ParentNode=" + myNode.getParentNode().getName() + "; child Node=" + myNode.getName() + "; index=" + myIndex);
                                it2.remove();
                            }
                        }

                        //add all its children to the parent node and insert in the same position where the CNF was
                        //TEMP solution
                        //if (myIndex != -1)
                        myNode.getParentNode().getChildNodes().addAll(myIndex, myNode.getChildNodes());
                            
                        //remove the CNF node from parseTree
                        it.remove();                 
                    }
                }
            }
        }
    }
    
    public void binarize() {

		ArrayList<Node> binarizedParseTree = new ArrayList<Node>();
		
		//Internal nonterminals from Markovization are indicated as "X>Y"
		
		for (int currentDepth = 0; currentDepth <=this.deepestLevel; currentDepth++) {
            for (Node myNode : this.associatedNodes) {
            	
            	 if (myNode.getDepth() == currentDepth) {
            		// System.out.println("iiimyNode=" + myNode.getName());
            		if (myNode.getChildNodes().size() < 2 ) {	//also "binary" rules must be binarized
            			binarizedParseTree.add(myNode);
            		}	
            		else {
            			           			
            			//VP --> VBZ NP PP wordt: (splits steeds eentje ad rechterkant eraf)
            			//VP -> <VP^PP> : dit betekent "een VP-regel (lhs) met rechts PP"
            			//<VP^PP> -> <VP^NP> PP
            			//<VP^NP> -> <VP^VBZ> NP
            			//<VP^VBZ> -> VBZ
            			
            			//replace current child from parent
            			Node parentNode = myNode.getParentNode();
            			String lhs = myNode.getName();	//=lhs
            			
            			Node newNode = new Node(lhs, parentNode);	//parentNode=null is allowed
	                    newNode.setType(parser.parameters.NONTERMINAL); 
	                    binarizedParseTree.add(newNode);	
         				           			
            			
            			if (!(parentNode==null)) {
         					//1) determine child/slot index of parent with respect to grandparent
                     		int whichChild = parentNode.getChildNodes().indexOf(myNode);

                     		//2) replace parent with myNode
                     		parentNode.replaceChildNode(whichChild, newNode);
                     		
         				}
            			
            			parentNode = newNode;
            			
            			//first make unary production VP -> <VP^PP>
            			String righmostChild = myNode.getChildNodes().get(myNode.getChildNodes().size()-1).getName();
            			String internalNodeName = lhs + ">" + righmostChild;
        				
        				//make this one child of parent, and add parent to this node
        				//currentNode is pointer to parent node
	                    newNode = new Node(internalNodeName, parentNode);	//parentNode=null is allowed
	                    newNode.setType(parser.parameters.NONTERMINAL); 
	                    binarizedParseTree.add(newNode);	
	
	                    //add this node and rightSplit as children to parent node
	                    if (!(parentNode==null)) parentNode.addChildNode(newNode);
	                    parentNode = newNode;
            			
            			for (int childNr =myNode.getChildNodes().size()-1; childNr > 0 ; childNr--) {
            				Node rightmostChildNode = myNode.getChildNodes().get(childNr);
            				Node oneBeforeRighmostChildNode = myNode.getChildNodes().get(childNr-1);
            				
            				internalNodeName = lhs + ">" + oneBeforeRighmostChildNode.getName();
            				
            				//make this one child of parent, and add parent to this node
            				//currentNode is pointer to parent node
		                    newNode = new Node(internalNodeName, parentNode);
		                    newNode.setType(parser.parameters.NONTERMINAL); 
		                    binarizedParseTree.add(newNode);	
		
		                    //add this node and rightSplit as children to parent node
		                    parentNode.addChildNode(newNode);
		                    parentNode.addChildNode(rightmostChildNode);	//keeps it own original children
		                    rightmostChildNode.replaceParentNode(parentNode);
		                    
            				parentNode = newNode;
            			}
            			//add unary production to left corner // <VP^VBZ> -> VBZ
            			Node leftmostChildNode = myNode.getChildNodes().get(0);
            			//internalNodeName = lhs + ">" + leftmostChildNode.getName();
            			
            			//newNode = new Node(internalNodeName, parentNode);
	                    //newNode.setType(dopparser.Main.NONTERMINAL); 
	                    //binarizedParseTree.add(newNode);	
	
	                    //add this node and rightSplit as children to parent node
	                    parentNode.addChildNode(leftmostChildNode);
	                    leftmostChildNode.replaceParentNode(parentNode);
	                 
         			}
            	 }	//if (myNode.getDepth() == currentDepth)
            }
		}
				
		this.associatedNodes.clear();
		this.associatedNodes.addAll(binarizedParseTree);	
		
	}
    
    
    /**
     * Supplies every node in the parseTree with its productionName
	 * which has the form parent*chi1*chi2*etc for nonterminal nodes, 
	 * and equals the node.getName() for terminal nodes
	 */
	public void assignProductionNamesToNodes() {
	  
        for (Node myNode : this.getNodes()) {
        	  	if (myNode.getType()==parser.parameters.TERMINAL){
        			//HPN node name is postag@word
        	  		//System.out.println("myNode=" + myNode.getName());
        			if (parameters.CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES)
        				myNode.setProductionName(myNode.getName());
        			else myNode.setProductionName(myNode.getName().split(" ")[0].trim() + "@" + myNode.getName().split(" ")[1].trim());
                
                }
                else {	//nonterminal
                	if (myNode.getChildNodes().size()==1) {
                   		myNode.setProductionName(myNode.getName()+"*" + myNode.getChildNodes().get(0).getName());
                	}
                	else {	//non-unary rule
                		//label compressor node: extract unique rule expansion from node plus daughters
                		String compressorNodeLabel = myNode.getName().split(" ")[0].trim();
                		for (Node chNode : myNode.getChildNodes()) compressorNodeLabel += "*" + chNode.getName().split(" ")[0].trim();
                		myNode.setProductionName(compressorNodeLabel);
                	
                	}	//non-unary rule
            	}	//if (myNode.getType()==dopparser.Main.NONTERMINAL)
        }	//for (Node myNode : myParseTree.getNodes())
	}
	
    public void calculateLabelFrequencies(HashMap<String, Integer> labelFrequencies1){
                
        for (Node myNode : this.associatedNodes) {
            if (myNode.getType()==parameters.NONTERMINAL) {
                if (labelFrequencies1.get(myNode.getName())==null) {
                    labelFrequencies1.put(myNode.getName(), 1);
                }
                else labelFrequencies1.put(myNode.getName(), labelFrequencies1.get(myNode.getName()).intValue()+1);
            }
        }
    }
    
    public void calculateNodeDepth(){
        int myDeepestLevel = 0;
        int iLabel = 0;
        
        for (Node myNode : this.associatedNodes) {
            iLabel++;
            //recursively find parent node, until you encounter TOP
            Node tempNode = myNode;
            int depth = 0;
            while (!(tempNode.getName().equals("TOP"))) {
              depth++;
              tempNode = tempNode.getParentNode();
            }
            myNode.setDepth(depth);
            
            if (depth>myDeepestLevel) myDeepestLevel = depth;
        }
        this.deepestLevel = myDeepestLevel;
        
        //if (DO_TREEBANK_COMPARISON) you must set the spans of the fringe nodes, and interpret them as terminals
        if (parameters.DIRECT_EVALUATION_NO_PARSING) {
            int wordPosition = 0;
            for (Node myNode : this.associatedNodes) {
                
                if (myNode.getChildNodes().size()==0) {
                    wordPosition++;
                    myNode.setType(parameters.TERMINAL);
                    //since nodes are assigned from left to right, index indicates word position =span for terminal nodes
                    myNode.setLeftSpan(wordPosition-1);
                    myNode.setRightSpan(wordPosition);
                }
            }
        }
    }
    
    public void calculateSubTrees(){
        
        //start with deepest level and climb up the tree
        for (int currentDepth = this.deepestLevel; currentDepth >=0; currentDepth--) {
            //xxxcheck for loop
            for (Node myNode : this.associatedNodes) {
                if (myNode.getDepth() == currentDepth) {
                    //node either has a single terminal as child: then nrSubtrees=0
                    //or it has two nonterminals as children: nrSubtrees= (Aj+1)(Bk+1)
                    //or it is itself terminal
                    if (myNode.getType()==parameters.NONTERMINAL){
                        if (myNode.getChildNodes().size()==1) myNode.setnrSubtrees(0);
                        else myNode.setnrSubtrees((myNode.getChildNodes().get(0).getnrSubtrees()+1) * (myNode.getChildNodes().get(1).getnrSubtrees()+1));
                    }
                }

            }
        }
    }
    
    
    public void calculateNodeSpans(){
        
        //start with deepest level and climb up the tree
        for (int currentDepth = this.deepestLevel; currentDepth >=0; currentDepth--) {
            for (Node myNode : this.associatedNodes) {
                if (myNode.getDepth() == currentDepth) {
                    //if node is terminal then span corresponds to word position
                    //if node is nonterminal, then left span is same as for leftmost child, and right span same as for rightmost child
                    if (myNode.getType()==parameters.NONTERMINAL){
                        if (myNode.getChildNodes().size()==0) System.out.println("index out of bounds: " + myNode.getName());
                        //if (myNode.getChildNodes().size()>0) {
                            myNode.setLeftSpan(myNode.getChildNodes().get(0).getLeftSpan());
                            myNode.setRightSpan(myNode.getChildNodes().get(myNode.getChildNodes().size()-1).getRightSpan());
                        //}
                        //System.out.println("Node=" + myNode.getName() + "; Depth=" + myNode.getDepth() + "; leftChild=" + myNode.getChildNodes().get(0).getName() + "; rightChild=" + myNode.getChildNodes().get(myNode.getChildNodes().size()-1).getName());
                         }
                    //the span of TERMINALS is already entered with the initiation of the Terminal nodes 
                }
            }
        }
    }
    
    public void calculateSubTrees_generalized(){
        
        //start with deepest level and climb up the tree
        for (int currentDepth = this.deepestLevel; currentDepth >=0; currentDepth--) {
            //xxxcheck for loop
            for (Node myNode : this.associatedNodes) {
                if (myNode.getDepth() == currentDepth) {
                    //node either has a single terminal as child: then nrSubtrees=0
                    //or it has two nonterminals as children: nrSubtrees= (Aj+1)(Bk+1)
                    //or it is itself terminal
                    if (myNode.getType()==parameters.NONTERMINAL && myNode.getChildNodes().size()>0){
                        int nrSubtrees = 1;
                        for (int iChild = 0; iChild < myNode.getChildNodes().size(); iChild++){
                        nrSubtrees = nrSubtrees*(myNode.getChildNodes().get(iChild).getnrSubtrees()+1) ;
                        }
                        myNode.setnrSubtrees(nrSubtrees);
                        //XXX hebben preterminals 1 of 0 subtrees?
                        //if (myNode.getChildNodes().size()==1 && myNode.getChildNodes().get(0).getType()==Main.TERMINAL) myNode.setnrSubtrees(0);
                    }
                }

            }
        }
    }
    
    public void removeEmptyNonTerminalNodes(){
        Node myNode;
        for (ListIterator<Node> it = this.associatedNodes.listIterator(); it.hasNext();) {
            myNode = (Node) it.next();
            //for (Node myNode : this.associatedNodes) {  && !(myNode.getParentNode()==null)
            if (myNode.getType() == 2 && myNode.getChildNodes().size()==0) {    //2=NONTERMINAL
                //remove reference in parentnode
            	
            	//System.out.println("myNode.getParentNode().getChildNodes()=" + myNode.getParentNode().getChildNodes());
                myNode.getParentNode().getChildNodes().remove(myNode);
                it.remove();
            }
        }
    }
    
    public void removeUnaryNodes() {
		
		ArrayList<Node> parseTreeNodes = this.getNodes();
		//ArrayList<dopparser.Node> reducedParseTree = new ArrayList<dopparser.Node>();
		//reducedParseTree.addAll(originalParseTree);
		ArrayList<Node> unaryNodes = new ArrayList<parser.Node>();
		
		//remove unary nodes from dopparser.parseTree
		//move through parseTree from bottom to top. if parent has only one child, then
		//1) determine child/slot index of parent with respect to grandparent
		//2) replace parent with child in grandparent children, and replace parent of child with grandparent
		//3) remove parent from the ArrayList
		
		for (int currentDepth = this.deepestLevel; currentDepth >=0; currentDepth--) {
            for (Node myNode : parseTreeNodes) {
            	
            	 if (myNode.getDepth() == currentDepth && !(unaryNodes.contains(myNode))) {
            		 
            		Node parentNode = myNode.getParentNode();
         			if (!(parentNode==null)) {
         				
         				//replace empty categories (bug that is introduced in preprocess Tuebingen
         				//if (myNode.getName().equals("")){
         					
         				//}
         				
                 		//if parent has only one child, except for TOP
                     	if (parentNode.getChildNodes().size()==1 && !(parentNode.getParentNode()==null)) {
                     		
                     		//1) determine child/slot index of parent with respect to grandparent
                     		int whichChild = parentNode.getParentNode().getChildNodes().indexOf(parentNode);

                     		//2) replace parent with myNode in grandparent children, and replace parent of myNode with grandparent
                     		parentNode.getParentNode().replaceChildNode(whichChild, myNode);
                     		myNode.replaceParentNode(parentNode.getParentNode());
                     		
                     		unaryNodes.add(parentNode);
                     	}
         			}
            	 }	//if (myNode.getDepth() == currentDepth)
            }
		}
		//3) remove parent from the ArrayList
		for (Node unaryNode : unaryNodes) parseTreeNodes.remove(unaryNode);
			
		//return myParseTree;
	}

    
    public String printToLatex(boolean blnPrintNrSubtrees, boolean blnPrintNodeSpans, String strText){
        //go counter-clockwise, start with TOP, find left child
        //if no child, then go to sister, if no more sister, then go up one level, go to next child
        //do this until you encounter again the TOP node from the right side, and after you have treated all its children
        
        //first set processedChildren of all nodes to 0
        for (Node myNode : this.associatedNodes) {
            myNode.processedChildren = 0;
        } 
        StringBuffer parseLatexFmt = new StringBuffer(); 
        
                //rechts van nonterminal: spatie ] 
                //overal moeten spaties voor behalve voor nonterminal moet .
                //multiwords en Latex commands zoals \ldots moeten in {}
               
        Node currentNode = this.associatedNodes.get(0); 
        boolean allChildrenOfTOPProcessed = false;
        
        while (allChildrenOfTOPProcessed ==false) {
            //only write [. first time you enter node
            if (currentNode.processedChildren==0){
                if (currentNode.getType()==parameters.NONTERMINAL) parseLatexFmt.append(" [.{").append(currentNode.getName());
                else {
                    parseLatexFmt.append(" {");
                    String terminalName = currentNode.getName();
                    //remove _ from name, because Latex doesn't like it
                    if (!parameters.BRACKET_FORMAT_WSJ_STYLE) {
                        terminalName = currentNode.getName().substring(0, currentNode.getName().length());
                    }
                    parseLatexFmt.append(terminalName);
                }
                //{} for multiwords
                
                if (blnPrintNrSubtrees && currentNode.getType()==parameters.NONTERMINAL) parseLatexFmt.append(" (" + currentNode.getnrSubtrees() + ")");
                if (blnPrintNodeSpans && currentNode.getType()==parameters.NONTERMINAL) parseLatexFmt.append(" (" + currentNode.getLeftSpan() + "-" + currentNode.getRightSpan() + ")");
                parseLatexFmt.append("}");
                
            }
            //(s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(up,[(mp,[(mp,[(p,[(van_,[])]),(np,[(voorschoten_,[])])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])]),(zp,[(hallo_,[]),(jij_,[]),(rp,[(daar_,[]),(auto_,[])])])])])])]).
                   
            //if there is an unprocessed child, make it current node and return to start of while-loop
            if (currentNode.getChildNodes().size()>currentNode.processedChildren) {
                currentNode = currentNode.getChildNodes().get(currentNode.processedChildren);
            }
            //if there are no more unprocessed children, write closing bracket, and mark node as processed with parent
            else {
                if (currentNode.getType()==parameters.NONTERMINAL) parseLatexFmt.append(" ]");
                //System.out.println("nrChildren=" + currentNode.getChildNodes().size() + "; processed=" +currentNode.processedChildren);
                //System.out.println("current node=" + currentNode.getName());
                currentNode.getParentNode().processedChildren++;
                //go to parent node
                currentNode = currentNode.getParentNode();
            }
            
            //check allChildrenOfTOPProcessed
            if (currentNode.getName().equals("TOP") && currentNode.getChildNodes().size() == currentNode.processedChildren)
            allChildrenOfTOPProcessed = true;
        }
        parseLatexFmt.append(" ]");
        
        /*
        (s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(mp,[(mp,[(p,[(van_,[])]),(np,
[(voorschoten_,[])])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),
(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])]),(zp,[(hallo_,[])])])])]).
         */
        
        if (!strText.equals("")) System.out.println("The " + strText + " parse tree is: " + parseLatexFmt.toString());
        return parseLatexFmt.toString();
    }
    
    public String printToLatexParseTreeSty(String strText, boolean printRankedETNs, boolean printStructureETNs){
        //go counter-clockwise, start with TOP, find left child
        //if no child, then go to sister, if no more sister, then go up one level, go to next child
        //do this until you encounter again the TOP node from the right side, and after you have treated all its children
        
    	//this method writes in parsetree.sty format, compare
    	//      \Tree [.S [.NP $the$ $woman$ ] [.VP [.Verb $read$ ] [.NP $the$ $book$ ] ] ]
		//with  \begin{parsetree} 	(.S. (.NP. `the' `woman' ) (.VP. (.Verb. `read') (.NP. `the' `book' ) ) )
		//		\end{parsetree}

		
        //first set processedChildren of all nodes to 0
        for (Node myNode : this.associatedNodes) {
            myNode.processedChildren = 0;
        } 
        StringBuffer parseLatexFmt = new StringBuffer(); 
        
                //rechts van nonterminal: spatie ] 
                //overal moeten spaties voor behalve voor nonterminal moet .
                //multiwords en Latex commands zoals \ldots moeten in {}
               
        Node currentNode = this.associatedNodes.get(0); 
        boolean allChildrenOfTOPProcessed = false;
        
        while (allChildrenOfTOPProcessed ==false) {
            //only write [. first time you enter node
            if (currentNode.processedChildren==0){
                if (currentNode.getType()==parameters.NONTERMINAL) {
                	parseLatexFmt.append(" (.").append(currentNode.getName());
                	//if (!(currentNode.getParentNode()==null)) parseLatexFmt.append(" [p=" + currentNode.getParentNode().getHPNNodeName() + "] ");
                	if (printStructureETNs) parseLatexFmt.append(" [").append(currentNode.getStructureETN().split("_")[1] + "-" + currentNode.getStructureETN().split("_")[2]).append("]"); 
                	if (printRankedETNs) parseLatexFmt.append(" [").append(currentNode.getRankNrInDerivation()).append("]");
                	parseLatexFmt.append(".");
                }
                else {
                    parseLatexFmt.append(" `");
                    String terminalName = currentNode.getName();
                    //remove _ from name, because Latex doesn't like it
                    if (!parameters.BRACKET_FORMAT_WSJ_STYLE) 
                        terminalName = currentNode.getName().substring(0, currentNode.getName().length());
                    
                    //System.out.println("terminalName=" + terminalName + "; currentNode.getStructureETN()=" + currentNode.getStructureETN());
                    if (printStructureETNs) 
                    	terminalName += " [" + currentNode.getStructureETN().split("_")[1] + "-" + currentNode.getStructureETN().split("_")[2] + "]";
                    if (printRankedETNs)  terminalName += " [" + currentNode.getRankNrInDerivation() + "]";  	
                    //if (!(currentNode.getParentNode()==null)) parseLatexFmt.append(" [p=" + currentNode.getParentNode().getHPNNodeName() + "] ");
                    parseLatexFmt.append(terminalName).append("'");
                }
                //{} for multiwords
                
                //if (blnPrintNrSubtrees && currentNode.getType()==Main.NONTERMINAL) parseLatexFmt.append(" (" + currentNode.getnrSubtrees() + ")");
                //if (blnPrintNodeSpans && currentNode.getType()==Main.NONTERMINAL) parseLatexFmt.append(" (" + currentNode.getLeftSpan() + "-" + currentNode.getRightSpan() + ")");
                parseLatexFmt.append("");
                
            }
            //(s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(up,[(mp,[(mp,[(p,[(van_,[])]),(np,[(voorschoten_,[])])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])]),(zp,[(hallo_,[]),(jij_,[]),(rp,[(daar_,[]),(auto_,[])])])])])])]).
                   
            //if there is an unprocessed child, make it current node and return to start of while-loop
            if (currentNode.getChildNodes().size()>currentNode.processedChildren) {
                currentNode = currentNode.getChildNodes().get(currentNode.processedChildren);
            }
            //if there are no more unprocessed children, write closing bracket, and mark node as processed with parent
            else {
                if (currentNode.getType()==parameters.NONTERMINAL) parseLatexFmt.append(" )");
                //System.out.println("nrChildren=" + currentNode.getChildNodes().size() + "; processed=" +currentNode.processedChildren);
                //System.out.println("current node=" + currentNode.getName());
                currentNode.getParentNode().processedChildren++;
                //go to parent node
                currentNode = currentNode.getParentNode();
            }
            
            //check allChildrenOfTOPProcessed
            if (currentNode.getName().equals("TOP") && currentNode.getChildNodes().size() == currentNode.processedChildren)
            allChildrenOfTOPProcessed = true;
        }
        parseLatexFmt.append(" )");
        
        /*
        (s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(mp,[(mp,[(p,[(van_,[])]),(np,
[(voorschoten_,[])])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),
(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])]),(zp,[(hallo_,[])])])])]).
         */
        
        if (!strText.equals("")) System.out.println("The " + strText + " parse tree is: " + parseLatexFmt.toString());
        return parseLatexFmt.toString();
    }
    
    public String printOVISFormat(boolean blnWithUnderScore){
        //go counter-clockwise, start with TOP, find left child
        //if no child, then go to sister, if no more sister, then go up one level, go to next child
        //do this until you encounter again the TOP node from the right side, and after you have treated all its children
        
        //first set processedChildren of all nodes to 0
        for (Node myNode : this.associatedNodes) {
            myNode.processedChildren = 0;
        } 
        StringBuffer parseOVISFmt = new StringBuffer(); 
        
        //(A,[comma-list])
        //(mp,[(morgenavond_,[])]).
                    
        Node currentNode = this.associatedNodes.get(0); 
        boolean allChildrenOfTOPProcessed = false;
        
        while (allChildrenOfTOPProcessed ==false) {
            //only write [. first time you enter node
            if (currentNode.processedChildren==0){
                if (currentNode.getType()==parameters.NONTERMINAL) parseOVISFmt.append("(").append(currentNode.getName()).append(",[");
                else {  //terminal
                    parseOVISFmt.append("(").append(currentNode.getName()).append(blnWithUnderScore?"_":"").append(",[])");
                }        
            }
            //OVIS: (s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(up,[(mp,[(mp,[(p,[(van_,[])]),(np,[(voorschoten_,[])])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])]),(zp,[(hallo_,[]),(jij_,[]),(rp,[(daar_,[]),(auto_,[])])])])])])]).
                   
            //if there is an unprocessed child, make it current node and return to start of while-loop
            if (currentNode.getChildNodes().size()>currentNode.processedChildren) {
                if (currentNode.processedChildren > 0) parseOVISFmt.append(",");
                currentNode = currentNode.getChildNodes().get(currentNode.processedChildren);
            }
            //if there are no more unprocessed children, write closing bracket, and mark node as processed with parent
            else {
                if (currentNode.getType()==parameters.NONTERMINAL) parseOVISFmt.append("])");
                //System.out.println("nrChildren=" + currentNode.getChildNodes().size() + "; processed=" +currentNode.processedChildren);
                //System.out.println("current node=" + currentNode.getName());
                currentNode.getParentNode().processedChildren++;
                //go to parent node
                currentNode = currentNode.getParentNode();
            }
            
            //check allChildrenOfTOPProcessed
            if (currentNode.getName().equals("TOP") && currentNode.getChildNodes().size() == currentNode.processedChildren)
            allChildrenOfTOPProcessed = true;
        }
        parseOVISFmt.append(")");
        
        /*
        (s,[(per,[(ik_,[])]),(vp,[(v,[(wil_,[])]),(mp,[(mp,[(p,[(van_,[])]),(np,
[(voorschoten_,[])])]),(mp,[(p,[(naar_,[])]),(np,[(np,[(den,[(den_,[])]),
(haag,[(haag_,[])])]),(np,[(np,[(centraal_,[])]),(n,[(station_,[])])])])]),(zp,[(hallo_,[])])])])]).
         */
      
        System.out.println("The parse tree in OVIS format is: " + parseOVISFmt.toString());
        return parseOVISFmt.toString();
    }
    
    public String printWSJFormat(){
        //go counter-clockwise, start with TOP, find left child
        //if no child, then go to sister, if no more sister, then go up one level, go to next child
        //do this until you encounter again the TOP node from the right side, and after you have treated all its children
        
        //first set processedChildren of all nodes to 0
        for (Node myNode : this.associatedNodes) {
            myNode.processedChildren = 0;
        } 
        StringBuffer parseWSJFmt = new StringBuffer(); 
        
        //(A,[comma-list])
        //(mp,[(morgenavond_,[])]).
        //wordt: (mp (POSTAG morgenavond) )  
        //dus: , --> spatie ; [] niet schrijven, [] wordt herhaling
        
        Node currentNode = this.associatedNodes.get(0); 
        boolean allChildrenOfTOPProcessed = false;
        
        while (allChildrenOfTOPProcessed ==false) {
            //only write [. first time you enter node
            if (currentNode.processedChildren==0){
                if (currentNode.getType()==parameters.NONTERMINAL) parseWSJFmt.append("(").append(currentNode.getName()).append(" ");
            	//for extract a treebank with all nodes equal X except for TOP and terminals
                //if (currentNode.getType()==Main.NONTERMINAL) {
                //	if (currentNode.getName().equals("TOP")) parseWSJFmt.append("(TOP ");
                //	else parseWSJFmt.append("(X ");
                //}
                else {  //terminal
                    //TEMP: parseWSJFmt.append("(").append(currentNode.getName()).append(" )");
                    parseWSJFmt.append("(").append(currentNode.getName()).append(" )");
                }        
            }
                   
            //if there is an unprocessed child, make it current node and return to start of while-loop
            if (currentNode.getChildNodes().size()>currentNode.processedChildren) {
                if (currentNode.processedChildren > 0) parseWSJFmt.append(" ");
                currentNode = currentNode.getChildNodes().get(currentNode.processedChildren);
            }
            //if there are no more unprocessed children, write closing bracket, and mark node as processed with parent
            else {
                if (currentNode.getType()==parameters.NONTERMINAL) parseWSJFmt.append(")");
                //System.out.println("nrChildren=" + currentNode.getChildNodes().size() + "; processed=" +currentNode.processedChildren);
                //System.out.println("current node=" + currentNode.getName());
                currentNode.getParentNode().processedChildren++;
                //go to parent node
                currentNode = currentNode.getParentNode();
            }
            
            //check allChildrenOfTOPProcessed
            if (currentNode.getName().equals("TOP") && currentNode.getChildNodes().size() == currentNode.processedChildren)
            allChildrenOfTOPProcessed = true;
        }
        parseWSJFmt.append(")");
        
       
        //System.out.println("The parse tree in WSJ format is: " + parseWSJFmt.toString());
        return parseWSJFmt.toString();
    }
    
    public String printSimple(boolean blnPrintNrSubtrees, boolean blnPrintNodeSpans, String strText){
        
        StringBuffer parseSimpleFmt = new StringBuffer(); 
        parseSimpleFmt.append(strText + ": ");
        
        for (Node myNode : this.associatedNodes) {
            if (myNode.getType()==parameters.NONTERMINAL && (myNode.getRightSpan()-myNode.getLeftSpan()>1)) parseSimpleFmt.append(" (" + myNode.getLeftSpan() + "-" + myNode.getRightSpan() + ")  ");
        }
        System.out.println(parseSimpleFmt.toString());
        return parseSimpleFmt.toString();
    }
    
    public String getYieldString(boolean hasPostags) {
        StringBuffer yield = new StringBuffer();
        
        for (Node aNode : associatedNodes) {
        	
            if (aNode.getType()==parameters.TERMINAL) {
            	//System.out.println("aNode.getName()=>>" + aNode.getName() + "<<");
            	if (hasPostags) yield.append(aNode.getName().split(" ")[1].trim() + " ");
            	else yield.append(aNode.getName().trim() + " ");
            }
        }
        return yield.toString().trim();
    }
    
    /**
     * Finds the `goalNode' of a node myNode in a parseTree
     * @param myParseTree
     * @param myNode
     * @return goalNode of myNode, that is the first parent for which the terminal branch is not attached to left child
     * plus the daughter of goalNode, that is the attach site
     */
     public static Node[] getGoalNode(parseTree myParseTree, Node myNode) {	//, Node topNode

 		//als het een terminal is (en niet de eerste) zoek dan de goalCategory 
 		Node goalNode = null;

 		boolean leftChild = true;
 		int childIndex =0;
 		Node parentNode = myNode.getParentNode();

 		//find parent node of projectedNode: skip parent nodes for which child is left child, and continue up until you find paren with more than one child
 		if (!(parentNode==null)) {

 			childIndex = parentNode.getChildNodes().indexOf(myNode);
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
 		Node attachNode = goalNode.getChildNodes().get(childIndex);
 		
 		return new Node[]{goalNode, attachNode};
 	}
    
     public static ArrayList<parseTree> extractParseTreesFromTreebank(String treebankFile, boolean blnFilePreprocessed, boolean blnCaseConversion) throws FileNotFoundException, IOException {
 		//ArrayList<parseTree> parseTrees = new ArrayList<parseTree>();
 		BufferedReader buff = null;

 		ArrayList<String> labeledSentences = new ArrayList<String> ();
 		//TAKE_OFF_SBJ: if true, disregards suffixes of the labels in gold standard, such as NP_SBJ, etc, and identifies all NPs, etc.
 	
 		//Main.PRINT_LATEX_TREEFILE = true;
 		//TAKE_OFF_SBJ: if true, disregards suffixes of the labels in gold standard, such as NP_SBJ, etc, and identifies all NPs, etc.
 		if (treebankFile.toLowerCase().contains("tuebingen") || treebankFile.toLowerCase().contains("toebingen")) parameters.TAKE_OFF_SBJ = false;
 		
 		int myCounter = 0;
 		String mySentence;
 		parseTree myParseTree = null;
      
 		ArrayList<parseTree> parseTrees = new ArrayList<parseTree>();
 		
 		buff = new BufferedReader(new FileReader(treebankFile));
 		while ((mySentence = buff.readLine()) !=null){
 		    
 		    myCounter++; 
 		    if (myCounter % 500 ==0) System.out.println("##############################" + myCounter + ": " +mySentence + "##############################");
 		    mySentence = mySentence.trim();
 		     
 		    //System.out.println("mySentence=" + mySentence);
 		    
 		    //preprocess Tuebing corpus!
 		    if (treebankFile.toLowerCase().contains("tuebingen")) {
 		    	mySentence = preprocessTuebingenSentence(mySentence);			
 		    }
 		    
 		    myParseTree = ExtractParseFromWSJText(mySentence, false);
 		    //System.out.println("ExtractParseFromWSJText=" + simpleSentence);   
 		    //(TOP (PRWD (FOOT (SYLL_L (ONS (`)) (NUC_L (e))) (SYLL_L (ONS (l)) (NUC_L (e))))) (PRWD (SYLL_L (ONS (m)) (NUC_L (a) )) (FOOT (SYLL_L (ONS (k)) (NUC_L (u))) (SYLL_L (ONS (l)) (NUC_L (e))))))
 		    //myParseTree.printToLatex(false, false, "");
 		    //you still need to remove nonTerminal nodes that have no children, recursively
 		    
 		    //skip all this to save time if you are working from preprocessed file
 		    if (!blnFilePreprocessed) {
 			    myParseTree.removeEmptyNonTerminalNodes();
 			    myParseTree.removeEmptyNonTerminalNodes();
 			    myParseTree.removeEmptyNonTerminalNodes();
 		    }
 		    myParseTree.calculateNodeDepth();
 		    
 		    parseTrees.add(myParseTree);
 		}	//for (String simpleSentence : completeSentences)
 		return parseTrees;
 	}
     
     
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
                            
                             //CREATE_SEPARATE_POSTAG_AND_LEXICAL_NODES
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
 	             if (myNode.getName().contains("-") && (myNode.getType()==parameters.NONTERMINAL)) 
 	            	if (!(myNode.getName().split("-")[0].equals(""))) myNode.setName(myNode.getName().split("-")[0]);
 	           //  ArrayList<String> childnodes = new ArrayList<String>();
 	            //for (Node childNode : myNode.getChildNodes()) {
 	           //     childnodes.add(childNode.getName());
 	            //}
 	           
 	           // System.out.println(myNode.getName() + " --> " + childnodes);
 	        }
         }
         return myParseTree;
     }
     
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
     
     public void unBinarize() {
     	//from deepest level to top level, replace every intermediate node (e.g., <VP...NP> with its children
     	//both if <X> has one or two children; thereby move right daughter of <X> node further to the right.
     	
     	//Internal nonterminals from Markovization are indicated as "X>Y"
 		ArrayList<Node> internalNodes = new ArrayList<Node>();
 		
 		for (int currentDepth = this.deepestLevel; currentDepth >=0; currentDepth--) {
             for (Node myNode : this.associatedNodes) {
             	
             	 if (myNode.getDepth() == currentDepth) {
             		 if (myNode.getName().contains(">")) {	//internal node
             			 internalNodes.add(myNode);
             		 }
             	 }
             }
 		}
 		
 		for (Node internalNode : internalNodes) {
 			
 			Node parentNode = internalNode.getParentNode();
 			//get the non-internal daughter of the parentNode, if there is one...
 			ArrayList<Node> nonInternalChildren = new ArrayList<Node>();
 			for (Node myNode : parentNode.getChildNodes()) {
 				 if (!(myNode.getName().contains(">"))) nonInternalChildren.add(myNode);
 			}
 			
 			//remove children from parent
 			parentNode.removeAllChildNodes();
 			
 			//get the children of the internal node, and attach them under the parent
 			for (Node childNode : internalNode.getChildNodes()) {
 				childNode.replaceParentNode(parentNode);
 				parentNode.addChildNode(childNode);
 			}
 			for (Node childNode : nonInternalChildren) {
 				parentNode.addChildNode(childNode);
 			}
 			
 			this.associatedNodes.remove(internalNode);
 		}
     }
     
    public String printSpans() {
        StringBuffer parseSpans = new StringBuffer();
        for (Node myNode : this.associatedNodes) {
            if (myNode.getRightSpan()-myNode.getLeftSpan()>1) parseSpans.append(myNode.getLeftSpan() + "-" + myNode.getRightSpan() + " ");  //myNode.getType()==Main.NONTERMINAL && (
        }
        //System.out.println("Spans: " + parseSpans);
        return parseSpans.toString();
    }
    
    
}
