/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import static documentindexer.DocumentIndexer.globalIndexingFlag;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

/**
 *
 * @author Mark
 */
public class IndexBuilderThread implements Runnable{
    TextArea indexingProcessArea;
    String url;
    ConcurrentHashMap<Object, Object> globalIndex;  // The index keeping all the correspondences between words and files.
    Button btnStart, btnQuery;
    Text loadingText;
    Thread automaticIndexUpdater;
    
    IndexBuilderThread(Text loadingText, TextArea indexingArea, String inputFolder, ConcurrentHashMap<Object, Object> index, Button btnStart, Button btnQuery, Thread automaticIndexUpdater) {
        indexingProcessArea = indexingArea;
        url = inputFolder;
        this.globalIndex = index;
        this.btnStart = btnStart;
        this.btnQuery = btnQuery;
        this.loadingText = loadingText;
        this.automaticIndexUpdater = automaticIndexUpdater;
    }
    
    
    @Override
    public void run() {
        
        List<File> subfolders;
        List<File> textFiles;
        try{
            subfolders = CustomMethods.findSubfolders(url);     // This way we can check if the passed URL is correct and be sure for the User to know if it is not.
            textFiles = CustomMethods.findtxtFiles(url);
        }catch(Exception e){
            CustomMethods.createPopUpWindow("Please enter a valid URL for the directory first.");
            globalIndexingFlag = false;
            return;
        }
        
        Platform.runLater(new GUILoadingStateUpdater(loadingText, "                                                    Building the index..."));    // Warning the User of the beginning of the indexing process.
        
        GUIUpdater guiUpdater = new GUIUpdater(indexingProcessArea, loadingText);    // Creating the Thread that will take care of the updating of the GUI to show the progresses.
        Thread guiUpdaterThread = new Thread(guiUpdater);   // Creating a Thread to display the progresses in the GUI.
        
        // Launching a new Thread for each folder and each .txt file.
        for(File dir : subfolders){
            (new Thread(new FolderThread(indexingProcessArea, dir, guiUpdater, globalIndex))).start();
        }
        for(File file : textFiles){
            (new Thread(new FileThread(indexingProcessArea, file, guiUpdater, globalIndex))).start();  // Launching the Thread that will index it.
        }
        
        guiUpdaterThread.start();   // Launching the Thread.
        
    }    
}
