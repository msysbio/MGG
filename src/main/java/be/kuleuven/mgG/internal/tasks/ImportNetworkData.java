package be.kuleuven.mgG.internal.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;

public class ImportNetworkData extends AbstractTask {
	
	
    private final MGGManager mggManager;
	
    
    public ImportNetworkData (MGGManager mggManager) {
    	this.mggManager=mggManager;
    }

    
    
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
				
		//get current network
		
		CyNetwork currentNetwork=mggManager.getCurrentNetwork();
		
		  
    	if(currentNetwork==null) {
    
		taskMonitor.showMessage(TaskMonitor.Level.ERROR, " No Network is currently loaded)");
		return;
		
	} 
				
		   
	    JSONArray networkData = new JSONArray();

	 
	    CyTable edgeTable = currentNetwork.getDefaultEdgeTable();
	    
	    
	    boolean weightColumnExists = edgeTable.getColumn("microbetag::weight") != null;
	    boolean hasWeights = false; 

	    //  are weights present?
	    if (weightColumnExists) {
	        for (CyEdge edge : currentNetwork.getEdgeList()) {
	            Double weight = edgeTable.getRow(edge.getSUID()).get("microbetag::weight", Double.class);
	            if (weight != null) {
	                hasWeights = true;
	                break; 
	            }
	        }
	    }
	    
	    // Add headers
	    JSONArray headers = new JSONArray();
	    
	    headers.add("Source Node");
	    headers.add("Target Node");
	    
	    if(hasWeights) {
	    	
	    	headers.add("weight");
	    }
	    networkData.add(headers);
	    

	   
	    for (CyEdge edge : currentNetwork.getEdgeList()) {
	        
	    	 JSONArray edgeArray = new JSONArray();

	    	    
	    	    String sourceNodeId = currentNetwork.getRow(edge.getSource()).get("name", String.class);
	    	    String targetNodeId = currentNetwork.getRow(edge.getTarget()).get("name", String.class);

	    	    
	    	    
	    	    Double weight = edgeTable.getRow(edge.getSUID()).get("microbetag::weight", Double.class);
	    	    
	       
	        if (hasWeights && weight!=null) {
	        	
	        	
	    	    		edgeArray.add(sourceNodeId);
	    	    		edgeArray.add(targetNodeId);
	    	    		edgeArray.add(weight);
	            
	    	    		networkData.add(edgeArray);
	            
	            } else if(!hasWeights) {
	                	
	            	
	            	
	            	taskMonitor.showMessage(TaskMonitor.Level.ERROR ,"Network data has no microbetag::weight value,"
	            			+ "network data will not import");
	            }
	        
	    }
	       

	    //  JSONObject for the network
	    JSONObject networkJsonObject = new JSONObject();
	    networkJsonObject.put("network", networkData);
	    
	    //  JSONObject for abundance data
	    JSONObject datajsonObject=new JSONObject();
	    datajsonObject=mggManager.getJsonObject();
	    
	    if (!hasWeights) {
	        mggManager.setNetworkObject(null);
	        mggManager.setPendingNetworkObject(null);
	        return;
	    }

	    java.util.List<String> unknown = mggManager.unknownNetworkNodes(networkJsonObject);
	    if (unknown.isEmpty()) {
	        //  If all are present
	        mggManager.setNetworkObject(networkJsonObject);
	        mggManager.setPendingNetworkObject(null);
	        taskMonitor.setTitle("Uploading Network");
	        taskMonitor.showMessage(TaskMonitor.Level.INFO ,"Network data loaded correctly");

	        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
	            "Network data loaded correctly", "Information", JOptionPane.INFORMATION_MESSAGE));

	    } else {

	        // Nodes without a taxonomy yet: the network is kept until their taxonomies (abundance table or
	        // taxonomy file) or the metadata (for nodes of metadata variables) are imported
	        // (MGGManager.acceptPendingNetwork)
	        mggManager.setNetworkObject(null);
	        mggManager.setPendingNetworkObject(networkJsonObject);
	        String message = (mggManager.hasTaxa() || mggManager.getMetadataJsonObject() != null
	                ? unknown.size() + " node(s) of the network have no taxonomy and are not metadata variables, e.g. "
	                    + String.join(", ", unknown.subList(0, Math.min(5, unknown.size()))) + ".\n"
	                : "The network is kept: import the taxonomies of its nodes.\n")
	            + MGGManager.whatNodesNeed() + "\nThe network is loaded once they are all known.";
	        taskMonitor.showMessage(TaskMonitor.Level.WARN, message.replace("\n", " "));
	        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
	            message, "Network not loaded yet", JOptionPane.WARNING_MESSAGE));
	    }
	}
	
		

		

  
	}
	    
