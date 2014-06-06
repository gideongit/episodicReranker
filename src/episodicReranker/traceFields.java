package episodicReranker;

public class traceFields {

	protected Integer commonHistory = 0;
	protected Double derivationLength = 0d;
	Integer[] viterbiPointer;	//points to a traceState in a treeletState preceding the current treeletState
	
	/**
	 * data structure for storing `state' properties of a trace: commonHistory, derivationLength and viterbiPointer
	 */
	public traceFields() {	//int sentenceNr, int rankNr
		//this.sentenceNr=sentenceNr;
		//this.rankNr=rankNr;
	}
	
	public void setCH(int commonHistory) {
		this.commonHistory = commonHistory;
	}
	
	public Integer getCH() {
		return this.commonHistory;
	}
	
	public void setDerivationLength(double derivationLength) {
		this.derivationLength = derivationLength;
	}
	
	public Double getDerivationLength() {
		return this.derivationLength;
	}
	
	public Integer[] getViterbiPointer() {
		return this.viterbiPointer;
	}
	
	public void setViterbiPointer(int sentenceNr, int rankNr) {	
		this.viterbiPointer = new Integer[2];//{sentenceNr, rankNr};
		this.viterbiPointer[0]=sentenceNr;
		this.viterbiPointer[1]=rankNr;
	}
}
