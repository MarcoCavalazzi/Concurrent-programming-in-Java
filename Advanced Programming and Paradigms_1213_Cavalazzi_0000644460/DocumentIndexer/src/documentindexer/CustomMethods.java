/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 *
 * @author Marco Carlo Cavalazzi
 */
public class CustomMethods {
    
    // Function that takes the directory's URL string and returns an ArrayList of all the folders in it.
    static List<File> findSubfolders(String url){
        List<File> folders = new ArrayList<File>();
        File dir = new File(url);
        File[] listFiles = dir.listFiles();
        if(listFiles != null  &&  listFiles.length > 0){
            for (File file : listFiles) {
                if( file.isDirectory() == true ){
                    folders.add(file);
                }
            }
        }
        
        return folders;
    }
    
    // Function that takes the directory's URL string and returns an ArrayList of all the .txt files in it.
    static List<File> findtxtFiles(String url){
        List<File> textFiles = new ArrayList<File>();
        File dir = new File(url);
        File[] listDirs = dir.listFiles();
        if(listDirs != null  &&  listDirs.length > 0){
            for (File file : listDirs) {
                if( file.getName().endsWith((".txt")) ){
                    textFiles.add(file);
                }
            }
        }
        
        return textFiles;
    }
    
    /* Taken the input string and the reference string this function creates a hyperlink.
     * that will have displayed the input string and will re-direct to the reference address.
     * Output: the resulting Hyperlink.
     */
    static Hyperlink hyperlinkMe(String text, String reference){
        Hyperlink link = new Hyperlink(text);
        link.setBorder(Border.EMPTY);
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    Desktop.getDesktop().open(new File(reference));
                } catch (IOException ex) {}
            }
        });
        link.setTooltip(new Tooltip(reference));
        
        return link;
    }
    
    /* This method creates and launches a pop-up window that signals a problem to the User. */
    static void createPopUpWindow(String s){
        Stage warningStage = new Stage();
        HBox comp = new HBox();
        Text nameField = new Text(s);   // Assigning the input string to a Text field
        nameField.setTextAlignment(TextAlignment.CENTER);   // If the text will have more than one line it will keep displaying the lines centered.
        comp.getChildren().add(nameField);    // Adding the Text field to the HBox
        comp.setAlignment(Pos.CENTER);        // Setting the HBox to display the items at the center
        
        Scene stageScene = new Scene(comp, 410, 80);
        warningStage.setTitle("Warning!");    // Setting the title of the window that will appear at the top left corner.
        warningStage.getIcons().add(new Image("file:icons/warning.png"));  // Defining the icon of the window.
        warningStage.setScene(stageScene);
        warningStage.show();
    }
    
    // This function removes duplicates from an array of Buttons and returns an array of Objects.
    static Object[] removeButtonsDuplicates(Object[] arr) {
        ArrayList<Object> whitelist = new ArrayList<Object>();

        for (Object element : arr) {
            String temp = ((Button)element).getText();
            if ( !whitelist.contains( temp ) ){
                whitelist.add(temp);
            }
        }

        return whitelist.toArray();
    }
    
}
