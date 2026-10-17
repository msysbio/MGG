package be.kuleuven.mgG.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Sets the microbetag server MGG sends the data to (Apps > MGG > Set microbetag server):
 * our public server, or your own, e.g. http://localhost:5000.
 */
public class SetServerTask extends AbstractTask {

    @Tunable(
            description="microbetag server",
            tooltip="<html>The microbetag server to annotate your data with: our public server (default),<br>"
                    + "or your own, e.g. http://localhost:5000. Leave empty for the public server.</html>",
            gravity=1.0,
            required=true
    )
    public String SERVER_URL = SendDataToServerTask.getServer();

    @Override
    public void run(TaskMonitor taskMonitor) {
        SendDataToServerTask.setServer(SERVER_URL);
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "MGG will send your data to " + SendDataToServerTask.annotateURL(SendDataToServerTask.getServer()));
    }
}
