/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import static documentindexer.DocumentIndexer.globalIndexingFlag;
import javafx.application.Platform;
import javafx.scene.control.Button;

/**
 *
 * @author Mark
 */
// This class will take care of the automatic updates regarding the index, that will start to take place after the first indexing process, launched from the User.
public class AutomaticIndexUpdater implements Runnable {
    long waitingTime;
    Button btnStart;

    public AutomaticIndexUpdater(Button start, long time) {
        this.btnStart = start;
        if(waitingTime > 0){
            this.waitingTime = time;
        }else{
            this.waitingTime = 60000;   // default value: 1 minute
        }
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        // The Thread waits for the time specified when creating the Thread and then updates the index, but only if the indexing function "buildIndex()" is not already active.

        Thread.currentThread().setName("AutomaticIndexUpdaterTHREAD");
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException ex) {
            // If this thread receives an interrupt() command it has to comply even if it was asleep.
            Thread.currentThread().interrupt();
            return;
        }

        while(true){  // If the function buildIndex() is already active we wait for another 'waitingTime' milliseconds.
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException ex) {
                // If this thread receives an interrupt() command it has to comply (even if it was asleep).
                Thread.currentThread().interrupt();
                return;
            }

            // After waiting the given time we check if the indexing process is already on. If not we force the updating of the index.
            if(globalIndexingFlag == false){
                // Telling the GUI that as soon as possible we have to execute this command on it.
                Platform.runLater(() -> {
                    btnStart.fire();    // Simulating a click on the "Build index" button in the GUI.
                });
            }
        }
    }
}
