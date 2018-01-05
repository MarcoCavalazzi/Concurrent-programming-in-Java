/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import javafx.scene.text.Text;

/**
 *
 * @author Mark
 */
public class GUILoadingStateUpdater implements Runnable {
    Text textArea;
    String str;
    
    GUILoadingStateUpdater(Text textArea, String s){
        this.textArea = textArea;
        this.str = s;
    }

    @Override
    public void run() {
        textArea.setVisible(true);
        textArea.setText(str);     // Appends the String to the GUI.
    }
}
