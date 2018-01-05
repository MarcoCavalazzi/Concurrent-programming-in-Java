/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.control.TextArea;

/**
 *
 * @author Marco Carlo Cavalazzi
 */
public class FileThread implements Runnable{
    File file;     // The file to examine.
    TextArea indexingProcessArea;   // The area in which we shall add text to in order to inform the User of the developments.
    GUIUpdater guiUpdater;          // This is the Thread that will update the GUI to show the progresses.
    ConcurrentHashMap<Object, Object> indexCollection;  // The index keeping all the correspondences between words and files.
    
    FileThread(TextArea processArea, File f, GUIUpdater guiUp, ConcurrentHashMap<Object, Object> index) {
        this.indexingProcessArea = processArea;
        this.file = f;
        guiUpdater = guiUp;
        this.indexCollection = index;
    }
    
    @Override
    public void run() {
        //DB statement: System.out.println("> File thread '"+ Thread.currentThread() +"' created! \t"+ file);
        guiUpdater.addToOutputQueue("> File thread '"+ Thread.currentThread() +"' created! \t\t"+ file);
        guiUpdater.newThreadStarting();
        Thread.currentThread().setName(Thread.currentThread().getName() +">>>Worker");
        
        BufferedReader fileBR;
        try {
            fileBR = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        } catch (Exception ex) {    // Necesarry if, for example, the file is not readable fro the User (protected, as a system file).
            guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
            Thread.currentThread().interrupt();
            return;
        }
        
        String line = null;    // A line of text in the .txt file.
        try {
            line = fileBR.readLine();
        } catch (IOException ex) {
            try {
                fileBR.close();
            } catch (IOException ex2) {}
            finally {
                guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
                Thread.currentThread().interrupt();
            }
        }
        while( line != null ){
            /* Checks for the PAUSE indexing and STOP indexing flags. */
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
            
            /* Starting to index a line of the file. */
            
            String [] tokens = line.split("\\s+", 100000);  // \\s+ means any number of whitespaces between tokens
            // We are also giving a limit on the amount of strings retrieved (100000) from a single line using the split method in order to avoid memory overloads (defensive programming).
            
            // We want to keep only the consistent strings (rejecting the empty ones)
            int tokensLength = tokens.length;
            for(int i=0; i<tokensLength; i++){
                if(tokens[i].length() > 0){     // This way we avoid considering empty strings.
                    
                    if(indexCollection.get(tokens[i]) == null){
                        // This word has not been memorized yet.
                        ArrayList<File> newList = new ArrayList<File>();
                        newList.add(file);
                        indexCollection.put(tokens[i], newList);
                    }else{
                        // The word has already been memorized.
                        // What we want in this case is to read the files' list and put in its place a new one which is copy of that one but with this file in it.
                        ArrayList<File> files = (ArrayList<File>) indexCollection.get(tokens[i]);   // Getting the 'value' linked to the 'key' = the examined word.
                        Boolean isPresent = files.contains(file);
                        if(!isPresent){  // If this file's name is not in the list yet...
                            files.add(file);    // we add it and replace the element in the concurrent hashmap.
                            indexCollection.put(tokens[i], files);
                        } // else: the word has been memorized in the concurrent hashmap and this file's name is already in the list in the 'value' position, we don't need to modify anything.
                    }
                    
                }
            }
            
            
            // Reading the next line.
            try {
                line = fileBR.readLine();
            }
            catch (IOException ex) {
                break;  // Stopping the reading in case of exception.
            }
        }
        
        
        
        // Once the job is finished...
        try {
            fileBR.close();
            //System.out.println("Indexing done. File closed.");
        } catch (IOException ex) {
            fileBR = null;
        }finally{
            guiUpdater.terminatingThread();  // Decreasing the global counter for active threads.
            try {
                super.finalize();
            } catch (Throwable ex) {}
        }
    }
}
