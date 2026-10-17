package be.kuleuven.mgG.internal.tasks;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;

public class ImportNetworkDataTaskFactory implements TaskFactory {
	
	private final MGGManager mggManager;
	
	public ImportNetworkDataTaskFactory (MGGManager mggManager) {
		this.mggManager=mggManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ImportNetworkData(mggManager));
	}

	@Override
	public boolean isReady() {
		
		// In any order: a network imported before its taxonomies (abundance table or taxonomy file) or metadata
		// is kept until they are imported
		return mggManager.getService(org.cytoscape.application.CyApplicationManager.class).getCurrentNetwork() != null;
	}

}
