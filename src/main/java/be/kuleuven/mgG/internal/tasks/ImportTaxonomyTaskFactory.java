package be.kuleuven.mgG.internal.tasks;

import java.io.File;

import javax.swing.JFileChooser;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import be.kuleuven.mgG.internal.model.MGGManager;

/** Apps > MGG > Import Data > Import Taxonomy: the taxonomy of the nodes of a network without an abundance table. */
public class ImportTaxonomyTaskFactory implements TaskFactory {

    private final MGGManager mggManager;

    public ImportTaxonomyTaskFactory(MGGManager mggManager) {
        this.mggManager = mggManager;
    }

    @Override
    public TaskIterator createTaskIterator() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Taxonomy of the nodes: node names and taxonomies, in two columns");
        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return new TaskIterator();
        }
        File selectedFile = fileChooser.getSelectedFile();
        return new TaskIterator(new ImportTaxonomyTask(selectedFile.getAbsolutePath(), mggManager));
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
