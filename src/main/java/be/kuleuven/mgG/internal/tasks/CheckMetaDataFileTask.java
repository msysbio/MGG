package be.kuleuven.mgG.internal.tasks;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.view.JSONDisplayPanel;

public class CheckMetaDataFileTask extends AbstractTask {

	
	 private final MGGManager mggManager;
	 
		
	 public CheckMetaDataFileTask(MGGManager mggManager) {
	    	
	    
	        
			this.mggManager = mggManager;
	               
	    }
	    

	
	 @Override
	    public void run(TaskMonitor taskMonitor) throws Exception {
	        JSONObject metaDataObject = mggManager.getMetadataJsonObject();
	        taskMonitor.setStatusMessage("MetaData Send " + metaDataObject.toJSONString());
	        
	        if (metaDataObject != null && metaDataObject.containsKey("metadata")) {
	                SwingUtilities.invokeLater(() -> {
	                    JSONDisplayPanel panel = new JSONDisplayPanel(mggManager, metaDataObject);
	                    displayPanelInFrame(panel, "Imported Metadata");
	                });
	            } else {
		              	
		            taskMonitor.setStatusMessage("No MetaData Imported");
		        
		        }}
	            
	        
	    

	    private void displayPanelInFrame(JSONDisplayPanel panel, String title) {
	        JFrame frame = new JFrame(title);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.getContentPane().add(panel);
	        frame.pack();
	        frame.setVisible(true);
	   
	    }
	    
}
