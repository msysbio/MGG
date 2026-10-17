package be.kuleuven.mgG.internal.tasks;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;

public class ImportMetadataTaskFactory implements TaskFactory {

	
	
	  	private final MGGManager mggManager;
	   

	    public ImportMetadataTaskFactory(MGGManager mggManager) {
	        this.mggManager = mggManager;
	    }

	  

	    @Override
	    public TaskIterator createTaskIterator() {
	    	
	    	 JFileChooser fileChooser = new JFileChooser();
	 	    int option = fileChooser.showOpenDialog(null);
	 	    if (option == JFileChooser.APPROVE_OPTION) {
	 	        File selectedFile = fileChooser.getSelectedFile();
	 	        String filePath = selectedFile.getAbsolutePath();

	 	        return new TaskIterator(new ImportMetadataTask(filePath, mggManager));
	 	    } else if (option == JFileChooser.CANCEL_OPTION) {
	 	        // if file selection is cancelled, return empty TaskIterator
	 	        return new TaskIterator();
	 	    } else {
	 	        
	 	        String errorMessage = "Error selecting file";	 	        
	 	        JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
	 	        // Return an empty TaskIterator 
	 	        return new TaskIterator();
	 	    }
	 	}

	    @Override
	    public boolean isReady() {
	    	
	    	// In any order: before or after the abundance table, the taxonomy file or the network
	        return true;
	   
	    }
	}

	
	
	

