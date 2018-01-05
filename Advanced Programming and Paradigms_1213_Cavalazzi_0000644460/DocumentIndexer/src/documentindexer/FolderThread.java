/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.control.TextArea;

/**
 *
 * @author Marco Carlo Cavalazzi
 */
public class FolderThread implements Runnable {
    File directory;    // The folder to examine.
    TextArea indexingProcessArea;   // The area in which we shall add text to in order to inform the User of the developments.
    GUIUpdater guiUpdater;      // This is the Thread that will update the GUI to show the progresses.
    ConcurrentHashMap<Object, Object> indexCollection;  // The index keeping all the correspondences between words and files.
    
    FolderThread(TextArea indexingArea, File dir, GUIUpdater guiUp, ConcurrentHashMap<Object, Object> index) {
        this.directory = dir;
        this.indexingProcessArea = indexingArea;
        guiUpdater = guiUp;
        indexCollection = index;
    }
    
    @Override
    public void run() {
        //DB statement: System.out.println("--- Folder thread '"+ Thread.currentThread() +"' created! \t"+ directory +"\n\r");
        guiUpdater.addToOutputQueue("--- Folder thread '"+ Thread.currentThread() +"' created! \t"+ directory);
        guiUpdater.newThreadStarting();
        Thread.currentThread().setName(Thread.currentThread().getName() +"-Folder");
        
        List<File> subfolders = CustomMethods.findSubfolders(directory.toString());
        List<File> textFiles = CustomMethods.findtxtFiles(directory.toString());
        
        for(File dir : subfolders){
            /* Checks for the PAUSE indexing and STOP indexing functions. */
            if(DocumentIndexer.globalStopFlag){     // The User pressed the STOP indexing button. We have to terminate the indexing process.
                guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
                Thread.currentThread().interrupt();
                return;
            }else{
                while(DocumentIndexer.globalPauseFlag){     // If the pause flag is "true" we wait until it is back to "false" or the User presses "Stop indexing".
                    if(DocumentIndexer.globalStopFlag){
                        guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
                        Thread.currentThread().interrupt();
                        return;
                    }else{
                        try {
                            Thread.sleep(1000);  // The Thread will wait x ms every cycle.
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
            
            (new Thread(new FolderThread(indexingProcessArea, dir, guiUpdater, indexCollection))).start();     // Launching a new Thread for a sub-folder.
        }
        
        for(File file : textFiles){
            /* Checks for the PAUSE indexing and STOP indexing functions. */
            if(DocumentIndexer.globalStopFlag){     // The User pressed the STOP indexing button. We have to terminate the indexing process.
                guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
                Thread.currentThread().interrupt();
                return;
            }else{
                while(DocumentIndexer.globalPauseFlag){     // If the pause flag is "true" we wait until it is back to "false" or the User presses "Stop indexing".
                    if(DocumentIndexer.globalStopFlag){
                        guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
                        Thread.currentThread().interrupt();
                        return;
                    }else{
                        try {
                            Thread.sleep(1000);  // The Thread will wait 100 ms every cycle.
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
            
            (new Thread(new FileThread(indexingProcessArea, file, guiUpdater, indexCollection))).start();  // Launching the Thread that will index it.
        }
        
        
        guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
        try {
            super.finalize();
        } catch (Throwable ex) {}
    }
    
}
