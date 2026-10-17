package be.kuleuven.mgG.internal.tasks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.utils.CSVReader;
import be.kuleuven.mgG.internal.view.JSONDisplayPanel;

public class ImportMetadataTask extends AbstractTask{
   
		final CySwingApplication swingApplication;
	    final CyApplicationManager cyApplicationManager;
	    
	    private final MGGManager mggManager;
	    
	    private String filePath;
	    
	    private JSONObject jsonResult;
	    
	    //@Tunable(description = "Display Data", groups = { "Display Settings" }, tooltip="If checked, the MetaData will be displayed in a panel")
    	//public boolean showMetaDataInPanel = true;
	    
	    
	    
	    
	    /**
	     * Constructor for the ImportMetadataTask class.
	     * 
	     * @param cytoscapeDesktopService The CySwingApplication service, which provides access to Cytoscape desktop components.
	     * @param cyApplicationManager2 The CyApplicationManager service, which provides access to the current network and view.
	     * @param filePath The path of the CSV file to import.
	     * 
	     */
	    
	    public ImportMetadataTask(String filePath,MGGManager mggManager) {
	    	
	    	this.swingApplication = mggManager.getService(CySwingApplication.class);
	        this.cyApplicationManager = mggManager.getService(CyApplicationManager.class);
	        this.filePath = filePath;
	        this.mggManager = mggManager;
	               
	    }
	    
	  
	    @Override
	    public void run(TaskMonitor taskMonitor) {
	        taskMonitor.setTitle("Importing Metadata File");
	        taskMonitor.setStatusMessage("Reading file");

	        try {
	            List<String[]> csvData = CSVReader.readCSV(taskMonitor, filePath);

	            // Process CSV data into JSON
	            JSONObject jsonResult = processCSVDataToJson(csvData);

	            // Set the JSON object in MGGManager
	            mggManager.setMetadataJsonObject(jsonResult);
	            
	            
	         
	            
	            taskMonitor.setStatusMessage("Metadata file imported successfully.");
	            taskMonitor.setProgress(1.0);

	            // A network imported before, with nodes of metadata variables, is loaded now
	            final String metadataMessage = "Metadata file loaded correctly" + mggManager.pendingNetworkNote();
	            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
	    	            metadataMessage, "Information", JOptionPane.INFORMATION_MESSAGE));

	           
	            
	            
	            
	        } catch (IOException e) {
	            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error while processing the file: " + e.getMessage());
	            e.printStackTrace();
	        }
	    }

	    
	    private void showDataInPanel(JSONObject jsonObject) {
		    JSONDisplayPanel panel = new JSONDisplayPanel(mggManager, jsonObject);
		 	
			 JFrame frame = new JFrame("Imported Metadata");
			 frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			 frame.getContentPane().add(panel); frame.pack(); frame.setVisible(true);
			 
    }
	    
	    
	    	private JSONObject processCSVDataToJson(List<String[]> csvData) {
	    		JSONObject jsonObject = new JSONObject();
	    	    JSONArray mainArray = new JSONArray();

	    	    if (!csvData.isEmpty()) {
	    	        //  adding the headers
	    	        String[] headers = csvData.remove(0);
	    	        JSONArray headerArray = new JSONArray();
	    	        for (String header : headers) {
	    	            headerArray.add(header);
	    	        }
	    	        mainArray.add(headerArray);

	    	        // adding each row of data
	    	        for (String[] row : csvData) {
	    	            JSONArray rowArray = new JSONArray();
	    	            for (String cell : row) {
	    	                rowArray.add(cell);
	    	            }
	    	            mainArray.add(rowArray);
	    	        }
	    	    }

	    	    jsonObject.put("metadata", mainArray);
	    	    return jsonObject;

	   
	    
	    	}
	}
