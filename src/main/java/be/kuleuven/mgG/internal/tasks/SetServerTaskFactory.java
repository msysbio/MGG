package be.kuleuven.mgG.internal.tasks;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class SetServerTaskFactory implements TaskFactory {

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new SetServerTask());
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
