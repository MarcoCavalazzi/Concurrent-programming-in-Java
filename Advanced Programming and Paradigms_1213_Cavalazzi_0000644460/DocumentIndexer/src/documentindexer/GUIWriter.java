/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import javafx.scene.control.TextArea;

/**
 *
 * @author Mark
 */
public class GUIWriter implements Runnable{
    TextArea textArea;
    String str;
    
    GUIWriter(TextArea textArea, String s){
        this.textArea = textArea;
        this.str = s;
    }

    @Override
    public void run() {
        textArea.appendText(str + "\n\r");     // Appends the String to the GUI interface with the carriage return.
    }
}