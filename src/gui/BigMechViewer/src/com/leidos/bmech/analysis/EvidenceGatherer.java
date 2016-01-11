package com.leidos.bmech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.leidos.bmech.model.WorkingSet;

import drae.j.VisualElement.VCell;
import drae.j.VisualElement.VCol;
import drae.j.VisualElement.VTable;
import drae.j.VisualElement.VText;

public class EvidenceGatherer {
	GeneGazetteer gg;
	
	public void loadGeneGazetteer(WorkingSet ws){
		gg = new GeneGazetteer(ws);	
		System.out.println("loaded gene gazetteer " + gg);
	}
	
	public void gatherEvidence(VTable table){
		
		EvidenceTable evMap = (EvidenceTable) table.evidence_table;
		List<VCol> cols = (List<VCol>) table.getCols();
		for(VCol col : cols){
			List<VCell> cells = (List<VCell>) col.getDataItems();
			for( VCell cell : cells ){
				gatherEvidenceForCell(cell, evMap);			
			}
			gatherEvidenceForCol(col, evMap);
		}

	}
	
	public void gatherEvidenceForCell(VCell cell, EvidenceTable evMap){
		List<Evidence> ret = new ArrayList<Evidence>();
		List<VText> texts = (List<VText>) cell.items;
		String text = (String)cell.getText();
		for(String word : text.split(" ")){
			String classification = gg.getItemClass(word.toLowerCase());
			if(classification != null){
				evMap.addEvidence(cell, classification, "BANNER-" +word, 0.75);
			}
		}
		
		//if(evMap.getEvidenceFor(cell).size())

		
	}
	
	public void gatherEvidenceForCol(VCol col, EvidenceTable evMap){
		//analyze header
		List<VCell> headerCells = (List<VCell>)col.getHeaderItems();
		//for now assume only 1 cell in the header cell
		if(!headerCells.isEmpty()){
			VCell headerCell =  headerCells.get(0);
			gatherEvidenceForCell(headerCell, evMap);
			if(((String)headerCell.getText()).equalsIgnoreCase("gene")){
				evMap.addEvidence(col, "gene", "GeneInHeader", 0.75);
			}
		}
		List<VCell> cells = (List<VCell>) col.getDataItems();
		List<Evidence> colDataEv = new ArrayList<Evidence>();
		for( VCell cell : cells ){
			Evidence colEvidence = deduce(evMap.getEvidenceFor(cell), "best");
			if (colEvidence != null){
				colDataEv.add(colEvidence);
			}
		}
		
		
		evMap.addEvidence(col, deduce(colDataEv, "best"));
		
		
	}
	
	public Evidence deduce(List<Evidence> list, String method){
		if(method.equals("best")){
			return deduceBest(list);
		}
		return null;
		
	}

	private Evidence deduceBest(List<Evidence> list) {
		double bestBelief = -2;
		Evidence bestEvidence = null;
		for(Evidence ev : list){
			if(ev.getBelief() >= bestBelief){
				bestBelief = ev.getBelief();
				bestEvidence = ev;
			}
		}
		if(bestEvidence == null){
			//no evidence was found
			return null;
		}
		return new Evidence(bestBelief,"BestEvidence", bestEvidence.getClassification() );

		
	}
	
	
}
