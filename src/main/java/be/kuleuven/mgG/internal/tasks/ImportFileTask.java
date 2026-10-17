package be.kuleuven.mgG.internal.tasks;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.IN_TOOL_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.BorderLayout;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.utils.CSVReader;
import be.kuleuven.mgG.internal.utils.HTTPUtils;
import be.kuleuven.mgG.internal.view.JSONDisplayPanel;
import be.kuleuven.mgG.internal.view.JsonResultPanel;




/**
 * This class represents a task for importing a CSV file and processing it into a JSON array.
 * 
 * The CSV file is read and parsed into a list of string arrays, where each array represents a row in the CSV file.
 * The task then creates a JSON array where each JSON object corresponds to a row in the CSV file.
 * The JSON array is then set in the MGGManager
 *
 * The task also provides options to display the JSON data in a panel and to write the JSON data to a file.
 * 
 */

public class ImportFileTask extends AbstractTask {
   
	final CySwingApplication swingApplication;
    final CyApplicationManager cyApplicationManager;
    
    private final MGGManager mggManager;
    
    private String filePath;
    
    private JSONObject jsonObject;
    
    
   
 
    // @Tunable(description = "Display Data", groups = { "Display Settings" },
    //tooltip="If checked, the Data will be displayed in a panel")
    //	public boolean showJSONInPanel = true;
    //    
    // @Tunable(description="Write JSON to file",groups = { "Create File Settings" },
    //tooltip="If checked, a new JSON file will be created in the same path as the original file",exampleStringValue="true")
    // public boolean writeToFile = false;  
    //    

    
    
    /**
     * Constructor for the ImportFileTask class.
     * 
     * @param cytoscapeDesktopService The CySwingApplication service, which provides access to Cytoscape desktop components.
     * @param cyApplicationManager2 The CyApplicationManager service, which provides access to the current network and view.
     * @param filePath The path of the CSV file to import.
     * 
     */
    
    public ImportFileTask(String filePath,MGGManager mggManager) {
    	
    	this.swingApplication = mggManager.getService(CySwingApplication.class);
        this.cyApplicationManager = mggManager.getService(CyApplicationManager.class);
        this.filePath = filePath;
        this.mggManager = mggManager;
               
    }
    
  
    @Override
    public void run(TaskMonitor taskMonitor) {
    	taskMonitor.setTitle("Importing abudance data file");
        taskMonitor.setStatusMessage("Reading file");

        try {
        	
            // Call CSVReader from Utils to parse the TSV/CSV file 
            List<String[]> csvData = CSVReader.readCSV(taskMonitor, filePath);
            
            String[] headers = null;
            for (int i = 0; i < csvData.size(); i++) {
                String[] row = csvData.get(i);
                if (row.length > 1) {
                    headers = row;
                    csvData.remove(i);  
                    break;
		            }
		        }
		        
            taskMonitor.setStatusMessage("Processing data");


          
            
	        JSONArray jsonArray = new JSONArray();
	        
	        
	        JSONArray header = new JSONArray();
	        
	        for (String hdr:headers) {
	        	header.add(hdr);
	        }
	        
	        jsonArray.add(header);
	        
	        
	        
	        for (String[] values : csvData) {
	            // Skip rows with only one column
	            if (values.length <= 1) {
	                continue;
	            }
	            
	            JSONArray row=new JSONArray();
	            	
	            for (String value:values) {
	            	row.add(value);
	            }

	            jsonArray.add(row);
	            
	        }
	         
	        
	        
	        // Create a new JSONObject
	        JSONObject jsonObject = new JSONObject();

	        // Add the jsonArray to the jsonObject
	        jsonObject.put("data", jsonArray);
	        
	        
	       
       
	        
	        // Set the JSON array in the MGGManager
            mggManager.setJsonObject(jsonObject);
            
                  
            taskMonitor.setProgress(1.0);
            taskMonitor.setStatusMessage("Abudance data imported successfully.");
            // A network imported before is loaded now, if its nodes are all known
            final String abundanceMessage = "Abundance data loaded correctly" + mggManager.pendingNetworkNote();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, 
    	            abundanceMessage, "Information", JOptionPane.INFORMATION_MESSAGE));
                                   
            
            
        } catch (IOException e) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, " Error while processing the file: " + e.getMessage());
            e.printStackTrace();
               
        }}
    
    
   
        
        private void showDataInPanel(JSONObject jsonObject) {
		    JSONDisplayPanel panel = new JSONDisplayPanel(mggManager, jsonObject);
		   // JsonResultPanel panel = new JsonResultPanel(mggManager, jsonObject);
		   // mggManager.registerService(panel, CytoPanelComponent.class, new Properties());
		    
			
			 JFrame frame = new JFrame("Imported OTU/ASV");
			 frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			 frame.getContentPane().add(panel); frame.pack(); frame.setVisible(true);
			 
    }
        

	
        
}