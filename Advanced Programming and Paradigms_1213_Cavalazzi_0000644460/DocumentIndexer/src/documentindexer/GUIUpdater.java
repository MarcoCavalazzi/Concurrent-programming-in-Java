/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import static documentindexer.DocumentIndexer.globalIndexingFlag;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

/**
 *
 * @author Marco Carlo Cavalazzi
 */
public class GUIUpdater implements Runnable {
    BlockingQueue<String> outputQueue = new LinkedBlockingQueue();      // Queue of the strings used to communicate with the user
    BlockingQueue<Integer> counterModificationsQueue = new LinkedBlockingQueue();      // Queue of the modifications to apply to the Thread global counter to know when the program is done.
    TextArea guiInterface;  // The area in the GUI in which we tell what kind of thread has been created and for what URL.
    Text loadingText;       // The area in which we tell to the USer if the index is being built or has been.
    int globalThreadsCounter;
    
    // Constructor
    public GUIUpdater(TextArea guiInt, Text loadingT){
        guiInterface = guiInt;
        globalThreadsCounter = 0;
        loadingText = loadingT;
    }
    
    // Function that adds the new thread to the count of active threads. We add to the BlockingQueue a +1, which will be added to the queue and used to modify the "globalThreadCounter" in the run() function.
    public synchronized void newThreadStarting(){
        boolean flag = false;
        while(flag == false){
            flag = counterModificationsQueue.offer(1);
        }
    }
    
    // Function that removes the thread to the count of active threads. We add to the BlockingQueue a -1, which will be added to the queue and used to modify the "globalThreadCounter" in the run() function.
    public synchronized void terminatingThread(){
        boolean flag = false;
        while(flag == false){
            flag = counterModificationsQueue.offer(-1);
        }
    }
    
    // Adds a string that has to be written in the GUI to the LinkedBlockingQueue.
    public void addToOutputQueue(String s){
        try{
            outputQueue.offer(s);
        }catch(Exception e){}
    }
    
    @Override
    public void run() {
        String temp;
        int value;
        Thread.currentThread().setName("GUIUpdaterTHREAD"); // Name given for debugging purposes.
        long startTime = System.nanoTime(); // Taking the time of the computer to calculate the execution time.
        
        while(true){
            // We display in the GUI every item in the queue (while deleting it from the queue).
            while( outputQueue.isEmpty() != true ){
                temp = outputQueue.poll();      // Removing an element from the queue
                // In order to modify the GUI without race conditions we use the following code:
                Platform.runLater(new GUIWriter(guiInterface, temp));
            }
            
            while(counterModificationsQueue.isEmpty() != true){
                value = counterModificationsQueue.poll();
                globalThreadsCounter += value;
            }
            
            // The queue is empty. We wait a little and check again.
            try {
                Thread.sleep(50);   // Waiting 10 ms.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            
            // If the globalCounter is 0 and no more modifications are in queue.
            if(counterModificationsQueue.isEmpty()  &&  globalThreadsCounter == 0){
                break;    // Stops the cycle
            }
        }
        
        long elapsedTime = System.nanoTime() - startTime;   // Reading the time used to execute the query.
        if(elapsedTime >= 0){           // Consistency check
            temp = "                                                    Index built.";
            // In order to modify the GUI without race conditions we use the following code:
            Platform.runLater(new GUILoadingStateUpdater(loadingText, temp));
            temp = "INDEX BUILT. Elapsed time: "+ elapsedTime +" ns  ~=  "+ elapsedTime/1000000 +"ms.";
            Platform.runLater(new GUIWriter(guiInterface, temp));
            System.out.println("INDEX BUILT. Elapsed time: "+ elapsedTime +" ns  ~=  "+ elapsedTime/1000000 +"ms.");    // Statement for Debugging purpose.
        }
        
        // Now the thread is commanded to stop. Before terminating togh it has to display all the remaining strings in the queue.
        while( outputQueue.isEmpty() != true ){
            temp = outputQueue.poll();
            // In order to modify the GUI without race conditions we use the following code:
            Platform.runLater(new GUIWriter(guiInterface, temp));
        }
        
        globalIndexingFlag = false;
        Thread.currentThread().interrupt();
    }
    
}